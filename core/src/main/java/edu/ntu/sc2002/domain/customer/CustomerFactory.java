package edu.ntu.sc2002.domain.customer;

/** Creates concrete Customer instances. Phase 1 produces only RegularCustomer. */
public class CustomerFactory {

    public Customer createCustomer(String id, int ordinal, long seed) {
        return new RegularCustomer(id, ordinal, seed);
    }
}
