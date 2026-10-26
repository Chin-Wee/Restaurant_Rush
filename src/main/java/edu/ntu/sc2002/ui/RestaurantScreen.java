package edu.ntu.sc2002.ui;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

import edu.ntu.sc2002.presentation.RestaurantUiState;
import edu.ntu.sc2002.ui.scene.RestaurantScene;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import javafx.util.StringConverter;

import edu.ntu.sc2002.presentation.RestaurantUiState.ActivateHappyHour;
import edu.ntu.sc2002.presentation.RestaurantUiState.ChefChoice;
import edu.ntu.sc2002.presentation.RestaurantUiState.ChefCommand;
import edu.ntu.sc2002.presentation.RestaurantUiState.CustomerSummary;
import edu.ntu.sc2002.presentation.RestaurantUiState.ResultSummary;
import edu.ntu.sc2002.presentation.RestaurantUiState.OrderSummary;
import edu.ntu.sc2002.presentation.RestaurantUiState.ResolveTurn;
import edu.ntu.sc2002.presentation.RestaurantUiState.TableSummary;
import edu.ntu.sc2002.presentation.RestaurantUiState.WaiterChoice;
import edu.ntu.sc2002.presentation.RestaurantUiState.WaiterCommand;

/** JavaFX HUD over the Stardew Valley-style room. Renders supplied state and emits commands. */
public final class RestaurantScreen {
    public static final int ROOM_WIDTH = RestaurantScene.ROOM_WIDTH;
    public static final int ROOM_HEIGHT = RestaurantScene.ROOM_HEIGHT;
    private static final Image ACTION_BUTTON_ART = new Image(Objects.requireNonNull(
            RestaurantScreen.class.getResource("/ui/panel_brown_damaged_dark.png"), "Missing UI button art")
            .toExternalForm(), false);
    private static final String SMALL_TEXT_STYLE = "-fx-text-fill: #f4eddb; -fx-font-size: 10px;";
    private final Pane root = new StackPane();
    private final AnchorPane overlay = new AnchorPane();
    private final VBox gameplayPanel = new VBox(5);
    private final HBox topHud = new HBox(18);
    private final Consumer<RestaurantUiState.Command> commandSink;
    private final RestaurantScene scene;
    private final boolean preview;
    private RestaurantUiState uiState;
    private ComboBox<WaiterChoice> waiterChoices;
    private ComboBox<ChefChoice> chefChoices;

    public RestaurantScreen(RestaurantScene scene, RestaurantUiState initialState,
            Consumer<RestaurantUiState.Command> commandSink) {
        this(scene, initialState, commandSink, false);
    }

    public RestaurantScreen(RestaurantScene scene, RestaurantUiState initialState,
            Consumer<RestaurantUiState.Command> commandSink, boolean preview) {
        this.scene = Objects.requireNonNull(scene);
        this.preview = preview;
        this.commandSink = Objects.requireNonNull(commandSink);
        root.getChildren().addAll(scene.getRoot(), overlay);
        overlay.setPickOnBounds(false);
        overlay.setStyle("-fx-background-color: transparent;");
        gameplayPanel.setId("gameplay-panel");
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
        if (!preview) scene.applyState(nextState.scene());
        renderTopHud();
        renderGameplayPanel(previousWaiterChoice, previousChefChoice);
    }

    private void renderTopHud() {
        topHud.getChildren().setAll(
                hudText(preview ? "UI PREVIEW" : "RESTAURANT RUSH"),
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
            feedback.setMaxHeight(28);
            feedback.setMinHeight(16);
            feedback.setTooltip(new javafx.scene.control.Tooltip(uiState.feedback().message()));
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

    private static Node resultView(ResultSummary result) {
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

}
