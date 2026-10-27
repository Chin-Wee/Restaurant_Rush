package edu.ntu.sc2002.application;

import java.util.List;

public record GameConfig(int maxTurns, int numTables, List<Integer> arrivalSchedule, long randomSeed) {
    public GameConfig {
        arrivalSchedule = List.copyOf(arrivalSchedule);
        if (maxTurns != 18 || numTables != 2 || !arrivalSchedule.equals(List.of(1, 4, 7, 10, 13, 16))) {
            throw new IllegalArgumentException("Stage 1 requires 18 turns, two tables, and arrivals on turns 1, 4, 7, 10, 13, 16");
        }
    }

    public static GameConfig stageOne(long seed) { return new GameConfig(18, 2, List.of(1, 4, 7, 10, 13, 16), seed); }
}
