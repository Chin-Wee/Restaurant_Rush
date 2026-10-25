package edu.ntu.sc2002.ui;

import javafx.scene.layout.Pane;

/** Stage 1's existing blank view, kept separate from application wiring and future rules. */
public final class RestaurantScreen {
    private final Pane root = new Pane();

    public Pane getRoot() {
        return root;
    }
}
