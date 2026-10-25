package edu.ntu.sc2002.ui.preview;

import java.util.List;
import java.util.OptionalDouble;
import edu.ntu.sc2002.presentation.RestaurantUiState;
import edu.ntu.sc2002.presentation.RestaurantUiState.*;

/** Fixed UI examples; no turn simulation or game rules. */
public final class RestaurantUiFixtures {
    private RestaurantUiFixtures() {}

    public static RestaurantUiState preview(int stage) {
        List<CustomerSummary> customers = switch (stage) {
            case 1 -> List.of(
                    new CustomerSummary("bob", "Bob", null, "Table 1", 76, null, "No order", "Not applicable"),
                    new CustomerSummary("mia", "Mia", null, "Queue #1", 100, null, "No order", "Not applicable"));
            case 2 -> List.of(
                    new CustomerSummary("bob", "Bob", "Regular", "Table 1", 76, "Burger", "READY", "Automatic when due"),
                    new CustomerSummary("mia", "Mia", "VIP", "Queue #1", 96, null, "No order", "Not applicable"),
                    new CustomerSummary("lee", "Lee", "Critic", "Queue #2", 88, null, "No order", "Not applicable"));
            case 3 -> List.of(
                    new CustomerSummary("bob", "Bob", "Regular", "Table 1", 76, "ComboMeal", "READY", "Automatic when due"),
                    new CustomerSummary("mia", "Mia", "VIP", "Table 2", 96, null, "No order", "Not applicable"),
                    new CustomerSummary("lee", "Lee", "Critic", "Queue #1", 88, null, "No order", "Not applicable"));
            default -> throw new IllegalArgumentException("Stage must be 1, 2, or 3");
        };
        List<TableSummary> tables = switch (stage) {
            case 1, 2 -> List.of(new TableSummary("Table 1", "Occupied", "Bob"),
                    new TableSummary("Table 2", "Available", null));
            case 3 -> List.of(new TableSummary("Table 1", "Occupied", "Bob"),
                    new TableSummary("Table 2", "Occupied", "Mia"));
            default -> throw new IllegalArgumentException("Stage must be 1, 2, or 3");
        };
        List<OrderSummary> orders = switch (stage) {
            case 1 -> List.of();
            case 2 -> List.of(new OrderSummary("order-bob", "Bob", "Burger", "READY", false));
            case 3 -> List.of(new OrderSummary("order-bob", "Bob", "ComboMeal", "READY", true));
            default -> throw new IllegalArgumentException("Stage must be 1, 2, or 3");
        };
        List<WaiterChoice> waiterChoices = switch (stage) {
            case 1 -> List.of(
                    new WaiterChoice(WaiterTask.SEAT, "mia", "table-2", null, "Seat Mia at Table 2"),
                    new WaiterChoice(WaiterTask.TAKE_ORDER, "bob", null, null, "Take Bob's order"),
                    new WaiterChoice(WaiterTask.WAIT, null, null, null, "Wait"));
            case 2 -> List.of(
                    new WaiterChoice(WaiterTask.SEAT, "mia", "table-2", null, "Seat Mia at Table 2"),
                    new WaiterChoice(WaiterTask.SERVE, null, null, "order-bob", "Serve Bob's Burger"),
                    new WaiterChoice(WaiterTask.WAIT, null, null, null, "Wait"));
            case 3 -> List.of(
                    new WaiterChoice(WaiterTask.TAKE_ORDER, "mia", null, null, "Take Mia's order"),
                    new WaiterChoice(WaiterTask.SERVE, null, null, "order-bob", "Serve ComboMeal"),
                    new WaiterChoice(WaiterTask.WAIT, null, null, null, "Wait"));
            default -> throw new IllegalArgumentException("Stage must be 1, 2, or 3");
        };
        StageThreeStatus stageThree = stage == 3
                ? new StageThreeStatus("Seated Mia at Table 2", true, 0)
                : null;
        return new RestaurantUiState(stage, 4, UiPhase.WAITER, 0.00, OptionalDouble.empty(),
                customers, tables, orders, waiterChoices, List.of(), false, stageThree, null, null);
    }

    /** Preview returned after a waiter takes Bob's order; the Chef can select it this turn. */
    public static RestaurantUiState previewAfterWaiter(int stage) {
        RestaurantUiState base = preview(stage);
        List<CustomerSummary> customers = base.customers().stream()
                .map(customer -> customer.id().equals("bob")
                        ? new CustomerSummary(customer.id(), customer.name(), customer.type(), customer.location(),
                                customer.satisfaction(), "Pasta", "PREPARING · 2 units", customer.paymentStatus())
                        : customer)
                .toList();
        return new RestaurantUiState(stage, base.turn(), UiPhase.CHEF, base.revenue(),
                base.averagePaidSatisfaction(), customers, base.tables(),
                List.of(new OrderSummary("order-bob-pasta", "Bob", "Pasta", "PREPARING · 2 units", false)),
                List.of(), List.of(new ChefChoice(ChefTask.ADVANCE_ORDER, "order-bob-pasta",
                        "Advance Bob's Pasta"), new ChefChoice(ChefTask.WAIT, null, "Wait")),
                false, base.stageThree(), null, null);
    }

    public static RestaurantUiState previewRejectedChoice(int stage) {
        RestaurantUiState base = preview(stage);
        return new RestaurantUiState(stage, base.turn(), base.phase(), base.revenue(), base.averagePaidSatisfaction(),
                base.customers(), base.tables(), base.orders(), base.waiterChoices(), base.chefChoices(),
                base.canResolveTurn(), base.stageThree(),
                new Feedback("That choice was rejected. Select an available task to try again.", true), null);
    }

    public static RestaurantUiState previewReadyToResolve(int stage) {
        RestaurantUiState base = previewAfterWaiter(stage);
        return new RestaurantUiState(stage, base.turn(), UiPhase.RESOLVE, base.revenue(),
                base.averagePaidSatisfaction(), base.customers(), base.tables(), base.orders(),
                List.of(), List.of(), true, base.stageThree(), null, null);
    }

    public static RestaurantUiState previewHappyHourActive() {
        RestaurantUiState base = preview(3);
        return new RestaurantUiState(3, base.turn(), base.phase(), base.revenue(), base.averagePaidSatisfaction(),
                base.customers(), base.tables(), base.orders(), base.waiterChoices(), base.chefChoices(),
                base.canResolveTurn(), new StageThreeStatus(base.stageThree().hostActivity(), false, 2),
                base.feedback(), base.result());
    }

    public static RestaurantUiState previewResult(int stage, boolean victory) {
        RestaurantUiState base = preview(stage);
        ResultSummary result = new ResultSummary(5, victory ? 1 : 3, 54.60,
                OptionalDouble.of(victory ? 72.4 : 48.2), 18, victory);
        return new RestaurantUiState(stage, 18, UiPhase.COMPLETE, result.revenue(),
                result.averagePaidSatisfaction(), base.customers(), base.tables(), base.orders(),
                List.of(), List.of(), false, base.stageThree(), null, result);
    }

}
