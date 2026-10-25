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
    private final List<String> events = new ArrayList<>();
    private PaymentRecord paymentRecord;
    private List<String> departures = List.of();

    public GameController() { this(GameConfig.stageOne(2026), new Restaurant(), new ScoreBoard()); }
    public GameController(GameConfig config, Restaurant restaurant, ScoreBoard scoreBoard) {
        this.config = config; this.restaurant = restaurant; this.scoreBoard = scoreBoard;
    }
    public TurnPhase phase() { return phase; }
    public int currentTurn() { return currentTurn; }
    Restaurant restaurant() { return restaurant; }
    ScoreBoard scoreBoard() { return scoreBoard; }
    public GameResult result() { return phase == TurnPhase.GAME_OVER ? scoreBoard.evaluateResult() : null; }

    /** Starts the turn so the UI can show the previous state before arrival. */
    public void beginTurn() {
        if (phase != TurnPhase.START_OF_TURN || currentTurn >= config.maxTurns()) throw new IllegalStateException("Cannot begin another turn");
        currentTurn++;
        events.clear(); departures = List.of(); paymentRecord = null;
        waiterTaskUsed = false;
        phase = TurnPhase.START_OF_TURN;
    }

    /** Step 1: registers the scheduled arrival after the start display. */
    public List<String> processArrival() {
        requirePhase(TurnPhase.START_OF_TURN);
        if (config.arrivalSchedule().contains(currentTurn)) {
            int ordinal = (int) config.arrivalSchedule().stream().filter(t -> t <= currentTurn).count();
            Customer customer = new Customer("C" + ordinal, ordinal, config.randomSeed() + ordinal);
            restaurant.registerArrival(customer);
            events.add("Arrival: " + customer.id() + " joins the queue.");
        }
        phase = TurnPhase.MANAGER_CHOICES;
        return List.copyOf(events);
    }

    public String submitWaiterDecision(WaiterDecision decision) {
        requirePhase(TurnPhase.MANAGER_CHOICES);
        if (waiterTaskUsed) return "The Waiter has already acted this turn.";
        if (decision == null || decision.taskType() == null) return "Choose a Waiter task.";
        Customer target = findCustomer(decision.targetCustomerId());
        switch (decision.taskType()) {
            case WAIT -> { if (decision.targetCustomerId() != null) return "Wait has no target."; events.add("Waiter waits."); }
            case SEAT -> {
                if (target == null || !restaurant.waitingQueue().getMembers().contains(target)) return "Choose a customer in the queue.";
                if (restaurant.tables().stream().noneMatch(t -> !t.isOccupied())) return "No table is free.";
                restaurant.waiter().seat(restaurant, target); events.add("Waiter seats " + target.id() + " at Table " + target.tableId() + ".");
            }
            case TAKE_ORDER -> {
                if (target == null || target.state() != CustomerState.SEATED) return "Choose a seated customer.";
                if (target.order() != null) return "That customer already ordered.";
                Order order = restaurant.waiter().takeOrder(target, restaurant.menu(), currentTurn);
                events.add(target.id() + " orders " + order.item().name() + ".");
            }
            case SERVE -> {
                if (target == null || target.order() == null) return "Choose a customer with an order.";
                if (!target.order().canServe(currentTurn)) return "Order must have been READY before this turn.";
                restaurant.waiter().serve(target, currentTurn);
                scoreBoard.recordServed();
                events.add("Waiter serves " + target.id() + "'s " + target.order().item().name() + ".");
            }
        }
        waiterTaskUsed = true;
        return null;
    }

    List<Customer> advanceableOrders() {
        return restaurant.customers().stream().filter(c -> c.order() != null
                && (c.order().status() == OrderStatus.PLACED || c.order().status() == OrderStatus.PREPARING)).toList();
    }

    public String submitChefDecision(ChefDecision decision) {
        requirePhase(TurnPhase.MANAGER_CHOICES);
        if (decision == null || decision.taskType() == null) return "Choose a Chef task.";
        Customer target = findCustomer(decision.targetCustomerId());
        if (decision.taskType() == ChefTaskType.ADVANCE) {
            if (target == null || target.order() == null
                    || (target.order().status() != OrderStatus.PLACED && target.order().status() != OrderStatus.PREPARING)) {
                return "Choose a customer with an order that is not ready.";
            }
            restaurant.chef().advance(target, currentTurn);
            events.add("Chef advances " + target.id() + "'s " + target.order().item().name()
                    + " (" + target.order().unitsDone() + "/" + target.order().item().preparationUnits() + ").");
        } else {
            if (decision.targetCustomerId() != null) return "Wait has no target.";
            events.add("Chef waits.");
        }
        resolveRemainder();
        return null;
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
            if (customer.state() == CustomerState.WAITING || customer.state() == CustomerState.SEATED) {
                customer.loseSatisfaction();
                if (customer.satisfaction().getValue() == 0) {
                    restaurant.abandon(customer); scoreBoard.recordDeparture();
                    left.add(customer.id()); events.add(customer.id() + " leaves unhappy.");
                }
            }
        }
        departures = List.copyOf(left);
        phase = currentTurn == config.maxTurns() ? TurnPhase.GAME_OVER : TurnPhase.START_OF_TURN;
    }

    public GameSnapshot snapshot() {
        List<String> waiting = restaurant.waitingQueue().getMembers().stream().map(Customer::id).toList();
        List<String> tables = restaurant.tables().stream().map(t -> t.isOccupied()
                ? "Table " + t.tableId() + ": " + t.getOccupant().id() : "Table " + t.tableId() + ": Available").toList();
        List<String> customers = restaurant.customers().stream().map(c -> c.id() + " " + c.state()
                + " satisfaction " + c.satisfaction().getValue() + (c.tableId() > 0 ? " Table " + c.tableId() : "")).toList();
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
        if (!restaurant.waitingQueue().isEmpty() && restaurant.tables().stream().anyMatch(t -> !t.isOccupied())) {
            available.add("Waiter SEAT: " + restaurant.waitingQueue().getMembers().stream().map(Customer::id).toList());
        }
        restaurant.customers().stream().filter(c -> c.state() == CustomerState.SEATED && c.order() == null)
                .forEach(c -> available.add("Waiter TAKE_ORDER: " + c.id()));
        restaurant.customers().stream().filter(c -> c.order() != null && c.order().canServe(currentTurn))
                .forEach(c -> available.add("Waiter SERVE: " + c.id()));
        available.add("Waiter WAIT");
        advanceableOrders().forEach(c -> available.add("Chef ADVANCE: " + c.id()));
        available.add("Chef WAIT");
        available.add("Cashier: automatic, oldest first, max one");
        return new GameSnapshot(currentTurn, phase, scoreBoard.revenueCents(), scoreBoard.averagePaidSatisfaction(),
                scoreBoard.servedCount(), scoreBoard.unhappyDepartures(), currentTurn,
                waiting, tables, customers, orders, activeOptions, advanceOptions, available);
    }

    public TurnResolution lastResolution() {
        if (phase != TurnPhase.START_OF_TURN && phase != TurnPhase.GAME_OVER) throw new IllegalStateException("Turn not resolved");
        return new TurnResolution(currentTurn, events, departures, paymentRecord, result());
    }

    private Customer findCustomer(String id) {
        if (id == null) return null;
        return restaurant.customers().stream().filter(c -> c.id().equalsIgnoreCase(id)).findFirst().orElse(null);
    }
    private void requirePhase(TurnPhase expected) { if (phase != expected) throw new IllegalStateException("Unexpected turn phase: " + phase); }
    public static String money(int cents) { return String.format(java.util.Locale.ROOT, "$%.2f", cents / 100.0); }
}
