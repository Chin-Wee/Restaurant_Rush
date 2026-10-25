package edu.ntu.sc2002.ui;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;

import edu.ntu.sc2002.ui.RestaurantUiState.ActivateHappyHour;
import edu.ntu.sc2002.ui.RestaurantUiState.ChefChoice;
import edu.ntu.sc2002.ui.RestaurantUiState.ChefCommand;
import edu.ntu.sc2002.ui.RestaurantUiState.CustomerSummary;
import edu.ntu.sc2002.ui.RestaurantUiState.GameResult;
import edu.ntu.sc2002.ui.RestaurantUiState.OrderSummary;
import edu.ntu.sc2002.ui.RestaurantUiState.ResolveTurn;
import edu.ntu.sc2002.ui.RestaurantUiState.TableSummary;
import edu.ntu.sc2002.ui.RestaurantUiState.WaiterChoice;
import edu.ntu.sc2002.ui.RestaurantUiState.WaiterCommand;

/** Stardew Valley-style ground and foreground layers, with a transparent overlay for UI text. */
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
    private static final Set<String> CUSTOMER_PEOPLE = Set.of("adam", "amelia", "bob");
    private static final double WALK_TILES_PER_SECOND = 2.8;
    private static final double CHEF_STOP_SECONDS = 0.6;
    private static final Font ACTOR_TAG_FONT = Font.font("System", FontWeight.BOLD, 13);
    private static final Image ACTION_BUTTON_ART = new Image(Objects.requireNonNull(
            RestaurantScreen.class.getResource("/ui/panel_brown_damaged_dark.png"), "Missing UI button art")
            .toExternalForm(), false);
    private static final String SMALL_TEXT_STYLE = "-fx-text-fill: #f4eddb; -fx-font-size: 10px;";
    private static final long WALK_FRAME_NANOS = 120_000_000L;
    private static final long IDLE_FRAME_NANOS = 400_000_000L;
    private static final Tile CUSTOMER_SPAWN = new Tile(0, 5);
    private static final Tile CUSTOMER_CHAIR = new Tile(3, 7);
    private static final Tile STOVE_APPROACH = new Tile(16, 3);
    private static final Tile[] DISH_SPOTS = {
            new Tile(15, 4), new Tile(16, 4), new Tile(17, 4), new Tile(18, 4)
    };
    private static final Tile[] CHEF_STOPS = {
            STOVE_APPROACH, DISH_SPOTS[0], DISH_SPOTS[1], DISH_SPOTS[2], DISH_SPOTS[3]
    };
    private static final String TILE_PATH = "/Modern tiles_Free/Interiors_free/16x16/split-16x16/Room_Builder_free_16x16/";
    private static final String INTERIORS_TILE_PATH = "/Modern tiles_Free/Interiors_free/16x16/split-16x16/Interiors_free_16x16/";
    private static final String KITCHEN_TILE_PATH = "/Kitchen and more tileset [16x16]/split-16x16/tileset/";

    private final Pane root = new StackPane();
    private final Random random = new Random();
    private final Canvas floorLayer = new Canvas();
    private final Canvas actorLayer = new Canvas();
    private final Canvas furnitureLayer = new Canvas();
    private final Canvas uiLayer = new Canvas();
    private final AnchorPane overlay = new AnchorPane();
    private final VBox gameplayPanel = new VBox(5);
    private final HBox topHud = new HBox(18);
    private final Consumer<RestaurantUiState.Command> commandSink;
    private RestaurantUiState uiState;
    private ComboBox<WaiterChoice> waiterChoices;
    private ComboBox<ChefChoice> chefChoices;
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
    private static final boolean[][] MOVEMENT_AREA = createMovementArea();
    private final Actor chef = new Actor(new Tile(17, 4), loadDirectionalFrames("adam", "idle", 6),
            loadDirectionalFrames("adam", "run", 6), null, Direction.UP);
    private final Actor waiter = new Actor(new Tile(12, 7), loadDirectionalFrames("alex", "idle", 6),
            loadDirectionalFrames("alex", "run", 6), null, Direction.LEFT);
    private final Actor cashier = new Actor(new Tile(14, 4), loadSingleDirection("amelia", "idle", "down", 6),
            null, null, Direction.DOWN);
    private final Actor firstCustomer = customer("bob", CUSTOMER_SPAWN);
    private final Actor[] actors = {chef, waiter, cashier, firstCustomer};
    private final boolean[] dishesPlaced = new boolean[DISH_SPOTS.length];
    private int chefStopIndex;
    private double chefPauseSeconds;
    private long previousAnimationNanos;
    private final AnimationTimer actorAnimation = new AnimationTimer() {
        @Override
        public void handle(long now) {
            double delta = previousAnimationNanos == 0 ? 0
                    : Math.min((now - previousAnimationNanos) / 1_000_000_000.0, 0.1);
            previousAnimationNanos = now;
            updateMovement(delta);
            drawActors(now);
            drawUi();
        }
    };

    public RestaurantScreen() {
        this(RestaurantUiState.preview(1), command -> {});
    }

    public RestaurantScreen(RestaurantUiState initialState, Consumer<RestaurantUiState.Command> commandSink) {
        this.commandSink = Objects.requireNonNull(commandSink);
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
        uiLayer.widthProperty().bind(root.widthProperty());
        uiLayer.heightProperty().bind(root.heightProperty());
        uiLayer.setMouseTransparent(true);
        root.getChildren().addAll(floorLayer, actorLayer, furnitureLayer, uiLayer, overlay);

        overlay.setPickOnBounds(false);
        overlay.setStyle("-fx-background-color: transparent;");
        gameplayPanel.setPadding(new Insets(7, 9, 7, 9));
        gameplayPanel.setStyle("-fx-background-color: rgba(30, 34, 48, 0.96);"
                + "-fx-border-color: #b98753; -fx-border-width: 2 0 0 0;");
        gameplayPanel.minHeightProperty().bind(root.heightProperty().multiply(0.20));
        gameplayPanel.prefHeightProperty().bind(root.heightProperty().multiply(0.20));
        gameplayPanel.maxHeightProperty().bind(root.heightProperty().multiply(0.20));
        AnchorPane.setLeftAnchor(gameplayPanel, 0.0);
        AnchorPane.setRightAnchor(gameplayPanel, 0.0);
        AnchorPane.setBottomAnchor(gameplayPanel, 0.0);

        topHud.setPadding(new Insets(9, 12, 9, 12));
        topHud.setAlignment(Pos.CENTER_LEFT);
        topHud.setStyle("-fx-background-color: rgba(37, 43, 61, 0.94);"
                + "-fx-border-color: #b98753; -fx-border-width: 1;");
        AnchorPane.setLeftAnchor(topHud, 14.0);
        AnchorPane.setTopAnchor(topHud, 14.0);
        overlay.getChildren().addAll(gameplayPanel, topHud);

        root.widthProperty().addListener((observable, oldValue, newValue) -> drawLayers());
        root.heightProperty().addListener((observable, oldValue, newValue) -> drawLayers());
        root.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                actorAnimation.stop();
                previousAnimationNanos = 0;
            } else {
                actorAnimation.start();
            }
        });
        setUiState(initialState);
    }

    public Pane getRoot() {
        return root;
    }

    /** Apply the controller's next immutable snapshot after a command is handled. */
    public void setUiState(RestaurantUiState nextState) {
        WaiterChoice previousWaiterChoice = waiterChoices == null ? null : waiterChoices.getValue();
        ChefChoice previousChefChoice = chefChoices == null ? null : chefChoices.getValue();
        uiState = Objects.requireNonNull(nextState);
        renderTopHud();
        renderGameplayPanel(previousWaiterChoice, previousChefChoice);
    }

    private void renderTopHud() {
        topHud.getChildren().setAll(
                hudText("UI PREVIEW"),
                hudText("STAGE " + uiState.stage()),
                hudText("TURN " + uiState.turn()),
                hudText("PHASE " + uiState.phase()),
                hudText("REVENUE " + formatMoney(uiState.revenue())),
                hudText("AVG PAID SAT " + formatSatisfaction(uiState.averagePaidSatisfaction())));
    }

    private static Label hudText(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #fff5dc; -fx-font-size: 12px; -fx-font-weight: bold;");
        return label;
    }

    private void renderGameplayPanel(WaiterChoice previousWaiterChoice, ChefChoice previousChefChoice) {
        gameplayPanel.getChildren().clear();
        if (uiState.result() != null) {
            gameplayPanel.getChildren().add(resultView(uiState.result()));
            return;
        }

        if (uiState.feedback() != null) {
            Label feedback = new Label(uiState.feedback().message());
            feedback.setWrapText(true);
            feedback.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: "
                    + (uiState.feedback().rejected() ? "#ffb4a6;" : "#b9f1c7;"));
            gameplayPanel.getChildren().add(feedback);
        }

        GridPane cards = new GridPane();
        cards.setHgap(7);
        cards.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        double[] widths = {34, 22, 22, 22};
        for (double width : widths) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(width);
            column.setHgrow(Priority.ALWAYS);
            column.setFillWidth(true);
            cards.getColumnConstraints().add(column);
        }
        cards.add(customerCard(), 0, 0);
        cards.add(tableAndOrderCard(), 1, 0);
        cards.add(waiterCard(previousWaiterChoice), 2, 0);
        cards.add(chefCard(previousChefChoice), 3, 0);
        VBox.setVgrow(cards, Priority.ALWAYS);
        gameplayPanel.getChildren().add(cards);
    }

    private Node customerCard() {
        VBox rows = new VBox(4);
        for (CustomerSummary customer : uiState.customers()) {
            String type = uiState.stage() >= 2 && customer.type() != null ? " · " + customer.type() : "";
            Label identity = smallText(customer.name() + type + " · " + customer.location()
                    + " · " + customer.satisfaction() + "/100");
            Label order = smallText((customer.foodItem() == null ? "No food chosen" : customer.foodItem())
                    + " · " + customer.orderStatus() + " · Cashier: " + customer.paymentStatus());
            rows.getChildren().addAll(identity, order);
        }
        if (uiState.customers().isEmpty()) {
            rows.getChildren().add(smallText("No customers"));
        }
        return card("CUSTOMERS / QUEUE", scrollable(rows));
    }

    private Node tableAndOrderCard() {
        VBox rows = new VBox(3);
        if (uiState.tables().isEmpty()) {
            rows.getChildren().add(smallText("No tables"));
        } else {
            for (TableSummary table : uiState.tables()) {
                String occupant = table.customerName() == null ? "" : " · " + table.customerName();
                rows.getChildren().add(smallText(table.name() + " · " + table.status() + occupant));
            }
        }
        if (uiState.orders().isEmpty()) {
            rows.getChildren().add(smallText("No active orders"));
        } else {
            for (OrderSummary order : uiState.orders()) {
                String combo = uiState.stage() == 3 && order.comboMeal() ? "COMBO · " : "";
                rows.getChildren().add(smallText(combo + order.customerName() + " · " + order.item()
                        + " · " + order.status()));
            }
        }
        if (uiState.stageThree() != null) {
            rows.getChildren().add(smallText("Host · " + uiState.stageThree().hostActivity()));
        }
        VBox content = new VBox(3, scrollable(rows), smallText("Cashier · payment is automatic"));
        return card("TABLES / ORDERS", content);
    }

    private Node waiterCard(WaiterChoice previousChoice) {
        VBox content = new VBox(5);
        waiterChoices = choiceBox("Choose available Waiter task", uiState.waiterChoices(),
                WaiterChoice::label);
        waiterChoices.setId("waiter-actions");
        selectIfAvailable(waiterChoices, previousChoice);
        Button submit = actionButton("Submit", 92, "Submit Waiter task");
        submit.setId("waiter-submit");
        submit.disableProperty().bind(waiterChoices.valueProperty().isNull());
        submit.setOnAction(event -> {
            WaiterChoice choice = waiterChoices.getValue();
            if (choice != null) {
                commandSink.accept(new WaiterCommand(choice));
            }
        });
        content.getChildren().addAll(waiterChoices, submit);
        return card("WAITER", content);
    }

    private Node chefCard(ChefChoice previousChoice) {
        VBox content = new VBox(5);
        chefChoices = choiceBox("Choose available Chef task", uiState.chefChoices(), ChefChoice::label);
        chefChoices.setId("chef-actions");
        selectIfAvailable(chefChoices, previousChoice);
        Button submit = actionButton("Submit", 82, "Submit Chef task");
        submit.setId("chef-submit");
        submit.disableProperty().bind(chefChoices.valueProperty().isNull());
        submit.setOnAction(event -> {
            ChefChoice choice = chefChoices.getValue();
            if (choice != null) {
                commandSink.accept(new ChefCommand(choice));
            }
        });
        FlowPane buttons = new FlowPane(5, 4);
        buttons.getChildren().add(submit);
        Button resolve = actionButton("Resolve", 82, "Resolve turn");
        resolve.setId("resolve-turn");
        resolve.setDisable(!uiState.canResolveTurn());
        resolve.setOnAction(event -> commandSink.accept(new ResolveTurn()));
        buttons.getChildren().add(resolve);
        if (uiState.stageThree() != null) {
            int remaining = uiState.stageThree().happyHourTurnsRemaining();
            if (uiState.stageThree().happyHourAvailable()) {
                Button happyHour = actionButton("Happy Hour", 96, "Activate Happy Hour");
                happyHour.setId("happy-hour");
                happyHour.setOnAction(event -> commandSink.accept(new ActivateHappyHour()));
                buttons.getChildren().add(happyHour);
            } else if (remaining > 0) {
                content.getChildren().add(smallText("Happy Hour · " + remaining + " turn"
                        + (remaining == 1 ? "" : "s") + " remaining"));
            } else {
                content.getChildren().add(smallText("Happy Hour · used"));
            }
        }
        content.getChildren().addAll(chefChoices, buttons);
        if (uiState.chefChoices().isEmpty()) {
            content.getChildren().add(smallText("Chef choices appear after Waiter submits."));
        }
        return card("CHEF / TURN", content);
    }

    private static <T> ComboBox<T> choiceBox(String prompt, List<T> choices,
            java.util.function.Function<T, String> labelFor) {
        ComboBox<T> box = new ComboBox<>();
        box.getItems().setAll(choices);
        box.setPromptText(prompt);
        box.setConverter(new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : labelFor.apply(value); }
            @Override public T fromString(String value) { return null; }
        });
        box.setMaxWidth(Double.MAX_VALUE);
        box.setPrefHeight(28);
        box.setDisable(choices.isEmpty());
        box.setStyle("-fx-font-size: 10px;");
        return box;
    }

    private static <T> void selectIfAvailable(ComboBox<T> box, T previous) {
        if (previous != null && box.getItems().contains(previous)) {
            box.setValue(previous);
        }
    }

    private static VBox card(String title, Node content) {
        Label heading = new Label(title);
        heading.setStyle("-fx-text-fill: #eab86f; -fx-font-size: 10px; -fx-font-weight: bold;");
        VBox result = new VBox(4, heading, content);
        result.setPadding(new Insets(6));
        result.setMinWidth(0);
        result.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        result.setStyle("-fx-background-color: #343a4c; -fx-border-color: #545a6b; -fx-border-width: 1;");
        VBox.setVgrow(content, Priority.ALWAYS);
        return result;
    }

    private static ScrollPane scrollable(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMinHeight(0);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private static Label sectionHeading(String text) {
        Label heading = new Label(text);
        heading.setStyle("-fx-text-fill: #d6c7a7; -fx-font-size: 9px; -fx-font-weight: bold;");
        return heading;
    }

    private static Label smallText(String text) {
        Label label = new Label(text);
        label.setStyle(SMALL_TEXT_STYLE);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private static Button actionButton(String label, double width, String accessibleText) {
        ImageView texture = new ImageView(ACTION_BUTTON_ART);
        texture.setFitWidth(width);
        texture.setFitHeight(28);
        texture.setPreserveRatio(false);
        texture.setSmooth(false);
        texture.setMouseTransparent(true);
        Label title = new Label(label);
        title.setStyle("-fx-text-fill: #fff4d7; -fx-font-size: 10px; -fx-font-weight: bold;");
        title.setMouseTransparent(true);
        StackPane graphic = new StackPane(texture, title);
        graphic.setMinSize(width, 28);
        graphic.setPrefSize(width, 28);
        graphic.setMaxSize(width, 28);
        Button button = new Button();
        button.setGraphic(graphic);
        button.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        button.setAccessibleText(accessibleText);
        button.setMinSize(width, 30);
        button.setPrefSize(width, 30);
        button.setMaxSize(width, 30);
        button.setPadding(Insets.EMPTY);
        button.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        return button;
    }

    private static Node resultView(GameResult result) {
        Label outcome = new Label(result.victory() ? "VICTORY" : "DEFEAT");
        outcome.setStyle("-fx-text-fill: " + (result.victory() ? "#b9f1c7" : "#ffb4a6")
                + "; -fx-font-size: 20px; -fx-font-weight: bold;");
        GridPane metrics = new GridPane();
        metrics.setHgap(18);
        String[][] values = {
                {"SERVED", Integer.toString(result.servedCount())},
                {"DEPARTURES", Integer.toString(result.departures())},
                {"REVENUE", formatMoney(result.revenue())},
                {"AVG PAID SAT", formatSatisfaction(result.averagePaidSatisfaction())},
                {"TURNS USED", Integer.toString(result.turnsUsed())}
        };
        for (int column = 0; column < values.length; column++) {
            VBox metric = new VBox(3, sectionHeading(values[column][0]), hudText(values[column][1]));
            metrics.add(metric, column, 0);
            ColumnConstraints constraint = new ColumnConstraints();
            constraint.setPercentWidth(20);
            constraint.setHgrow(Priority.ALWAYS);
            metrics.getColumnConstraints().add(constraint);
        }
        GridPane.setHgrow(metrics, Priority.ALWAYS);
        VBox view = new VBox(8, outcome, metrics);
        view.setAlignment(Pos.CENTER_LEFT);
        view.setPadding(new Insets(5, 10, 5, 10));
        view.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(metrics, Priority.ALWAYS);
        return view;
    }

    private static String formatMoney(double amount) {
        return String.format(Locale.ROOT, "$%.2f", amount);
    }

    private static String formatSatisfaction(java.util.OptionalDouble satisfaction) {
        return satisfaction.isPresent()
                ? String.format(Locale.ROOT, "%.1f / 100", satisfaction.getAsDouble())
                : "N/A";
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
        long now = System.nanoTime();
        drawActors(now);
        drawFurniture();
        drawUi();
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

    private static Image[][] loadSingleDirection(String person, String action, String direction, int count) {
        Image[][] frames = new Image[Direction.values().length][];
        Direction facing = Direction.fromName(direction);
        frames[facing.ordinal()] = loadFrames(person, action, direction, count);
        return frames;
    }

    private static Image[][] loadDirectionalFrames(String person, String action, int count) {
        Image[][] frames = new Image[Direction.values().length][];
        for (Direction direction : Direction.values()) {
            frames[direction.ordinal()] = loadFrames(person, action, direction.path, count);
        }
        return frames;
    }

    private static Actor customer(String person, Tile spawn) {
        if (!CUSTOMER_PEOPLE.contains(person)) {
            throw new IllegalArgumentException("Not an allowed customer: " + person);
        }
        Actor actor = new Actor(spawn, loadDirectionalFrames(person, "idle", 6),
                loadDirectionalFrames(person, "run", 6), loadFrames(person, "sit3", "right", 3), Direction.RIGHT);
        actor.follow(requirePath(spawn, CUSTOMER_CHAIR), true);
        return actor;
    }

    private static boolean[][] createMovementArea() {
        boolean[][] movement = new boolean[ROWS][COLUMNS];
        for (int row = FLOOR_TOP_ROW; row < ROWS; row++) {
            Arrays.fill(movement[row], true);
        }
        for (int column = 0; column < KITCHEN_LEFT_COLUMN; column++) {
            block(movement, column, FLOOR_TOP_ROW);
        }
        for (int row = 0; row < PATCH_ROWS; row++) {
            for (int column = KITCHEN_LEFT_COLUMN; column < COLUMNS; column++) {
                block(movement, column, row);
            }
        }
        for (int row = FLOOR_TOP_ROW + 1; row <= KITCHEN_BOTTOM_ROW; row++) {
            block(movement, KITCHEN_LEFT_COLUMN - 1, row);
        }
        block(movement, KITCHEN_LEFT_COLUMN, KITCHEN_BOTTOM_ROW - 1);
        for (int column = KITCHEN_LEFT_COLUMN; column < COLUMNS; column++) {
            block(movement, column, KITCHEN_BOTTOM_ROW);
        }
        for (int tableColumn : DINING_TABLE_COLUMNS) {
            int chairColumn = tableColumn - 1;
            block(movement, chairColumn, DINING_ROW - 1);
            block(movement, chairColumn, DINING_ROW);
            block(movement, tableColumn, DINING_ROW);
        }
        return movement;
    }

    private static void block(boolean[][] movement, int column, int row) {
        movement[row][column] = false;
    }

    private static List<Tile> requirePath(Tile start, Tile destination) {
        List<Tile> path = findPath(start, destination);
        if (path.isEmpty()) {
            throw new IllegalStateException("No movement path from " + start + " to " + destination);
        }
        return path;
    }

    // Stardew Valley's PathFindController uses four-way A* with a Manhattan heuristic for tile routes.
    private static List<Tile> findPath(Tile start, Tile destination) {
        if (!inside(start) || !inside(destination) || !MOVEMENT_AREA[start.row][start.column]) {
            return List.of();
        }
        Comparator<SearchNode> priority = Comparator.comparingInt(SearchNode::score)
                .thenComparingLong(SearchNode::order);
        PriorityQueue<SearchNode> open = new PriorityQueue<>(priority);
        Map<Tile, Integer> costs = new HashMap<>();
        Map<Tile, Tile> previous = new HashMap<>();
        Set<Tile> closed = new HashSet<>();
        long order = 0;
        costs.put(start, 0);
        open.add(new SearchNode(start, 0, manhattan(start, destination), order++));

        while (!open.isEmpty()) {
            SearchNode current = open.remove();
            if (current.cost != costs.getOrDefault(current.tile, Integer.MAX_VALUE) || !closed.add(current.tile)) {
                continue;
            }
            if (current.tile.equals(destination)) {
                return reconstructPath(start, destination, previous);
            }
            for (Direction direction : Direction.values()) {
                Tile next = new Tile(current.tile.column + direction.dx, current.tile.row + direction.dy);
                if (!inside(next) || (!MOVEMENT_AREA[next.row][next.column] && !next.equals(destination))
                        || closed.contains(next)) {
                    continue;
                }
                int nextCost = current.cost + 1;
                if (nextCost < costs.getOrDefault(next, Integer.MAX_VALUE)) {
                    costs.put(next, nextCost);
                    previous.put(next, current.tile);
                    open.add(new SearchNode(next, nextCost, nextCost + manhattan(next, destination), order++));
                }
            }
        }
        return List.of();
    }

    private static List<Tile> reconstructPath(Tile start, Tile destination, Map<Tile, Tile> previous) {
        List<Tile> path = new ArrayList<>();
        for (Tile tile = destination; tile != null; tile = previous.get(tile)) {
            path.add(tile);
            if (tile.equals(start)) {
                Collections.reverse(path);
                return path;
            }
        }
        return List.of();
    }

    private static boolean inside(Tile tile) {
        return tile.column >= 0 && tile.column < COLUMNS && tile.row >= 0 && tile.row < ROWS;
    }

    private static int manhattan(Tile left, Tile right) {
        return Math.abs(left.column - right.column) + Math.abs(left.row - right.row);
    }

    private void updateMovement(double delta) {
        firstCustomer.advance(delta);
        if (chef.isMoving()) {
            if (chef.advance(delta)) {
                int arrivedStop = chefStopIndex - 1;
                if (arrivedStop > 0) {
                    dishesPlaced[arrivedStop - 1] = true;
                    drawFurniture();
                }
                chefPauseSeconds = CHEF_STOP_SECONDS;
            }
        } else if (chefPauseSeconds > 0) {
            chefPauseSeconds = Math.max(0, chefPauseSeconds - delta);
        } else if (chefStopIndex < CHEF_STOPS.length) {
            chef.follow(requirePath(chef.tile(), CHEF_STOPS[chefStopIndex]), false);
            chefStopIndex++;
        } else {
            wander(chef);
        }
        wander(waiter);
    }

    private void wander(Actor actor) {
        if (actor.isMoving() || actor.seated) {
            return;
        }
        // ponytail: test walkers avoid furniture only; add actor occupancy when collision-aware routes matter.
        Tile start = actor.tile();
        Tile destination;
        List<Tile> path;
        do {
            destination = new Tile(random.nextInt(COLUMNS), FLOOR_TOP_ROW + random.nextInt(ROWS - FLOOR_TOP_ROW));
            path = destination.equals(start) || !MOVEMENT_AREA[destination.row][destination.column]
                    ? List.of() : findPath(start, destination);
        } while (path.isEmpty());
        actor.follow(path, false);
    }

    private void drawActors(long now) {
        drawLayer(actorLayer, graphics -> {
            drawTarget(graphics, STOVE_APPROACH, Color.web("#F5C96B"));
            for (Tile dishSpot : DISH_SPOTS) {
                drawTarget(graphics, dishSpot, Color.web("#67D1B0"));
            }
            for (Actor actor : actors) {
                graphics.drawImage(actor.frame(now), actor.column * TILE_SIZE,
                        (actor.row - 1) * TILE_SIZE, TILE_SIZE, TILE_SIZE * 2);
            }
        });
    }

    private void drawUi() {
        drawLayer(uiLayer, graphics -> {
            graphics.setFont(ACTOR_TAG_FONT);
            graphics.setTextAlign(TextAlignment.CENTER);
            drawActorTag(graphics, chef, "Adam");
            drawActorTag(graphics, waiter, "Alex");
            drawActorTag(graphics, cashier, "Amelia");
            drawActorTag(graphics, firstCustomer, "Bob");
        });
    }

    private static void drawWhiteText(GraphicsContext graphics, String label, double x, double y) {
        graphics.setFill(Color.web("#252B3D"));
        graphics.fillText(label, x + 1, y + 1);
        graphics.setFill(Color.WHITE);
        graphics.fillText(label, x, y);
    }

    private static void drawActorTag(GraphicsContext graphics, Actor actor, String label) {
        drawWhiteText(graphics, label, actor.column * TILE_SIZE + TILE_SIZE / 2,
                (actor.row - 1) * TILE_SIZE - 8);
    }

    private static void drawTarget(GraphicsContext graphics, Tile tile, Color color) {
        graphics.setStroke(color);
        graphics.setLineWidth(3);
        graphics.strokeRect(tile.column * TILE_SIZE + 6, tile.row * TILE_SIZE + 6,
                TILE_SIZE - 12, TILE_SIZE - 12);
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
            for (int tableColumn : DINING_TABLE_COLUMNS) {
                int chairColumn = tableColumn - 1;
                drawTile(graphics, diningChairTop, chairColumn, DINING_ROW - 1);
                drawTile(graphics, diningChairBottom, chairColumn, DINING_ROW);
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
                drawTile(graphics, diningTable, tableColumn, DINING_ROW);
            }
            drawDishes(graphics);
        });
    }

    private void drawDishes(GraphicsContext graphics) {
        for (int i = 0; i < dishesPlaced.length; i++) {
            if (!dishesPlaced[i]) {
                continue;
            }
            Tile counter = DISH_SPOTS[i];
            double x = counter.column * TILE_SIZE;
            double y = (counter.row + 1) * TILE_SIZE;
            graphics.setFill(Color.web("#F3EFDE"));
            graphics.fillOval(x + 18, y + 18, 28, 23);
            graphics.setStroke(Color.web("#737985"));
            graphics.setLineWidth(2);
            graphics.strokeOval(x + 18, y + 18, 28, 23);
            graphics.setFill(Color.web("#D98749"));
            graphics.fillOval(x + 26, y + 24, 12, 10);
        }
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

    private static final class Actor {
        private final Image[][] idleFrames;
        private final Image[][] runFrames;
        private final Image[] sittingFrames;
        private double column;
        private double row;
        private Direction direction;
        private List<Tile> path = List.of();
        private int nextPathIndex;
        private double progress;
        private boolean sitOnArrival;
        private boolean seated;

        private Actor(Tile start, Image[][] idleFrames, Image[][] runFrames, Image[] sittingFrames,
                Direction direction) {
            this.idleFrames = idleFrames;
            this.runFrames = runFrames;
            this.sittingFrames = sittingFrames;
            this.direction = direction;
            column = start.column;
            row = start.row;
        }

        private Tile tile() {
            return new Tile((int) Math.round(column), (int) Math.round(row));
        }

        private void follow(List<Tile> path, boolean sitOnArrival) {
            this.path = path;
            nextPathIndex = path.size() > 1 ? 1 : path.size();
            progress = 0;
            this.sitOnArrival = sitOnArrival;
            seated = path.size() == 1 && sitOnArrival;
            if (seated) {
                direction = Direction.RIGHT;
            }
        }

        private boolean isMoving() {
            return nextPathIndex < path.size();
        }

        private boolean advance(double delta) {
            if (!isMoving() || delta <= 0) {
                return false;
            }
            Tile from = path.get(nextPathIndex - 1);
            Tile to = path.get(nextPathIndex);
            direction = Direction.between(from, to);
            progress = Math.min(1, progress + delta * WALK_TILES_PER_SECOND);
            column = from.column + (to.column - from.column) * progress;
            row = from.row + (to.row - from.row) * progress;
            if (progress < 1) {
                return false;
            }
            nextPathIndex++;
            progress = 0;
            if (isMoving()) {
                return false;
            }
            path = List.of();
            nextPathIndex = 0;
            if (sitOnArrival) {
                seated = true;
                direction = Direction.RIGHT;
            }
            return true;
        }

        private Image frame(long now) {
            Image[] frames;
            long frameDuration;
            if (seated) {
                frames = sittingFrames;
                frameDuration = IDLE_FRAME_NANOS;
            } else if (isMoving() && runFrames != null) {
                frames = runFrames[direction.ordinal()];
                frameDuration = WALK_FRAME_NANOS;
            } else {
                frames = idleFrames[direction.ordinal()];
                if (frames == null) {
                    frames = Arrays.stream(idleFrames).filter(Objects::nonNull).findFirst().orElseThrow();
                }
                frameDuration = IDLE_FRAME_NANOS;
            }
            return frames[(int) ((now / frameDuration) % frames.length)];
        }
    }

    private record Tile(int column, int row) {}

    private record SearchNode(Tile tile, int cost, int score, long order) {}

    private enum Direction {
        RIGHT(1, 0, "right"),
        UP(0, -1, "up"),
        LEFT(-1, 0, "left"),
        DOWN(0, 1, "down");

        private final int dx;
        private final int dy;
        private final String path;

        Direction(int dx, int dy, String path) {
            this.dx = dx;
            this.dy = dy;
            this.path = path;
        }

        private static Direction fromName(String name) {
            return Arrays.stream(values()).filter(direction -> direction.path.equals(name)).findFirst().orElseThrow();
        }

        private static Direction between(Tile from, Tile to) {
            int dx = to.column - from.column;
            int dy = to.row - from.row;
            return Arrays.stream(values()).filter(direction -> direction.dx == dx && direction.dy == dy)
                    .findFirst().orElseThrow();
        }
    }
}
