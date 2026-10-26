package edu.ntu.sc2002.application.contract;

import edu.ntu.sc2002.domain.GameResult;
import edu.ntu.sc2002.domain.PaymentRecord;
import java.util.List;

/** Immutable command result. Customer references crossing the UI boundary are IDs. */
public record TurnResolution(int turn, List<String> events, String newArrival, List<String> departingCustomers,
        PaymentRecord paymentRecord, boolean isSuccessful, String errorMessage, GameResult result) {
    public TurnResolution {
        events = List.copyOf(events);
        departingCustomers = List.copyOf(departingCustomers);
        if (isSuccessful == (errorMessage != null)) throw new IllegalArgumentException("Inconsistent command result");
    }
}
