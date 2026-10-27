package edu.ntu.sc2002.presentation;

import edu.ntu.sc2002.application.GameController;
import edu.ntu.sc2002.application.contract.*;
import edu.ntu.sc2002.presentation.RestaurantUiState.*;
import java.util.Objects;

/** UI session orchestration only. The controller validates and executes every game action. */
public final class RestaurantUiAdapter {
    private final GameController controller;
    private GameSnapshot turnStart;
    private WaiterDecision acceptedWaiter;
    private ChefDecision pendingChef;
    private ResolvedTurn lastTurn;
    private RestaurantUiState state;

    public RestaurantUiAdapter(GameController controller) { this.controller = Objects.requireNonNull(controller); }

    public RestaurantUiState start() {
        if (state != null) throw new IllegalStateException("Session already started");
        controller.beginTurn();
        TurnResolution arrival = controller.processArrival();
        turnStart = controller.snapshot();
        return refresh(new Feedback(String.join(" ", arrival.events()), false));
    }

    public RestaurantUiState state() {
        if (state == null) throw new IllegalStateException("Session has not started");
        return state;
    }

    public RestaurantUiState handle(Command command) {
        GameSnapshot snapshot = state().snapshot();
        if (snapshot.phase() == TurnPhase.GAME_OVER) return reject("Game complete.");
        if (command instanceof WaiterCommand waiter) {
            if (snapshot.waiterActions().stream().noneMatch(a -> a.decision().equals(waiter.decision()))) return reject("Choose an available Waiter task.");
            TurnResolution response = controller.submitWaiterDecision(waiter.decision());
            if (!response.isSuccessful()) return reject(response.errorMessage());
            acceptedWaiter = waiter.decision();
            return refresh(new Feedback("Waiter task selected. Choose the Chef task, then Resolve Turn.", false));
        }
        if (command instanceof ChefCommand chef) {
            if (!snapshot.chefActions().contains(chef.decision())) return reject("Choose an available Chef task.");
            pendingChef = chef.decision();
            return refresh(new Feedback("Chef task selected. Resolve Turn to apply it.", false));
        }
        if (command instanceof ResolveTurn && pendingChef != null) {
            TurnResolution response = controller.submitChefDecision(pendingChef);
            if (!response.isSuccessful()) return reject(response.errorMessage());
            lastTurn = new ResolvedTurn(turnStart, controller.snapshot(), acceptedWaiter, pendingChef, response);
            acceptedWaiter = null;
            pendingChef = null;
            if (controller.phase() != TurnPhase.GAME_OVER) {
                controller.beginTurn();
                controller.processArrival();
                turnStart = controller.snapshot();
            }
            return refresh(new Feedback("Turn " + response.turn() + " complete.", false));
        }
        return reject("That choice is unavailable. Your selection has been kept.");
    }

    private RestaurantUiState reject(String message) { return refresh(new Feedback(message, true)); }
    private RestaurantUiState refresh(Feedback feedback) {
        state = new RestaurantUiState(controller.snapshot(), pendingChef, lastTurn, feedback);
        return state;
    }
}
