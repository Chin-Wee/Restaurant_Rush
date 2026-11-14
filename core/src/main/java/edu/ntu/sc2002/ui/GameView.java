package edu.ntu.sc2002.ui;

import edu.ntu.sc2002.application.contract.*;
import edu.ntu.sc2002.domain.GameResult;

/** Presentation boundary. Decisions are submitted in Waiter-then-Chef order. */
public interface GameView {
    /**
     * Renders the start-of-turn state.
     *
     * @param snapshot immutable engine snapshot
     */
    void displayTurnStart(GameSnapshot snapshot);

    /**
     * Offers the manager's decisions for the current turn.
     * Console collects and submits decisions sequentially before returning the accepted pair.
     * JavaFX reads the staged pair without submission or blocking; it returns null until both
     * choices are ready and after resolution. Callers must not resubmit an accepted pair.
     *
     * @return accepted choices, or null while the JavaFX view is still collecting them
     */
    TurnChoicesDTO getManagerChoices();

    /**
     * Shows a rejected-command explanation.
     *
     * @param message validation message
     */
    void displayError(String message);

    /**
     * Renders the end-of-turn summary.
     *
     * @param resolution result of the submitted commands
     */
    void displayTurnSummary(TurnResolution resolution);

    /**
     * Renders the final game result.
     *
     * @param result final victory or defeat
     */
    void displayEndGameResult(GameResult result);
}
