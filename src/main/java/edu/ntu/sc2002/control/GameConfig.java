package edu.ntu.sc2002.control;
import java.util.List;
public record GameConfig(int maxTurns, int numTables, List<Integer> arrivalSchedule, long randomSeed) {
    public GameConfig { arrivalSchedule = List.copyOf(arrivalSchedule); }
    public static GameConfig stageOne(long seed) {
        return new GameConfig(18, 2, List.of(1, 4, 7, 10, 13, 16), seed);
    }
}
