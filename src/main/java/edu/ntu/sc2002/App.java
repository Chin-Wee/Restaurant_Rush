package edu.ntu.sc2002;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("Restaurant Rush");
        stage.setScene(new Scene(new Pane(), 800, 600));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
