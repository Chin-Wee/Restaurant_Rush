package edu.ntu.sc2002;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import edu.ntu.sc2002.ui.RestaurantScreen;
import edu.ntu.sc2002.ui.StartMenuScreen;

public class App extends Application {
    private StartMenuScreen startMenu;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Restaurant Rush");
        Scene restaurant = new Scene(new RestaurantScreen().getRoot(),
                RestaurantScreen.ROOM_WIDTH, RestaurantScreen.ROOM_HEIGHT);
        startMenu = new StartMenuScreen(() -> {
            stage.setScene(restaurant);
            stage.sizeToScene();
        });
        stage.setScene(new Scene(startMenu.getRoot(), 1280, 720));
        stage.setResizable(true);
        stage.show();
        startMenu.start();
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
}
