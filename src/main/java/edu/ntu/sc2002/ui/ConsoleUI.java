package edu.ntu.sc2002.ui;

import edu.ntu.sc2002.control.*;
import edu.ntu.sc2002.entity.GameResult;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Reads typed commands and displays immutable results from the controller. */
public final class ConsoleUI {
    private final Scanner input;
    private final GameController controller;
    private int displayedEvents;

    public ConsoleUI() { this(new GameController()); }
    public ConsoleUI(GameController controller) { this(new Scanner(System.in), controller); }
    public ConsoleUI(Scanner input, GameController controller) {
        this.input = input;
        this.controller = controller;
    }

    public void run() {
        System.out.println("Restaurant Rush — Stage 1 (seed " + controller.randomSeed() + ")");
        try {
            while (controller.phase() != TurnPhase.GAME_OVER) {
                controller.beginTurn();
                displayTurnStart(controller.snapshot());
                TurnResolution arrival = controller.processArrival();
                displayEvents(arrival);
                if (arrival.newArrival() != null) {
                    System.out.println("Available after arrival: " + controller.snapshot().availableStaffTasks());
                }
                promptManagerChoices();
                displayTurnSummary(controller.lastResolution());
            }
        } catch (NoSuchElementException ended) {
            System.out.println("Input ended. Game interrupted before a result.");
            return;
        }
        displayEndGameResult(controller.result());
    }

    public void promptManagerChoices() {
        TurnResolution response;
        do {
            response = controller.submitWaiterDecision(readWaiterDecision());
            if (!response.isSuccessful()) displayError(response.errorMessage());
        } while (!response.isSuccessful());
        // Show the customer's chosen food before asking for the Chef's task.
        displayEvents(response);
        do {
            response = controller.submitChefDecision(readChefDecision());
            if (!response.isSuccessful()) displayError(response.errorMessage());
        } while (!response.isSuccessful());
    }

    private WaiterDecision readWaiterDecision() {
        while (true) {
            System.out.println("Waiter task [1 seat, 2 take order, 3 serve, 4 wait]:");
            WaiterTaskType task = switch (readNumber()) {
                case 1 -> WaiterTaskType.SEAT;
                case 2 -> WaiterTaskType.TAKE_ORDER;
                case 3 -> WaiterTaskType.SERVE;
                case 4 -> WaiterTaskType.WAIT;
                default -> null;
            };
            if (task == null) { displayError("Choose 1–4."); continue; }
            return new WaiterDecision(task, task == WaiterTaskType.WAIT ? null : promptCustomer());
        }
    }

    private ChefDecision readChefDecision() {
        while (true) {
            System.out.println("Chef task [1 advance order, 2 wait]:");
            int option = readNumber();
            if (option == 2) return new ChefDecision(ChefTaskType.WAIT, null);
            if (option != 1) { displayError("Choose 1 or 2."); continue; }
            var advanceable = controller.snapshot().advanceableOrderOptions();
            if (advanceable.isEmpty()) { displayError("No order can be advanced."); continue; }
            System.out.println("Advance which customer order? " + advanceable);
            return new ChefDecision(ChefTaskType.ADVANCE, input.nextLine().trim());
        }
    }

    private String promptCustomer() {
        System.out.println("Customer ID? " + controller.snapshot().activeCustomerOptions());
        return input.nextLine().trim();
    }

    private int readNumber() {
        try { return Integer.parseInt(input.nextLine().trim()); }
        catch (NumberFormatException invalid) { return -1; }
    }

    private void displayEvents(TurnResolution resolution) {
        resolution.events().subList(displayedEvents, resolution.events().size()).forEach(System.out::println);
        displayedEvents = resolution.events().size();
    }

    private static String satisfaction(Double value) {
        return value == null ? "N/A" : String.format(Locale.ROOT, "%.1f", value);
    }

    public void displayTurnStart(GameSnapshot snapshot) {
        displayedEvents = 0;
        System.out.printf("%nTURN %d | Revenue %s | Average paid satisfaction %s%n",
                snapshot.turn(), GameController.money(snapshot.revenueCents()), satisfaction(snapshot.averagePaidSatisfaction()));
        System.out.println("Queue: " + snapshot.waitingQueue());
        snapshot.tables().forEach(System.out::println);
        snapshot.customers().forEach(System.out::println);
        snapshot.orders().forEach(System.out::println);
        snapshot.availableStaffTasks().forEach(System.out::println);
    }

    public void displayError(String message) { System.out.println("Invalid choice: " + message + " Try again."); }

    public void displayTurnSummary(TurnResolution resolution) {
        displayEvents(resolution);
        GameSnapshot s = controller.snapshot();
        System.out.printf("End turn %d: served %d | paid %d | departures %d | revenue %s | average paid satisfaction %s | turns used %d%n",
                resolution.turn(), s.servedCount(), s.paidCount(), s.unhappyDepartures(), GameController.money(s.revenueCents()),
                satisfaction(s.averagePaidSatisfaction()), s.turnsUsed());
    }

    public void displayEndGameResult(GameResult result) {
        GameSnapshot s = controller.snapshot();
        System.out.printf("%n%s — served %d, paid %d, departures %d, revenue %s, average paid satisfaction %s, turns used %d%n",
                result, s.servedCount(), s.paidCount(), s.unhappyDepartures(), GameController.money(s.revenueCents()),
                satisfaction(s.averagePaidSatisfaction()), s.turnsUsed());
    }
}
