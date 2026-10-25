package edu.ntu.sc2002.entity;

public final class Waiter {
    public void seat(Restaurant restaurant, Customer customer) {
        restaurant.seat(customer);
    }
    public Order takeOrder(Customer customer, Menu menu, int turn) {
        MenuItem item = customer.chooseItem(menu);
        customer.placeOrder(item);
        return customer.order();
    }
    public void serve(Customer customer, int turn) {
        customer.serveOrder(turn);
    }
}
