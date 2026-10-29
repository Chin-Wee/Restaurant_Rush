package edu.ntu.sc2002;

import java.util.Map;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import edu.ntu.sc2002.application.GameConfig;
import edu.ntu.sc2002.application.TurnController;
import edu.ntu.sc2002.presentation.JavaFXView;
import edu.ntu.sc2002.ui.StartMenuScreen;

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
        Runnable openRestaurant = () -> {
            gameView = new JavaFXView(stage, new TurnController(GameConfig.stageOne(selectedSeed)));
            ((javafx.scene.layout.Region) stage.getScene().getRoot()).setPrefSize(
                    Integer.parseInt(options.getOrDefault("width", "1280")),
                    Integer.parseInt(options.getOrDefault("height", "720")));
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
        if (gameView != null) gameView.dispose();
        if (startMenu != null) {
            startMenu.dispose();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
