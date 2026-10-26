package edu.ntu.sc2002.control;

import edu.ntu.sc2002.entity.*;
import java.util.ArrayList;
import java.util.List;

/** Runs the Stage 1 six-step game loop. UI never mutates domain state. */
public final class GameController {
    private final GameConfig config;
    private final Restaurant restaurant;
    private final ScoreBoard scoreBoard;
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
        this.config = java.util.Objects.requireNonNull(config);
        this.restaurant = java.util.Objects.requireNonNull(restaurant);
        this.scoreBoard = java.util.Objects.requireNonNull(scoreBoard);
        if (restaurant.tables().size() != config.numTables()) throw new IllegalArgumentException("Table count differs from configuration");
    }
    public TurnPhase phase() { return phase; }
    public int currentTurn() { return currentTurn; }
    public long randomSeed() { return config.randomSeed(); }
    Restaurant restaurant() { return restaurant; }
    ScoreBoard scoreBoard() { return scoreBoard; }
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
            Customer customer = new Customer("C" + ordinal, ordinal, config.randomSeed() + ordinal);
            restaurant.registerArrival(customer);
            newArrival = customer.id();
            events.add("Arrival: " + customer.id() + " joins the queue.");
        }
        phase = TurnPhase.MANAGER_CHOICES;
        return response(null);
    }

    public TurnResolution submitWaiterDecision(WaiterDecision decision) {
        requirePhase(TurnPhase.MANAGER_CHOICES);
        if (waiterTaskUsed) return response("The Waiter has already acted this turn.");
        if (decision == null || decision.taskType() == null) return response("Choose a Waiter task.");
        Customer target = findCustomer(decision.targetCustomerId());
        switch (decision.taskType()) {
            case WAIT -> { if (decision.targetCustomerId() != null) return response("Wait has no target."); events.add("Waiter waits."); }
            case SEAT -> {
                if (target == null || !restaurant.waitingQueue().getMembers().contains(target)) return response("Choose a customer in the queue.");
                if (restaurant.tables().stream().noneMatch(t -> !t.isOccupied())) return response("No table is free.");
                restaurant.waiter().seat(restaurant, target);
                events.add("Waiter seats " + target.id() + " at Table " + restaurant.tableFor(target).tableId() + ".");
            }
            case TAKE_ORDER -> {
                if (target == null || target.state() != CustomerState.SEATED) return response("Choose a seated customer.");
                if (target.order() != null) return response("That customer already ordered.");
                Order order = restaurant.waiter().takeOrder(target, restaurant.menu(), currentTurn);
                events.add(target.id() + " orders " + order.item().name() + ".");
            }
            case SERVE -> {
                if (target == null || target.order() == null) return response("Choose a customer with an order.");
                if (!target.order().canServe(currentTurn)) return response("Order must have been READY before this turn.");
                restaurant.waiter().serve(target, currentTurn);
                scoreBoard.recordServed();
                events.add("Waiter serves " + target.id() + "'s " + target.order().item().name() + ".");
            }
        }
        waiterTaskUsed = true;
        return response(null);
    }

    List<Customer> advanceableOrders() {
        return restaurant.customers().stream().filter(c -> c.order() != null
                && (c.order().status() == OrderStatus.PLACED || c.order().status() == OrderStatus.PREPARING)).toList();
    }

    public TurnResolution submitChefDecision(ChefDecision decision) {
        requirePhase(TurnPhase.MANAGER_CHOICES);
        if (!waiterTaskUsed) return response("Choose the Waiter task before the Chef task.");
        if (decision == null || decision.taskType() == null) return response("Choose a Chef task.");
        Customer target = findCustomer(decision.targetCustomerId());
        if (decision.taskType() == ChefTaskType.ADVANCE) {
            if (target == null || target.order() == null
                    || (target.order().status() != OrderStatus.PLACED && target.order().status() != OrderStatus.PREPARING)) {
                return response("Choose a customer with an order that is not ready.");
            }
            restaurant.chef().advance(target, currentTurn);
            events.add("Chef advances " + target.id() + "'s " + target.order().item().name()
                    + " (" + target.order().unitsDone() + "/" + target.order().item().preparationUnits() + ").");
        } else {
            if (decision.targetCustomerId() != null) return response("Wait has no target.");
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

    public GameSnapshot snapshot() {
        List<String> waiting = restaurant.waitingQueue().getMembers().stream().map(Customer::id).toList();
        List<String> tables = restaurant.tables().stream().map(t -> t.isOccupied()
                ? "Table " + t.tableId() + ": " + t.getOccupant().id() : "Table " + t.tableId() + ": Available").toList();
        List<String> customers = restaurant.customers().stream().map(c -> {
            Table table = restaurant.tableFor(c);
            return c.id() + " " + c.state() + " satisfaction " + c.satisfaction().getValue()
                    + (table == null ? "" : " Table " + table.tableId());
        }).toList();
        List<String> activeOptions = restaurant.customers().stream()
                .filter(c -> c.state() != CustomerState.PAID && c.state() != CustomerState.LEFT)
                .map(c -> c.id() + " (" + c.state() + ")").toList();
        List<String> advanceOptions = advanceableOrders().stream()
                .map(c -> c.id() + " (" + c.order().item().name() + ")").toList();
        List<String> orders = restaurant.customers().stream().filter(c -> c.order() != null).map(c ->
                c.id() + " " + c.order().item().name() + " " + c.order().status()
                        + (c.order().status() == OrderStatus.PREPARING ? " " + c.order().unitsDone()
                        + "/" + c.order().item().preparationUnits() : "")).toList();
        List<String> available = new ArrayList<>();
        boolean choicesOpen = turnStarted && phase != TurnPhase.GAME_OVER;
        if (choicesOpen && !waiterTaskUsed) {
            if (!restaurant.waitingQueue().isEmpty() && restaurant.tables().stream().anyMatch(t -> !t.isOccupied())) {
                available.add("Waiter SEAT: " + restaurant.waitingQueue().getMembers().stream().map(Customer::id).toList());
            }
            restaurant.customers().stream().filter(c -> c.state() == CustomerState.SEATED && c.order() == null)
                    .forEach(c -> available.add("Waiter TAKE_ORDER: " + c.id()));
            restaurant.customers().stream().filter(c -> c.order() != null && c.order().canServe(currentTurn))
                    .forEach(c -> available.add("Waiter SERVE: " + c.id()));
            available.add("Waiter WAIT");
        }
        boolean chefOpen = choicesOpen && phase == TurnPhase.MANAGER_CHOICES && waiterTaskUsed;
        if (choicesOpen && !chefOpen) available.add("Chef: choices follow the Waiter task");
        if (chefOpen) {
            advanceableOrders().forEach(c -> available.add("Chef ADVANCE: " + c.id()));
            available.add("Chef WAIT");
        }
        if (choicesOpen) available.add("Cashier: automatic, oldest first, max one");
        double average = scoreBoard.getAveragePaidSatisfaction();
        return new GameSnapshot(currentTurn, phase, scoreBoard.revenueCents(), Double.isNaN(average) ? null : average,
                scoreBoard.servedCount(), scoreBoard.paidCount(), scoreBoard.unhappyDepartures(), currentTurn - (turnStarted ? 1 : 0),
                waiting, tables, customers, orders, choicesOpen ? activeOptions : List.of(),
                chefOpen ? advanceOptions : List.of(), available);
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
    public static String money(int cents) { return String.format(java.util.Locale.ROOT, "$%.2f", cents / 100.0); }
}
