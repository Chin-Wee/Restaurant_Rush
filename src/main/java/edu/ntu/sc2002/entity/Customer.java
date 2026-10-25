package edu.ntu.sc2002.entity;
import java.util.List;
import java.util.Random;
public final class Customer {
    private final String id;
    private final int arrivalOrdinal;
    private final Satisfaction satisfaction = new Satisfaction();
    private final long choiceSeed;
    private CustomerState state = CustomerState.WAITING;
    private Order order;
    private Integer readyToPayTurn;
    private int tableId = -1;
    public Customer(String id, int arrivalOrdinal, long choiceSeed) {
        this.id = id; this.arrivalOrdinal = arrivalOrdinal; this.choiceSeed = choiceSeed;
    }
    public String id() { return id; }
    public int arrivalOrdinal() { return arrivalOrdinal; }
    public Satisfaction satisfaction() { return satisfaction; }
    public CustomerState state() { return state; }
    public Order order() { return order; }
    public Integer readyToPayTurn() { return readyToPayTurn; }
    public int tableId() { return tableId; }
    public MenuItem chooseItem(Menu menu) {
        List<MenuItem> items = menu.getItems();
        return items.get(new Random(choiceSeed).nextInt(items.size()));
    }
    public void seat(int table) { state = CustomerState.SEATED; tableId = table; }
    public void placeOrder(MenuItem item) { order = new Order(item, item.priceCents()); }
    public void serveOrder(int turn) { order.serve(turn); state = CustomerState.EATING; }
    public void finishEating(int turn) {
        if (state == CustomerState.EATING && order.servedTurn() < turn) { state = CustomerState.READY_TO_PAY; readyToPayTurn = turn; }
    }
    public boolean canPay(int turn) { return state == CustomerState.READY_TO_PAY && readyToPayTurn < turn; }
    public void paid() { order.markPaid(); state = CustomerState.PAID; }
    public void loseSatisfaction() { satisfaction.deduct(8); }
    public void leave() { if (order != null && order.status() != OrderStatus.PAID) order.cancel(); state = CustomerState.LEFT; tableId = -1; }
}
