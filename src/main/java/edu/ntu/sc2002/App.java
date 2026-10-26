package edu.ntu.sc2002;

import java.util.Map;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import edu.ntu.sc2002.ui.RestaurantScreen;
import edu.ntu.sc2002.application.GameConfig;
import edu.ntu.sc2002.application.GameController;
import edu.ntu.sc2002.presentation.RestaurantUiAdapter;
import edu.ntu.sc2002.presentation.RestaurantUiState;
import edu.ntu.sc2002.ui.preview.RestaurantUiFixtures;
import edu.ntu.sc2002.ui.preview.ScenePreviewDriver;
import edu.ntu.sc2002.ui.scene.RestaurantScene;
import edu.ntu.sc2002.ui.StartMenuScreen;

public class App extends Application {
    private StartMenuScreen startMenu;
    private RestaurantScreen restaurantScreen;
    private RestaurantScene room;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Restaurant Rush");
        Map<String, String> options = getParameters().getNamed();
        boolean preview = options.keySet().stream().anyMatch(key -> key.startsWith("preview")
                && !key.equals("previewWidth") && !key.equals("previewHeight"));
        boolean direct = preview && Boolean.parseBoolean(options.getOrDefault("previewDirect", "false"))
                || Boolean.parseBoolean(options.getOrDefault("gameDirect", "false"));
        long seed;
        try {
            seed = Long.parseLong(options.getOrDefault("seed", "2026"));
        } catch (NumberFormatException invalid) {
            System.err.println("Seed must be a 64-bit integer.");
            javafx.application.Platform.exit();
            return;
        }
        final long selectedSeed = seed;
        Runnable openRestaurant = () -> {
            room = new RestaurantScene();
            if (preview) {
                new ScenePreviewDriver(room);
                RestaurantUiState state = previewState(options, Integer.parseInt(options.getOrDefault("previewStage", "1")));
                restaurantScreen = new RestaurantScreen(room, state,
                        command -> System.out.println("UI command: " + command), true);
            } else {
                RestaurantUiAdapter adapter = new RestaurantUiAdapter(new GameController(GameConfig.stageOne(selectedSeed)));
                restaurantScreen = new RestaurantScreen(room, adapter.start(),
                        command -> restaurantScreen.setUiState(adapter.handle(command)));
            }
            stage.setScene(new Scene(restaurantScreen.getRoot(),
                    Integer.parseInt(options.getOrDefault("previewWidth", "1280")),
                    Integer.parseInt(options.getOrDefault("previewHeight", "720"))));
            stage.sizeToScene();
        };
        if (direct) {
            openRestaurant.run();
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
        if (room != null) room.dispose();
        if (startMenu != null) {
            startMenu.dispose();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static RestaurantUiState previewState(Map<String, String> options, int stage) {
        if (Boolean.parseBoolean(options.getOrDefault("previewResult", "false"))) {
            return RestaurantUiFixtures.previewResult(stage,
                    Boolean.parseBoolean(options.getOrDefault("previewVictory", "true")));
        }
        if (Boolean.parseBoolean(options.getOrDefault("previewRejected", "false"))) {
            return RestaurantUiFixtures.previewRejectedChoice(stage);
        }
        if (Boolean.parseBoolean(options.getOrDefault("previewReadyToResolve", "false"))) {
            return RestaurantUiFixtures.previewReadyToResolve(stage);
        }
        if (Boolean.parseBoolean(options.getOrDefault("previewHappyHourActive", "false"))) {
            return RestaurantUiFixtures.previewHappyHourActive();
        }
        if (Boolean.parseBoolean(options.getOrDefault("previewAfterWaiter", "false"))) {
            return RestaurantUiFixtures.previewAfterWaiter(stage);
        }
        return RestaurantUiFixtures.preview(stage);
    }
}
