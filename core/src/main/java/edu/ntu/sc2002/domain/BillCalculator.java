package edu.ntu.sc2002.domain;

import edu.ntu.sc2002.domain.customer.Customer;
import edu.ntu.sc2002.domain.menu.MenuItem;

/** Pricing service. Phase 1 quotes the item's base price with no modifiers. */
public class BillCalculator {

    public int quote(MenuItem item, Customer customer, int turn) {
        return item.priceCents();
    }
}
