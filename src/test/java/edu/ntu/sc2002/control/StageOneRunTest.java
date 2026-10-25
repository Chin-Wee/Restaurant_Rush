package edu.ntu.sc2002.control;

import static org.junit.Assert.*;

import edu.ntu.sc2002.entity.*;
import org.junit.Test;

public class StageOneRunTest {
    private GameController run(long seed, boolean serveCustomers) {
        GameController game = new GameController(GameConfig.stageOne(seed), new Restaurant(), new ScoreBoard());
        while (game.phase() != TurnPhase.GAME_OVER) {
            game.beginTurn(); game.processArrival();
            Restaurant restaurant = game.restaurant();
            WaiterTaskType task = WaiterTaskType.WAIT;
            String targetId = null;
            if (serveCustomers) {
                Customer target = restaurant.customers().stream()
                        .filter(c -> c.order() != null && c.order().canServe(game.currentTurn())).findFirst().orElse(null);
                if (target != null) task = WaiterTaskType.SERVE;
                else if ((target = restaurant.customers().stream()
                        .filter(c -> c.state() == CustomerState.SEATED && c.order() == null).findFirst().orElse(null)) != null) {
                    task = WaiterTaskType.TAKE_ORDER;
                } else if (restaurant.tables().stream().anyMatch(t -> !t.isOccupied())
                        && !restaurant.waitingQueue().isEmpty()) {
                    task = WaiterTaskType.SEAT;
                    target = restaurant.waitingQueue().getMembers().get(0);
                }
                if (target != null) targetId = target.id();
            }
            assertTrue(game.submitWaiterDecision(new WaiterDecision(task, targetId)).isSuccessful());
            Customer order = serveCustomers ? game.advanceableOrders().stream().findFirst().orElse(null) : null;
            assertTrue(game.submitChefDecision(order == null ? new ChefDecision(ChefTaskType.WAIT, null)
                    : new ChefDecision(ChefTaskType.ADVANCE, order.id())).isSuccessful());
        }
        return game;
    }

    @Test public void seed2026WithOldestReadyAndOldestQueuePolicyWins() {
        GameController game = run(2026, true);
        assertEquals(18, game.currentTurn());
        assertEquals(GameResult.VICTORY, game.result());
    }

    @Test public void seed2026WithWaitEveryTurnLoses() {
        GameController game = run(2026, false);
        assertEquals(18, game.currentTurn());
        assertEquals(GameResult.DEFEAT, game.result());
        assertEquals(0, game.scoreBoard().servedCount());
    }
}
