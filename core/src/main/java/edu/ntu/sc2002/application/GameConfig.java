package edu.ntu.sc2002.application;

import edu.ntu.sc2002.domain.customer.CustomerType;
import java.util.Collections;
import java.util.List;

/** Base setting; customerTypes.get(i) is the type of the customer arriving on arrivalSchedule.get(i). */
public record GameConfig(int maxTurns, int numTables, List<Integer> arrivalSchedule, long randomSeed,
        List<CustomerType> customerTypes) {
    private static final List<Integer> BASE_ARRIVALS = List.of(1, 4, 7, 10, 13, 16);

    public GameConfig {
        arrivalSchedule = List.copyOf(arrivalSchedule);
        customerTypes = List.copyOf(customerTypes);
        if (maxTurns != 18 || numTables != 2 || !arrivalSchedule.equals(BASE_ARRIVALS)
                || customerTypes.size() != arrivalSchedule.size()) {
            throw new IllegalArgumentException("The base setting requires 18 turns, two tables, and one typed arrival on turns 1, 4, 7, 10, 13, 16");
        }
    }

    /** Six Regular customers; kept to check that Stage 1 behaviour is unchanged. */
    public static GameConfig stageOne(long seed) {
        return new GameConfig(18, 2, BASE_ARRIVALS, seed, Collections.nCopies(BASE_ARRIVALS.size(), CustomerType.REGULAR));
    }

    /** The turn-4 arrival is a VIP and the turn-10 arrival is a Critic; the rest are Regular. */
    public static GameConfig stageTwo(long seed) {
        return new GameConfig(18, 2, BASE_ARRIVALS, seed, List.of(CustomerType.REGULAR, CustomerType.VIP,
                CustomerType.REGULAR, CustomerType.CRITIC, CustomerType.REGULAR, CustomerType.REGULAR));
    }

    public List<CustomerType> getCustomerTypes() { return customerTypes; }

    /** Requires a turn on which a customer arrives. */
    public CustomerType customerTypeAt(int turn) {
        int index = arrivalSchedule.indexOf(turn);
        if (index < 0) throw new IllegalArgumentException("No customer arrives on turn " + turn);
        return customerTypes.get(index);
    }
}
