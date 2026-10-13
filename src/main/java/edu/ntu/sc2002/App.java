package edu.ntu.sc2002;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import edu.ntu.sc2002.ui.StartMenuScreen;

public class App extends Application {
    private StartMenuScreen startMenu;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Restaurant Rush");
        Scene placeholder = new Scene(new Pane(), 1280, 720);
        startMenu = new StartMenuScreen(() -> stage.setScene(placeholder));
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
