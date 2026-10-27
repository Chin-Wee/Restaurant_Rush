package edu.ntu.sc2002.application;
import edu.ntu.sc2002.application.contract.TurnPhase;
import edu.ntu.sc2002.application.contract.TurnResolution;
import edu.ntu.sc2002.application.contract.GameSnapshot;
import edu.ntu.sc2002.application.contract.ChefChoiceDTO;
import edu.ntu.sc2002.application.contract.ChefTaskType;
import edu.ntu.sc2002.application.contract.WaiterChoiceDTO;
import edu.ntu.sc2002.application.contract.WaiterTaskType;
import edu.ntu.sc2002.application.contract.TurnChoicesDTO;

import edu.ntu.sc2002.domain.*;
import edu.ntu.sc2002.domain.customer.*;
import edu.ntu.sc2002.domain.staff.*;
import java.util.ArrayList;
import java.util.List;

/** Runs the Stage 1 six-step game loop. UI never mutates domain state. */
public class TurnController {
    private final GameConfig config;
    private final Restaurant restaurant;
    private WaiterChoiceDTO acceptedWaiter;
    private int resolutionStep;
    private TurnPhase phase = TurnPhase.START_OF_TURN;
    private int currentTurn = 1;
    private boolean turnStarted;
    private String newArrival;
    private final List<String> events = new ArrayList<>();
    private PaymentRecord paymentRecord;
    private List<String> departures = List.of();

    public TurnController() { this(GameConfig.stageOne(2026)); }
    public TurnController(GameConfig config) { this(config, new Restaurant()); }
    public TurnController(GameConfig config, Restaurant restaurant) {
        this.config = java.util.Objects.requireNonNull(config);
        this.restaurant = java.util.Objects.requireNonNull(restaurant);
        if (restaurant.getTables().size() != config.numTables()) throw new IllegalArgumentException("Table count differs from configuration");
    }
    public TurnPhase phase() { return phase; }
    public int currentTurn() { return currentTurn; }
    public long randomSeed() { return config.randomSeed(); }
    public GameResult result() { return phase == TurnPhase.GAME_OVER ? restaurant.evaluateResult() : GameResult.IN_PROGRESS; }

    /** Starts the turn so the UI can show the previous state before arrival. */
    public TurnResolution beginTurn() {
        if (turnStarted || phase != TurnPhase.START_OF_TURN) throw new IllegalStateException("Cannot begin another turn");
        if (resolutionStep == 6) currentTurn++;
        events.clear(); departures = List.of(); paymentRecord = null;
        acceptedWaiter = null;
        resolutionStep = 0;
        restaurant.getStaffList().forEach(staff -> staff.startTurn(currentTurn));
        restaurant.getActiveOrders().forEach(o -> o.startTurn(currentTurn));
        newArrival = null;
        turnStarted = true;
        return response(null);
    }

    /** Step 1: registers the scheduled arrival after the start display. */
    public TurnResolution processArrival() {
        requirePhase(TurnPhase.START_OF_TURN);
        if (!turnStarted) throw new IllegalStateException("Begin the turn before processing arrivals");
        if (config.arrivalSchedule().contains(currentTurn)) {
            int ordinal = restaurant.customers().size() + 1;
            Customer customer = new Customer("C" + ordinal, ordinal, config.randomSeed() + ordinal);
            restaurant.registerArrival(customer);
            newArrival = customer.getId();
            events.add("Arrival: " + customer.getId() + " joins the queue.");
        }
        phase = TurnPhase.MANAGER_CHOICES;
        return response(null);
    }

    public TurnResolution submitWaiterDecision(WaiterChoiceDTO decision) {
        requirePhase(TurnPhase.MANAGER_CHOICES);
        String error = waiterError(decision);
        if (error != null) return response(error);
        Customer target = customerForOrdinal(decision.targetId());
        switch (decision.taskType()) {
            case WAIT -> {
                restaurant.getStaff(Waiter.class).waitTurn();
                events.add("Waiter waits.");
            }
            case SEAT -> {
                restaurant.seat(target);
                events.add("Waiter seats " + target.getId() + " at Table " + restaurant.tableFor(target).tableId() + ".");
            }
            case TAKE_ORDER -> {
                Order order = restaurant.getStaff(Waiter.class).takeOrder(target, target.chooseMenuItem(restaurant.menu().getItems()));
                restaurant.registerOrder(order);
                events.add(target.getId() + " orders " + order.getMenuItem().getName() + ".");
            }
            case SERVE -> { /* Service is performed in step 3, before the Chef. */ }
        }
        acceptedWaiter = decision;
        return response(null);
    }

    public TurnResolution submitChefDecision(ChefChoiceDTO decision) {
        requirePhase(TurnPhase.MANAGER_CHOICES);
        String error = chefError(decision);
        if (error != null) return response(error);
        executeStep3KitchenAndServing(new TurnChoicesDTO(acceptedWaiter, decision));
        executeStep4EatingAndPayment();
        executeStep5WaitingAndAbandonment();
        executeStep6CheckResult();
        return response(null);
    }

    /** New orders are exposed during selection; service resolves before the Chef's preparation. */
    private void executeStep3KitchenAndServing(TurnChoicesDTO choices) {
        var waiter = choices.getWaiterChoice();
        var chef = choices.getChefChoice();
        if (waiter.taskType() == WaiterTaskType.SERVE) {
            Customer served = customerForOrdinal(waiter.targetId());
            if (!restaurant.getStaff(Waiter.class).serveOrder(served.order())) throw new IllegalStateException("Accepted order is not servable");
            restaurant.recordServed();
            events.add("Waiter serves " + served.getId() + "'s " + served.order().getMenuItem().getName() + ".");
        }
        Customer target = customerForOrder(chef.targetOrderId());
        if (chef.taskType() == ChefTaskType.COOK) {
            restaurant.getStaff(Chef.class).prepareOrder(target.order());
            events.add("Chef advances " + target.getId() + "'s " + target.order().getMenuItem().getName()
                    + " (" + target.order().unitsDone() + "/" + target.order().getMenuItem().getPrepUnitsNeeded() + ").");
        } else {
            restaurant.getStaff(Chef.class).waitTurn();
            events.add("Chef waits.");
        }
        phase = TurnPhase.RESOLVING;
        resolutionStep = 3;
    }

    private void executeStep4EatingAndPayment() {
        requireResolutionStep(3);
        for (Customer customer : restaurant.customers()) customer.finishEating(currentTurn);
        Customer paying = restaurant.oldestPaymentCandidate(currentTurn);
        if (paying != null) {
            paymentRecord = new PaymentRecord(paying.getId(),
                    paying.calculateBill(paying.order().getMenuItem().getPriceCents()), paying.getSatisfaction());
            restaurant.getStaff(Cashier.class).processPayment(paying, paying.order(), restaurant);
            events.add(paymentRecord.customerId() + " pays " + money(paymentRecord.lockedPriceCents()) + ".");
        }
        resolutionStep = 4;
    }

    private void executeStep5WaitingAndAbandonment() {
        requireResolutionStep(4);
        List<String> left = new ArrayList<>();
        for (Customer customer : restaurant.customers()) {
            if (customer.isWaitingForTableOrFood()) {
                customer.applyTurnEndDecay();
                if (customer.getSatisfaction() == 0) {
                    restaurant.abandon(customer);
                    left.add(customer.getId()); events.add(customer.getId() + " leaves unhappy.");
                }
            }
        }
        departures = List.copyOf(left);
        resolutionStep = 5;
    }

    private void executeStep6CheckResult() {
        requireResolutionStep(5);
        turnStarted = false;
        phase = currentTurn == config.maxTurns() ? TurnPhase.GAME_OVER : TurnPhase.START_OF_TURN;
        resolutionStep = 6;
    }

    private void requireResolutionStep(int expected) {
        requirePhase(TurnPhase.RESOLVING);
        if (resolutionStep != expected) throw new IllegalStateException("Turn step out of sequence");
    }
    private Customer customerForOrdinal(int ordinal) {
        return restaurant.customers().stream().filter(c -> c.arrivalOrdinal() == ordinal).findFirst().orElse(null);
    }
    private Customer customerForOrder(int orderId) {
        return restaurant.getActiveOrders().stream().filter(o -> o.getOrderId() == orderId)
                .map(Order::getCustomer).findFirst().orElse(null);
    }

    private String waiterError(WaiterChoiceDTO decision) {
        if (acceptedWaiter != null || restaurant.getStaff(Waiter.class).hasActed()) return "The Waiter has already acted this turn.";
        if (decision == null || decision.taskType() == null) return "Choose a Waiter task.";
        Customer target = customerForOrdinal(decision.targetId());
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
        if (acceptedWaiter == null) return "Choose the Waiter task before the Chef task.";
        if (decision == null || decision.taskType() == null) return "Choose a Chef task.";
        Customer target = customerForOrder(decision.targetOrderId());
        if (decision.taskType() == ChefTaskType.WAIT) return null;
        return target == null || target.order() == null
                || (target.order().getStatus() != OrderStatus.PENDING && target.order().getStatus() != OrderStatus.IN_PREPARATION)
                ? "Choose a customer with an order that is not ready." : null;
    }

    public GameSnapshot snapshot() {
        var waiting = restaurant.getWaitingQueue().getQueuedCustomers().stream().map(Customer::getId).toList();
        var tables = restaurant.getTables().stream().map(t -> new GameSnapshot.TableView(t.tableId(),
                t.isOccupied() ? t.getSeatedCustomer().getId() : null)).toList();
        var customers = restaurant.customers().stream().map(c -> {
            Table table = restaurant.tableFor(c);
            int queueIndex = waiting.indexOf(c.getId());
            return new GameSnapshot.CustomerView(c.getId(), c.arrivalOrdinal(), c.getState(), c.getSatisfaction(),
                    table == null ? null : table.tableId(), queueIndex < 0 ? null : queueIndex + 1);
        }).toList();
        var orders = restaurant.customers().stream().filter(c -> c.order() != null).map(c ->
                new GameSnapshot.OrderView(c.order().getOrderId(), c.getId(), c.order().getMenuItem().getName(), c.order().getStatus(),
                        c.order().unitsDone(), c.order().getMenuItem().getPrepUnitsNeeded(),
                        c.calculateBill(c.order().getMenuItem().getPriceCents()))).toList();
        List<GameSnapshot.WaiterAction> waiterActions = new ArrayList<>();
        List<ChefChoiceDTO> chefActions = new ArrayList<>();
        if (turnStarted && phase == TurnPhase.MANAGER_CHOICES) {
            Integer nextTable = tables.stream().filter(t -> t.occupantId() == null).map(GameSnapshot.TableView::id).findFirst().orElse(null);
            for (WaiterTaskType task : WaiterTaskType.values()) {
                if (task == WaiterTaskType.WAIT) continue;
                for (Customer customer : restaurant.customers()) {
                    if (task == WaiterTaskType.SERVE && customer.order() == null) continue;
                    var decision = new WaiterChoiceDTO(task, customer.arrivalOrdinal());
                    if (waiterError(decision) == null) waiterActions.add(new GameSnapshot.WaiterAction(decision,
                            task == WaiterTaskType.SEAT ? nextTable : null, customer.getId()));
                }
            }
            var waiterWait = new WaiterChoiceDTO(WaiterTaskType.WAIT, 0);
            if (waiterError(waiterWait) == null) waiterActions.add(new GameSnapshot.WaiterAction(waiterWait, null, null));
            for (Order order : restaurant.getActiveOrders()) {
                var decision = new ChefChoiceDTO(ChefTaskType.COOK, order.getOrderId());
                if (chefError(decision) == null) chefActions.add(decision);
            }
            var chefWait = new ChefChoiceDTO(ChefTaskType.WAIT, 0);
            if (chefError(chefWait) == null) chefActions.add(chefWait);
        }
        double average = restaurant.getAverageSatisfaction();
        return new GameSnapshot(currentTurn, phase, restaurant.revenueCents(), Double.isNaN(average) ? null : average,
                restaurant.servedCount(), restaurant.paidCount(), restaurant.unhappyDepartures(), currentTurn - (turnStarted || resolutionStep == 0 ? 1 : 0),
                waiting, tables, customers, orders, waiterActions, chefActions);
    }

    public TurnResolution lastResolution() {
        if (turnStarted || resolutionStep != 6) throw new IllegalStateException("Turn not resolved");
        return response(null);
    }

    private TurnResolution response(String error) {
        return new TurnResolution(currentTurn, events, newArrival, departures, paymentRecord,
                error == null, error, result());
    }

    private void requirePhase(TurnPhase expected) { if (phase != expected) throw new IllegalStateException("Unexpected turn phase: " + phase); }
    public static String money(int cents) { return String.format(java.util.Locale.ROOT, "$%.2f", cents / 100.0); }
}
