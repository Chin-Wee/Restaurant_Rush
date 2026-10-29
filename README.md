# Restaurant Rush

JavaFX pixel-art restaurant game for SC2002.

## Setup

Install **JDK 17** no need to install Maven or JavaFX handled on project

- **Mac:** `brew install openjdk@17`
- **Windows:** download [Adoptium Temurin 17](https://adoptium.net/temurin/releases/?os=windows&version=17), tick "Add to PATH" during install

Check it works:

```
java -version
```

Should show `17.x.x`.

## Run (GUI)

**Mac / Linux:**

```sh
./mvnw -Pvisualization clean install
./mvnw -f visualization/pom.xml javafx:run
```

**Windows:**

```powershell
.\mvnw.cmd -Pvisualization clean install
.\mvnw.cmd -f visualization\pom.xml javafx:run
```

## Run (CLI)

```sh
./mvnw clean install
java -cp core/target/classes edu.ntu.sc2002.ConsoleApp
```

## Stage 1 reproducible runs

Run `java -cp core/target/classes edu.ntu.sc2002.ConsoleApp 2026` after building.
Waiter choices: 1 seat, 2 take order, 3 serve, 4 wait. Chef choices: 1 advance one
preparation unit, 2 wait. Enter the customer ID when prompted.
Customers choose from the menu using Java `Random(seed + arrival ordinal)`;
arrival ordinals start at 1. Automatic payment prioritizes arrival order among
customers eligible to pay.

For a victory, use this schedule:

| Turn | Waiter | Chef |
| --- | --- | --- |
| 1 | Seat C1 | Wait |
| 2 | Take order C1 | Advance C1 |
| 3 | Serve C1 | Wait |
| 4 | Seat C2 | Wait |
| 5 | Take order C2 | Advance C2 |
| 6 | Wait | Advance C2 |
| 7 | Serve C2 | Wait |
| 8 | Seat C3 | Wait |
| 9 | Take order C3 | Advance C3 |
| 10 | Serve C3 | Wait |
| 11 | Seat C4 | Wait |
| 12 | Take order C4 | Advance C4 |
| 13 | Serve C4 | Wait |
| 14 | Seat C5 | Wait |
| 15 | Take order C5 | Advance C5 |
| 16 | Seat C6 | Advance C5 |
| 17 | Serve C5 | Wait |
| 18 | Take order C6 | Advance C6 |

Expected after turn 18: **VICTORY**, served 5, paid 4, departures 0, revenue
**$51.00**, average paid satisfaction **78.0**.

For a defeat with the same seed, choose Waiter **4** and Chef **2** on all 18 turns.
Expected: **DEFEAT**, served 0, paid 0, departures 2, revenue **$0.00**, average
paid satisfaction **N/A**. Both runs use all 18 turns.

## Javadoc

Javadoc comes with JDK 17. Generate the core API documentation from the repository root:

```sh
./mvnw javadoc:aggregate
```

Include the visualization API with `./mvnw -Pvisualization javadoc:aggregate`.
On Windows, use `.\mvnw.cmd` in place of `./mvnw`.
Open `target/reports/apidocs/index.html`. Generated documentation stays ignored by Git.

## Project structure

```
Restaurant_Rush/
├── core/           ← game engine, domain model
└── visualization/  ← JavaFX UI
```

## Asset credits

- City 2 backgrounds — Craftpix (non-commercial)
- Character sprites — Modern tiles free pack (non-commercial)
- Friendly Scribbles font — @kmlgames

## Phase 1 design and contracts

`RestaurantRush_ClassDiagram_Phase1.drawio` is the updated Phase 1 diagram merged
in revision `a7fc4f7`. It governs the Stage 1 structure; the assignment governs the
six-step turn rules. Stage 2/3 behavior is not included. The shipped-game reference
for the existing prepare/serve kitchen flow is
[Overcooked](https://www.playstation.com/en-us/games/overcooked-/); its timing rules
are not substituted for the assignment's turn rules. Existing pixel-art rendering,
animations, audio and assets are reused without selecting or changing assets.
Menu media players are disposed on shutdown so the normal launch path exits cleanly.

- `TurnController` holds `ScoreBoard`, the sole owner of service/departure counts,
  paid revenue, paid-time satisfaction and victory evaluation. `Restaurant` owns
  two tables, its queue, menu and three staff (one of each role). Active customers
  are derived from the queue and occupied tables; ScoreBoard retains the totals
  after payment or abandonment.
- Each `Customer` owns zero or one `Order`. Orders have no customer back-reference
  or order ID. Commands and presentation dishes use string customer IDs. An order
  stores its locked cents and preparation progress; timing uses explicit turn
  arguments. Table occupancy is derived from its customer reference.
- `Staff` is an identity-only abstract base. The controller accepts one Waiter
  task, then one Chef unit or Wait, then resolves eating, at most one payment,
  waiting loss and the final turn check. Failed commands leave these limits and
  engine state unchanged. Only `GAME_OVER` terminates either frontend.
- `ConsoleUI` and `JavaFXView` implement `GameView`. Immutable records and copied
  lists cross the view boundary; domain objects do not. The old GUI adapter is
  folded into `JavaFXView`. The unused, obsolete root `src/.../App.java` launcher
  was removed; the Maven visualization module owns the JavaFX entry point.

The updated queue contract is `dequeueCustomer(customer): void`. It removes that
customer, preserves all other queue positions, and rejects a missing target.
There is no zero-argument dequeue or separate public remove operation. The
renamed `Customer.isApplySatisfactionLoss(): void` applies the regular waiting
penalty. `FoodTypes` defines SALAD, BURGER and PASTA and drives base-menu setup.
`Restaurant` no longer exposes the removed `getCustomers()` or `abandon()` APIs.
It implements selected seating through the drawn `seat(customer)` operation:
validate the selection and free table, assign the customer, then dequeue that
customer. The drawn `Waiter.seatCustomer(queue, table)` seats the queue head when
used directly. The controller still charges every manager seating decision as
its one Waiter task, so this division does not add another action per turn.

The controller has no customer-history list or separate completed-turn counter.
Active customers are collected from the queue and occupied tables in arrival
order. Scheduled arrivals determine IDs and choice seeds, so IDs never restart
when earlier customers leave. Existing turn/task flags determine completed turns.
Snapshots contain active customers/orders; paid and abandoned records are no
longer retained as full objects. Payments, departure events and all aggregate
statistics remain available through TurnResolution and ScoreBoard.

The supplied diagram is unchanged. Necessary private helpers, read accessors,
immutable boundary rows and Java record methods remain implementation details.
Cashier's reversed parameter label `Restaurant: restaurant` is interpreted as
`restaurant: Restaurant`. Queue/table membership follows customer state: waiting
customers are queued, seated/eating/paying customers occupy a table, and paid or
abandoned customers occupy neither. Payment eligibility remains checked by
Cashier before the parameterless commit operations.

## Logic parity verification (2026-10-29)

The offline core/visualization build passed. External checks made 644 independent
logic assertions and compared 1,800 turns across 100 seeds with the previous
implementation. After excluding historical customer/order rows, all active
customer states, choices, counters, statistics and command resolutions matched.
Actual offline CLI victory, defeat, Pasta, selected-customer priority, invalid
input/action retries and three EOF paths also matched gameplay output. The only
snapshot-content change is the removal of departed customer/order history. The
comparison also checked all 27 modeled logic classifiers: 198 of 199 listed rows
match as written; Cashier's reversed parameter label is the sole literal discrepancy.

Run the build and README game schedules above to reproduce the game checks.
This direct selected-seating and turn-count check is reproducible after the build
on macOS or Linux:

```sh
java -ea --source 17 --class-path core/target/classes /dev/stdin <<'JAVA'
import edu.ntu.sc2002.application.*;
import edu.ntu.sc2002.application.contract.*;
import edu.ntu.sc2002.domain.*;
import edu.ntu.sc2002.domain.customer.*;
import edu.ntu.sc2002.domain.staff.*;
import java.util.*;
class PhaseOneCheck {
    public static void main(String[] args) {
        var room = new Restaurant(2);
        var first = new Customer("C1", 1, 2027);
        var chosen = new Customer("C2", 2, 2028);
        room.registerArrival(first);
        room.registerArrival(chosen);
        assert room.seat(chosen).getSeatedCustomer() == chosen;
        assert room.getWaitingQueue().getQueuedCustomers().equals(List.of(first));
        assert room.getStaff(Waiter.class).seatCustomer(room.getWaitingQueue(), room.getTables().get(1));
        var game = new TurnController(GameConfig.stageOne(2026));
        for (int turn = 1; turn <= 18; turn++) {
            game.beginTurn();
            assert game.currentTurn() == turn && game.snapshot().turnsUsed() == turn - 1;
            game.processArrival();
            if (turn == 16) assert game.snapshot().waitingQueue().contains("C6");
            assert game.submitWaiterDecision(new WaiterChoiceDTO(WaiterTaskType.WAIT, null)).isSuccessful();
            assert game.submitChefDecision(new ChefChoiceDTO(ChefTaskType.WAIT, null)).isSuccessful();
            assert game.snapshot().turnsUsed() == turn && game.lastResolution().turn() == turn;
        }
        assert game.phase() == TurnPhase.GAME_OVER;
        assert game.snapshot().unhappyDepartures() == 2;
    }
}
JAVA
```

Temporary test sources, classes, traces, logs and screenshots were kept outside
the checkout and removed after verification. No test framework, dependency,
source test or production test hook was added. No stage tag was moved.

## Historical V2 verification notes (2026-10-29)

Build both modules using the existing local Maven cache:

```sh
./mvnw -o -Pvisualization clean install -DskipTests
```

`-o` requires dependencies to have been downloaded once. The game itself needs no
network access. To launch the GUI directly with a reproducible seed:

```sh
./mvnw -o -f visualization/pom.xml javafx:run -Djavafx.args="--gameDirect=true --seed=2026"
```

Use the victory/defeat schedules above in either frontend. In the GUI select a
Waiter task and Submit, select a Chef task and Submit, then Resolve. Newly taken
orders are available immediately in the Chef dropdown.

The assignment's Pasta example is reproducible with seed **2027**: seat C1 on
turn 1; take C1's order and advance it on turn 2; Waiter Wait and advance C1 on
turn 3; serve C1 and Chef Wait on turn 4; both staff Wait on turns 5 and 6.
Expected: READY on 3, SERVED on 4, READY_TO_PAY on 5, PAID on 6, revenue **$18.00**,
recorded satisfaction **76**. Continue to turn 18; there is no early ending.

Verification used temporary harnesses outside the checkout, with no added test
framework, dependency, source test, debug flag or production test hook:

| Check | Recorded outcome |
| --- | --- |
| Actual CLI processes, seed 2026 | Both complete 18-turn README outcomes reproduced before and after refactoring. |
| Actual JavaFX controls | Full victory and defeat selected through ComboBox controls and Submit/Resolve buttons; normalized snapshots matched CLI/controller traces on every turn. The normal Start Game path, clean shutdown and direct launch were exercised. C2 was also seated ahead of queued C1 through the controls. |
| Independent assignment model | A separate six-step model reproduced all 18 end-of-turn semantic states for both schedules; baseline and refactored controller/GUI traces matched it after V2 enum renames. |
| Core edge checks | 152 assertions covered setup/menu, immutable boundaries, deterministic choices, selected seating, preparation and timing, locked prices, paid-time averaging, oldest eligible payment, abandonment/cancellation/cleanup, clamping, task limits and all three victory boundaries. |
| CLI rejection/retry | 15 invalid-number/ID/action retries preserved every turn outcome. Full-table rejection, EOF at four input points and invalid seed/argument count were also exercised. |
| JavaFX rejection/retry | Re-firing stale Waiter and Resolve buttons on every turn explained rejection and left engine snapshots unchanged; current controls remained usable. Reading manager choices did not submit commands. |
| Pasta timing | Actual CLI and JavaFX controls reproduced the turn 3/4/5/6 states and paid satisfaction 76. |
| Offline execution | macOS `sandbox-exec -p '(version 1) (allow default) (deny network*)'` ran CLI victory/defeat/retry checks and GUI victory/defeat/Pasta runs successfully. |

Temporary harnesses, traces, screenshots and logs were removed after recording the
results. No temporary tests remain in the repository. Existing stage tags remain
unchanged; this refactor does not retag the original Stage 1 submission.

The external audit checked **29 modeled types and 199 declared members**, including
public parameter signatures, private modeled fields, interface implementation and
staff inheritance. The commands below record the temporary verification entry
points; their harness files were deleted afterward as required.

```sh
CHECKS=/tmp/restaurant-rush-v2.7fTg8e
java -cp core/target/classes:"$CHECKS/refactor" CoreCheck
python3 "$CHECKS/cli.py" core/target/classes final
python3 "$CHECKS/cli_edges.py"
python3 "$CHECKS/assignment_model.py"
python3 "$CHECKS/audit.py"
FX=$(find /Users/cwee/.m2/repository/org/openjfx -name '*21.0.12-mac-aarch64.jar' | paste -sd: -)
sandbox-exec -p '(version 1) (allow default) (deny network*)' java --module-path "$FX" --add-modules javafx.controls,javafx.media -cp core/target/classes:visualization/target/classes:"$CHECKS/refactor" GuiCheck --out="$CHECKS/refactor-gui-victory.trace"
```

Additional GUI runs used the same command with `--gameDirect=true --defeat`,
`--gameDirect=true --seed=2027 --pasta`, or `--gameDirect=true --priority` and a
separate output path. Each run used actual controls on the JavaFX application
thread, including stale-control retries. Screenshots were inspected before
removal. The feature graph was refreshed with `graphify update . --force` (the
refactor removes old methods) and host-extracted README semantics. Graphify folds
the two seating overloads into one node, producing a self-loop for their call;
the direct source/signature audit distinguishes them. Drawio is unsupported by
Graphify and was audited directly. Canonical graph integration remains a parent
step after this branch is reviewed and integrated into `main`.
