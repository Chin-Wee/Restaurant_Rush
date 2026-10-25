package edu.ntu.sc2002;

import edu.ntu.sc2002.control.GameConfig;
import edu.ntu.sc2002.control.GameController;
import edu.ntu.sc2002.ui.ConsoleUI;

public final class App {
    private App() {}

    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: java -cp target/classes edu.ntu.sc2002.App [integer seed]");
            return;
        }
        final long seed;
        try {
            seed = args.length == 0 ? 2026 : Long.parseLong(args[0]);
        } catch (NumberFormatException invalid) {
            System.err.println("Seed must be a 64-bit integer.");
            return;
        }
        new ConsoleUI(new GameController(GameConfig.stageOne(seed))).run();
    }
}
