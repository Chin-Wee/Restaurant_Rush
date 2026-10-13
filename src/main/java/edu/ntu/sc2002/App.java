package edu.ntu.sc2002;

import javafx.application.Application;
import javafx.scene.Scene;
import edu.ntu.sc2002.ui.RestaurantScreen;
import javafx.stage.Stage;
import edu.ntu.sc2002.ui.StartMenuScreen;

public class App extends Application {
    private StartMenuScreen startMenu;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Restaurant Rush");
        stage.setScene(new Scene(new RestaurantScreen().getRoot(), 800, 600));
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
