package edu.ntu.sc2002.application;
import edu.ntu.sc2002.application.contract.TurnPhase;
import edu.ntu.sc2002.application.contract.TurnResolution;
import edu.ntu.sc2002.application.contract.GameSnapshot;
import edu.ntu.sc2002.application.contract.ChefDecision;
import edu.ntu.sc2002.application.contract.ChefTaskType;
import edu.ntu.sc2002.application.contract.WaiterDecision;
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
    private final Waiter waiter;
    private final Chef chef;
    private final Cashier cashier;
    private WaiterDecision acceptedWaiter;
    private ChefDecision pendingChef;
    private int resolutionStep;
    private TurnPhase phase = TurnPhase.START_OF_TURN;
    private int currentTurn;
    private boolean turnStarted;
    private String newArrival;
    private final List<String> events = new ArrayList<>();
    private PaymentRecord paymentRecord;
    private List<String> departures = List.of();

    public TurnController() { this(GameConfig.stageOne(2026)); }
    public TurnController(GameConfig config) { this(config, new Restaurant(config.numTables())); }
    public TurnController(GameConfig config, Restaurant restaurant) {
        this.config = java.util.Objects.requireNonNull(config);
        this.restaurant = java.util.Objects.requireNonNull(restaurant);
        if (restaurant.getTables().size() != config.numTables()) throw new IllegalArgumentException("Table count differs from configuration");
        waiter = restaurant.waiter();
        chef = restaurant.chef();
        cashier = restaurant.cashier();
    }
    public TurnPhase phase() { return phase; }
    public int currentTurn() { return currentTurn; }
    public long randomSeed() { return config.randomSeed(); }
    Restaurant restaurant() { return restaurant; }
    public GameResult result() { return phase == TurnPhase.GAME_OVER ? restaurant.evaluateResult() : GameResult.IN_PROGRESS; }

    /** Starts the turn so the UI can show the previous state before arrival. */
    public TurnResolution beginTurn() {
        if (turnStarted || phase != TurnPhase.START_OF_TURN || currentTurn >= config.maxTurns()) throw new IllegalStateException("Cannot begin another turn");
        currentTurn++;
        events.clear(); departures = List.of(); paymentRecord = null;
        acceptedWaiter = null;
        pendingChef = null;
        resolutionStep = 0;
        waiter.startTurn(currentTurn);
        chef.startTurn(currentTurn);
        cashier.startTurn(currentTurn);
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
            int ordinal = (int) config.arrivalSchedule().stream().filter(t -> t <= currentTurn).count();
            Customer customer = new Customer("C" + ordinal, ordinal, config.randomSeed() + ordinal);
            restaurant.registerArrival(customer);
            newArrival = customer.getId();
            events.add("Arrival: " + customer.getId() + " joins the queue.");
        }
        phase = TurnPhase.MANAGER_CHOICES;
        return response(null);
    }

    public TurnResolution submitWaiterDecision(WaiterDecision decision) {
        requirePhase(TurnPhase.MANAGER_CHOICES);
        String error = waiterError(decision);
        if (error != null) return response(error);
        Customer target = findCustomer(decision.targetCustomerId());
        switch (decision.taskType()) {
            case WAIT -> {
                waiter.waitTurn();
                events.add("Waiter waits.");
            }
            case SEAT -> {
                restaurant.seat(target);
                events.add("Waiter seats " + target.getId() + " at Table " + restaurant.tableFor(target).tableId() + ".");
            }
            case TAKE_ORDER -> {
                Order order = waiter.takeOrder(target, target.chooseItem(restaurant.menu()));
                restaurant.registerOrder(order);
                events.add(target.getId() + " orders " + order.getMenuItem().getName() + ".");
            }
            case SERVE -> {
                if (!waiter.serveOrder(target.order())) throw new IllegalStateException("Accepted order is not servable");
                restaurant.recordServed();
                events.add("Waiter serves " + target.getId() + "'s " + target.order().getMenuItem().getName() + ".");
            }
        }
        acceptedWaiter = decision;
        return response(null);
    }

    /** Compatibility entry point used by clients with separate typed decisions. */
    public TurnResolution submitTurnChoices(WaiterDecision waiterTask, ChefDecision chefTask) {
        requirePhase(TurnPhase.MANAGER_CHOICES);
        if (!waiter.hasActed()) {
            TurnResolution waiterResult = submitWaiterDecision(waiterTask);
            if (!waiterResult.isSuccessful()) return waiterResult;
        } else if (!java.util.Objects.equals(waiterTask, acceptedWaiter)) {
            return response("The Waiter task is already accepted.");
        }
        return submitChefDecision(chefTask);
    }

    public TurnResolution submitChefDecision(ChefDecision decision) {
        requirePhase(TurnPhase.MANAGER_CHOICES);
        String error = chefError(decision);
        if (error != null) return response(error);
        pendingChef = decision;
        runTurn();
        return response(null);
    }

    /** Run until manager input is needed, or resolve choices already supplied by step 2. */
    public void runTurn() {
        if (phase == TurnPhase.START_OF_TURN) {
            if (!turnStarted) beginTurn();
            executeStep1Arrivals();
            return;
        }
        requirePhase(TurnPhase.MANAGER_CHOICES);
        executeStep3KitchenAndServing();
        executeStep4EatingAndPayment();
        executeStep5WaitingAndAbandonment();
        executeStep6CheckResult();
    }

    public void executeStep1Arrivals() { processArrival(); }

    /** Accepted Waiter work is retained if the Chef choice is rejected. */
    public void executeStep2ManagerChoices(TurnChoicesDTO choices) {
        requirePhase(TurnPhase.MANAGER_CHOICES);
        java.util.Objects.requireNonNull(choices);
        var choice = choices.getWaiterChoice();
        Customer target = choice.getTaskType() == WaiterTaskType.SERVE
                ? customerForOrder(choice.getTargetId()) : customerForOrdinal(choice.getTargetId());
        var decision = new WaiterDecision(choice.getTaskType(), target == null ? null : target.getId());
        if (!waiter.hasActed()) {
            TurnResolution response = submitWaiterDecision(decision);
            if (!response.isSuccessful()) throw new IllegalArgumentException(response.errorMessage());
        } else if (!decision.equals(acceptedWaiter)) {
            throw new IllegalArgumentException("The Waiter task is already accepted.");
        }
        var chefChoice = choices.getChefChoice();
        Customer chefTarget = customerForOrder(chefChoice.getTargetOrderId());
        var chefDecision = new ChefDecision(chefChoice.isCooking() ? ChefTaskType.ADVANCE : ChefTaskType.WAIT,
                chefTarget == null ? null : chefTarget.getId());
        String error = chefError(chefDecision);
        if (error != null) throw new IllegalArgumentException(error);
        pendingChef = chefDecision;
    }

    /** Waiter work is accepted immediately so the Chef can prepare an order placed this turn. */
    public void executeStep3KitchenAndServing() {
        requirePhase(TurnPhase.MANAGER_CHOICES);
        String error = chefError(pendingChef);
        if (error != null) throw new IllegalStateException(error);
        Customer target = findCustomer(pendingChef.targetCustomerId());
        if (pendingChef.taskType() == ChefTaskType.ADVANCE) {
            chef.prepareOrder(target.order());
            events.add("Chef advances " + target.getId() + "'s " + target.order().getMenuItem().getName()
                    + " (" + target.order().unitsDone() + "/" + target.order().getMenuItem().getPrepUnitsNeeded() + ").");
        } else {
            chef.waitTurn();
            events.add("Chef waits.");
        }
        phase = TurnPhase.RESOLVING;
        resolutionStep = 3;
    }

    public void executeStep4EatingAndPayment() {
        requireResolutionStep(3);
        for (Customer customer : restaurant.customers()) customer.finishEating(currentTurn);
        Customer paying = restaurant.oldestPaymentCandidate(currentTurn);
        if (paying != null) {
            paymentRecord = new PaymentRecord(paying.getId(), paying.order().lockedPriceCents(), paying.getSatisfaction());
            cashier.processPayment(paying, paying.order(), restaurant);
            events.add(paymentRecord.customerId() + " pays " + money(paymentRecord.lockedPriceCents()) + ".");
        }
        resolutionStep = 4;
    }

    public void executeStep5WaitingAndAbandonment() {
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

    public GameResult executeStep6CheckResult() {
        requireResolutionStep(5);
        turnStarted = false;
        phase = currentTurn == config.maxTurns() ? TurnPhase.GAME_OVER : TurnPhase.START_OF_TURN;
        resolutionStep = 6;
        return phase == TurnPhase.GAME_OVER ? restaurant.evaluateResult() : GameResult.IN_PROGRESS;
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

    private String waiterError(WaiterDecision decision) {
        if (waiter.hasActed()) return "The Waiter has already acted this turn.";
        if (decision == null || decision.taskType() == null) return "Choose a Waiter task.";
        Customer target = findCustomer(decision.targetCustomerId());
        return switch (decision.taskType()) {
            case WAIT -> decision.targetCustomerId() == null ? null : "Wait has no target.";
            case SEAT -> target == null || !restaurant.getWaitingQueue().getQueuedCustomers().contains(target)
                    ? "Choose a customer in the queue."
                    : restaurant.getTables().stream().noneMatch(t -> !t.isOccupied()) ? "No table is free." : null;
            case TAKE_ORDER -> target == null || target.getState() != CustomerState.SEATED ? "Choose a seated customer."
                    : target.order() != null ? "That customer already ordered." : null;
            case SERVE -> target == null || target.order() == null ? "Choose a customer with an order."
                    : !target.order().canServe(currentTurn) ? "Order must have been READY before this turn." : null;
        };
    }

    private String chefError(ChefDecision decision) {
        if (!waiter.hasActed()) return "Choose the Waiter task before the Chef task.";
        if (decision == null || decision.taskType() == null) return "Choose a Chef task.";
        Customer target = findCustomer(decision.targetCustomerId());
        if (decision.taskType() == ChefTaskType.WAIT) return decision.targetCustomerId() == null ? null : "Wait has no target.";
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
                new GameSnapshot.OrderView("order-" + c.getId(), c.getId(), c.order().getMenuItem().getName(), c.order().getStatus(),
                        c.order().unitsDone(), c.order().getMenuItem().getPrepUnitsNeeded(), c.order().lockedPriceCents())).toList();
        List<GameSnapshot.WaiterAction> waiterActions = new ArrayList<>();
        List<ChefDecision> chefActions = new ArrayList<>();
        if (turnStarted && phase == TurnPhase.MANAGER_CHOICES) {
            Integer nextTable = tables.stream().filter(t -> t.occupantId() == null).map(GameSnapshot.TableView::id).findFirst().orElse(null);
            for (WaiterTaskType task : WaiterTaskType.values()) {
                if (task == WaiterTaskType.WAIT) continue;
                for (Customer customer : restaurant.customers()) {
                    var decision = new WaiterDecision(task, customer.getId());
                    if (waiterError(decision) == null) waiterActions.add(new GameSnapshot.WaiterAction(decision,
                            task == WaiterTaskType.SEAT ? nextTable : null));
                }
            }
            var waiterWait = new WaiterDecision(WaiterTaskType.WAIT, null);
            if (waiterError(waiterWait) == null) waiterActions.add(new GameSnapshot.WaiterAction(waiterWait, null));
            for (Customer customer : restaurant.customers()) {
                var decision = new ChefDecision(ChefTaskType.ADVANCE, customer.getId());
                if (chefError(decision) == null) chefActions.add(decision);
            }
            var chefWait = new ChefDecision(ChefTaskType.WAIT, null);
            if (chefError(chefWait) == null) chefActions.add(chefWait);
        }
        double average = restaurant.getAverageSatisfaction();
        return new GameSnapshot(currentTurn, phase, restaurant.revenueCents(), Double.isNaN(average) ? null : average,
                restaurant.servedCount(), restaurant.paidCount(), restaurant.unhappyDepartures(), currentTurn - (turnStarted ? 1 : 0),
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
        return restaurant.customers().stream().filter(c -> c.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }
    private void requirePhase(TurnPhase expected) { if (phase != expected) throw new IllegalStateException("Unexpected turn phase: " + phase); }
    public static String money(int cents) { return String.format(java.util.Locale.ROOT, "$%.2f", cents / 100.0); }
}
