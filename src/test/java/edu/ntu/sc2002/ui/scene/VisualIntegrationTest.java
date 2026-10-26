package edu.ntu.sc2002.ui.scene;

import static org.junit.Assert.*;
import edu.ntu.sc2002.application.GameController;
import edu.ntu.sc2002.presentation.RestaurantUiAdapter;
import edu.ntu.sc2002.presentation.RestaurantUiState;
import edu.ntu.sc2002.presentation.RestaurantUiState.*;
import edu.ntu.sc2002.ui.RestaurantScreen;
import edu.ntu.sc2002.ui.StartMenuScreen;
import edu.ntu.sc2002.ui.preview.RestaurantUiFixtures;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import org.junit.BeforeClass;
import org.junit.Test;

/** Exercises actual JavaFX controls and scene reconciliation on the FX thread. */
public class VisualIntegrationTest {
    @BeforeClass public static void toolkit() throws Exception {
        var ready = new java.util.concurrent.CountDownLatch(1);
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.countDown(); });
        assertTrue(ready.await(20, TimeUnit.SECONDS));
    }

    private void fx(Runnable check) throws Exception {
        FutureTask<Void> task = new FutureTask<>(check, null);
        Platform.runLater(task);
        task.get(20, TimeUnit.SECONDS);
    }

    @Test public void queueSeatsAppearancesAndChairExitPathsAreMappedWithoutTextParsing() throws Exception {
        fx(() -> {
            var state = new SceneState(List.of(new SceneCustomer("C1", 0, 1, null),
                    new SceneCustomer("C2", 1, 2, null), new SceneCustomer("C3", 2, null, 1)), List.of(), null, 0, 0);
            var targets = RestaurantScene.customerTargets(state);
            assertEquals(new RestaurantScene.Tile(3, 7), targets.get("C1").tile());
            assertEquals(new RestaurantScene.Tile(8, 7), targets.get("C2").tile());
            assertEquals(new RestaurantScene.Tile(0, 5), targets.get("C3").tile());
            assertEquals("bob", targets.get("C1").person());
            assertEquals("adam", targets.get("C2").person());
            assertEquals("amelia", targets.get("C3").person());
            assertFalse(RestaurantScene.findPath(new RestaurantScene.Tile(3, 7), new RestaurantScene.Tile(0, 5)).isEmpty());
            assertFalse(RestaurantScene.findPath(new RestaurantScene.Tile(8, 7), new RestaurantScene.Tile(0, 5)).isEmpty());
        });
    }

    private static Object field(Object object, String name) {
        try {
            var field = object.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(object);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }

    @Test public void repeatedSnapshotsDoNotRestartRoutesAndNewStateReplacesOldActorsAndDishes() throws Exception {
        fx(() -> {
            RestaurantScene room = new RestaurantScene();
            try {
                var seated = new SceneState(List.of(new SceneCustomer("C1", 0, 1, null)), List.of(1), 1, 1, 1);
                room.applyState(seated);
                Map<?, ?> actors = (Map<?, ?>) field(room, "actors");
                Object customer = actors.get("C1");
                Object route = field(customer, "path");
                room.applyState(seated);
                assertSame(route, field(customer, "path"));
                assertTrue(((boolean[]) field(room, "dishesPlaced"))[0]);
                assertEquals(new RestaurantScene.Tile(16, 3), field(actors.get("chef"), "destination"));
                room.applyState(new SceneState(List.of(new SceneCustomer("C1", 0, 2, null)), List.of(), 2, 2, 1));
                assertEquals(new RestaurantScene.Tile(8, 7), field(customer, "destination"));
                assertFalse(((boolean[]) field(room, "dishesPlaced"))[0]);
                room.applyState(SceneState.empty());
                assertFalse(actors.containsKey("C1"));
                assertEquals(3, actors.size());
            } finally { room.dispose(); }
        });
    }

    @SuppressWarnings("unchecked")
    @Test public void realControlsRefreshChoicesResolveTurnsAndKeepOverlayAtTwentyPercent() throws Exception {
        fx(() -> {
            RestaurantUiAdapter adapter = new RestaurantUiAdapter(new GameController());
            RestaurantScene room = new RestaurantScene();
            RestaurantScreen[] view = new RestaurantScreen[1];
            view[0] = new RestaurantScreen(room, adapter.start(), c -> view[0].setUiState(adapter.handle(c)));
            Stage stage = new Stage();
            try {
                stage.setScene(new Scene(view[0].getRoot(), 1280, 720));
                stage.show();
                var root = view[0].getRoot();
                root.applyCss(); root.layout();
                ComboBox<WaiterChoice> waiter = (ComboBox<WaiterChoice>) root.lookup("#waiter-actions");
                waiter.setValue(adapter.state().waiterChoices().get(0));
                view[0].setUiState(adapter.handle(new ActivateHappyHour()));
                assertEquals(waiter.getValue(), ((ComboBox<?>) root.lookup("#waiter-actions")).getValue());
                ((Button) root.lookup("#waiter-submit")).fire();
                assertEquals(UiPhase.CHEF, adapter.state().phase());
                ComboBox<ChefChoice> chef = (ComboBox<ChefChoice>) root.lookup("#chef-actions");
                chef.setValue(adapter.state().chefChoices().get(0));
                ((Button) root.lookup("#chef-submit")).fire();
                assertEquals(UiPhase.RESOLVE, adapter.state().phase());
                assertFalse(((Button) root.lookup("#resolve-turn")).isDisabled());
                ((Button) root.lookup("#resolve-turn")).fire();
                assertEquals(2, adapter.state().turn());
                for (int[] size : new int[][] {{1280, 720}, {960, 600}, {1600, 900}}) {
                    stage.setWidth(size[0]); stage.setHeight(size[1]);
                    root.resize(size[0], size[1]); root.applyCss(); root.layout();
                    assertEquals(root.getHeight() * .2, root.lookup("#gameplay-panel").getBoundsInParent().getHeight(), 1.0);
                    assertTrue(root.lookup("#waiter-submit").getBoundsInParent().getHeight() >= 28);
                    capture(root, "live-" + size[0] + "x" + size[1]);
                }
                for (int previewStage : new int[] {1, 2, 3}) {
                    RestaurantScreen preview = new RestaurantScreen(room, RestaurantUiFixtures.preview(previewStage), c -> {}, true);
                    stage.setScene(new Scene(preview.getRoot(), 1280, 720));
                    preview.getRoot().applyCss(); preview.getRoot().layout();
                    assertEquals(previewStage == 3, preview.getRoot().lookup("#happy-hour") != null);
                    capture(preview.getRoot(), "preview-stage-" + previewStage);
                }
                StartMenuScreen menu = new StartMenuScreen(() -> {});
                try {
                    stage.setScene(new Scene(menu.getRoot(), 1280, 720));
                    menu.getRoot().applyCss(); menu.getRoot().layout();
                    capture(menu.getRoot(), "main-menu");
                } finally { menu.dispose(); }
            } finally { stage.close(); room.dispose(); }
        });
    }

    private static void capture(javafx.scene.layout.Pane root, String name) {
        WritableImage image = root.snapshot(null, null);
        BufferedImage output = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < output.getHeight(); y++) for (int x = 0; x < output.getWidth(); x++)
            output.setRGB(x, y, image.getPixelReader().getArgb(x, y));
        try {
            Files.createDirectories(Path.of("target/qa"));
            ImageIO.write(output, "png", Path.of("target/qa/" + name + ".png").toFile());
        } catch (java.io.IOException error) { throw new AssertionError(error); }
    }
}
