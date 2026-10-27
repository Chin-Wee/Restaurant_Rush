package edu.ntu.sc2002.application;

import java.util.List;

public record GameConfig(int maxTurns, int numTables, List<Integer> arrivalSchedule, long randomSeed) {
    public GameConfig {
        arrivalSchedule = List.copyOf(arrivalSchedule);
        if (maxTurns < 1 || numTables < 1 || arrivalSchedule.stream().anyMatch(t -> t < 1 || t > maxTurns)
                || arrivalSchedule.stream().distinct().count() != arrivalSchedule.size()
                || !arrivalSchedule.equals(arrivalSchedule.stream().sorted().toList())) {
            throw new IllegalArgumentException("Invalid game configuration");
        }
    }

    public static GameConfig stageOne(long seed) { return new GameConfig(18, 2, List.of(1, 4, 7, 10, 13, 16), seed); }
}
