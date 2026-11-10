package edu.ntu.sc2002.presentation;

import edu.ntu.sc2002.application.TurnController;
import edu.ntu.sc2002.application.contract.*;
import edu.ntu.sc2002.presentation.RestaurantUiState.*;
import java.util.Objects;
import edu.ntu.sc2002.domain.GameResult;
import edu.ntu.sc2002.domain.HappyHourStatus;
import edu.ntu.sc2002.ui.RestaurantScreen;
import edu.ntu.sc2002.ui.scene.RestaurantScene;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX presentation adapter. It collects Host, Waiter and Chef commands, forwards them to the
 * controller and renders immutable snapshots; it never applies game rules itself.
 */
public final class JavaFXView implements edu.ntu.sc2002.ui.GameView {
    private final Stage stage;
    private final TurnController controller;
    private TurnChoicesDTO currentChoices;
    private final RestaurantScene room;
    private final RestaurantScreen screen;
    private GameSnapshot turnStart;
    private GameSnapshot.WaiterAction acceptedWaiter;
    private ChefChoiceDTO pendingChef;
    private HostChoiceDTO pendingHost;
    private ResolvedTurn lastTurn;
    private RestaurantUiState state;
    private boolean happyHourActivated;

    /**
     * Builds the game scene and starts the first turn/arrival.
     *
     * @param stage primary JavaFX stage
     * @param controller engine controller for the selected difficulty
     */
    public JavaFXView(Stage stage, TurnController controller) {
        this.stage = Objects.requireNonNull(stage);
        this.controller = Objects.requireNonNull(controller);
        room = new RestaurantScene();
        controller.beginTurn();
        TurnResolution arrival = controller.processArrival();
        turnStart = controller.snapshot();
        state = new RestaurantUiState(turnStart, null, null, new Feedback(String.join(" ", arrival.events()), false));
        screen = new RestaurantScreen(room, state, this::handle);
        stage.setScene(new Scene(screen.getRoot(), 1280, 720));
        stage.sizeToScene();
        displayTurnStart(turnStart);
    }

    /** Event handlers collect a pair; reading it never blocks or submits a command. */
    public TurnChoicesDTO getManagerChoices() { return currentChoices; }
    public void displayTurnStart(GameSnapshot snapshot) {
        state = new RestaurantUiState(snapshot, pendingChef, lastTurn, state.feedback());
        screen.setUiState(state);
    }
    public void displayError(String message) { refresh(new Feedback(message, true)); }
    public void displayTurnSummary(TurnResolution resolution) {
        refresh(new Feedback("Turn " + resolution.turn() + " complete.", false));
    }
    public void displayEndGameResult(GameResult result) { refresh(new Feedback(result.toString(), false)); }
    /** Releases screen animations and the room's animation timer. */
    public void dispose() { screen.dispose(); room.dispose(); }

    private void handle(GameSnapshot renderedSnapshot, Command command) {
        if (renderedSnapshot != state.snapshot()) {
            displayError("That choice is stale. Choose a current task.");
            return;
        }

        GameSnapshot snapshot = state.snapshot();
        if (snapshot.phase() == TurnPhase.GAME_OVER) { displayError("Game complete."); return; }
        if (command instanceof HappyHourCommand) {
            if (snapshot.happyHourStatus() != HappyHourStatus.AVAILABLE || snapshot.hostActions().isEmpty()) {
                displayError("Happy Hour can only be activated at the start of a turn, before the staff act.");
                return;
            }
            TurnResolution response = controller.activateHappyHour();
            if (!response.isSuccessful()) { displayError(response.errorMessage()); return; }
            happyHourActivated = true;
            refresh(new Feedback(String.join(" ", response.events()), false));
            return;
        }
        if (command instanceof HostCommand host) {
            var action = snapshot.hostActions().stream().filter(a -> a.decision().equals(host.decision())).findFirst().orElse(null);
            if (action == null) { displayError("Choose an available Host task."); return; }
            TurnResolution response = controller.submitHostDecision(host.decision());
            if (!response.isSuccessful()) { displayError(response.errorMessage()); return; }
            pendingHost = host.decision();
            refresh(new Feedback("Host task selected. Choose the Waiter task.", false));
            return;
        }
        if (command instanceof WaiterCommand waiter) {
            if (!snapshot.hostActions().isEmpty()) { displayError("Choose the Host task before the Waiter task."); return; }
            var action = snapshot.waiterActions().stream().filter(a -> a.decision().equals(waiter.decision())).findFirst().orElse(null);
            if (action == null) { displayError("Choose an available Waiter task."); return; }
            TurnResolution response = controller.submitWaiterDecision(waiter.decision());
            if (!response.isSuccessful()) { displayError(response.errorMessage()); return; }
            acceptedWaiter = action;
            refresh(new Feedback("Waiter task selected. Choose the Chef task, then Resolve Turn.", false));
            return;
        }
        if (command instanceof ChefCommand chef) {
            if (!snapshot.chefActions().contains(chef.decision())) { displayError("Choose an available Chef task."); return; }
            pendingChef = chef.decision();
            currentChoices = new TurnChoicesDTO(acceptedWaiter.decision(), pendingChef, pendingHost, happyHourActivated);
            refresh(new Feedback("Chef task selected. Resolve Turn to apply it.", false));
            return;
        }
        if (command instanceof ResolveTurn && pendingChef != null) {
            TurnResolution response = controller.submitChefDecision(getManagerChoices().getChefChoice());
            if (!response.isSuccessful()) { displayError(response.errorMessage()); return; }
            lastTurn = new ResolvedTurn(turnStart, controller.snapshot(), acceptedWaiter, pendingHost, pendingChef, response);
            acceptedWaiter = null;
            pendingChef = null;
            pendingHost = null;
            currentChoices = null;
            happyHourActivated = false;
            if (controller.phase() != TurnPhase.GAME_OVER) {
                controller.beginTurn();
                controller.processArrival();
                turnStart = controller.snapshot();
                displayTurnStart(turnStart);
            }
            displayTurnSummary(response);
            if (controller.phase() == TurnPhase.GAME_OVER) displayEndGameResult(controller.result());
            return;
        }
        displayError("That choice is unavailable. Your selection has been kept.");
    }

    private void refresh(Feedback feedback) {
        state = new RestaurantUiState(controller.snapshot(), pendingChef, lastTurn, feedback);
        screen.setUiState(state);
    }
}
