package edu.ntu.sc2002.ui.preview;

import edu.ntu.sc2002.ui.scene.RestaurantScene;

/** Scene testing only: scripted seating, dish placement and random wandering. */
public final class ScenePreviewDriver {
    private static final int[][] CHEF_STOPS = {{16, 3}, {15, 4}, {16, 4}, {17, 4}, {18, 4}};
    private final RestaurantScene scene;
    private int nextStop;
    private boolean chefWasMoving;
    private double pause;

    public ScenePreviewDriver(RestaurantScene scene) {
        this.scene = scene;
        scene.moveTo("customer", 3, 7, true);
        scene.setAnimationStep(this::advance);
    }

    private void advance(double delta) {
        boolean moving = scene.isMoving("chef");
        if (chefWasMoving && !moving) {
            if (nextStop > 1) { scene.showDish(nextStop - 2); }
            pause = 0.6;
        }
        if (!moving) {
            if (pause > 0) {
                pause = Math.max(0, pause - delta);
            } else if (nextStop < CHEF_STOPS.length) {
                int[] stop = CHEF_STOPS[nextStop++];
                scene.moveTo("chef", stop[0], stop[1], false);
            } else {
                scene.wander("chef");
            }
        }
        chefWasMoving = scene.isMoving("chef");
        scene.wander("waiter");
    }
}
