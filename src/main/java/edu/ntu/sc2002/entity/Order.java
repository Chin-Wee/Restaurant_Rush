package edu.ntu.sc2002.entity;
public final class Order {
    private final MenuItem item;
    private final int lockedPriceCents;
    private int unitsDone;
    private OrderStatus status = OrderStatus.PLACED;
    private Integer readyTurn;
    private Integer servedTurn;
    public Order(MenuItem item, int lockedPriceCents) { this.item = item; this.lockedPriceCents = lockedPriceCents; }
    public MenuItem item() { return item; }
    public int lockedPriceCents() { return lockedPriceCents; }
    public int unitsDone() { return unitsDone; }
    public OrderStatus status() { return status; }
    public Integer readyTurn() { return readyTurn; }
    public Integer servedTurn() { return servedTurn; }
    public void advance(int turn) {
        if (status != OrderStatus.PLACED && status != OrderStatus.PREPARING) throw new IllegalStateException("Order is not being prepared");
        status = OrderStatus.PREPARING;
        if (++unitsDone >= item.preparationUnits()) { status = OrderStatus.READY; readyTurn = turn; }
    }
    public boolean canServe(int turn) { return status == OrderStatus.READY && readyTurn < turn; }
    public void serve(int turn) {
        if (!canServe(turn)) throw new IllegalStateException("Order must be ready before this turn");
        status = OrderStatus.SERVED; servedTurn = turn;
    }
    public void markPaid() { if (status != OrderStatus.SERVED) throw new IllegalStateException("Order was not served"); status = OrderStatus.PAID; }
    public void cancel() { if (status != OrderStatus.PAID) status = OrderStatus.CANCELLED; }
}
