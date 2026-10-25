package edu.ntu.sc2002.control;

import static org.junit.Assert.*;

import edu.ntu.sc2002.entity.*;
import org.junit.Test;

public class StageOneGameTest {
    private GameController game(long seed) {
        return new GameController(GameConfig.stageOne(seed), new Restaurant(), new ScoreBoard());
    }
    private void waiter(GameController game, WaiterTaskType task, String id) {
        assertTrue(game.submitWaiterDecision(new WaiterDecision(task, id)).isSuccessful());
    }
    private void chef(GameController game, ChefTaskType task, String id) {
        assertTrue(game.submitChefDecision(new ChefDecision(task, id)).isSuccessful());
    }
    private void begin(GameController game) { game.beginTurn(); game.processArrival(); }

    @Test public void reproducesPastaExampleAndDefersPayment() {
        GameController game = game(2); // Customer C1 uses Random(3), which chooses Pasta.
        begin(game); waiter(game, WaiterTaskType.SEAT, "C1"); chef(game, ChefTaskType.WAIT, null);
        assertEquals(92, game.restaurant().customers().get(0).satisfaction().getValue());
        begin(game); waiter(game, WaiterTaskType.TAKE_ORDER, "C1"); chef(game, ChefTaskType.ADVANCE, "C1");
        assertEquals("Pasta", game.restaurant().customers().get(0).order().item().name());
        assertEquals(84, game.restaurant().customers().get(0).satisfaction().getValue());
        begin(game); waiter(game, WaiterTaskType.WAIT, null); chef(game, ChefTaskType.ADVANCE, "C1");
        assertEquals(OrderStatus.READY, game.restaurant().customers().get(0).order().status());
        begin(game); waiter(game, WaiterTaskType.SERVE, "C1"); chef(game, ChefTaskType.WAIT, null);
        assertEquals(76, game.restaurant().customers().get(0).satisfaction().getValue());
        assertEquals(1, game.scoreBoard().servedCount());
        begin(game); waiter(game, WaiterTaskType.WAIT, null); chef(game, ChefTaskType.WAIT, null);
        assertEquals(CustomerState.READY_TO_PAY, game.restaurant().customers().get(0).state());
        assertEquals(0, game.scoreBoard().revenueCents());
        begin(game); waiter(game, WaiterTaskType.WAIT, null); chef(game, ChefTaskType.WAIT, null);
        assertEquals(1800, game.scoreBoard().revenueCents());
        assertEquals(76.0, game.scoreBoard().averagePaidSatisfaction(), 0.0);
    }

    @Test public void invalidChoicesDoNotChangeStateOrConsumeTask() {
        GameController game = game(2026); begin(game);
        assertFalse(game.submitWaiterDecision(new WaiterDecision(WaiterTaskType.SEAT, "missing")).isSuccessful());
        assertEquals(1, game.restaurant().waitingQueue().getMembers().size());
        assertTrue(game.submitWaiterDecision(new WaiterDecision(WaiterTaskType.SEAT, "C1")).isSuccessful());
        assertFalse(game.submitChefDecision(new ChefDecision(ChefTaskType.ADVANCE, "C1")).isSuccessful());
        assertEquals(100, game.restaurant().customers().get(0).satisfaction().getValue());
        assertEquals(TurnPhase.MANAGER_CHOICES, game.phase());
    }

    @Test public void waiterCanCompleteOnlyOneValidTaskPerTurn() {
        GameController game = game(2026); begin(game);
        waiter(game, WaiterTaskType.SEAT, "C1");
        assertFalse(game.submitWaiterDecision(new WaiterDecision(WaiterTaskType.TAKE_ORDER, "C1")).isSuccessful());
        assertNull(game.restaurant().customers().get(0).order());
        assertEquals(CustomerState.SEATED, game.restaurant().customers().get(0).state());
    }

    @Test public void victoryThresholdsAreAllRequired() {
        ScoreBoard win = new ScoreBoard();
        for (int i = 0; i < 4; i++) {
            win.recordServed(); win.recordPayment(new PaymentRecord("C" + i, 1200, 75));
        }
        assertEquals(GameResult.VICTORY, win.evaluateResult());
        ScoreBoard lose = new ScoreBoard();
        for (int i = 0; i < 4; i++) {
            lose.recordServed(); lose.recordPayment(new PaymentRecord("C" + i, 1100, 75));
        }
        assertEquals(GameResult.DEFEAT, lose.evaluateResult());
        ScoreBoard lowSatisfaction = new ScoreBoard();
        for (int i = 0; i < 4; i++) {
            lowSatisfaction.recordServed();
            lowSatisfaction.recordPayment(new PaymentRecord("C" + i, 1500, 59));
        }
        assertEquals(GameResult.DEFEAT, lowSatisfaction.evaluateResult());
        ScoreBoard tooFew = new ScoreBoard();
        for (int i = 0; i < 3; i++) {
            tooFew.recordServed(); tooFew.recordPayment(new PaymentRecord("C" + i, 1800, 90));
        }
        assertEquals(GameResult.DEFEAT, tooFew.evaluateResult());
        ScoreBoard exact = new ScoreBoard();
        for (int cents : new int[] {900, 900, 900, 1800}) {
            exact.recordServed(); exact.recordPayment(new PaymentRecord("boundary", cents, 60));
        }
        assertEquals(4500, exact.revenueCents());
        assertEquals(GameResult.VICTORY, exact.evaluateResult());
        ScoreBoard centBelow = new ScoreBoard();
        for (int cents : new int[] {900, 900, 900, 1799}) {
            centBelow.recordServed(); centBelow.recordPayment(new PaymentRecord("boundary", cents, 60));
        }
        assertEquals(GameResult.DEFEAT, centBelow.evaluateResult());
    }
}
