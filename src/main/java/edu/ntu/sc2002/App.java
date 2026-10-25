package edu.ntu.sc2002;

import edu.ntu.sc2002.control.GameConfig;
import edu.ntu.sc2002.control.GameController;
import edu.ntu.sc2002.entity.Restaurant;
import edu.ntu.sc2002.entity.ScoreBoard;
import edu.ntu.sc2002.ui.ConsoleUI;

public final class App {
    private App() {}
    public static void main(String[] args) {
        long seed = args.length == 0 ? 2026 : Long.parseLong(args[0]);
        GameController game = new GameController(GameConfig.stageOne(seed), new Restaurant(), new ScoreBoard());
        new ConsoleUI(game).run();
    }
}
