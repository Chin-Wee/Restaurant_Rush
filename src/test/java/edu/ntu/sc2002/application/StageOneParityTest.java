package edu.ntu.sc2002.application;
import edu.ntu.sc2002.application.*;
import edu.ntu.sc2002.application.contract.*;

import static org.junit.Assert.*;
import edu.ntu.sc2002.domain.*;
import edu.ntu.sc2002.domain.customer.*;
import edu.ntu.sc2002.domain.menu.*;
import edu.ntu.sc2002.domain.staff.*;
import java.util.List;
import org.junit.Test;

/** Behavioral checks derived from assignment sections 2–4, independent of the UML inventory. */
public class StageOneParityTest {
    private void begin(GameController game) { game.beginTurn(); game.processArrival(); }
    private void waitTurn(GameController game) {
        assertTrue(game.submitWaiterDecision(new WaiterDecision(WaiterTaskType.WAIT, null)).isSuccessful());
        assertTrue(game.submitChefDecision(new ChefDecision(ChefTaskType.WAIT, null)).isSuccessful());
    }

    @Test public void turnProtocolCannotSkipArrivalsOrResolveBeforeWaiter() {
        GameController game = new GameController();
        assertThrows(IllegalStateException.class, game::processArrival);
        assertThrows(IllegalStateException.class, game::lastResolution);
        game.beginTurn();
        assertThrows(IllegalStateException.class, game::beginTurn);
        assertEquals(1, game.currentTurn());
        assertThrows(IllegalStateException.class, game::lastResolution);
        assertEquals("C1", game.processArrival().newArrival());
        assertThrows(IllegalStateException.class, game::processArrival);
        assertFalse(game.submitChefDecision(new ChefDecision(ChefTaskType.WAIT, null)).isSuccessful());
        assertEquals(100, game.restaurant().customers().get(0).satisfaction().getValue());
        waitTurn(game);
        assertEquals(1, game.lastResolution().turn());
    }

    @Test public void availabilityFollowsAcceptedTaskAndInvalidRetryDoesNotMutate() {
        GameController game = new GameController();
        begin(game);
        var before = game.snapshot();
        TurnResolution invalid = game.submitWaiterDecision(new WaiterDecision(WaiterTaskType.TAKE_ORDER, "C1"));
        assertFalse(invalid.isSuccessful());
        assertNotNull(invalid.errorMessage());
        assertEquals(before, game.snapshot());
        assertTrue(game.submitWaiterDecision(new WaiterDecision(WaiterTaskType.SEAT, "C1")).isSuccessful());
        assertTrue(game.snapshot().waiterActions().isEmpty());
        assertTrue(game.snapshot().chefActions().contains(new ChefDecision(ChefTaskType.WAIT, null)));
        assertTrue(game.submitChefDecision(new ChefDecision(ChefTaskType.WAIT, null)).isSuccessful());
        assertThrows(IllegalStateException.class,
                () -> game.submitChefDecision(new ChefDecision(ChefTaskType.WAIT, null)));
        while (game.phase() != TurnPhase.GAME_OVER) { begin(game); waitTurn(game); }
        assertTrue(game.snapshot().waiterActions().isEmpty());
        assertTrue(game.snapshot().chefActions().isEmpty());
        assertThrows(IllegalStateException.class, game::beginTurn);
    }

    @Test public void baseMenuConfigurationAndSixArrivalsAreExact() {
        GameConfig config = GameConfig.stageOne(2026);
        assertEquals(18, config.maxTurns());
        assertEquals(2, config.numTables());
        assertEquals(List.of(1, 4, 7, 10, 13, 16), config.arrivalSchedule());
        assertEquals(List.of(new MenuItem("Salad", 900, 1), new MenuItem("Burger", 1500, 1),
                new MenuItem("Pasta", 1800, 2)), new Menu().getItems());
        GameController game = new GameController(config);
        for (int turn = 1; turn <= 18; turn++) {
            game.beginTurn();
            TurnResolution arrival = game.processArrival();
            assertEquals(config.arrivalSchedule().contains(turn), arrival.newArrival() != null);
            waitTurn(game);
            assertEquals(turn, game.snapshot().turnsUsed());
            if (turn < 18) assertNull(game.result());
        }
        assertEquals(6, game.restaurant().customers().size());
        assertTrue(game.scoreBoard().paidCount() <= 5);
    }

    @Test public void configuredTablesAreUsedAndSnapshotsCannotBeChanged() {
        GameController game = new GameController(new GameConfig(18, 3, List.of(1), 2026));
        begin(game);
        assertEquals(3, game.snapshot().tables().size());
        assertNull(game.snapshot().averagePaidSatisfaction());
        assertThrows(UnsupportedOperationException.class, () -> game.snapshot().waitingQueue().clear());
        assertThrows(UnsupportedOperationException.class, () -> new Menu().getItems().clear());
    }

    @Test public void readyDishCannotBeServedUntilNextTurn() {
        Customer customer = new Customer("C1", 1, 0);
        new Table(1).assign(customer);
        Order order = customer.placeOrder(new MenuItem("Salad", 900, 1), 900);
        order.advance(2);
        assertFalse(order.canServe(2));
        assertThrows(IllegalStateException.class, () -> customer.serveOrder(2));
        assertEquals(OrderStatus.READY, order.status());
        customer.serveOrder(3);
        customer.finishEating(3);
        assertEquals(CustomerState.EATING, customer.state());
        customer.finishEating(4);
        assertFalse(customer.canPay(4));
        assertTrue(customer.canPay(5));
    }

    @Test public void cashierPaysOneOlderCustomerThenOtherOnFollowingTurn() {
        Restaurant restaurant = new Restaurant();
        Customer young = new Customer("young", 2, 0);
        Customer old = new Customer("old", 1, 0);
        for (Customer c : List.of(young, old)) {
            restaurant.registerArrival(c);
            restaurant.seat(c);
            c.placeOrder(new MenuItem("Salad", 900, 1), 900).advance(1);
            c.serveOrder(2);
            c.finishEating(3);
        }
        GameController game = new GameController(new GameConfig(18, 2, List.of(), 0), restaurant, new ScoreBoard());
        for (int turn = 1; turn <= 3; turn++) {
            begin(game); waitTurn(game);
            assertEquals(0, game.scoreBoard().paidCount());
            assertTrue(restaurant.tables().stream().allMatch(Table::isOccupied));
        }
        begin(game); waitTurn(game);
        assertEquals(CustomerState.PAID, old.state());
        assertNull(restaurant.tableFor(old));
        assertEquals(CustomerState.READY_TO_PAY, young.state());
        assertEquals(1, game.scoreBoard().paidCount());
        assertEquals(900, game.scoreBoard().revenueCents());
        old.satisfaction().deduct(100);
        assertEquals(100.0, game.scoreBoard().getAveragePaidSatisfaction(), 0.0);
        assertEquals(1, restaurant.tables().stream().filter(Table::isOccupied).count());
        begin(game); waitTurn(game);
        assertEquals(CustomerState.PAID, young.state());
        assertEquals(2, game.scoreBoard().paidCount());
        assertTrue(restaurant.tables().stream().noneMatch(Table::isOccupied));
    }

    @Test public void abandonmentCancelsUnpaidOrderRemovesQueueAndFreesTable() {
        GameController game = new GameController();
        begin(game);
        assertTrue(game.submitWaiterDecision(new WaiterDecision(WaiterTaskType.SEAT, "C1")).isSuccessful());
        assertTrue(game.submitChefDecision(new ChefDecision(ChefTaskType.WAIT, null)).isSuccessful());
        begin(game);
        assertTrue(game.submitWaiterDecision(new WaiterDecision(WaiterTaskType.TAKE_ORDER, "C1")).isSuccessful());
        assertTrue(game.submitChefDecision(new ChefDecision(ChefTaskType.WAIT, null)).isSuccessful());
        for (int turn = 3; turn <= 13; turn++) { begin(game); waitTurn(game); }
        Customer customer = game.restaurant().customers().get(0);
        assertEquals(CustomerState.LEFT, customer.state());
        assertEquals(0, customer.satisfaction().getValue());
        assertEquals(OrderStatus.CANCELLED, customer.order().status());
        assertEquals(0, game.scoreBoard().revenueCents());
        assertTrue(game.restaurant().tables().stream().noneMatch(Table::isOccupied));
        while (game.phase() != TurnPhase.GAME_OVER) { begin(game); waitTurn(game); }
        assertFalse(game.restaurant().waitingQueue().getMembers().stream().anyMatch(c -> c.state() == CustomerState.LEFT));
    }

    @Test public void terminalCustomerAndDuplicateOrderCannotBeReactivated() {
        Customer customer = new Customer("C1", 1, 0);
        MenuItem salad = new MenuItem("Salad", 900, 1);
        assertThrows(IllegalStateException.class, () -> customer.placeOrder(salad, 900));
        Restaurant restaurant = new Restaurant();
        restaurant.registerArrival(customer);
        restaurant.seat(customer);
        Order order = customer.placeOrder(salad, 900);
        assertThrows(IllegalStateException.class, () -> customer.placeOrder(salad, 900));
        assertSame(order, customer.order());
        customer.satisfaction().deduct(100);
        restaurant.abandon(customer);
        assertNull(restaurant.tableFor(customer));
        assertThrows(IllegalStateException.class, () -> customer.serveOrder(3));
        Table free = new Table(2);
        assertThrows(IllegalStateException.class, () -> free.assign(customer));
        assertFalse(free.isOccupied());
    }

    @Test public void cashierUsesArrivalPriorityEvenWhenLaterArrivalWasReadyEarlier() {
        // Construct two eligible customers to distinguish arrival age from payment-wait age.
        Restaurant restaurant = new Restaurant();
        Customer olderArrival = new Customer("older-arrival", 1, 0);
        Customer laterArrival = new Customer("later-arrival", 2, 0);
        for (Customer customer : List.of(laterArrival, olderArrival)) {
            restaurant.registerArrival(customer);
            restaurant.seat(customer);
            customer.placeOrder(new MenuItem("Salad", 900, 1), 900);
        }
        laterArrival.order().advance(6);
        laterArrival.serveOrder(7);
        laterArrival.finishEating(8);
        olderArrival.order().advance(9);
        olderArrival.serveOrder(10);
        olderArrival.finishEating(11);
        assertEquals(Integer.valueOf(8), laterArrival.readyToPayTurn());
        assertEquals(Integer.valueOf(11), olderArrival.readyToPayTurn());
        assertEquals("older-arrival", restaurant.cashier().collectOldest(restaurant, 12).customerId());
        assertTrue(laterArrival.canPay(12));
        assertEquals("later-arrival", restaurant.cashier().collectOldest(restaurant, 13).customerId());
    }

    @Test public void satisfactionBoundsAndCustomerChoicesAreDeterministic() {
        Satisfaction satisfaction = new Satisfaction();
        assertThrows(IllegalArgumentException.class, () -> satisfaction.deduct(-1));
        assertThrows(IllegalArgumentException.class, () -> satisfaction.add(-1));
        satisfaction.deduct(Integer.MAX_VALUE);
        assertEquals(0, satisfaction.getValue());
        satisfaction.add(Integer.MAX_VALUE);
        assertEquals(100, satisfaction.getValue());
        Customer a = new Customer("a", 1, 3);
        Customer b = new Customer("b", 2, 3);
        assertEquals(a.chooseItem(new Menu()), b.chooseItem(new Menu()));
    }

    @Test public void turnSixteenArrivalCannotPayWithinEighteenTurns() {
        GameController game = new GameController();
        for (int turn = 1; turn <= 15; turn++) { begin(game); waitTurn(game); }
        begin(game);
        assertTrue(game.submitWaiterDecision(new WaiterDecision(WaiterTaskType.SEAT, "C6")).isSuccessful());
        assertTrue(game.submitChefDecision(new ChefDecision(ChefTaskType.WAIT, null)).isSuccessful());
        begin(game);
        assertTrue(game.submitWaiterDecision(new WaiterDecision(WaiterTaskType.TAKE_ORDER, "C6")).isSuccessful());
        assertTrue(game.submitChefDecision(new ChefDecision(ChefTaskType.ADVANCE, "C6")).isSuccessful());
        begin(game);
        Customer last = game.restaurant().customers().get(5);
        WaiterTaskType task = last.order().canServe(18) ? WaiterTaskType.SERVE : WaiterTaskType.WAIT;
        assertTrue(game.submitWaiterDecision(new WaiterDecision(task, task == WaiterTaskType.WAIT ? null : "C6")).isSuccessful());
        assertTrue(game.submitChefDecision(new ChefDecision(ChefTaskType.WAIT, null)).isSuccessful());
        assertFalse(last.canPay(18));
        assertNotEquals(CustomerState.PAID, last.state());
        assertEquals(0, game.scoreBoard().paidCount());
    }

    @Test public void managerCanChooseOrderAndKitchenPriorityWhileInvalidSeatDoesNotConsumeTask() {
        Restaurant restaurant = new Restaurant();
        Customer first = new Customer("first", 1, 3);
        Customer second = new Customer("second", 2, 3);
        Customer queued = new Customer("queued", 3, 3);
        for (Customer customer : List.of(first, second, queued)) restaurant.registerArrival(customer);
        restaurant.seat(first); restaurant.seat(second);
        assertThrows(IllegalStateException.class, () -> restaurant.tables().get(0).vacate());
        GameController game = new GameController(new GameConfig(18, 2, List.of(), 0), restaurant, new ScoreBoard());
        begin(game);
        var before = game.snapshot();
        assertFalse(game.submitWaiterDecision(new WaiterDecision(WaiterTaskType.SEAT, "queued")).isSuccessful());
        assertEquals(before, game.snapshot());
        assertTrue(game.submitWaiterDecision(new WaiterDecision(WaiterTaskType.TAKE_ORDER, "second")).isSuccessful());
        assertTrue(game.submitChefDecision(new ChefDecision(ChefTaskType.ADVANCE, "second")).isSuccessful());
        assertNull(first.order());
        assertEquals(1, second.order().unitsDone());
        begin(game);
        assertTrue(game.submitWaiterDecision(new WaiterDecision(WaiterTaskType.TAKE_ORDER, "first")).isSuccessful());
        assertTrue(game.submitChefDecision(new ChefDecision(ChefTaskType.ADVANCE, "second")).isSuccessful());
        assertEquals(0, first.order().unitsDone());
        assertEquals(OrderStatus.READY, second.order().status());
        begin(game);
        var prior = game.snapshot();
        assertFalse(game.submitWaiterDecision(new WaiterDecision(WaiterTaskType.TAKE_ORDER, "first")).isSuccessful());
        assertEquals(prior, game.snapshot());
        waitTurn(game);
    }
}
