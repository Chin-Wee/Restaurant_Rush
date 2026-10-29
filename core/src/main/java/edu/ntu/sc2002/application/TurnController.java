package edu.ntu.sc2002.application;

import edu.ntu.sc2002.application.contract.*;
import edu.ntu.sc2002.domain.*;
import edu.ntu.sc2002.domain.customer.*;
import edu.ntu.sc2002.domain.staff.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Runs the Stage 1 six-step game loop. UI never mutates domain state. */
public class TurnController {
    private final GameConfig config;
    private final Restaurant restaurant;
    private final ScoreBoard scoreBoard = new ScoreBoard();
    private TurnPhase phase = TurnPhase.START_OF_TURN;
    private int currentTurn = 1;
    private boolean waiterTaskUsed;
    private boolean turnStarted;
    private int completedTurns;
    private String newArrival;
    private final List<String> events = new ArrayList<>();
    private PaymentRecord paymentRecord;
    private List<String> departures = List.of();

    public TurnController() { this(GameConfig.stageOne(2026)); }
    public TurnController(GameConfig config) {
        this.config = Objects.requireNonNull(config);
        restaurant = new Restaurant(config.numTables());
    }
    public TurnPhase phase() { return phase; }
    public int currentTurn() { return currentTurn; }
    public long randomSeed() { return config.randomSeed(); }
    /** During play this is a provisional threshold evaluation, never a termination signal. */
    public GameResult result() { return scoreBoard.evaluateResult(); }

    /** Starts the turn so the UI can show the previous state before arrival. */
    public TurnResolution beginTurn() {
        if (turnStarted || phase != TurnPhase.START_OF_TURN) throw new IllegalStateException("Cannot begin another turn");
        currentTurn = completedTurns + 1;
        events.clear(); departures = List.of(); paymentRecord = null; newArrival = null;
        waiterTaskUsed = false;
        turnStarted = true;
        return response(null);
    }

    /** Step 1: registers the scheduled arrival after the start display. */
    public TurnResolution processArrival() {
        if (!turnStarted || phase != TurnPhase.START_OF_TURN) throw new IllegalStateException("Begin the turn before processing arrivals");
        if (config.arrivalSchedule().contains(currentTurn)) {
            int ordinal = restaurant.getCustomers().size() + 1;
            Customer customer = new Customer("C" + ordinal, ordinal, config.randomSeed() + ordinal);
            restaurant.registerArrival(customer);
            newArrival = customer.getId();
            events.add("Arrival: " + customer.getId() + " joins the queue.");
        }
        phase = TurnPhase.MANAGER_CHOICES;
        return response(null);
    }

    /** The Waiter acts first so newly placed orders are available to the Chef immediately. */
    public TurnResolution submitWaiterDecision(WaiterChoiceDTO decision) {
        String error = waiterError(decision);
        if (error != null) return response(error);
        Customer target = customerForId(decision.targetCustomerId());
        switch (decision.taskType()) {
            case WAIT -> events.add("Waiter waits.");
            case SEAT -> {
                Table table = restaurant.seat(target);
                events.add("Waiter seats " + target.getId() + " at Table " + table.tableId() + ".");
            }
            case TAKE_ORDER -> {
                Order order = restaurant.getStaff(Waiter.class).takeOrder(target, restaurant.getMenu());
                events.add(target.getId() + " orders " + order.getMenuItem().getName() + ".");
            }
            case SERVE -> {
                restaurant.getStaff(Waiter.class).serve(target, currentTurn);
                scoreBoard.recordServed();
                events.add("Waiter serves " + target.getId() + "'s " + target.order().getMenuItem().getName() + ".");
            }
        }
        waiterTaskUsed = true;
        return response(null);
    }

    public TurnResolution submitChefDecision(ChefChoiceDTO decision) {
        String error = chefError(decision);
        if (error != null) return response(error);
        phase = TurnPhase.RESOLVING;
        if (decision.taskType() == ChefTaskType.ADVANCE) {
            Customer target = customerForId(decision.targetCustomerId());
            restaurant.getStaff(Chef.class).prepareOrder(target.order(), currentTurn);
            events.add("Chef advances " + target.getId() + "'s " + target.order().getMenuItem().getName()
                    + " (" + target.order().unitsDone() + "/" + target.order().getMenuItem().getPrepUnitsNeeded() + ").");
        } else events.add("Chef waits.");
        executeEatingAndPayment();
        executeWaitingAndAbandonment();
        completedTurns++;
        turnStarted = false;
        phase = currentTurn == config.maxTurns() ? TurnPhase.GAME_OVER : TurnPhase.START_OF_TURN;
        return response(null);
    }

    private void executeEatingAndPayment() {
        for (Customer customer : restaurant.getCustomers()) customer.finishEating(currentTurn);
        Customer paying = restaurant.oldestPaymentCandidate(currentTurn);
        if (paying != null) {
            paymentRecord = restaurant.getStaff(Cashier.class).processPayment(paying, restaurant, currentTurn);
            scoreBoard.recordPayment(paymentRecord);
            events.add(paymentRecord.customerId() + " pays " + money(paymentRecord.lockedPriceCents()) + ".");
        }
    }

    private void executeWaitingAndAbandonment() {
        List<String> left = new ArrayList<>();
        for (Customer customer : restaurant.getCustomers()) {
            if (customer.isWaitingForTableOrFood()) {
                customer.applyWaitingLoss();
                if (customer.getSatisfaction() == 0) {
                    restaurant.abandon(customer);
                    scoreBoard.recordDeparture();
                    left.add(customer.getId()); events.add(customer.getId() + " leaves unhappy.");
                }
            }
        }
        departures = List.copyOf(left);
    }

    private Customer customerForId(String id) {
        return restaurant.getCustomers().stream().filter(c -> c.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }
    private String waiterError(WaiterChoiceDTO decision) {
        if (phase != TurnPhase.MANAGER_CHOICES) return "Waiter choices are unavailable in phase " + phase + ".";
        if (waiterTaskUsed) return "The Waiter has already acted this turn.";
        if (decision == null) return "Choose a Waiter task.";
        Customer target = customerForId(decision.targetCustomerId());
        return switch (decision.taskType()) {
            case WAIT -> null;
            case SEAT -> target == null || !restaurant.getWaitingQueue().getQueuedCustomers().contains(target)
                    ? "Choose a customer in the queue."
                    : restaurant.getTables().stream().noneMatch(t -> !t.isOccupied()) ? "No table is free." : null;
            case TAKE_ORDER -> target == null || target.getState() != CustomerState.SEATED ? "Choose a seated customer."
                    : target.order() != null ? "That customer already ordered." : null;
            case SERVE -> target == null || target.order() == null ? "Choose a customer with an order."
                    : !target.order().canServe(currentTurn) ? "Order must have been READY before this turn." : null;
        };
    }
    private String chefError(ChefChoiceDTO decision) {
        if (phase != TurnPhase.MANAGER_CHOICES) return "Chef choices are unavailable in phase " + phase + ".";
        if (!waiterTaskUsed) return "Choose the Waiter task before the Chef task.";
        if (decision == null) return "Choose a Chef task.";
        if (decision.taskType() == ChefTaskType.WAIT) return null;
        Customer target = customerForId(decision.targetCustomerId());
        return target == null || target.order() == null
                || (target.order().getStatus() != OrderStatus.PLACED && target.order().getStatus() != OrderStatus.PREPARING)
                ? "Choose a customer with an order that is not ready." : null;
    }

    public GameSnapshot snapshot() {
        var waiting = restaurant.getWaitingQueue().getQueuedCustomers().stream().map(Customer::getId).toList();
        var tables = restaurant.getTables().stream().map(t -> new GameSnapshot.TableView(t.tableId(),
                t.isOccupied() ? t.getSeatedCustomer().getId() : null)).toList();
        var customers = restaurant.getCustomers().stream().map(c -> {
            Table table = restaurant.tableFor(c);
            int queueIndex = waiting.indexOf(c.getId());
            return new GameSnapshot.CustomerView(c.getId(), c.arrivalOrdinal(), c.getState(), c.getSatisfaction(),
                    table == null ? null : table.tableId(), queueIndex < 0 ? null : queueIndex + 1);
        }).toList();
        var orders = restaurant.getCustomers().stream().filter(c -> c.order() != null).map(c ->
                new GameSnapshot.OrderView(c.getId(), c.order().getMenuItem().getName(), c.order().getStatus(),
                        c.order().unitsDone(), c.order().getMenuItem().getPrepUnitsNeeded(), c.order().lockedPriceCents())).toList();
        List<GameSnapshot.WaiterAction> waiterActions = new ArrayList<>();
        List<ChefChoiceDTO> chefActions = new ArrayList<>();
        if (turnStarted && phase == TurnPhase.MANAGER_CHOICES) {
            Integer nextTable = tables.stream().filter(t -> t.occupantId() == null).map(GameSnapshot.TableView::id).findFirst().orElse(null);
            for (WaiterTaskType task : WaiterTaskType.values()) {
                if (task == WaiterTaskType.WAIT) continue;
                for (Customer customer : restaurant.getCustomers()) {
                    var decision = new WaiterChoiceDTO(task, customer.getId());
                    if (waiterError(decision) == null) waiterActions.add(new GameSnapshot.WaiterAction(decision,
                            task == WaiterTaskType.SEAT ? nextTable : null, customer.getId()));
                }
            }
            var waiterWait = new WaiterChoiceDTO(WaiterTaskType.WAIT, null);
            if (waiterError(waiterWait) == null) waiterActions.add(new GameSnapshot.WaiterAction(waiterWait, null, null));
            for (Customer customer : restaurant.getCustomers()) {
                var decision = new ChefChoiceDTO(ChefTaskType.ADVANCE, customer.getId());
                if (chefError(decision) == null) chefActions.add(decision);
            }
            var chefWait = new ChefChoiceDTO(ChefTaskType.WAIT, null);
            if (chefError(chefWait) == null) chefActions.add(chefWait);
        }
        double average = scoreBoard.averagePaidSatisfaction();
        return new GameSnapshot(currentTurn, phase, scoreBoard.revenueCents(), Double.isNaN(average) ? null : average,
                scoreBoard.servedCount(), scoreBoard.paidCount(), scoreBoard.unhappyDepartures(), completedTurns,
                waiting, tables, customers, orders, waiterActions, chefActions);
    }
    public TurnResolution lastResolution() {
        if (turnStarted || completedTurns == 0) throw new IllegalStateException("Turn not resolved");
        return response(null);
    }
    private TurnResolution response(String error) {
        return new TurnResolution(currentTurn, events, newArrival, departures, paymentRecord,
                error == null, error, result());
    }
    public static String money(int cents) { return String.format(java.util.Locale.ROOT, "$%.2f", cents / 100.0); }
}
