package edu.ntu.sc2002.ui;

import edu.ntu.sc2002.control.*;
import edu.ntu.sc2002.entity.*;
import java.util.Scanner;

/** Stage 1 boundary: formats snapshots and retries invalid choices. */
public final class ConsoleUI {
    private final Scanner input;
    private final GameController controller;

    public ConsoleUI() { this(new GameController()); }
    public ConsoleUI(GameController controller) { this(new Scanner(System.in), controller); }
    public ConsoleUI(Scanner input, GameController controller) { this.input = input; this.controller = controller; }

    public void run() {
        System.out.println("Restaurant Rush — Stage 1 (seed 2026)");
        while (controller.phase() != TurnPhase.GAME_OVER) {
            controller.beginTurn();
            displayTurnStart(controller.snapshot());
            for (String event : controller.processArrival()) System.out.println(event);
            System.out.println("Available after arrival: " + controller.snapshot().availableStaffTasks());
            WaiterDecision waiter = readWaiterDecision();
            String error;
            while ((error = controller.submitWaiterDecision(waiter)) != null) {
                displayError(error); waiter = readWaiterDecision();
            }
            Customer ordered = controller.restaurant().customers().stream()
                    .filter(c -> c.order() != null && c.order().status() == OrderStatus.PLACED)
                    .reduce((first, second) -> second).orElse(null);
            if (ordered != null) System.out.println("Selected food: " + ordered.id() + " chose " + ordered.order().item().name() + ".");
            ChefDecision chef = readChefDecision();
            while ((error = controller.submitChefDecision(chef)) != null) {
                displayError(error); chef = readChefDecision();
            }
            displayTurnSummary(controller.lastResolution());
        }
        displayEndGameResult(controller.result());
    }

    private WaiterDecision readWaiterDecision() {
        while (true) {
            System.out.println("Waiter task [1 seat, 2 take order, 3 serve, 4 wait]:");
            int option = readNumber();
            WaiterTaskType task = switch (option) {
                case 1 -> WaiterTaskType.SEAT;
                case 2 -> WaiterTaskType.TAKE_ORDER;
                case 3 -> WaiterTaskType.SERVE;
                case 4 -> WaiterTaskType.WAIT;
                default -> null;
            };
            if (task == null) { displayError("Choose 1–4."); continue; }
            String id = task == WaiterTaskType.WAIT ? null : promptCustomer();
            return new WaiterDecision(task, id);
        }
    }

    private ChefDecision readChefDecision() {
        while (true) {
            System.out.println("Chef task [1 advance order, 2 wait]:");
            int option = readNumber();
            if (option == 2) return new ChefDecision(ChefTaskType.WAIT, null);
            if (option != 1) { displayError("Choose 1 or 2."); continue; }
            if (controller.advanceableOrders().isEmpty()) {
                displayError("No order can be advanced."); continue;
            }
            System.out.println("Advance which customer order? " + controller.advanceableOrders().stream()
                    .map(c -> c.id() + " (" + c.order().item().name() + ")").toList());
            return new ChefDecision(ChefTaskType.ADVANCE, input.nextLine().trim());
        }
    }

    private String promptCustomer() {
        System.out.println("Customer ID? " + controller.restaurant().customers().stream()
                .filter(c -> c.state() != CustomerState.PAID && c.state() != CustomerState.LEFT)
                .map(c -> c.id() + " (" + c.state() + ")").toList());
        return input.nextLine().trim();
    }

    private int readNumber() {
        try { return Integer.parseInt(input.nextLine().trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    public void displayTurnStart(GameSnapshot snapshot) {
        System.out.printf("%nTURN %d | Revenue %s | Average paid satisfaction %s%n",
                snapshot.turn(), GameController.money(snapshot.revenueCents()),
                Double.isNaN(snapshot.averagePaidSatisfaction()) ? "N/A" : String.format("%.1f", snapshot.averagePaidSatisfaction()));
        System.out.println("Queue: " + snapshot.waitingQueue());
        snapshot.tables().forEach(System.out::println);
        snapshot.customers().forEach(System.out::println);
        snapshot.orders().forEach(System.out::println);
        snapshot.availableStaffTasks().forEach(System.out::println);
    }
    public void displayError(String message) { System.out.println("Invalid choice: " + message + " Try again."); }
    public void displayTurnSummary(TurnResolution resolution) {
        resolution.events().forEach(System.out::println);
        ScoreBoard s = controller.scoreBoard();
        System.out.printf("End turn %d: served %d | departures %d | revenue %s | average paid satisfaction %s | turns used %d%n",
                resolution.turn(), s.servedCount(), s.unhappyDepartures(), GameController.money(s.revenueCents()),
                Double.isNaN(s.averagePaidSatisfaction()) ? "N/A" : String.format("%.1f", s.averagePaidSatisfaction()), resolution.turn());
    }
    public void displayEndGameResult(GameResult result) {
        System.out.printf("%n%s — served %d, departures %d, revenue %s, average paid satisfaction %s, turns used %d%n",
                result, controller.scoreBoard().servedCount(), controller.scoreBoard().unhappyDepartures(),
                GameController.money(controller.scoreBoard().revenueCents()),
                Double.isNaN(controller.scoreBoard().averagePaidSatisfaction()) ? "N/A" : String.format("%.1f", controller.scoreBoard().averagePaidSatisfaction()),
                controller.currentTurn());
    }
}
