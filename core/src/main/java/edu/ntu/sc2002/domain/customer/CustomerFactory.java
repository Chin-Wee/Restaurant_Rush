package edu.ntu.sc2002.domain.customer;

import java.util.Objects;

/** The only place that maps a CustomerType to a concrete Customer; game rules then use the object polymorphically. */
public final class CustomerFactory {
    public CustomerFactory() {}

    /**
     * Creates the concrete customer subtype for an arrival.
     *
     * @param type requested customer behaviour
     * @param id unique customer ID
     * @param arrivalOrdinal one-based arrival order used for deterministic choices and payment priority
     * @param choiceSeed seed for reproducible random dish selection
     * @return the concrete customer instance
     * @throws NullPointerException if {@code type} is null
     */
    public Customer createCustomer(CustomerType type, String id, int arrivalOrdinal, long choiceSeed) {
        return switch (Objects.requireNonNull(type)) {
            case REGULAR -> new RegularCustomer(id, arrivalOrdinal, choiceSeed);
            case VIP -> new VIPCustomer(id, arrivalOrdinal, choiceSeed);
            case CRITIC -> new CriticCustomer(id, arrivalOrdinal, choiceSeed);
        };
    }
}
