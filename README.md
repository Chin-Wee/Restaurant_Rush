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

## Phase 1 V2 design and contracts

`RestaurantRush_ClassDiagram_Phase1.drawio` is an unchanged copy of the supplied
Phase 1 V2 diagram. It governs the Stage 1 structure; the assignment governs the
six-step turn rules. Stage 2/3 behavior is not included. The shipped-game reference
for the existing prepare/serve kitchen flow is
[Overcooked](https://www.playstation.com/en-us/games/overcooked-/); its timing rules
are not substituted for the assignment's turn rules. Existing pixel-art rendering,
animations, audio and assets are reused without selecting or changing assets.
Menu media players are disposed on shutdown so the normal launch path exits cleanly.

- `TurnController` holds `ScoreBoard`, the sole owner of service/departure counts,
  paid revenue, paid-time satisfaction and victory evaluation. `Restaurant` owns
  two tables, its queue, menu, three staff (one of each role), and customer history.
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

V2 omits several implementation details. These are explicit additions rather than
changes to the supplied diagram:

| Detail | Implementation and reason |
| --- | --- |
| Selected seating vs. `Waiter.seatCustomer(queue, table)` | Keep that two-argument operation for the queue head; a three-argument helper accepts the manager's selected customer. `Restaurant.seat(customer)` uses the helper, removes only that customer and preserves the remaining queue order. |
| Sequential choices vs. `getManagerChoices()` | Console collects and submits Waiter then Chef with retry loops and returns the accepted pair. Its caller never submits that pair again. JavaFX events submit the Waiter once, stage the Chef, and submit the Chef once on Resolve; the getter only reads the staged pair, returning `null` until complete. It never blocks the JavaFX thread. |
| Midgame `GameResult` | V2 defines only `VICTORY` and `DEFEAT`. Results during play evaluate current thresholds provisionally. Both frontends use `TurnPhase.GAME_OVER` to terminate after turn 18, including when thresholds were met earlier. |
| Parameterless `paid()` / `completePayment(customer)` | These commit a customer already in `READY_TO_PAY`. `Cashier.processPayment(customer, restaurant, turn)` checks `canPay(turn)` first, enforcing the extra payment delay at the turn-aware boundary. |
| Read access and history | Customer exposes `order()`, `arrivalOrdinal()` and `isWaitingForTableOrFood()`; Order exposes `lockedPriceCents()`, `unitsDone()` and `servedTurn()`; Table exposes `tableId()`. Restaurant retains customer history for `getCustomers()` and completed/departed snapshot rows. None adds another order owner. |
| Controller/view bookkeeping | Completed-turn count, immutable snapshot row records, event/payment/departure buffers, seed/money helpers, and presentation state support the listed operations. Extra view fields retain rendering and selected choices; they contain no game rules. |
| Diagram notation omissions | Record constructors/accessors are implicit in Java. The Cashier parameter label `Restaurant: restaurant` is read as `restaurant: Restaurant`. The drawn Waiter dependency ends at a Customer operation cell; service calls the listed `Customer.serveOrder(turn)`. The supplied drawing remains byte-for-byte unchanged. |

No unresolved contract conflict requires an ownership or gameplay amendment.

## Stage 1 verification notes (2026-10-29)

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
