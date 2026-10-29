package edu.ntu.sc2002.ui.console;

import edu.ntu.sc2002.application.*;
import edu.ntu.sc2002.application.contract.*;
import edu.ntu.sc2002.domain.GameResult;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Reads typed commands and displays immutable results from the controller. */
public final class ConsoleUI implements edu.ntu.sc2002.ui.GameView {
    private final Scanner input;
    private final TurnController controller;
    private int displayedEvents;

    public ConsoleUI() { this(new TurnController()); }
    public ConsoleUI(TurnController controller) { this(new Scanner(System.in), controller); }
    public ConsoleUI(Scanner input, TurnController controller) {
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
                System.out.println("Available tasks: " + staffTasks(controller.snapshot()));
                getManagerChoices();
                displayTurnSummary(controller.lastResolution());
            }
        } catch (NoSuchElementException ended) {
            System.out.println("Input ended. Game interrupted before a result.");
            return;
        }
        displayEndGameResult(controller.result());
    }

    public TurnChoicesDTO getManagerChoices() {
        TurnResolution response;
        WaiterChoiceDTO waiter;
        ChefChoiceDTO chef;
        do {
            waiter = readWaiterChoice();
            response = controller.submitWaiterDecision(waiter);
            if (!response.isSuccessful()) displayError(response.errorMessage());
        } while (!response.isSuccessful());
        // Show the customer's chosen food before asking for the Chef's task.
        displayEvents(response);
        do {
            chef = readChefChoice();
            response = controller.submitChefDecision(chef);
            if (!response.isSuccessful()) displayError(response.errorMessage());
        } while (!response.isSuccessful());
        return new TurnChoicesDTO(waiter, chef);
    }

    private WaiterChoiceDTO readWaiterChoice() {
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
            if (task == WaiterTaskType.WAIT) return new WaiterChoiceDTO(task, null);
            String id = promptCustomer();
            GameSnapshot snapshot = controller.snapshot();
            var customer = snapshot.customers().stream().filter(c -> c.id().equalsIgnoreCase(id)).findFirst().orElse(null);
            if (customer == null) { displayError("Unknown customer ID."); continue; }
            return new WaiterChoiceDTO(task, customer.id());
        }
    }

    private ChefChoiceDTO readChefChoice() {
        while (true) {
            System.out.println("Chef task [1 advance order, 2 wait]:");
            int option = readNumber();
            if (option == 2) return new ChefChoiceDTO(ChefTaskType.WAIT, null);
            if (option != 1) { displayError("Choose 1 or 2."); continue; }
            GameSnapshot snapshot = controller.snapshot();
            var advanceable = snapshot.chefActions().stream().filter(d -> d.taskType() == ChefTaskType.ADVANCE)
                    .map(ChefChoiceDTO::targetCustomerId).toList();
            if (advanceable.isEmpty()) { displayError("No order can be advanced."); continue; }
            System.out.println("Advance which customer order? " + advanceable);
            String id = input.nextLine().trim();
            var order = snapshot.orders().stream().filter(o -> o.customerId().equalsIgnoreCase(id)).findFirst().orElse(null);
            if (order == null) { displayError("No order belongs to that customer."); continue; }
            return new ChefChoiceDTO(ChefTaskType.ADVANCE, order.customerId());
        }
    }

    private String promptCustomer() {
        System.out.println("Customer ID? " + controller.snapshot().customers().stream().filter(c -> c.state() != edu.ntu.sc2002.domain.customer.CustomerState.PAID && c.state() != edu.ntu.sc2002.domain.customer.CustomerState.LEFT).map(GameSnapshot.CustomerView::id).toList());
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
                snapshot.turn(), TurnController.money(snapshot.revenueCents()), satisfaction(snapshot.averagePaidSatisfaction()));
        System.out.println("Queue: " + snapshot.waitingQueue());
        snapshot.tables().forEach(t -> System.out.println("Table " + t.id() + ": " + (t.occupantId() == null ? "Available" : t.occupantId())));
        snapshot.customers().forEach(c -> System.out.println(c.id() + " " + c.state() + " satisfaction " + c.satisfaction()
                + (c.tableId() == null ? "" : " Table " + c.tableId())));
        snapshot.orders().forEach(o -> System.out.println(o.customerId() + " " + o.item() + " " + o.status()
                + " " + o.unitsDone() + "/" + o.preparationUnits()));
        staffTasks(snapshot).forEach(System.out::println);
    }

    private static java.util.List<String> staffTasks(GameSnapshot snapshot) {
        java.util.List<String> labels = new java.util.ArrayList<>();
        snapshot.waiterActions().forEach(a -> labels.add("Waiter " + a.decision().taskType() + ": " + a.customerId()));
        if (snapshot.chefActions().isEmpty()) labels.add("Chef: choices follow the Waiter task");
        snapshot.chefActions().forEach(a -> labels.add("Chef " + a.taskType() + ": "
                + (a.taskType() == ChefTaskType.WAIT ? "null" : a.targetCustomerId())));
        labels.add("Cashier: at most one eligible payment, automatic");
        return labels;
    }

    public void displayError(String message) { System.out.println("Invalid choice: " + message + " Try again."); }

    public void displayTurnSummary(TurnResolution resolution) {
        displayEvents(resolution);
        GameSnapshot s = controller.snapshot();
        System.out.printf("End turn %d: served %d | paid %d | departures %d | revenue %s | average paid satisfaction %s | turns used %d%n",
                resolution.turn(), s.servedCount(), s.paidCount(), s.unhappyDepartures(), TurnController.money(s.revenueCents()),
                satisfaction(s.averagePaidSatisfaction()), s.turnsUsed());
    }

    public void displayEndGameResult(GameResult result) {
        GameSnapshot s = controller.snapshot();
        System.out.printf("%n%s — served %d, paid %d, departures %d, revenue %s, average paid satisfaction %s, turns used %d%n",
                result, s.servedCount(), s.paidCount(), s.unhappyDepartures(), TurnController.money(s.revenueCents()),
                satisfaction(s.averagePaidSatisfaction()), s.turnsUsed());
    }
}
