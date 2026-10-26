package edu.ntu.sc2002.presentation;

import static org.junit.Assert.*;
import edu.ntu.sc2002.application.GameController;
import edu.ntu.sc2002.application.contract.*;
import edu.ntu.sc2002.presentation.RestaurantUiState.*;
import java.util.Scanner;
import org.junit.Test;

public class RestaurantUiAdapterTest {
    private WaiterChoice waiter(RestaurantUiAdapter adapter, WaiterTask task, String id) {
        return adapter.state().waiterChoices().stream()
                .filter(c -> c.task() == task && java.util.Objects.equals(c.customerId(), id)).findFirst().orElseThrow();
    }
    private ChefChoice chef(RestaurantUiAdapter adapter, String customerId) {
        String orderId = customerId == null ? null : adapter.state().orders().stream()
                .filter(o -> o.customerName().equals(customerId)).findFirst().orElseThrow().id();
        return adapter.state().chefChoices().stream().filter(c -> java.util.Objects.equals(c.orderId(), orderId))
                .findFirst().orElseThrow();
    }
    private void resolveWaitingChef(RestaurantUiAdapter adapter) {
        adapter.handle(new ChefCommand(chef(adapter, null)));
        adapter.handle(new ResolveTurn());
    }

    @Test public void waiterRefreshesSameTurnChefAndResolveCommitsExactlyOnce() {
        GameController engine = new GameController();
        RestaurantUiAdapter adapter = new RestaurantUiAdapter(engine);
        var initial = adapter.start();
        assertEquals(UiPhase.WAITER, initial.phase());
        assertTrue(initial.averagePaidSatisfaction().isEmpty());
        assertTrue(initial.chefChoices().isEmpty());
        assertFalse(initial.canResolveTurn());
        adapter.handle(new WaiterCommand(waiter(adapter, WaiterTask.SEAT, "C1")));
        resolveWaitingChef(adapter);
        adapter.handle(new WaiterCommand(waiter(adapter, WaiterTask.TAKE_ORDER, "C1")));
        var beforeChef = engine.snapshot();
        assertEquals("Salad", beforeChef.orders().get(0).item());
        assertTrue(adapter.state().chefChoices().stream().anyMatch(c -> c.task() == ChefTask.ADVANCE_ORDER));
        adapter.handle(new ChefCommand(chef(adapter, "C1")));
        assertEquals(beforeChef, engine.snapshot());
        assertEquals(UiPhase.RESOLVE, adapter.state().phase());
        assertTrue(adapter.state().canResolveTurn());
        var next = adapter.handle(new ResolveTurn());
        assertEquals(3, next.turn());
        assertEquals(1, engine.snapshot().orders().get(0).unitsDone());
        assertEquals(java.util.List.of(1), next.scene().readyTableIds());
        var after = engine.snapshot();
        var rejected = adapter.handle(new ResolveTurn());
        assertTrue(rejected.feedback().rejected());
        assertEquals(after, engine.snapshot());
    }

    @Test public void invalidCommandsKeepAvailableChoicesPendingChefAndEngineState() {
        GameController engine = new GameController();
        RestaurantUiAdapter adapter = new RestaurantUiAdapter(engine);
        adapter.start();
        var before = engine.snapshot();
        var choices = adapter.state().waiterChoices();
        var forged = new WaiterChoice(WaiterTask.SEAT, "C1", "table-2", null, "Invalid table");
        assertTrue(adapter.handle(new WaiterCommand(forged)).feedback().rejected());
        assertEquals(before, engine.snapshot());
        assertEquals(choices, adapter.state().waiterChoices());
        adapter.handle(new WaiterCommand(waiter(adapter, WaiterTask.WAIT, null)));
        adapter.handle(new ChefCommand(chef(adapter, null)));
        before = engine.snapshot();
        assertTrue(adapter.handle(new ActivateHappyHour()).feedback().rejected());
        assertTrue(adapter.state().canResolveTurn());
        assertEquals(before, engine.snapshot());
        adapter.handle(new ResolveTurn());
        assertEquals(2, adapter.state().turn());
    }

    @Test public void typedSnapshotsAreImmutableDetachedAndActionsFollowControllerPhase() {
        GameController engine = new GameController();
        assertTrue(engine.snapshot().waiterActions().isEmpty());
        engine.beginTurn();
        assertTrue(engine.snapshot().waiterActions().isEmpty());
        engine.processArrival();
        var snapshot = engine.snapshot();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.customers().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.waiterActions().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.chefActions().clear());
        assertEquals(Integer.valueOf(1), snapshot.customers().get(0).queuePosition());
        assertNull(snapshot.customers().get(0).tableId());
        var seat = snapshot.waiterActions().stream().filter(a -> a.decision().taskType() == WaiterTaskType.SEAT).findFirst().orElseThrow();
        assertEquals(Integer.valueOf(1), seat.tableId());
        assertTrue(engine.submitWaiterDecision(seat.decision()).isSuccessful());
        assertNull(snapshot.customers().get(0).tableId());
        assertEquals(Integer.valueOf(1), engine.snapshot().customers().get(0).tableId());
        assertTrue(engine.snapshot().waiterActions().isEmpty());
        assertEquals(1, engine.snapshot().chefActions().size());
    }

    private RestaurantUiState replay(String resource) {
        RestaurantUiAdapter adapter = new RestaurantUiAdapter(new GameController());
        adapter.start();
        try (Scanner input = new Scanner(getClass().getResourceAsStream("/stage1/" + resource + "-seed-2026.txt"))) {
            while (adapter.state().result() == null) {
                int option = input.nextInt();
                WaiterTask task = switch (option) {
                    case 1 -> WaiterTask.SEAT;
                    case 2 -> WaiterTask.TAKE_ORDER;
                    case 3 -> WaiterTask.SERVE;
                    case 4 -> WaiterTask.WAIT;
                    default -> throw new AssertionError("Invalid fixture task");
                };
                adapter.handle(new WaiterCommand(waiter(adapter, task, option == 4 ? null : input.next())));
                int chefOption = input.nextInt();
                adapter.handle(new ChefCommand(chef(adapter, chefOption == 2 ? null : input.next())));
                assertTrue(adapter.state().canResolveTurn());
                adapter.handle(new ResolveTurn());
            }
        }
        var finalState = adapter.state();
        assertEquals(18, finalState.result().turnsUsed());
        assertEquals(UiPhase.COMPLETE, finalState.phase());
        assertTrue(finalState.waiterChoices().isEmpty());
        assertTrue(finalState.chefChoices().isEmpty());
        assertFalse(finalState.canResolveTurn());
        assertTrue(adapter.handle(new ResolveTurn()).feedback().rejected());
        assertEquals(finalState.result(), adapter.state().result());
        return finalState;
    }

    @Test public void victoryReplayMatchesCliAndRemovesPaidActors() {
        var state = replay("victory");
        assertTrue(state.result().victory());
        assertEquals(5, state.result().servedCount());
        assertEquals(51.0, state.result().revenue(), 0);
        assertEquals(78.0, state.result().averagePaidSatisfaction().orElseThrow(), 0);
        assertFalse(state.scene().customers().stream().anyMatch(c -> c.id().equals("C1")));
    }

    @Test public void defeatReplayMatchesCliAndKeepsUnpaidAverageAbsent() {
        var state = replay("defeat");
        assertFalse(state.result().victory());
        assertEquals(2, state.result().departures());
        assertEquals(0, state.result().revenue(), 0);
        assertTrue(state.result().averagePaidSatisfaction().isEmpty());
        assertTrue(state.scene().readyTableIds().isEmpty());
        assertFalse(state.scene().customers().stream().anyMatch(c -> c.id().equals("C1")));
    }
}
