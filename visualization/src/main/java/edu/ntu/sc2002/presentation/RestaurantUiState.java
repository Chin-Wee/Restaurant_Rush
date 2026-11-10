package edu.ntu.sc2002.presentation;

import edu.ntu.sc2002.application.contract.*;

/**
 * UI-only session state layered around the engine's immutable snapshot. It tracks the staged Chef
 * command, the last resolved turn and the latest feedback without owning any game rules.
 *
 * @param snapshot current engine snapshot
 * @param pendingChef staged Chef command, or null before the Chef submits
 * @param lastTurn most recently resolved turn, or null before the first resolution
 * @param feedback latest feedback message
 */
public record RestaurantUiState(GameSnapshot snapshot, ChefChoiceDTO pendingChef,
        ResolvedTurn lastTurn, Feedback feedback) {
    /** @return true once a Chef command has been staged and the turn can be resolved */
    public boolean canResolveTurn() { return pendingChef != null; }

    /** @return true when the engine phase is {@link TurnPhase#GAME_OVER} */
    public boolean complete() { return snapshot.phase() == TurnPhase.GAME_OVER; }

    /** @return a short UI phase label used by the HUD */
    public String phase() {
        return complete() ? "COMPLETE" : canResolveTurn() ? "RESOLVE"
                : !snapshot.hostActions().isEmpty() ? "HOST"
                : snapshot.waiterActions().isEmpty() ? "CHEF" : "WAITER";
    }

    /** Commands emitted by the UI and handled by the JavaFX adapter. */
    public sealed interface Command permits WaiterCommand, ChefCommand, HostCommand, ResolveTurn, HappyHourCommand {}

    /** A submitted Waiter command. */
    public record WaiterCommand(WaiterChoiceDTO decision) implements Command {}

    /** A staged Chef command. */
    public record ChefCommand(ChefChoiceDTO decision) implements Command {}

    /** Stage 3: the Host seats one selected queued customer, or waits. */
    public record HostCommand(HostChoiceDTO decision) implements Command {}

    /** Requests resolution of the staged Chef command. */
    public record ResolveTurn() implements Command {}

    /** Stage 3: switches on the once-per-game Happy Hour at the start of a turn. */
    public record HappyHourCommand() implements Command {}

    /**
     * Latest UI feedback.
     *
     * @param message text shown in the gameplay panel
     * @param rejected whether the message describes a rejected command
     */
    public record Feedback(String message, boolean rejected) {}

    /** One successful Resolve, retaining engine data instead of copying metrics or visual positions. */
    public record ResolvedTurn(GameSnapshot before, GameSnapshot after,
            GameSnapshot.WaiterAction waiter, HostChoiceDTO host, ChefChoiceDTO chef, TurnResolution resolution) {
        public String chefCustomerId() {
            return chef.targetCustomerId();
        }
    }
}
