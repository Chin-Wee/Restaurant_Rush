package edu.ntu.sc2002;

import edu.ntu.sc2002.application.GameConfig;
import edu.ntu.sc2002.application.TurnController;
import edu.ntu.sc2002.domain.customer.CustomerType;
import edu.ntu.sc2002.ui.console.ConsoleUI;
import java.util.List;
import java.util.Locale;

/** Command-line entry point that parses the seed/difficulty and runs the console game loop. */
public final class ConsoleApp {
    private ConsoleApp() {}

    /**
     * Launches the Stage 3 console game.
     *
     * @param args optional {@code [seed] [easy|medium|hard]}; the seed defaults to 2026 and the
     *             difficulty defaults to easy
     */
    public static void main(String[] args) {
        if (args.length > 2) {
            System.err.println("Usage: java -cp core/target/classes edu.ntu.sc2002.ConsoleApp [integer seed] [easy|medium|hard]");
            return;
        }
        final long seed;
        try {
            seed = args.length == 0 ? 2026 : Long.parseLong(args[0]);
        } catch (NumberFormatException invalid) {
            System.err.println("Seed must be a 64-bit integer.");
            return;
        }
        String difficulty = args.length == 2 ? args[1].toLowerCase(Locale.ROOT) : "easy";
        if (!List.of("easy", "medium", "hard").contains(difficulty)) {
            System.err.println("Difficulty must be easy, medium or hard.");
            return;
        }
        System.out.println("Restaurant Rush — Stage 3 (seed " + seed + ", " + difficulty + ")");
        new ConsoleUI(new java.util.Scanner(System.in), new TurnController(configFor(difficulty, seed))).run();
    }

    /** Easy keeps the original 18-turn arrival schedule and customer mix. */
    private static GameConfig configFor(String difficulty, long seed) {
        return switch (difficulty) {
            case "medium" -> new GameConfig(16, 2, List.of(1, 2, 4, 6, 8, 10, 12, 14), seed,
                    List.of(CustomerType.REGULAR, CustomerType.VIP, CustomerType.REGULAR,
                            CustomerType.CRITIC, CustomerType.REGULAR, CustomerType.CRITIC,
                            CustomerType.VIP, CustomerType.REGULAR));
            case "hard" -> new GameConfig(14, 2, List.of(1, 2, 3, 5, 6, 8, 9, 11, 12), seed,
                    List.of(CustomerType.REGULAR, CustomerType.CRITIC, CustomerType.VIP,
                            CustomerType.CRITIC, CustomerType.REGULAR, CustomerType.CRITIC,
                            CustomerType.VIP, CustomerType.CRITIC, CustomerType.REGULAR));
            default -> GameConfig.stageTwo(seed);
        };
    }
}
