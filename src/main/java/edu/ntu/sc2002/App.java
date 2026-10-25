package edu.ntu.sc2002;

import java.util.Map;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import edu.ntu.sc2002.ui.RestaurantScreen;
import edu.ntu.sc2002.presentation.RestaurantUiState;
import edu.ntu.sc2002.ui.preview.RestaurantUiFixtures;
import edu.ntu.sc2002.ui.preview.ScenePreviewDriver;
import edu.ntu.sc2002.ui.scene.RestaurantScene;
import edu.ntu.sc2002.ui.StartMenuScreen;

public class App extends Application {
    private StartMenuScreen startMenu;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Restaurant Rush");
        Map<String, String> options = getParameters().getNamed();
        int previewStage = Integer.parseInt(options.getOrDefault("previewStage", "1"));
        RestaurantUiState preview = previewState(options, previewStage);
        int previewWidth = Integer.parseInt(options.getOrDefault("previewWidth",
                Integer.toString(RestaurantScreen.ROOM_WIDTH)));
        int previewHeight = Integer.parseInt(options.getOrDefault("previewHeight",
                Integer.toString(RestaurantScreen.ROOM_HEIGHT)));
        RestaurantScene room = new RestaurantScene();
        new ScenePreviewDriver(room);
        Scene restaurant = new Scene(new RestaurantScreen(room, preview,
                command -> System.out.println("UI command: " + command)).getRoot(),
                previewWidth, previewHeight);
        startMenu = new StartMenuScreen(() -> {
            stage.setScene(restaurant);
            stage.sizeToScene();
        });
        boolean directPreview = Boolean.parseBoolean(options.getOrDefault("previewDirect", "false"));
        stage.setScene(directPreview ? restaurant : new Scene(startMenu.getRoot(), 1280, 720));
        stage.setResizable(true);
        stage.show();
        if (!directPreview) {
            startMenu.start();
        }
    }

    @Override
    public void stop() {
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
