package edu.ntu.sc2002.presentation;

import edu.ntu.sc2002.application.GameController;
import edu.ntu.sc2002.application.contract.*;
import edu.ntu.sc2002.domain.GameResult;
import edu.ntu.sc2002.domain.OrderStatus;
import edu.ntu.sc2002.domain.customer.CustomerState;
import edu.ntu.sc2002.presentation.RestaurantUiState.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;

/** The only engine-to-visualization bridge. Holds a pending Chef selection, never game rules. */
public final class RestaurantUiAdapter {
    private final GameController controller;
    private final Map<WaiterChoice, WaiterDecision> waiterCommands = new LinkedHashMap<>();
    private final Map<ChefChoice, ChefDecision> chefCommands = new LinkedHashMap<>();
    private ChefDecision pendingChef;
    private Integer waiterTableId;
    private int waiterRevision;
    private int chefRevision;
    private boolean started;
    private RestaurantUiState state;

    public RestaurantUiAdapter(GameController controller) { this.controller = Objects.requireNonNull(controller); }

    public RestaurantUiState start() {
        if (started) throw new IllegalStateException("Session already started");
        started = true;
        controller.beginTurn();
        return refresh(feedback(controller.processArrival()));
    }

    public RestaurantUiState state() {
        if (!started) throw new IllegalStateException("Session has not started");
        return state;
    }

    public RestaurantUiState handle(Command command) {
        state();
        if (controller.phase() == TurnPhase.GAME_OVER) return refresh(new Feedback("Game complete.", true));
        if (command instanceof WaiterCommand waiter) {
            WaiterDecision decision = waiterCommands.get(waiter.choice());
            if (decision == null) return reject();
            TurnResolution response = controller.submitWaiterDecision(decision);
            if (response.isSuccessful() && decision.taskType() != WaiterTaskType.WAIT) {
                waiterTableId = controller.snapshot().customers().stream()
                        .filter(c -> c.id().equals(decision.targetCustomerId())).findFirst().orElseThrow().tableId();
                waiterRevision++;
            }
            return refresh(feedback(response));
        }
        if (command instanceof ChefCommand chef) {
            ChefDecision decision = chefCommands.get(chef.choice());
            if (decision == null) return reject();
            pendingChef = decision;
            return refresh(new Feedback("Chef task selected. Resolve Turn to apply it.", false));
        }
        if (command instanceof ResolveTurn && pendingChef != null) {
            TurnResolution response = controller.submitChefDecision(pendingChef);
            if (!response.isSuccessful()) return refresh(feedback(response));
            if (pendingChef.taskType() == ChefTaskType.ADVANCE) chefRevision++;
            pendingChef = null;
            String summary = "End turn " + response.turn() + ": " + String.join(" ", response.events());
            if (controller.phase() != TurnPhase.GAME_OVER) {
                controller.beginTurn();
                summary += " " + String.join(" ", controller.processArrival().events());
            }
            return refresh(new Feedback(summary, false));
        }
        return reject();
    }

    private RestaurantUiState reject() {
        return refresh(new Feedback("That choice is unavailable. Your selection has been kept.", true));
    }

    private static Feedback feedback(TurnResolution resolution) {
        String message = resolution.isSuccessful() ? String.join(" ", resolution.events()) : resolution.errorMessage();
        return message.isBlank() ? null : new Feedback(message, !resolution.isSuccessful());
    }

    private RestaurantUiState refresh(Feedback feedback) {
        GameSnapshot snapshot = controller.snapshot();
        waiterCommands.clear();
        chefCommands.clear();
        for (var action : snapshot.waiterActions()) {
            WaiterDecision d = action.decision();
            String id = d.targetCustomerId();
            var order = snapshot.orders().stream().filter(o -> o.customerId().equals(id)).findFirst().orElse(null);
            String label = switch (d.taskType()) {
                case SEAT -> "Seat " + id + " at Table " + action.tableId();
                case TAKE_ORDER -> "Take " + id + "'s order";
                case SERVE -> "Serve " + id + "'s " + order.item();
                case WAIT -> "Wait";
            };
            waiterCommands.put(new WaiterChoice(WaiterTask.valueOf(d.taskType().name()), id,
                    action.tableId() == null ? null : "table-" + action.tableId(),
                    d.taskType() == WaiterTaskType.SERVE ? order.id() : null, label), d);
        }
        for (ChefDecision d : snapshot.chefActions()) {
            var order = snapshot.orders().stream().filter(o -> o.customerId().equals(d.targetCustomerId())).findFirst().orElse(null);
            chefCommands.put(new ChefChoice(d.taskType() == ChefTaskType.WAIT ? ChefTask.WAIT : ChefTask.ADVANCE_ORDER,
                    order == null ? null : order.id(), order == null ? "Wait" : "Advance " + order.customerId() + "'s " + order.item()), d);
        }
        var customers = snapshot.customers().stream().map(c -> {
            var order = snapshot.orders().stream().filter(o -> o.customerId().equals(c.id())).findFirst().orElse(null);
            String location = c.tableId() != null ? "Table " + c.tableId()
                    : c.queuePosition() != null ? "Queue #" + c.queuePosition() : c.state().name();
            String payment = switch (c.state()) {
                case PAID -> "Paid";
                case READY_TO_PAY -> "Awaiting automatic collection";
                case EATING -> "Eating";
                case LEFT -> "Departed unpaid";
                default -> "Not due";
            };
            return new CustomerSummary(c.id(), c.id(), null, location, c.satisfaction(), order == null ? null : order.item(),
                    order == null ? "No order" : order.status() + " " + order.unitsDone() + "/" + order.preparationUnits(), payment);
        }).toList();
        var tables = snapshot.tables().stream().map(t -> new TableSummary("Table " + t.id(),
                t.occupantId() == null ? "Available" : "Occupied", t.occupantId())).toList();
        var orders = snapshot.orders().stream().map(o -> new OrderSummary(o.id(), o.customerId(), o.item(),
                o.status() + " " + o.unitsDone() + "/" + o.preparationUnits(), false)).toList();
        var sceneCustomers = snapshot.customers().stream().filter(c -> c.state() != CustomerState.PAID && c.state() != CustomerState.LEFT)
                .map(c -> new SceneCustomer(c.id(), c.arrivalOrdinal() - 1, c.tableId(), c.queuePosition())).toList();
        var readyTables = snapshot.orders().stream().filter(o -> o.status() == OrderStatus.READY)
                .flatMap(o -> snapshot.customers().stream().filter(c -> c.id().equals(o.customerId())))
                .map(GameSnapshot.CustomerView::tableId).filter(Objects::nonNull).toList();
        OptionalDouble average = snapshot.averagePaidSatisfaction() == null ? OptionalDouble.empty()
                : OptionalDouble.of(snapshot.averagePaidSatisfaction());
        boolean complete = controller.phase() == TurnPhase.GAME_OVER;
        ResultSummary result = complete ? new ResultSummary(snapshot.servedCount(), snapshot.unhappyDepartures(),
                snapshot.revenueCents() / 100.0, average, snapshot.turnsUsed(), controller.result() == GameResult.VICTORY) : null;
        state = new RestaurantUiState(1, snapshot.turn(), complete ? UiPhase.COMPLETE
                : pendingChef != null ? UiPhase.RESOLVE : !waiterCommands.isEmpty() ? UiPhase.WAITER : UiPhase.CHEF,
                snapshot.revenueCents() / 100.0, average, customers, tables, orders,
                List.copyOf(waiterCommands.keySet()), List.copyOf(chefCommands.keySet()), pendingChef != null, null, feedback, result,
                new SceneState(sceneCustomers, readyTables, waiterTableId, waiterRevision, chefRevision));
        return state;
    }
}
