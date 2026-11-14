package edu.ntu.sc2002.application;

import edu.ntu.sc2002.domain.customer.CustomerType;
import java.util.Collections;
import java.util.List;

/**
 * Immutable difficulty and arrival configuration for a game.
 * The i-th arrival turn in {@code arrivalSchedule} uses the i-th type in {@code customerTypes}.
 * The canonical constructor validates that turns are strictly increasing, within range and paired
 * with exactly one customer type.
 *
 * @param maxTurns maximum number of turns in the game
 * @param numTables number of tables; the current game requires two
 * @param arrivalSchedule strictly increasing arrival turns
 * @param randomSeed seed used for reproducible customer choices
 * @param customerTypes customer type for each scheduled arrival
 */
public record GameConfig(int maxTurns, int numTables, List<Integer> arrivalSchedule, long randomSeed,
        List<CustomerType> customerTypes) {
    private static final List<Integer> BASE_ARRIVALS = List.of(1, 4, 7, 10, 13, 16);

    public GameConfig {
        arrivalSchedule = List.copyOf(arrivalSchedule);
        customerTypes = List.copyOf(customerTypes);
        if (maxTurns < 1 || numTables != 2 || arrivalSchedule.isEmpty()
                || customerTypes.size() != arrivalSchedule.size()) {
            throw new IllegalArgumentException("A game requires one or more arrivals, two tables, and one customer type per arrival");
        }
        for (int i = 0; i < arrivalSchedule.size(); i++) {
            int turn = arrivalSchedule.get(i);
            if (turn < 1 || turn > maxTurns) {
                throw new IllegalArgumentException("Arrival turns must be between 1 and maxTurns");
            }
            if (i > 0 && turn <= arrivalSchedule.get(i - 1)) {
                throw new IllegalArgumentException("Arrival turns must be strictly increasing");
            }
        }
    }

    /**
     * Builds the Stage 1 base setting with six Regular customers.
     *
     * @param seed seed used for reproducible food choices
     * @return the validated Stage 1 configuration
     */
    public static GameConfig stageOne(long seed) {
        return new GameConfig(18, 2, BASE_ARRIVALS, seed, Collections.nCopies(BASE_ARRIVALS.size(), CustomerType.REGULAR));
    }

    /**
     * Builds the Stage 2 base setting: the turn-4 arrival is a VIP and the turn-10 arrival is a
     * Critic; the remaining arrivals are Regular.
     *
     * @param seed seed used for reproducible food choices
     * @return the validated Stage 2 configuration
     */
    public static GameConfig stageTwo(long seed) {
        return new GameConfig(18, 2, BASE_ARRIVALS, seed, List.of(CustomerType.REGULAR, CustomerType.VIP,
                CustomerType.REGULAR, CustomerType.CRITIC, CustomerType.REGULAR, CustomerType.REGULAR));
    }

    public List<CustomerType> getCustomerTypes() { return customerTypes; }

    /**
     * Returns the customer type scheduled to arrive on the given turn.
     *
     * @param turn arrival turn to look up
     * @return the scheduled customer type
     * @throws IllegalArgumentException if no customer arrives on that turn
     */
    public CustomerType customerTypeAt(int turn) {
        int index = arrivalSchedule.indexOf(turn);
        if (index < 0) throw new IllegalArgumentException("No customer arrives on turn " + turn);
        return customerTypes.get(index);
    }
}
