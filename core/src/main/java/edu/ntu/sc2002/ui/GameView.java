package edu.ntu.sc2002.ui;

import edu.ntu.sc2002.application.contract.*;
import edu.ntu.sc2002.domain.GameResult;

/** Presentation boundary. Decisions are submitted in Waiter-then-Chef order. */
public interface GameView {
    void displayTurnStart(GameSnapshot snapshot);
    /**
     * Console collects and submits decisions sequentially before returning the accepted pair.
     * JavaFX reads the staged pair without submission or blocking; it returns null until both
     * choices are ready and after resolution. Callers must not resubmit an accepted pair.
     */
    TurnChoicesDTO getManagerChoices();
    void displayError(String message);
    void displayTurnSummary(TurnResolution resolution);
    void displayEndGameResult(GameResult result);
}
