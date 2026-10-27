package edu.ntu.sc2002.application;

import edu.ntu.sc2002.domain.GameResult;

/** Compatibility entry point for the existing CLI and visualization clients. */
public final class GameController extends TurnController {
    public GameController() { super(); }
    public GameController(GameConfig config) { super(config); }

    /** Existing clients use null until a final result is available. */
    @Override public GameResult result() {
        GameResult result = super.result();
        return result == GameResult.IN_PROGRESS ? null : result;
    }
}
