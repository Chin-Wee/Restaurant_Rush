package edu.ntu.sc2002.ui.console;

import edu.ntu.sc2002.application.*;
import edu.ntu.sc2002.application.contract.*;
import edu.ntu.sc2002.domain.GameResult;
import edu.ntu.sc2002.domain.HappyHourStatus;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Reads typed commands and displays immutable results from the controller. */
public final class ConsoleUI implements edu.ntu.sc2002.ui.GameView {
    private final Scanner input;
    private final TurnController controller;
    private int displayedEvents;

    /**
     * Creates a console view over the supplied input stream and controller.
     *
     * @param input source of typed commands
     * @param controller engine controller to drive
     */
    public ConsoleUI(Scanner input, TurnController controller) {
        this.input = input;
        this.controller = controller;
    }

    /**
     * Runs the command-line loop until the controller reaches {@link TurnPhase#GAME_OVER} or input
     * ends. It displays the post-arrival snapshot and the previous turn's summary before collecting
     * the current turn's commands.
     */
    public void run() {
        TurnResolution completedTurn = null;
        GameSnapshot completedSnapshot = null;
        int completedEventOffset = 0;
        try {
            while (controller.phase() != TurnPhase.GAME_OVER) {
                controller.beginTurn();
                TurnResolution arrival = controller.processArrival();
                displayTurnStart(controller.snapshot());
                if (completedTurn != null) {
                    displayTurnSummary(completedTurn, completedSnapshot, completedEventOffset);
                }
                displayEvents(arrival);
                getManagerChoices();

                // Capture the completed turn before beginTurn resets the controller and event cursor.
                completedTurn = controller.lastResolution();
                completedSnapshot = controller.snapshot();
                completedEventOffset = displayedEvents;
            }
        } catch (NoSuchElementException ended) {
            System.out.println("Input ended. Game interrupted before a result.");
            return;
        }
        if (completedTurn != null) {
            displayTurnSummary(completedTurn, completedSnapshot, completedEventOffset);
        }
        displayEndGameResult(controller.result());
    }

    public TurnChoicesDTO getManagerChoices() {
        TurnResolution response;
        boolean happyHourRequested = readHappyHourChoice();
        HostChoiceDTO host;
        WaiterChoiceDTO waiter;
        ChefChoiceDTO chef;
        do {
            host = readHostChoice();
            response = controller.submitHostDecision(host);
            if (!response.isSuccessful()) displayError(response.errorMessage());
        } while (!response.isSuccessful());
        displayEvents(response);
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
        return new TurnChoicesDTO(waiter, chef, host, happyHourRequested);
    }

    /** Stage 3: offered once per game at the start of a turn, before the Host acts. */
    private boolean readHappyHourChoice() {
        GameSnapshot snapshot = controller.snapshot();
        if (snapshot.happyHourStatus() != HappyHourStatus.AVAILABLE || snapshot.hostActions().isEmpty()) return false;
        while (true) {
            System.out.println("Happy Hour is available: 20% off orders for two turns, waiting customers recover 15/turn.");
            System.out.println("Activate Happy Hour? [1 yes, 2 no]:");
            int option = readNumber();
            if (option == 2) return false;
            if (option != 1) { displayError("Choose 1 or 2."); continue; }
            TurnResolution response = controller.activateHappyHour();
            if (!response.isSuccessful()) { displayError(response.errorMessage()); continue; }
            displayEvents(response);
            System.out.println("Happy Hour: " + happyHourLabel(controller.snapshot()));
            return true;
        }
    }

    /** Stage 3: the Host seats one selected queued customer, in addition to the Waiter's task. */
    private HostChoiceDTO readHostChoice() {
        while (true) {
            System.out.println("Host task [1 seat, 2 wait]:");
            int option = readNumber();
            if (option == 2) return new HostChoiceDTO(HostTaskType.WAIT, null);
            if (option != 1) { displayError("Choose 1 or 2."); continue; }
            GameSnapshot snapshot = controller.snapshot();
            var targets = snapshot.hostActions().stream()
                    .filter(a -> a.decision().taskType() == HostTaskType.SEAT)
                    .map(GameSnapshot.HostAction::customerId).toList();
            if (targets.isEmpty()) { displayError("No queued customer can be seated."); continue; }
            System.out.println("Seat which customer? " + targets);
            String id = input.nextLine().trim();
            var customer = snapshot.customers().stream().filter(c -> c.id().equalsIgnoreCase(id)).findFirst().orElse(null);
            if (customer == null) { displayError("Unknown customer ID."); continue; }
            return new HostChoiceDTO(HostTaskType.SEAT, customer.id());
        }
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
        displayEvents(resolution, displayedEvents);
        displayedEvents = resolution.events().size();
    }

    private static void displayEvents(TurnResolution resolution, int firstUnreadEvent) {
        resolution.events().subList(firstUnreadEvent, resolution.events().size()).forEach(System.out::println);
    }

    private static String satisfaction(Double value) {
        return value == null ? "N/A" : String.format(Locale.ROOT, "%.1f", value);
    }

    public void displayTurnStart(GameSnapshot snapshot) {
        displayedEvents = 0;
        System.out.printf("%nTURN %d | Revenue %s | Average paid satisfaction %s%n",
                snapshot.turn(), TurnController.money(snapshot.revenueCents()), satisfaction(snapshot.averagePaidSatisfaction()));
        System.out.println("Happy Hour: " + happyHourLabel(snapshot));
        System.out.println("Queue: " + snapshot.waitingQueue());
        snapshot.tables().forEach(t -> System.out.println("Table " + t.id() + ": " + (t.occupantId() == null ? "Available" : t.occupantId())));
        snapshot.customers().forEach(c -> System.out.println(c.id() + " " + c.type() + " " + c.state() + " satisfaction " + c.satisfaction()
                + (c.tableId() == null ? "" : " Table " + c.tableId())));
        snapshot.orders().forEach(o -> System.out.println(o.customerId() + " " + o.item() + " " + o.status()
                + " " + o.unitsDone() + "/" + o.preparationUnits() + " bill " + TurnController.money(o.lockedPriceCents())));
        staffTasks(snapshot).forEach(System.out::println);
    }

    private static java.util.List<String> staffTasks(GameSnapshot snapshot) {
        java.util.List<String> labels = new java.util.ArrayList<>();
        snapshot.hostActions().forEach(a -> labels.add("Host " + a.decision().taskType() + ": "
                + (a.decision().taskType() == HostTaskType.WAIT ? "null" : a.customerId())));
        if (snapshot.hostActions().isEmpty()) labels.add("Host: already acted this turn");
        snapshot.waiterActions().forEach(a -> labels.add("Waiter " + a.decision().taskType() + ": " + a.customerId()));
        if (snapshot.chefActions().isEmpty()) labels.add("Chef: choices follow the Host and Waiter tasks");
        snapshot.chefActions().forEach(a -> labels.add("Chef " + a.taskType() + ": "
                + (a.taskType() == ChefTaskType.WAIT ? "null" : a.targetCustomerId())));
        labels.add("Cashier: at most one eligible payment, automatic");
        return labels;
    }

    private static String happyHourLabel(GameSnapshot snapshot) {
        return snapshot.happyHourStatus() == HappyHourStatus.ACTIVE
                ? snapshot.happyHourStatus() + " (" + snapshot.happyHourRemainingTurns() + " turns remaining)"
                : snapshot.happyHourStatus().toString();
    }

    public void displayError(String message) { System.out.println("Invalid choice: " + message + " Try again."); }

    public void displayTurnSummary(TurnResolution resolution) {
        displayTurnSummary(resolution, controller.snapshot(), displayedEvents);
        displayedEvents = resolution.events().size();
    }

    private static void displayTurnSummary(TurnResolution resolution, GameSnapshot s, int firstUnreadEvent) {
        displayEvents(resolution, firstUnreadEvent);
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
