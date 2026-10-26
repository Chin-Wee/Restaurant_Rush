package edu.ntu.sc2002.ui.console;
import edu.ntu.sc2002.application.*;
import edu.ntu.sc2002.application.contract.*;

import static org.junit.Assert.*;
import edu.ntu.sc2002.ConsoleApp;
import edu.ntu.sc2002.ui.console.ConsoleUI;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Pattern;
import org.junit.Test;

public class ConsoleUiTest {
    private String capture(GameController game, String commands) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            new ConsoleUI(new Scanner(commands), game).run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private String commands(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/stage1/" + name + "-seed-2026.txt")) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test public void recordedCliVictoryRunsAllEighteenTurnsAndShowsChoicesBeforeChef() throws Exception {
        GameController game = new GameController();
        String output = capture(game, commands("victory"));
        assertTrue(output.contains("VICTORY"));
        for (String required : new String[] {"Queue:", "Table 1:", "Table 2:", "satisfaction 92",
                "READY", "SERVED", "PAID", "PREPARING", "Chef:", "revenue $51.00", "turns used 18"}) {
            assertTrue("Missing display field: " + required, output.contains(required));
        }
        assertEquals(18, Pattern.compile("(?m)^TURN \\d+").matcher(output).results().count());
        assertEquals(6, Pattern.compile("(?m)^Arrival: C\\d+").matcher(output).results().count());
        assertTrue(output.indexOf("TURN 1") < output.indexOf("Arrival: C1"));
        int choice = output.indexOf("C1 orders Salad.");
        assertTrue(choice > output.indexOf("TURN 2"));
        assertTrue(output.indexOf("Chef task", choice) < output.indexOf("End turn 2:"));
        assertEquals(1, output.split("C1 orders Salad\\.", -1).length - 1);
        assertEquals(4, game.snapshot().paidCount());
        assertEquals(5, game.snapshot().servedCount());
        assertEquals(5100, game.snapshot().revenueCents());
        assertEquals(78.0, game.snapshot().averagePaidSatisfaction(), 0.0);
        assertEquals(18, game.snapshot().turnsUsed());
    }

    @Test public void recordedCliDefeatKeepsNaAndZeroUnpaidRevenue() throws Exception {
        GameController game = new GameController();
        String output = capture(game, commands("defeat"));
        assertTrue(output.contains("DEFEAT"));
        assertTrue(output.contains("Average paid satisfaction N/A"));
        assertTrue(output.contains("revenue $0.00"));
        assertEquals(0, game.snapshot().paidCount());
        assertEquals(2, game.snapshot().unhappyDepartures());
        assertNull(game.snapshot().averagePaidSatisfaction());
        assertEquals(18, game.currentTurn());
    }

    @Test public void malformedNumberAndInvalidTargetRetryWithoutSpendingTask() {
        GameController game = new GameController();
        String firstTurn = "bad\n2\nC1\n1\nC1\n2\n";
        String output = capture(game, firstTurn + "4\n2\n".repeat(17));
        assertTrue(output.contains("Choose 1–4."));
        assertTrue(output.contains("Choose a seated customer."));
        assertTrue(output.contains("Waiter seats C1 at Table 1."));
        assertEquals(18, game.currentTurn());
    }

    @Test public void eofInterruptsCleanlyAndDoesNotInventResult() {
        GameController game = new GameController(GameConfig.stageOne(2));
        String output = capture(game, "");
        assertTrue(output.contains("seed 2"));
        assertTrue(output.contains("Game interrupted"));
        assertFalse(output.contains("VICTORY"));
        assertFalse(output.contains("DEFEAT"));
        assertNull(game.result());
        assertEquals(0, game.snapshot().turnsUsed());
    }

    @Test public void malformedSeedShowsFeedbackInsteadOfStackTrace() {
        PrintStream original = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            ConsoleApp.main(new String[] {"invalid"});
        } finally {
            System.setErr(original);
        }
        assertTrue(buffer.toString(StandardCharsets.UTF_8).contains("Seed must be a 64-bit integer."));
    }
}
