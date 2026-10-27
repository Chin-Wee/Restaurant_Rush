package edu.ntu.sc2002.domain;
import edu.ntu.sc2002.domain.customer.Customer;
import edu.ntu.sc2002.domain.menu.MenuItem;

import java.util.Objects;

/** Quotes the price locked into an order when it is taken. */
public final class BillCalculator {
    /** Stage 1 has no price modifiers, so the quote is the item's price. */
    public int quote(MenuItem item, Customer customer, int turn) {
        Objects.requireNonNull(item);
        Objects.requireNonNull(customer);
        return item.priceCents();
    }
}
