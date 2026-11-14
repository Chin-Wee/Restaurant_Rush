package edu.ntu.sc2002;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import edu.ntu.sc2002.application.GameConfig;
import edu.ntu.sc2002.application.TurnController;
import edu.ntu.sc2002.domain.customer.CustomerType;
import edu.ntu.sc2002.presentation.JavaFXView;
import edu.ntu.sc2002.ui.StartMenuScreen;

/** JavaFX application entry point that selects difficulty and opens the start menu or game screen. */
public class App extends Application {
    private StartMenuScreen startMenu;
    private JavaFXView gameView;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Restaurant Rush");
        Map<String, String> options = getParameters().getNamed();
        boolean direct = Boolean.parseBoolean(options.getOrDefault("gameDirect", "false"));
        long seed;
        try {
            seed = Long.parseLong(options.getOrDefault("seed", "2026"));
        } catch (NumberFormatException invalid) {
            System.err.println("Seed must be a 64-bit integer.");
            javafx.application.Platform.exit();
            return;
        }
        final long selectedSeed = seed;
        Consumer<String> openRestaurant = difficulty -> {
            String normalized = normalizeDifficulty(difficulty);
            stage.setTitle("Restaurant Rush — "
                    + normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1));
            gameView = new JavaFXView(stage, new TurnController(configFor(normalized, selectedSeed)));
            ((javafx.scene.layout.Region) stage.getScene().getRoot()).setPrefSize(
                    Integer.parseInt(options.getOrDefault("width", "1280")),
                    Integer.parseInt(options.getOrDefault("height", "720")));
            stage.sizeToScene();
        };
        if (direct) {
            openRestaurant.accept(normalizeDifficulty(options.getOrDefault("difficulty", "easy")));
        } else {
            startMenu = new StartMenuScreen(openRestaurant);
            stage.setScene(new Scene(startMenu.getRoot(), 1280, 720));
        }
        stage.setResizable(true);
        stage.show();
        if (!direct) {
            startMenu.start();
        }
    }

    @Override
    public void stop() {
        if (gameView != null) gameView.dispose();
        if (startMenu != null) {
            startMenu.dispose();
        }
    }

    /** Easy keeps the original 18-turn arrival schedule and customer mix. */
    private static GameConfig configFor(String difficulty, long seed) {
        return switch (normalizeDifficulty(difficulty)) {
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

    private static String normalizeDifficulty(String difficulty) {
        String normalized = difficulty == null ? "easy" : difficulty.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "medium", "hard" -> normalized;
            default -> "easy";
        };
    }

    /**
     * JavaFX launcher.
     *
     * @param args JavaFX named parameters such as {@code --seed}, {@code --difficulty} and
     *             {@code --gameDirect}
     */
    public static void main(String[] args) {
        launch(args);
    }

}
