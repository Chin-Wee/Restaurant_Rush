package edu.ntu.sc2002.ui;

import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;

/** Separate ground and foreground tile layers, following Stardew Valley's room layering. */
public final class RestaurantScreen {
    private static final int TILE_SIZE = 64;
    private static final int COLUMNS = 1280 / TILE_SIZE;
    private static final int ROWS = 720 / TILE_SIZE;
    private static final int FLOOR_TOP_ROW = 2;
    private static final int PATCH_COLUMNS = 6;
    private static final int PATCH_ROWS = 3;
    private static final int DINING_ROW = 7;
    private static final int[] DINING_TABLE_COLUMNS = {4, 9};
    private static final int KITCHEN_LEFT_COLUMN = COLUMNS - PATCH_COLUMNS;
    private static final int KITCHEN_BOTTOM_ROW = FLOOR_TOP_ROW + PATCH_ROWS;
    public static final int ROOM_WIDTH = COLUMNS * TILE_SIZE;
    public static final int ROOM_HEIGHT = ROWS * TILE_SIZE;
    private static final int KITCHEN_SOURCE_ROW = 10;
    private static final String TILE_PATH = "/Modern tiles_Free/Interiors_free/16x16/split-16x16/Room_Builder_free_16x16/";
    private static final String INTERIORS_TILE_PATH = "/Modern tiles_Free/Interiors_free/16x16/split-16x16/Interiors_free_16x16/";
    private static final String KITCHEN_TILE_PATH = "/Kitchen and more tileset [16x16]/split-16x16/tileset/";

    private final Pane root = new Pane();
    private final Canvas floorLayer = new Canvas();
    private final Canvas actorLayer = new Canvas();
    private final Canvas furnitureLayer = new Canvas();
    private final Image wood = loadTile("c04_r13.png");
    private final Image trim = loadTile("c00_r12.png");
    private final Image gray = loadTile("c05_r17.png");
    private final Image kitchenLeftEdge = loadInteriorsTile("c00_r07.png");
    private final Image kitchenBottomLeftCorner = loadInteriorsTile("c00_r08.png");
    private final Image kitchenBottomEdge = loadInteriorsTile("c01_r08.png");
    private final Image kitchenBottomInsideTile = loadInteriorsTile("c03_r09.png");
    private final Image kitchenAboveBottomInsideTile = loadInteriorsTile("c03_r08.png");
    private final Image tileRightOfComputer = loadInteriorsTile("c03_r05.png");
    private final Image diningTable = loadInteriorsTile("c06_r13.png");
    private final Image diningChairTop = loadInteriorsTile("c06_r21.png");
    private final Image diningChairBottom = loadInteriorsTile("c06_r22.png");
    private final Image[][] kitchenChunk = loadKitchenChunk();
    private final Actor[] actors = {
            new Actor(loadFrames("adam", "idle", "up", 6), 17, 4),
            new Actor(loadFrames("alex", "idle", "left", 6), 12, 7),
            new Actor(loadFrames("amelia", "idle", "down", 6), 14, 4),
            new Actor(loadFrames("bob", "sit3", "down", 3), 3, 6)
    };
    private final AnimationTimer actorAnimation = new AnimationTimer() {
        @Override
        public void handle(long now) {
            drawActors(now);
        }
    };

    public RestaurantScreen() {
        root.setStyle("-fx-background-color: #252B3D;");
        floorLayer.setId("floor-layer");
        actorLayer.setId("actor-layer");
        furnitureLayer.setId("furniture-layer");
        floorLayer.setMouseTransparent(true);
        actorLayer.setMouseTransparent(true);
        furnitureLayer.setMouseTransparent(true);
        floorLayer.widthProperty().bind(root.widthProperty());
        floorLayer.heightProperty().bind(root.heightProperty());
        actorLayer.widthProperty().bind(root.widthProperty());
        actorLayer.heightProperty().bind(root.heightProperty());
        furnitureLayer.widthProperty().bind(root.widthProperty());
        furnitureLayer.heightProperty().bind(root.heightProperty());
        root.getChildren().addAll(floorLayer, actorLayer, furnitureLayer);
        root.widthProperty().addListener((observable, oldValue, newValue) -> drawLayers());
        root.heightProperty().addListener((observable, oldValue, newValue) -> drawLayers());
        root.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                actorAnimation.stop();
            } else {
                actorAnimation.start();
            }
        });
    }

    public Pane getRoot() {
        return root;
    }

    private static Image loadTile(String filename) {
        return new Image(Objects.requireNonNull(RestaurantScreen.class.getResource(TILE_PATH + filename),
                "Missing tile: " + filename).toExternalForm(), false);
    }

    private static Image loadInteriorsTile(String filename) {
        return new Image(Objects.requireNonNull(RestaurantScreen.class.getResource(INTERIORS_TILE_PATH + filename),
                "Missing tile: " + filename).toExternalForm(), false);
    }

    private static Image[][] loadKitchenChunk() {
        Image[][] tiles = new Image[PATCH_ROWS][PATCH_COLUMNS];
        for (int row = 0; row < PATCH_ROWS; row++) {
            for (int column = 0; column < PATCH_COLUMNS; column++) {
                String filename = String.format("c%02d_r%02d.png", column, KITCHEN_SOURCE_ROW + row);
                URL resource = RestaurantScreen.class.getResource(KITCHEN_TILE_PATH + filename);
                if (resource != null) {
                    tiles[row][column] = new Image(resource.toExternalForm(), false);
                }
            }
        }
        return tiles;
    }

    private void drawLayers() {
        drawFloor();
        drawActors(System.nanoTime());
        drawFurniture();
    }

    private static Image[] loadFrames(String person, String action, String direction, int count) {
        Image[] frames = new Image[count];
        String path = "/characters/" + person + "/" + action + "/" + direction + "/frame-%02d.png";
        for (int frame = 0; frame < count; frame++) {
            URL resource = RestaurantScreen.class.getResource(String.format(path, frame));
            frames[frame] = new Image(Objects.requireNonNull(resource, "Missing character frame: " + path)
                    .toExternalForm(), false);
        }
        return frames;
    }

    private void drawActors(long now) {
        drawLayer(actorLayer, graphics -> {
            for (Actor actor : actors) {
                Image frame = actor.frames[(int) ((now / 400_000_000L) % actor.frames.length)];
                graphics.drawImage(frame, actor.column * TILE_SIZE, (actor.row - 1) * TILE_SIZE,
                        TILE_SIZE, TILE_SIZE * 2);
            }
        });
    }

    private void drawFloor() {
        drawLayer(floorLayer, graphics -> {
            for (int row = FLOOR_TOP_ROW; row < ROWS; row++) {
                for (int column = 0; column < COLUMNS; column++) {
                    Image tile = row < FLOOR_TOP_ROW + PATCH_ROWS
                            && column >= KITCHEN_LEFT_COLUMN
                            && column < KITCHEN_LEFT_COLUMN + PATCH_COLUMNS ? gray : wood;
                    drawTile(graphics, tile, column, row);
                }
            }
        });
    }

    private void drawFurniture() {
        drawLayer(furnitureLayer, graphics -> {
            for (int column = 0; column < KITCHEN_LEFT_COLUMN; column++) {
                drawTile(graphics, trim, column, FLOOR_TOP_ROW);
            }
            int borderColumn = KITCHEN_LEFT_COLUMN - 1;
            for (int row = FLOOR_TOP_ROW + 1; row < KITCHEN_BOTTOM_ROW; row++) {
                drawTile(graphics, kitchenLeftEdge, borderColumn, row);
            }
            drawTile(graphics, kitchenBottomLeftCorner, borderColumn, KITCHEN_BOTTOM_ROW);
            drawTile(graphics, kitchenAboveBottomInsideTile, KITCHEN_LEFT_COLUMN, KITCHEN_BOTTOM_ROW - 1);
            drawTile(graphics, kitchenBottomInsideTile, KITCHEN_LEFT_COLUMN, KITCHEN_BOTTOM_ROW);
            for (int column = KITCHEN_LEFT_COLUMN + 1; column < COLUMNS - 1; column++) {
                drawTile(graphics, tileRightOfComputer, column, KITCHEN_BOTTOM_ROW);
            }
            drawTile(graphics, kitchenBottomEdge, COLUMNS - 1, KITCHEN_BOTTOM_ROW);
            for (int row = 0; row < PATCH_ROWS; row++) {
                int destinationRow = FLOOR_TOP_ROW + row - PATCH_ROWS + 1;
                for (int column = 0; column < PATCH_COLUMNS; column++) {
                    Image tile = kitchenChunk[row][column];
                    if (tile != null) {
                        int destinationColumn = KITCHEN_LEFT_COLUMN + column;
                        drawTile(graphics, tile, destinationColumn, destinationRow);
                    }
                }
            }
            for (int tableColumn : DINING_TABLE_COLUMNS) {
                int chairColumn = tableColumn - 1;
                drawTile(graphics, diningChairTop, chairColumn, DINING_ROW - 1);
                drawTile(graphics, diningChairBottom, chairColumn, DINING_ROW);
                drawTile(graphics, diningTable, tableColumn, DINING_ROW);
            }
        });
    }

    private void drawLayer(Canvas layer, Consumer<GraphicsContext> drawTiles) {
        double width = layer.getWidth();
        double height = layer.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        double mapWidth = COLUMNS * TILE_SIZE;
        double mapHeight = ROWS * TILE_SIZE;
        double scale = Math.min(width / mapWidth, height / mapHeight);
        GraphicsContext graphics = layer.getGraphicsContext2D();
        graphics.clearRect(0, 0, width, height);
        graphics.save();
        graphics.setImageSmoothing(false);
        graphics.translate((width - mapWidth * scale) / 2, (height - mapHeight * scale) / 2);
        graphics.scale(scale, scale);
        drawTiles.accept(graphics);
        graphics.restore();
    }

    private static void drawTile(GraphicsContext graphics, Image tile, int column, int row) {
        graphics.drawImage(tile, column * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
    }

    private record Actor(Image[] frames, int column, int row) {}
}
