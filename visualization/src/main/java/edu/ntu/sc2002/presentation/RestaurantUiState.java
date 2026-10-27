package edu.ntu.sc2002.presentation;

import edu.ntu.sc2002.application.contract.*;

/** UI-only session details around the engine's immutable snapshot. */
public record RestaurantUiState(GameSnapshot snapshot, ChefChoiceDTO pendingChef,
        ResolvedTurn lastTurn, Feedback feedback) {
    public boolean canResolveTurn() { return pendingChef != null; }
    public boolean complete() { return snapshot.phase() == TurnPhase.GAME_OVER; }
    public String phase() {
        return complete() ? "COMPLETE" : canResolveTurn() ? "RESOLVE"
                : snapshot.waiterActions().isEmpty() ? "CHEF" : "WAITER";
    }

    public sealed interface Command permits WaiterCommand, ChefCommand, ResolveTurn {}
    public record WaiterCommand(WaiterChoiceDTO decision) implements Command {}
    public record ChefCommand(ChefChoiceDTO decision) implements Command {}
    public record ResolveTurn() implements Command {}
    public record Feedback(String message, boolean rejected) {}
    /** One successful Resolve, retaining engine data instead of copying metrics or visual positions. */
    public record ResolvedTurn(GameSnapshot before, GameSnapshot after,
            GameSnapshot.WaiterAction waiter, ChefChoiceDTO chef, TurnResolution resolution) {
        public String chefCustomerId() {
            return after.orders().stream().filter(o -> o.id() == chef.targetOrderId())
                    .map(GameSnapshot.OrderView::customerId).findFirst().orElse(null);
        }
    }
}
