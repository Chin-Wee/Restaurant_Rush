package edu.ntu.sc2002.application;
import edu.ntu.sc2002.application.contract.TurnPhase;
import edu.ntu.sc2002.application.contract.TurnResolution;
import edu.ntu.sc2002.application.contract.GameSnapshot;
import edu.ntu.sc2002.application.contract.ChefDecision;
import edu.ntu.sc2002.application.contract.ChefTaskType;
import edu.ntu.sc2002.application.contract.WaiterDecision;
import edu.ntu.sc2002.application.contract.WaiterTaskType;

import edu.ntu.sc2002.domain.*;
import edu.ntu.sc2002.domain.customer.*;
import edu.ntu.sc2002.domain.menu.*;
import edu.ntu.sc2002.domain.staff.*;
import java.util.ArrayList;
import java.util.List;

/** Runs the Stage 1 six-step game loop. UI never mutates domain state. */
public final class GameController {
    private final GameConfig config;
    private final Restaurant restaurant;
    private final ScoreBoard scoreBoard;
    private final CustomerFactory customerFactory;
    private final BillCalculator billCalculator = new BillCalculator();
    private TurnPhase phase = TurnPhase.START_OF_TURN;
    private int currentTurn;
    private boolean waiterTaskUsed;
    private boolean turnStarted;
    private String newArrival;
    private final List<String> events = new ArrayList<>();
    private PaymentRecord paymentRecord;
    private List<String> departures = List.of();

    public GameController() { this(GameConfig.stageOne(2026)); }
    public GameController(GameConfig config) { this(config, new Restaurant(config.numTables()), new ScoreBoard()); }
    public GameController(GameConfig config, Restaurant restaurant, ScoreBoard scoreBoard) {
        this(config, restaurant, scoreBoard, new CustomerFactory());
    }
    public GameController(GameConfig config, Restaurant restaurant, ScoreBoard scoreBoard, CustomerFactory customerFactory) {
        this.config = java.util.Objects.requireNonNull(config);
        this.restaurant = java.util.Objects.requireNonNull(restaurant);
        this.scoreBoard = java.util.Objects.requireNonNull(scoreBoard);
        this.customerFactory = java.util.Objects.requireNonNull(customerFactory);
        if (restaurant.tables().size() != config.numTables()) throw new IllegalArgumentException("Table count differs from configuration");
    }
    public TurnPhase phase() { return phase; }
    public int currentTurn() { return currentTurn; }
    public long randomSeed() { return config.randomSeed(); }
    public GameResult result() { return phase == TurnPhase.GAME_OVER ? scoreBoard.evaluateResult() : null; }

    /** Starts the turn so the UI can show the previous state before arrival. */
    public TurnResolution beginTurn() {
        if (turnStarted || phase != TurnPhase.START_OF_TURN || currentTurn >= config.maxTurns()) throw new IllegalStateException("Cannot begin another turn");
        currentTurn++;
        events.clear(); departures = List.of(); paymentRecord = null;
        waiterTaskUsed = false;
        newArrival = null;
        turnStarted = true;
        return response(null);
    }

    /** Step 1: registers the scheduled arrival after the start display. */
    public TurnResolution processArrival() {
        requirePhase(TurnPhase.START_OF_TURN);
        if (!turnStarted) throw new IllegalStateException("Begin the turn before processing arrivals");
        if (config.arrivalSchedule().contains(currentTurn)) {
            int ordinal = (int) config.arrivalSchedule().stream().filter(t -> t <= currentTurn).count();
            Customer customer = customerFactory.createCustomer("C" + ordinal, ordinal, config.randomSeed() + ordinal);
            restaurant.registerArrival(customer);
            newArrival = customer.id();
            events.add("Arrival: " + customer.id() + " joins the queue.");
        }
        phase = TurnPhase.MANAGER_CHOICES;
        return response(null);
    }

    /**
     * Submits both manager choices for the turn. The Waiter task is applied first because the Chef
     * may advance the order it just took; if the Chef choice is then rejected, the Waiter task stays
     * applied and the Chef choice can be resubmitted through {@link #submitChefDecision}.
     */
    public TurnResolution submitTurnChoices(WaiterDecision waiterTask, ChefDecision chefTask) {
        TurnResolution waiterResult = submitWaiterDecision(waiterTask);
        return waiterResult.isSuccessful() ? submitChefDecision(chefTask) : waiterResult;
    }

    public TurnResolution submitWaiterDecision(WaiterDecision decision) {
        requirePhase(TurnPhase.MANAGER_CHOICES);
        String error = waiterError(decision);
        if (error != null) return response(error);
        Customer target = findCustomer(decision.targetCustomerId());
        switch (decision.taskType()) {
            case WAIT -> events.add("Waiter waits.");
            case SEAT -> {
                restaurant.waiter().seat(restaurant, target);
                events.add("Waiter seats " + target.id() + " at Table " + restaurant.tableFor(target).tableId() + ".");
            }
            case TAKE_ORDER -> {
                Order order = restaurant.waiter().takeOrder(target, restaurant.menu(), billCalculator, currentTurn);
                events.add(target.id() + " orders " + order.item().name() + ".");
            }
            case SERVE -> {
                restaurant.waiter().serve(target, currentTurn);
                scoreBoard.recordServed();
                events.add("Waiter serves " + target.id() + "'s " + target.order().item().name() + ".");
            }
        }
        waiterTaskUsed = true;
        return response(null);
    }

    public TurnResolution submitChefDecision(ChefDecision decision) {
        requirePhase(TurnPhase.MANAGER_CHOICES);
        String error = chefError(decision);
        if (error != null) return response(error);
        Customer target = findCustomer(decision.targetCustomerId());
        if (decision.taskType() == ChefTaskType.ADVANCE) {
            restaurant.chef().advance(target, currentTurn);
            events.add("Chef advances " + target.id() + "'s " + target.order().item().name()
                    + " (" + target.order().unitsDone() + "/" + target.order().item().preparationUnits() + ").");
        } else {
            events.add("Chef waits.");
        }
        resolveRemainder();
        return response(null);
    }

    private void resolveRemainder() {
        phase = TurnPhase.RESOLVING;
        for (Customer customer : restaurant.customers()) customer.finishEating(currentTurn);
        paymentRecord = restaurant.cashier().collectOldest(restaurant, currentTurn);
        if (paymentRecord != null) {
            scoreBoard.recordPayment(paymentRecord);
            events.add(paymentRecord.customerId() + " pays " + money(paymentRecord.lockedPriceCents()) + ".");
        }
        List<String> left = new ArrayList<>();
        for (Customer customer : restaurant.customers()) {
            if (customer.isWaitingForTableOrFood()) {
                customer.applyWaitingLoss();
                if (customer.satisfaction().getValue() == 0) {
                    restaurant.abandon(customer); scoreBoard.recordDeparture();
                    left.add(customer.id()); events.add(customer.id() + " leaves unhappy.");
                }
            }
        }
        departures = List.copyOf(left);
        turnStarted = false;
        phase = currentTurn == config.maxTurns() ? TurnPhase.GAME_OVER : TurnPhase.START_OF_TURN;
    }

    private String waiterError(WaiterDecision decision) {
        if (waiterTaskUsed) return "The Waiter has already acted this turn.";
        if (decision == null || decision.taskType() == null) return "Choose a Waiter task.";
        Customer target = findCustomer(decision.targetCustomerId());
        return switch (decision.taskType()) {
            case WAIT -> decision.targetCustomerId() == null ? null : "Wait has no target.";
            case SEAT -> target == null || !restaurant.waitingQueue().getMembers().contains(target)
                    ? "Choose a customer in the queue."
                    : restaurant.tables().stream().noneMatch(t -> !t.isOccupied()) ? "No table is free." : null;
            case TAKE_ORDER -> target == null || target.state() != CustomerState.SEATED ? "Choose a seated customer."
                    : target.order() != null ? "That customer already ordered." : null;
            case SERVE -> target == null || target.order() == null ? "Choose a customer with an order."
                    : !target.order().canServe(currentTurn) ? "Order must have been READY before this turn." : null;
        };
    }

    private String chefError(ChefDecision decision) {
        if (!waiterTaskUsed) return "Choose the Waiter task before the Chef task.";
        if (decision == null || decision.taskType() == null) return "Choose a Chef task.";
        Customer target = findCustomer(decision.targetCustomerId());
        if (decision.taskType() == ChefTaskType.WAIT) return decision.targetCustomerId() == null ? null : "Wait has no target.";
        return target == null || target.order() == null
                || (target.order().status() != OrderStatus.PLACED && target.order().status() != OrderStatus.PREPARING)
                ? "Choose a customer with an order that is not ready." : null;
    }

    public GameSnapshot snapshot() {
        var waiting = restaurant.waitingQueue().getMembers().stream().map(Customer::id).toList();
        var tables = restaurant.tables().stream().map(t -> new GameSnapshot.TableView(t.tableId(),
                t.isOccupied() ? t.getOccupant().id() : null)).toList();
        var customers = restaurant.customers().stream().map(c -> {
            Table table = restaurant.tableFor(c);
            int queueIndex = waiting.indexOf(c.id());
            return new GameSnapshot.CustomerView(c.id(), c.arrivalOrdinal(), c.state(), c.satisfaction().getValue(),
                    table == null ? null : table.tableId(), queueIndex < 0 ? null : queueIndex + 1);
        }).toList();
        var orders = restaurant.customers().stream().filter(c -> c.order() != null).map(c ->
                new GameSnapshot.OrderView("order-" + c.id(), c.id(), c.order().item().name(), c.order().status(),
                        c.order().unitsDone(), c.order().item().preparationUnits(), c.order().lockedPriceCents())).toList();
        List<GameSnapshot.WaiterAction> waiterActions = new ArrayList<>();
        List<ChefDecision> chefActions = new ArrayList<>();
        if (turnStarted && phase == TurnPhase.MANAGER_CHOICES) {
            Integer nextTable = tables.stream().filter(t -> t.occupantId() == null).map(GameSnapshot.TableView::id).findFirst().orElse(null);
            for (WaiterTaskType task : WaiterTaskType.values()) {
                if (task == WaiterTaskType.WAIT) continue;
                for (Customer customer : restaurant.customers()) {
                    var decision = new WaiterDecision(task, customer.id());
                    if (waiterError(decision) == null) waiterActions.add(new GameSnapshot.WaiterAction(decision,
                            task == WaiterTaskType.SEAT ? nextTable : null));
                }
            }
            var waiterWait = new WaiterDecision(WaiterTaskType.WAIT, null);
            if (waiterError(waiterWait) == null) waiterActions.add(new GameSnapshot.WaiterAction(waiterWait, null));
            for (Customer customer : restaurant.customers()) {
                var decision = new ChefDecision(ChefTaskType.ADVANCE, customer.id());
                if (chefError(decision) == null) chefActions.add(decision);
            }
            var chefWait = new ChefDecision(ChefTaskType.WAIT, null);
            if (chefError(chefWait) == null) chefActions.add(chefWait);
        }
        double average = scoreBoard.getAveragePaidSatisfaction();
        return new GameSnapshot(currentTurn, phase, scoreBoard.revenueCents(), Double.isNaN(average) ? null : average,
                scoreBoard.servedCount(), scoreBoard.paidCount(), scoreBoard.unhappyDepartures(), currentTurn - (turnStarted ? 1 : 0),
                waiting, tables, customers, orders, waiterActions, chefActions);
    }

    public TurnResolution lastResolution() {
        if (turnStarted || currentTurn == 0) throw new IllegalStateException("Turn not resolved");
        return response(null);
    }

    private TurnResolution response(String error) {
        return new TurnResolution(currentTurn, events, newArrival, departures, paymentRecord,
                error == null, error, result());
    }

    private Customer findCustomer(String id) {
        if (id == null) return null;
        return restaurant.customers().stream().filter(c -> c.id().equalsIgnoreCase(id)).findFirst().orElse(null);
    }
    private void requirePhase(TurnPhase expected) { if (phase != expected) throw new IllegalStateException("Unexpected turn phase: " + phase); }
    private static String money(int cents) { return String.format(java.util.Locale.ROOT, "$%.2f", cents / 100.0); }
}
