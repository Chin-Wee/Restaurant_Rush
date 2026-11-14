package edu.ntu.sc2002.ui;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;

import edu.ntu.sc2002.presentation.RestaurantUiState;
import edu.ntu.sc2002.ui.scene.RestaurantScene;
import edu.ntu.sc2002.application.contract.*;
import edu.ntu.sc2002.application.contract.GameSnapshot.*;
import edu.ntu.sc2002.domain.GameResult;
import edu.ntu.sc2002.domain.HappyHourStatus;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.Interpolator;
import javafx.util.Duration;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import edu.ntu.sc2002.presentation.RestaurantUiState.WaiterCommand;
import edu.ntu.sc2002.presentation.RestaurantUiState.ChefCommand;
import edu.ntu.sc2002.presentation.RestaurantUiState.HostCommand;
import edu.ntu.sc2002.presentation.RestaurantUiState.HappyHourCommand;
import edu.ntu.sc2002.presentation.RestaurantUiState.ResolveTurn;
import edu.ntu.sc2002.presentation.RestaurantUiState.ResolvedTurn;

/** JavaFX HUD over the Stardew Valley-style room. Renders supplied state and emits commands. */
public final class RestaurantScreen {
    public static final int ROOM_WIDTH = RestaurantScene.ROOM_WIDTH;
    public static final int ROOM_HEIGHT = RestaurantScene.ROOM_HEIGHT;
    private static final Image ACTION_BUTTON_ART = new Image(Objects.requireNonNull(
            RestaurantScreen.class.getResource("/ui/panel_brown_damaged_dark.png"), "Missing UI button art")
            .toExternalForm(), false);
    private static final String SMALL_TEXT_STYLE = "-fx-text-fill: #f4eddb; -fx-font-size: 14px;";
    private final Pane root = new StackPane();
    private final AnchorPane overlay = new AnchorPane();
    private final VBox gameplayPanel = new VBox(3);
    private final FlowPane topHud = new FlowPane(18, 4);
    private final VBox turnBanner = new VBox(8);
    private Animation turnCelebration;
    private int lastCelebratedTurn;
    private final BiConsumer<GameSnapshot, RestaurantUiState.Command> commandSink;
    private final RestaurantScene scene;
    private RestaurantUiState uiState;
    private ComboBox<WaiterAction> waiterChoices;
    private ComboBox<ChefChoiceDTO> chefChoices;
    private ComboBox<HostChoiceDTO> hostChoices;

    /**
     * Builds the HUD over an existing room scene.
     *
     * @param scene rendered room
     * @param initialState initial UI state to display
     * @param commandSink callback that forwards UI commands to the controller adapter
     */
    public RestaurantScreen(RestaurantScene scene, RestaurantUiState initialState,
            BiConsumer<GameSnapshot, RestaurantUiState.Command> commandSink) {
        this.scene = Objects.requireNonNull(scene);
        this.commandSink = Objects.requireNonNull(commandSink);
        root.setStyle(UiEffects.fontStyle());
        root.setMinSize(0, 0);
        overlay.setMinSize(0, 0);
        root.getChildren().addAll(scene.getRoot(), overlay);
        overlay.setPickOnBounds(false);
        overlay.setStyle("-fx-background-color: transparent;");
        gameplayPanel.setId("gameplay-panel");
        gameplayPanel.setPadding(new Insets(4, 9, 4, 9));
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
        topHud.setMaxWidth(Double.MAX_VALUE);
        topHud.setStyle("-fx-background-color: rgba(37, 43, 61, 0.94);"
                + "-fx-border-color: #b98753; -fx-border-width: 1;");
        AnchorPane.setLeftAnchor(topHud, 14.0);
        AnchorPane.setRightAnchor(topHud, 14.0);
        AnchorPane.setTopAnchor(topHud, 14.0);
        turnBanner.setId("turn-summary");
        turnBanner.setVisible(false);
        turnBanner.setMouseTransparent(true);
        turnBanner.setAlignment(Pos.CENTER);
        turnBanner.setPadding(new Insets(20, 26, 20, 26));
        turnBanner.setStyle("-fx-background-color: rgba(30,34,48,.97); -fx-border-color: #eab86f;"
                + "-fx-border-width: 3; -fx-effect: dropshadow(gaussian, #eab86f88, 24, .15, 0, 3);");
        root.widthProperty().addListener((observable, oldWidth, width) -> {
            AnchorPane.setLeftAnchor(turnBanner, Math.max(16, width.doubleValue() * .16));
            AnchorPane.setRightAnchor(turnBanner, Math.max(16, width.doubleValue() * .16));
        });
        root.heightProperty().addListener((observable, oldHeight, height) ->
                AnchorPane.setTopAnchor(turnBanner, Math.max(72, height.doubleValue() * .12)));
        overlay.getChildren().addAll(gameplayPanel, topHud, turnBanner);

        setUiState(initialState);
    }

    /** @return the root pane containing the room and HUD overlay */
    public Pane getRoot() {
        return root;
    }

    /** Apply the controller's next immutable snapshot after a command is handled. */
    public void setUiState(RestaurantUiState nextState) {
        HostChoiceDTO previousHostChoice = hostChoices == null ? null : hostChoices.getValue();
        WaiterAction previousWaiterChoice = waiterChoices == null ? null : waiterChoices.getValue();
        ChefChoiceDTO previousChefChoice = chefChoices == null ? null : chefChoices.getValue();
        if (uiState == null) scene.showSnapshot(nextState.snapshot());
        uiState = Objects.requireNonNull(nextState);
        renderTopHud();
        renderGameplayPanel(previousHostChoice, previousWaiterChoice, previousChefChoice);
        if (nextState.lastTurn() != null && nextState.lastTurn().resolution().turn() > lastCelebratedTurn) {
            scene.playTurn(nextState.lastTurn(), nextState.snapshot());
            showTurnSummary(nextState.lastTurn());
        }
    }

    /** Stops the turn-summary animation, if one is running. */
    public void dispose() { if (turnCelebration != null) turnCelebration.stop(); }

    private void showTurnSummary(ResolvedTurn turn) {
        GameSnapshot summary = turn.after();
        lastCelebratedTurn = turn.resolution().turn();
        if (turnCelebration != null) turnCelebration.stop();
        Label title = new Label("TURN " + summary.turn() + " COMPLETE!");
        title.setId("turn-summary-title");
        title.setStyle("-fx-font-size: 38px; -fx-font-weight: bold; -fx-text-fill: #ffdc8c;");
        Label events = new Label(String.join("\n", turn.resolution().events()));
        events.setId("turn-summary-events");
        events.setWrapText(true);
        events.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #fff5dc;");
        Label totals = new Label("SERVED " + summary.servedCount() + "  ·  PAID " + summary.paidCount()
                + "  ·  DEPARTURES " + summary.unhappyDepartures() + "  ·  REVENUE " + formatMoney(summary.revenueCents())
                + "  ·  AVG PAID SAT " + formatSatisfaction(summary.averagePaidSatisfaction())
                + "  ·  TURNS USED " + summary.turnsUsed());
        totals.setWrapText(true);
        totals.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #b9f1c7;");
        turnBanner.getChildren().setAll(title, events, totals);
        turnBanner.setVisible(true);
        FadeTransition appear = new FadeTransition(Duration.millis(160), turnBanner);
        appear.setFromValue(0); appear.setToValue(1);
        ScaleTransition pop = new ScaleTransition(Duration.millis(260), turnBanner);
        pop.setFromX(.9); pop.setFromY(.9); pop.setToX(1); pop.setToY(1);
        pop.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition rise = new TranslateTransition(Duration.millis(260), turnBanner);
        rise.setFromY(18); rise.setToY(0);
        FadeTransition dismiss = new FadeTransition(Duration.millis(500), turnBanner);
        dismiss.setToValue(0);
        turnCelebration = new SequentialTransition(new ParallelTransition(appear, pop, rise),
                new PauseTransition(Duration.seconds(3.5)), dismiss);
        turnCelebration.setOnFinished(event -> turnBanner.setVisible(false));
        turnCelebration.play();
    }

    private void renderTopHud() {
        topHud.getChildren().setAll(
                identifiedHud("RESTAURANT RUSH", "game-title"),
                identifiedHud("STAGE 3", "stage-label"),
                hudText("TURN " + uiState.snapshot().turn()),
                hudText("PHASE " + uiState.phase()),
                hudText("REVENUE " + formatMoney(uiState.snapshot().revenueCents())),
                hudText("AVG PAID SAT " + formatSatisfaction(uiState.snapshot().averagePaidSatisfaction())));
    }

    private static Label identifiedHud(String text, String id) {
        Label label = hudText(text); label.setId(id); return label;
    }

    private static Label hudText(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #fff5dc; -fx-font-size: 16px; -fx-font-weight: bold;");
        return label;
    }

    private void renderGameplayPanel(HostChoiceDTO previousHostChoice, WaiterAction previousWaiterChoice,
            ChefChoiceDTO previousChefChoice) {
        gameplayPanel.getChildren().clear();
        if (uiState.complete()) {
            gameplayPanel.getChildren().add(resultView(uiState.snapshot(), uiState.lastTurn().resolution().result()));
            return;
        }

        if (uiState.feedback() != null) {
            Label feedback = new Label(uiState.feedback().message());
            feedback.setWrapText(true);
            feedback.setMaxHeight(28);
            feedback.setMinHeight(22);
            feedback.setTooltip(new javafx.scene.control.Tooltip(uiState.feedback().message()));
            feedback.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
                    + (uiState.feedback().rejected() ? "#ffb4a6;" : "#b9f1c7;"));
            gameplayPanel.getChildren().add(feedback);
        }

        GridPane cards = new GridPane();
        cards.setHgap(7);
        cards.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        double[] widths = {24, 16, 16, 16, 16, 12};
        for (double width : widths) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(width);
            column.setHgrow(Priority.ALWAYS);
            column.setFillWidth(true);
            cards.getColumnConstraints().add(column);
        }
        cards.add(customerCard(), 0, 0);
        cards.add(tableAndOrderCard(), 1, 0);
        cards.add(hostCard(previousHostChoice), 2, 0);
        cards.add(waiterCard(previousWaiterChoice), 3, 0);
        cards.add(chefCard(previousChefChoice), 4, 0);
        cards.add(managerCard(), 5, 0);
        VBox.setVgrow(cards, Priority.ALWAYS);
        gameplayPanel.getChildren().add(cards);
    }

    private Node customerCard() {
        VBox rows = new VBox(4);
        for (CustomerView customer : uiState.snapshot().customers()) {
            String location = customer.tableId() != null ? "Table " + customer.tableId()
                    : customer.queuePosition() != null ? "Queue #" + customer.queuePosition() : customer.state().name();
            Label identity = smallText(customer.id() + " · " + customer.type() + " · " + location + " · " + customer.satisfaction() + "/100");
            OrderView order = uiState.snapshot().orders().stream().filter(o -> o.customerId().equals(customer.id())).findFirst().orElse(null);
            rows.getChildren().addAll(identity, smallText((order == null ? "No food chosen · No order" : order.item() + " · " + order.status())
                    + " · State: " + customer.state()));
        }
        if (uiState.snapshot().customers().isEmpty()) rows.getChildren().add(smallText("No customers"));
        return card("CUSTOMERS / QUEUE", scrollable(rows));
    }

    private Node tableAndOrderCard() {
        VBox rows = new VBox(3);
        for (TableView table : uiState.snapshot().tables()) {
            rows.getChildren().add(smallText("Table " + table.id() + " · "
                    + (table.occupantId() == null ? "Available" : "Occupied · " + table.occupantId())));
        }
        if (uiState.snapshot().orders().isEmpty()) rows.getChildren().add(smallText("No active orders"));
        for (OrderView order : uiState.snapshot().orders()) {
            rows.getChildren().add(smallText(order.customerId() + " · " + order.item() + " · " + order.status()
                    + " " + order.unitsDone() + "/" + order.preparationUnits() + " · " + formatMoney(order.lockedPriceCents())));
        }
        VBox content = new VBox(3, scrollable(rows), smallText("Cashier · payment is automatic"));
        return card("TABLES / ORDERS", content);
    }

    private Node waiterCard(WaiterAction previousChoice) {
        VBox content = new VBox(3);
        boolean hostPending = !uiState.snapshot().hostActions().isEmpty();
        waiterChoices = choiceBox("Choose available Waiter task", uiState.snapshot().waiterActions(),
                this::waiterLabel);
        waiterChoices.setId("waiter-actions");
        waiterChoices.setDisable(hostPending || waiterChoices.getItems().isEmpty());
        selectIfAvailable(waiterChoices, previousChoice);
        Button submit = actionButton("Submit", 92, "Submit Waiter task");
        submit.setId("waiter-submit");
        submit.disableProperty().bind(waiterChoices.disabledProperty().or(waiterChoices.valueProperty().isNull()));
        GameSnapshot renderedSnapshot = uiState.snapshot();
        ComboBox<WaiterAction> renderedChoices = waiterChoices;
        submit.setOnAction(event -> {
            WaiterAction choice = renderedChoices.getValue();
            if (choice != null) {
                commandSink.accept(renderedSnapshot, new WaiterCommand(choice.decision()));
            }
        });
        content.getChildren().addAll(waiterChoices, submit);
        if (hostPending) content.getChildren().add(smallText("Choose the Host task first."));
        return card("WAITER", scrollable(content));
    }

    /** Stage 3 Host task: the manager seats a selected queued customer or waits. */
    private Node hostCard(HostChoiceDTO previousChoice) {
        VBox content = new VBox(3);
        List<HostChoiceDTO> actions = uiState.snapshot().hostActions().stream()
                .map(GameSnapshot.HostAction::decision).toList();
        hostChoices = choiceBox("Choose available Host task", actions, this::hostLabel);
        hostChoices.setId("host-actions");
        selectIfAvailable(hostChoices, previousChoice);
        Button submit = actionButton("Submit", 82, "Submit Host task");
        submit.setId("host-submit");
        submit.disableProperty().bind(hostChoices.disabledProperty().or(hostChoices.valueProperty().isNull()));
        GameSnapshot renderedSnapshot = uiState.snapshot();
        ComboBox<HostChoiceDTO> renderedChoices = hostChoices;
        submit.setOnAction(event -> {
            HostChoiceDTO choice = renderedChoices.getValue();
            if (choice != null) {
                commandSink.accept(renderedSnapshot, new HostCommand(choice));
            }
        });
        content.getChildren().addAll(hostChoices, submit);
        if (actions.isEmpty()) content.getChildren().add(smallText("Host already acted this turn."));
        return card("HOST", scrollable(content));
    }

    private Node chefCard(ChefChoiceDTO previousChoice) {
        VBox content = new VBox(3);
        chefChoices = choiceBox("Choose available Chef task", uiState.snapshot().chefActions(), this::chefLabel);
        chefChoices.setId("chef-actions");
        selectIfAvailable(chefChoices, previousChoice);
        Button submit = actionButton("Submit", 82, "Submit Chef task");
        submit.setId("chef-submit");
        submit.disableProperty().bind(chefChoices.disabledProperty().or(chefChoices.valueProperty().isNull()));
        GameSnapshot renderedSnapshot = uiState.snapshot();
        ComboBox<ChefChoiceDTO> renderedChoices = chefChoices;
        submit.setOnAction(event -> {
            ChefChoiceDTO choice = renderedChoices.getValue();
            if (choice != null) {
                commandSink.accept(renderedSnapshot, new ChefCommand(choice));
            }
        });
        FlowPane buttons = new FlowPane(5, 4);
        buttons.getChildren().add(submit);
        Button resolve = actionButton("Resolve", 82, "Resolve turn");
        resolve.setId("resolve-turn");
        resolve.setDisable(!uiState.canResolveTurn());
        resolve.setOnAction(event -> commandSink.accept(renderedSnapshot, new ResolveTurn()));
        buttons.getChildren().add(resolve);
        content.getChildren().addAll(chefChoices, buttons);
        if (uiState.snapshot().chefActions().isEmpty()) {
            content.getChildren().add(smallText("Chef choices appear after the Host and Waiter act."));
        }
        return card("CHEF / TURN", scrollable(content));
    }

    private String hostLabel(HostChoiceDTO decision) {
        if (decision.taskType() == HostTaskType.WAIT) return "Wait";
        var action = uiState.snapshot().hostActions().stream()
                .filter(a -> a.decision().equals(decision)).findFirst().orElse(null);
        return action == null ? "Seat " + decision.targetCustomerId()
                : "Seat " + action.customerId() + " at Table " + action.tableId();
    }

    /** Stage 3 manager action: Happy Hour may be switched on once, at the start of a turn. */
    private Node managerCard() {
        VBox content = new VBox(4);
        HappyHourStatus status = uiState.snapshot().happyHourStatus();
        String statusText = switch (status) {
            case AVAILABLE -> "Available";
            case ACTIVE -> "Active · " + uiState.snapshot().happyHourRemainingTurns() + " turn(s) left";
            case EXHAUSTED -> "Used";
        };
        content.getChildren().add(smallText("Happy Hour: " + statusText));
        Button activate = actionButton("Happy Hour", 104, "Activate Happy Hour");
        activate.setId("happy-hour");
        activate.setDisable(status != HappyHourStatus.AVAILABLE
                || uiState.snapshot().hostActions().isEmpty());
        GameSnapshot renderedSnapshot = uiState.snapshot();
        activate.setOnAction(event -> commandSink.accept(renderedSnapshot, new HappyHourCommand()));
        content.getChildren().add(activate);
        content.getChildren().add(smallText("Once per game: 20% off orders taken this turn and the next; waiting guests recover 15 satisfaction."));
        return card("MANAGER", scrollable(content));
    }

    private String waiterLabel(WaiterAction action) {
        WaiterChoiceDTO d = action.decision();
        return switch (d.taskType()) {
            case SEAT -> "Seat " + action.customerId() + " at Table " + action.tableId();
            case TAKE_ORDER -> "Take " + action.customerId() + "'s order";
            case SERVE -> "Serve " + action.customerId() + "'s order";
            case WAIT -> "Wait";
        };
    }

    private String chefLabel(ChefChoiceDTO decision) {
        if (decision.taskType() == ChefTaskType.WAIT) return "Wait";
        var order = uiState.snapshot().orders().stream().filter(o -> o.customerId().equals(decision.targetCustomerId()))
                .findFirst().orElseThrow();
        return "Advance " + order.customerId() + "'s " + order.item();
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
        box.setCellFactory(list -> choiceCell(labelFor));
        box.setButtonCell(choiceCell(labelFor));
        box.setMaxWidth(Double.MAX_VALUE);
        box.setPrefHeight(28);
        box.setDisable(choices.isEmpty());
        box.setStyle(UiEffects.fontStyle() + "-fx-font-size: 14px;");
        return box;
    }

    private static <T> ListCell<T> choiceCell(java.util.function.Function<T, String> labelFor) {
        return new ListCell<>() {
            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : labelFor.apply(item));
                setFont(UiEffects.font(14));
            }
        };
    }

    private static <T> void selectIfAvailable(ComboBox<T> box, T previous) {
        if (previous != null && box.getItems().contains(previous)) {
            box.setValue(previous);
        }
    }

    private static VBox card(String title, Node content) {
        Label heading = new Label(title);
        heading.setStyle("-fx-text-fill: #eab86f; -fx-font-size: 14px; -fx-font-weight: bold;");
        VBox result = new VBox(4, heading, content);
        result.setPadding(new Insets(4));
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
        heading.setStyle("-fx-text-fill: #d6c7a7; -fx-font-size: 13px; -fx-font-weight: bold;");
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
        title.setStyle("-fx-text-fill: #fff4d7; -fx-font-size: 14px; -fx-font-weight: bold;");
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
        UiEffects.install(button);
        return button;
    }

    private static Node resultView(GameSnapshot result, GameResult outcomeResult) {
        Label outcome = new Label((outcomeResult == GameResult.VICTORY) ? "VICTORY" : "DEFEAT");
        outcome.setStyle("-fx-text-fill: " + ((outcomeResult == GameResult.VICTORY) ? "#b9f1c7" : "#ffb4a6")
                + "; -fx-font-size: 20px; -fx-font-weight: bold;");
        GridPane metrics = new GridPane();
        metrics.setHgap(18);
        String[][] values = {
                {"SERVED", Integer.toString(result.servedCount())},
                {"PAID", Integer.toString(result.paidCount())},
                {"DEPARTURES", Integer.toString(result.unhappyDepartures())},
                {"REVENUE", formatMoney(result.revenueCents())},
                {"AVG PAID SAT", formatSatisfaction(result.averagePaidSatisfaction())},
                {"TURNS USED", Integer.toString(result.turnsUsed())}
        };
        for (int column = 0; column < values.length; column++) {
            VBox metric = new VBox(3, sectionHeading(values[column][0]), hudText(values[column][1]));
            metrics.add(metric, column, 0);
            ColumnConstraints constraint = new ColumnConstraints();
            constraint.setPercentWidth(100.0 / values.length);
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

    private static String formatMoney(int cents) {
        return String.format(Locale.ROOT, "$%.2f", cents / 100.0);
    }

    private static String formatSatisfaction(Double satisfaction) {
        return satisfaction != null
                ? String.format(Locale.ROOT, "%.1f / 100", satisfaction)
                : "N/A";
    }

}
