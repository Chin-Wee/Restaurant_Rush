package edu.ntu.sc2002.domain.customer;

import java.util.Objects;

/** The only place that maps a CustomerType to a concrete Customer; game rules then use the object polymorphically. */
public final class CustomerFactory {
    /** Critics' preferred dish: Pasta is the slowest item, so Critics test kitchen priority. */
    public static final String CRITIC_PREFERRED_ITEM = "Pasta";

    private CustomerFactory() {}

    /** The seed keeps random menu choices reproducible for a given game seed. */
    public static Customer createCustomer(CustomerType type, String id, int arrivalOrdinal, long choiceSeed) {
        return switch (Objects.requireNonNull(type)) {
            case REGULAR -> new RegularCustomer(id, arrivalOrdinal, choiceSeed);
            case VIP -> new VIPCustomer(id, arrivalOrdinal, choiceSeed);
            case CRITIC -> new CriticCustomer(id, arrivalOrdinal, CRITIC_PREFERRED_ITEM);
        };
    }
}
