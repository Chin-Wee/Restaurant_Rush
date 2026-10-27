package edu.ntu.sc2002.domain.customer;

/** Creates arriving customers so the controller does not depend on concrete customer types. */
public final class CustomerFactory {
    public Customer createCustomer(String id, int ordinal, long seed) {
        return new RegularCustomer(id, ordinal, seed);
    }
}
