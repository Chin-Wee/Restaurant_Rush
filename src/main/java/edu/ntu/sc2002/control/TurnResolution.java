package edu.ntu.sc2002.control;
import edu.ntu.sc2002.entity.GameResult;
import edu.ntu.sc2002.entity.PaymentRecord;
import java.util.List;
public record TurnResolution(int turn, List<String> events, List<String> departingCustomers,
        PaymentRecord paymentRecord, GameResult result) {
    public TurnResolution { events = List.copyOf(events); departingCustomers = List.copyOf(departingCustomers); }
}
