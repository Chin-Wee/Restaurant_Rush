package edu.ntu.sc2002.ui;

import edu.ntu.sc2002.application.contract.*;
import edu.ntu.sc2002.domain.GameResult;

/** Presentation boundary. Decisions are submitted in Waiter-then-Chef order. */
public interface GameView {
    void displayTurnStart(GameSnapshot snapshot);
    /** Console collects decisions sequentially; JavaFX returns the current staged pair without blocking. */
    TurnChoicesDTO getManagerChoices();
    void displayError(String message);
    void displayTurnSummary(TurnResolution resolution);
    void displayEndGameResult(GameResult result);
}
