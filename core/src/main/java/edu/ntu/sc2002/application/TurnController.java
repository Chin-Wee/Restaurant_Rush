package edu.ntu.sc2002.application;

import edu.ntu.sc2002.application.contract.*;
import edu.ntu.sc2002.domain.*;
import edu.ntu.sc2002.domain.customer.*;
import edu.ntu.sc2002.domain.staff.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Application-level turn orchestrator. It owns the current phase, the restaurant aggregate and
 * the scoreboard, validates manager commands and applies the six turn steps in order. The UI only
 * reads immutable snapshots/resolutions and never mutates domain state directly.
 *
 * <p>Commands are accepted incrementally: Host, then Waiter, then Chef, with the final Chef
 * submission resolving eating, payment, waiting loss and the next turn.
 */
public class TurnController {
    private final GameConfig config;
    private final Restaurant restaurant;
    private final CustomerFactory customerFactory = new CustomerFactory();
    private final ScoreBoard scoreBoard = new ScoreBoard();
    private TurnPhase phase = TurnPhase.START_OF_TURN;
    private int currentTurn = 1;
    private boolean waiterTaskUsed;
    private boolean hostTaskUsed;
    private boolean turnStarted;
    private String newArrival;
    private final List<String> events = new ArrayList<>();
    private PaymentRecord paymentRecord;
    private List<String> departures = List.of();

    /**
     * Creates a controller and the restaurant aggregate for the supplied configuration.
     *
     * @param config validated game configuration
     * @throws NullPointerException if {@code config} is null
     * @throws IllegalArgumentException if the restaurant rejects the configured table count
     */
    public TurnController(GameConfig config) {
        this.config = Objects.requireNonNull(config);
        restaurant = new Restaurant(config.numTables());
    }
    public TurnPhase phase() { return phase; }
    public int currentTurn() { return currentTurn; }
    /** During play this is a provisional threshold evaluation, never a termination signal. */
    public GameResult result() { return scoreBoard.evaluateResult(); }

    /**
     * Starts a new turn, resets per-turn command state and applies any active Happy Hour recovery.
     * The start-of-turn snapshot is produced after this call so the UI can show the previous state
     * before the scheduled arrival is registered.
     *
     * @return an empty successful resolution for the new turn
     * @throws IllegalStateException if a turn is already active or the game is not ready to start one
     */
    public TurnResolution beginTurn() {
        if (turnStarted || phase != TurnPhase.START_OF_TURN) throw new IllegalStateException("Cannot begin another turn");
        // A used Waiter task with no active turn identifies the previously completed turn.
        if (waiterTaskUsed) currentTurn++;
        events.clear(); departures = List.of(); paymentRecord = null; newArrival = null;
        waiterTaskUsed = false;
        hostTaskUsed = false;
        turnStarted = true;
        if (restaurant.getHappyHourPolicy().isActive()) recoverWaitingCustomers();
        return response(null);
    }

    /**
     * Step 1: creates and registers the customer scheduled for the current turn, if any, then
     * moves the controller into {@link TurnPhase#MANAGER_CHOICES}.
     *
     * @return a successful resolution containing the arrival event
     * @throws IllegalStateException if the turn has not been started or is in the wrong phase
     */
    public TurnResolution processArrival() {
        if (!turnStarted || phase != TurnPhase.START_OF_TURN) throw new IllegalStateException("Begin the turn before processing arrivals");
        if (config.arrivalSchedule().contains(currentTurn)) {
            int ordinal = config.arrivalSchedule().indexOf(currentTurn) + 1;
            Customer customer = customerFactory.createCustomer(config.customerTypeAt(currentTurn),
                    "C" + ordinal, ordinal, config.randomSeed() + ordinal);
            restaurant.registerArrival(customer);
            newArrival = customer.getId();
            events.add("Arrival: " + customer.getId() + " (" + customer.getType() + ") joins the queue.");
        }
        phase = TurnPhase.MANAGER_CHOICES;
        return response(null);
    }

    /**
     * Stage 3: applies the Host's single task for the turn. The Host may seat one explicitly
     * selected queued customer at the first free table, or wait. Rejected commands do not consume
     * the Host task.
     *
     * @param decision Host command to validate and apply
     * @return a successful resolution, or a failed resolution explaining the rejected command
     */
    public TurnResolution submitHostDecision(HostChoiceDTO decision) {
        String error = hostError(decision);
        if (error != null) return response(error);
        switch (decision.taskType()) {
            case WAIT -> events.add("Host waits.");
            case SEAT -> {
                Customer target = customerForId(decision.targetCustomerId());
                Table table = restaurant.getTables().stream().filter(t -> !t.isOccupied()).findFirst()
                        .orElseThrow(() -> new IllegalStateException("No table is free"));
                if (!restaurant.getStaff(Host.class).seatCustomer(target, restaurant.getWaitingQueue(), table)) {
                    return response("Unable to seat " + target.getId() + ".");
                }
                events.add("Host seats " + target.getId() + " at Table " + table.tableId() + ".");
            }
        }
        hostTaskUsed = true;
        return response(null);
    }

    /**
     * Applies the Waiter's single task for the turn. Seating, ordering and serving are validated
     * against the current snapshot; an invalid command returns a failed resolution without consuming
     * the task. The Waiter acts before the Chef so newly placed orders can be advanced immediately.
     *
     * @param decision Waiter command to validate and apply
     * @return a successful resolution, or a failed resolution explaining the rejected command
     */
    public TurnResolution submitWaiterDecision(WaiterChoiceDTO decision) {
        String error = waiterError(decision);
        if (error != null) return response(error);
        Customer target = customerForId(decision.targetCustomerId());
        switch (decision.taskType()) {
            case WAIT -> events.add("Waiter waits.");
            case SEAT -> {
                Waiter waiter = restaurant.getStaff(Waiter.class);
                Table table = restaurant.getTables().stream().filter(t -> !t.isOccupied()).findFirst()
                        .orElseThrow(() -> new IllegalStateException("No table is free"));
                if (!waiter.seatCustomer(target, restaurant.getWaitingQueue(), table)) {
                    return response("Unable to seat " + target.getId() + ".");
                }
                events.add("Waiter seats " + target.getId() + " at Table " + table.tableId() + ".");
            }
            case TAKE_ORDER -> {
                Order order = restaurant.getStaff(Waiter.class).takeOrder(
                        target, restaurant.getMenu(), restaurant.getHappyHourPolicy());
                int menuPrice = order.getMenuItem().getPriceCents();
                events.add(target.getId() + " orders " + order.getMenuItem().getName() + " for " + money(order.lockedPriceCents())
                        + (order.lockedPriceCents() == menuPrice ? "" : " (menu " + money(menuPrice) + ")") + ".");
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

    /**
     * Applies the Chef's task and resolves the remainder of the turn. Successful submission advances
     * preparation or records a wait, then finishes eating, performs at most one automatic payment,
     * applies waiting loss/abandonment, advances the Happy Hour clock and moves to the next phase.
     *
     * @param decision Chef command to validate and apply
     * @return a successful resolution, or a failed resolution explaining the rejected command
     */
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
        for (Customer customer : activeCustomers()) customer.finishEating(currentTurn);
        boolean paymentDue = restaurant.getTables().stream().filter(Table::isOccupied)
                .map(Table::getSeatedCustomer).anyMatch(c -> c.canPay(currentTurn));
        if (paymentDue) {
            Customer paying = restaurant.oldestPaymentCandidate(currentTurn);
            paymentRecord = restaurant.getStaff(Cashier.class).processPayment(paying, restaurant, currentTurn);
            scoreBoard.recordPayment(paymentRecord);
            events.add(paymentRecord.customerId() + " pays " + money(paymentRecord.lockedPriceCents()) + ".");
        }
        List<String> left = new ArrayList<>();
        for (Customer customer : activeCustomers()) {
            if (customer.getState() == CustomerState.WAITING || customer.getState() == CustomerState.SEATED) {
                customer.applySatisfactionLoss();
                if (customer.getSatisfaction() == 0) {
                    Table table = customer.getState() == CustomerState.SEATED ? restaurant.tableFor(customer) : null;
                    customer.leave();
                    if (restaurant.getWaitingQueue().getQueuedCustomers().contains(customer)) {
                        restaurant.getWaitingQueue().dequeueCustomer(customer);
                    }
                    if (table != null) table.releaseTable();
                    scoreBoard.recordDeparture();
                    left.add(customer.getId()); events.add(customer.getId() + " leaves unhappy.");
                }
            }
        }
        departures = List.copyOf(left);
        restaurant.getHappyHourPolicy().decrementTurn();
        turnStarted = false;
        phase = currentTurn == config.maxTurns() ? TurnPhase.GAME_OVER : TurnPhase.START_OF_TURN;
        return response(null);
    }

    /**
     * Stage 3: activates the once-per-game Happy Hour at the start of a turn, before the Host acts.
     * On success, waiting/seated customers immediately recover the policy's satisfaction amount.
     *
     * @return a successful activation resolution, or a failed resolution if activation is not allowed
     */
    public TurnResolution activateHappyHour() {
        String error = happyHourError();
        if (error != null) return response(error);
        if (restaurant.getHappyHourPolicy().activate(currentTurn)) {
            events.add("Happy Hour activated: orders taken this turn and the next get 20% off.");
            recoverWaitingCustomers();
        }
        return response(null);
    }
    private String happyHourError() {
        if (phase != TurnPhase.MANAGER_CHOICES) return "Happy Hour can only be activated at the start of a turn.";
        if (hostTaskUsed) return "Happy Hour must be activated before the Host acts.";
        if (waiterTaskUsed) return "Happy Hour must be activated before the Waiter acts.";
        return switch (restaurant.getHappyHourPolicy().getStatus()) {
            case ACTIVE -> "Happy Hour is already active.";
            case EXHAUSTED -> "Happy Hour has already been used this game.";
            case AVAILABLE -> null;
        };
    }
    /** Customers still waiting for a table or food recover satisfaction at the start of each active turn. */
    private void recoverWaitingCustomers() {
        HappyHourPolicy policy = restaurant.getHappyHourPolicy();
        for (Customer customer : activeCustomers()) {
            if (customer.getState() != CustomerState.WAITING && customer.getState() != CustomerState.SEATED) continue;
            int before = customer.getSatisfaction();
            customer.addSatisfaction(policy.recoveryAmount());
            if (customer.getSatisfaction() > before) {
                events.add(customer.getId() + " recovers " + (customer.getSatisfaction() - before)
                        + " satisfaction (Happy Hour).");
            }
        }
    }

    private Customer customerForId(String id) {
        return activeCustomers().stream().filter(c -> c.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }
    private List<Customer> activeCustomers() {
        return Stream.concat(restaurant.getWaitingQueue().getQueuedCustomers().stream(),
                restaurant.getTables().stream().filter(Table::isOccupied).map(Table::getSeatedCustomer))
                .sorted(Comparator.comparingInt(Customer::arrivalOrdinal)).toList();
    }
    private String hostError(HostChoiceDTO decision) {
        if (phase != TurnPhase.MANAGER_CHOICES) return "Host choices are unavailable in phase " + phase + ".";
        if (hostTaskUsed) return "The Host has already acted this turn.";
        if (decision == null) return "Choose a Host task.";
        Customer target = customerForId(decision.targetCustomerId());
        return switch (decision.taskType()) {
            case WAIT -> null;
            case SEAT -> target == null || !restaurant.getWaitingQueue().getQueuedCustomers().contains(target)
                    ? "Choose a customer in the queue."
                    : restaurant.getTables().stream().noneMatch(t -> !t.isOccupied()) ? "No table is free." : null;
        };
    }
    private String waiterError(WaiterChoiceDTO decision) {
        if (phase != TurnPhase.MANAGER_CHOICES) return "Waiter choices are unavailable in phase " + phase + ".";
        if (!hostTaskUsed) return "Choose the Host task before the Waiter task.";
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
        if (!hostTaskUsed) return "Choose the Host task before the Chef task.";
        if (!waiterTaskUsed) return "Choose the Waiter task before the Chef task.";
        if (decision == null) return "Choose a Chef task.";
        if (decision.taskType() == ChefTaskType.WAIT) return null;
        Customer target = customerForId(decision.targetCustomerId());
        return target == null || target.order() == null
                || (target.order().getStatus() != OrderStatus.PLACED && target.order().getStatus() != OrderStatus.PREPARING)
                ? "Choose a customer with an order that is not ready." : null;
    }

    /**
     * Builds the immutable read model used by both frontends. Only active customers are included;
     * paid and abandoned customers remain represented through aggregate scoreboard totals.
     *
     * @return a snapshot of the current turn, phase, active customers and available commands
     */
    public GameSnapshot snapshot() {
        var customers = activeCustomers();
        var waiting = restaurant.getWaitingQueue().getQueuedCustomers().stream().map(Customer::getId).toList();
        var tables = restaurant.getTables().stream().map(t -> new GameSnapshot.TableView(t.tableId(),
                t.isOccupied() ? t.getSeatedCustomer().getId() : null)).toList();
        var customerViews = customers.stream().map(c -> {
            Table table = c.getState() == CustomerState.WAITING ? null : restaurant.tableFor(c);
            int queueIndex = waiting.indexOf(c.getId());
            return new GameSnapshot.CustomerView(c.getId(), c.arrivalOrdinal(), c.getType(), c.getState(), c.getSatisfaction(),
                    table == null ? null : table.tableId(), queueIndex < 0 ? null : queueIndex + 1);
        }).toList();
        var orders = customers.stream().filter(c -> c.order() != null).map(c ->
                new GameSnapshot.OrderView(c.getId(), c.order().getMenuItem().getName(), c.order().getStatus(),
                        c.order().unitsDone(), c.order().getMenuItem().getPrepUnitsNeeded(), c.order().lockedPriceCents())).toList();
        List<GameSnapshot.WaiterAction> waiterActions = new ArrayList<>();
        List<ChefChoiceDTO> chefActions = new ArrayList<>();
        List<GameSnapshot.HostAction> hostActions = new ArrayList<>();
        if (turnStarted && phase == TurnPhase.MANAGER_CHOICES) {
            Integer nextTable = tables.stream().filter(t -> t.occupantId() == null).map(GameSnapshot.TableView::id).findFirst().orElse(null);
            if (!hostTaskUsed) {
                for (Customer customer : customers) {
                    var decision = new HostChoiceDTO(HostTaskType.SEAT, customer.getId());
                    if (hostError(decision) == null) {
                        hostActions.add(new GameSnapshot.HostAction(decision, nextTable, customer.getId()));
                    }
                }
                var hostWait = new HostChoiceDTO(HostTaskType.WAIT, null);
                if (hostError(hostWait) == null) hostActions.add(new GameSnapshot.HostAction(hostWait, null, null));
            }
            for (WaiterTaskType task : WaiterTaskType.values()) {
                if (task == WaiterTaskType.WAIT) continue;
                for (Customer customer : customers) {
                    var decision = new WaiterChoiceDTO(task, customer.getId());
                    if (waiterError(decision) == null) waiterActions.add(new GameSnapshot.WaiterAction(decision,
                            task == WaiterTaskType.SEAT ? nextTable : null, customer.getId()));
                }
            }
            var waiterWait = new WaiterChoiceDTO(WaiterTaskType.WAIT, null);
            if (waiterError(waiterWait) == null) waiterActions.add(new GameSnapshot.WaiterAction(waiterWait, null, null));
            for (Customer customer : customers) {
                var decision = new ChefChoiceDTO(ChefTaskType.ADVANCE, customer.getId());
                if (chefError(decision) == null) chefActions.add(decision);
            }
            var chefWait = new ChefChoiceDTO(ChefTaskType.WAIT, null);
            if (chefError(chefWait) == null) chefActions.add(chefWait);
        }
        double average = scoreBoard.averagePaidSatisfaction();
        return new GameSnapshot(currentTurn, phase, scoreBoard.revenueCents(), Double.isNaN(average) ? null : average,
                scoreBoard.servedCount(), scoreBoard.paidCount(), scoreBoard.unhappyDepartures(),
                turnStarted || !waiterTaskUsed ? currentTurn - 1 : currentTurn,
                waiting, tables, customerViews, orders, waiterActions, chefActions, hostActions,
                restaurant.getHappyHourPolicy().getStatus(), restaurant.getHappyHourPolicy().getRemainingTurns());
    }
    /**
     * Returns the resolution for the most recently completed turn.
     *
     * @return the completed turn's events, payment and result
     * @throws IllegalStateException if the current turn has not been resolved yet
     */
    public TurnResolution lastResolution() {
        if (turnStarted || !waiterTaskUsed) throw new IllegalStateException("Turn not resolved");
        return response(null);
    }
    private TurnResolution response(String error) {
        return new TurnResolution(currentTurn, events, newArrival, departures, paymentRecord,
                error == null, error, result());
    }

    /**
     * Formats integer cents as a two-decimal dollar amount using the root locale.
     *
     * @param cents amount in cents
     * @return formatted currency string, for example {@code $12.50}
     */
    public static String money(int cents) { return String.format(java.util.Locale.ROOT, "$%.2f", cents / 100.0); }
}
