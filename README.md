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

On the title screen, **Menu** reveals Easy / Medium / Hard. **Easy is selected
by default** and is the original Stage 3 game.

## Run (CLI)

```sh
./mvnw clean install
java -cp core/target/classes edu.ntu.sc2002.ConsoleApp [seed] [easy|medium|hard]
```

The seed defaults to `2026` and the difficulty defaults to `easy`.

## Stage 1: original three-item schedule (historical)

These outcomes belong to the original three-item Stage 1 menu. This branch's
Stage 3 menu adds three combo meals and the Host has its own manager choice, so
seeded dish choices and seating differ here; use the Stage 3 runs below for this
checkout.

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

## Stage 2: VIP and Critic customers

The base setting keeps six arrivals on turns 1, 4, 7, 10, 13 and 16. The turn-4 arrival (C2) is a **VIP** and the turn-10 arrival (C4) is a **Critic**; the rest are Regular. `GameConfig.stageOne(seed)` still builds six Regulars.

| Customer | Satisfaction loss per waiting turn | Bill | Order choice |
| --- | --- | --- | --- |
| Regular | 8 | Full price | Seeded random (`Random(seed + arrival ordinal)`) |
| VIP | 5 | 10% off, rounded to the nearest cent | Seeded random |
| Critic | 12 | Full price | Always **Pasta**; the most expensive item if it is not on the menu |

`Customer` is now abstract; `RegularCustomer`, `VIPCustomer` and `CriticCustomer` supply `satisfactionLossPerTurn` and `calculateBill`; `CriticCustomer` also overrides `chooseItem`. `TurnController` creates arrivals through its `CustomerFactory`, the only place that maps a `CustomerType` to a subclass. The bill is locked into the order when it is taken, e.g. `C2 orders Pasta for $16.20 (menu $18.00).` The GUI tags VIPs in gold and Critics in red. Stage 3 changed one shared method name: `Customer.applyWaitingLoss()` replaces `isApplySatisfactionLoss()` and applies the type's waiting loss only while the customer is waiting for a table or food.

## Stage 3: Host, combo meals and Happy Hour

The Phase 3 class diagram (`RestaurantRush_ClassDiagram_Phase3.drawio`) governs this
stage. Stage 3 supports three data-only difficulty presets. **Easy** keeps the
original Stage 2 schedule and mix through `GameConfig.stageTwo(seed)`; **Medium**
and **Hard** use the existing `GameConfig` constructor with a shorter turn limit,
a denser single-arrival schedule and a different customer-type mix. No new
class-diagram class, field or method is added for difficulty.

### Rules

- **Host.** The manager gives the Host one task each turn: seat any one queued
  customer at the first free table, or wait. This is in addition to the Waiter's
  task and does not consume it. `Host.seatCustomer(customer, queue, table)` is the
  same operation as the Waiter's. If every table is occupied, only Wait is
  available and the customer keeps queueing.
- **Combo meals.** `Menu` keeps the three base items (Salad $9.00 / 1 unit,
  Burger $15.00 / 1 unit, Pasta $18.00 / 2 units) and adds every pair as a
  `ComboMeal`: Salad + Burger $21.60 / 2 units, Salad + Pasta $24.30 / 3 units,
  Burger + Pasta $29.70 / 3 units. Preparation is the sum of the two components;
  the price is 90% of their combined price, rounded to the nearest cent.
- **Combo choice rule.** A customer chooses uniformly at random from the whole
  menu (base items and combos) with `Random(seed + arrival ordinal)`, the same
  deterministic rule used in earlier stages. Critics keep their preference:
  Pasta, or the most expensive item if Pasta is absent. Combo items are ordinary
  `MenuItem` values to the order and kitchen flow, so they use the same
  preparation, serve and payment timing.
- **Happy Hour.** The manager may switch it on once per game at the start of a
  turn, before the Host acts. It applies to orders taken that turn and the
  next. Orders taken while it is active cost 20% less; the customer's own
  discount applies first, then Happy Hour, and the price is locked at order
  time. Example: a VIP's Burger is $15.00 x 0.9 x 0.8 = $10.80. Customers still
  waiting for a table or food recover 15 satisfaction at the start of each
  active turn, capped at 100. Orders already placed and customers who have left
  are unaffected. The CLI prints `AVAILABLE`, `ACTIVE (n turns remaining)` or
  `EXHAUSTED`; the GUI shows the same status in the Manager card, enables the
  button only before the Host acts, and disables it after use.

### Difficulty

Difficulty only changes `GameConfig` data. Every preset still has two tables,
one typed arrival per listed turn, and the same victory threshold (`paid >= 4`,
`revenue >= $45.00`, `average paid satisfaction >= 60`).

| Difficulty | Turns | Arrival turns | Customer mix |
| --- | ---: | --- | --- |
| Easy | 18 | 1, 4, 7, 10, 13, 16 | Regular, VIP, Regular, Critic, Regular, Regular |
| Medium | 16 | 1, 2, 4, 6, 8, 10, 12, 14 | Regular, VIP, Regular, Critic, Regular, Critic, VIP, Regular |
| Hard | 14 | 1, 2, 3, 5, 6, 8, 9, 11, 12 | Regular, Critic, VIP, Critic, Regular, Critic, VIP, Critic, Regular |

Easy is unchanged. Medium adds two arrivals and removes two turns. Hard adds
three more arrivals, removes four more turns and leans on Critics, so the
manager has to choose which customers to seat, order for and serve. Arrival
turns must be strictly increasing; multiple customers on the same turn are not
supported.

### Files added or changed for Stage 3

| File | Change and reason |
| --- | --- |
| `domain/staff/Host.java` | New staff role with `seatCustomer(customer, queue, table)`, the same seating operation as the Waiter. |
| `application/contract/HostTaskType.java`, `HostDecision.java` | The manager's Host task (`SEAT`/`WAIT`) and its selected customer. |
| `domain/menu/ComboMeal.java` | New `MenuItem` subtype holding two components; computes the sum of prep units and 90% price. |
| `domain/menu/MenuItem.java` | Removed `final` so `ComboMeal` can extend it, as the diagram's generalization requires. |
| `domain/menu/Menu.java` | Builds the three pairwise combos; `addItem` stores the supplied item so a combo keeps its runtime type. |
| `domain/HappyHourPolicy.java`, `domain/HappyHourStatus.java` | New once-per-game policy and its `AVAILABLE`/`ACTIVE`/`EXHAUSTED` states. |
| `domain/Restaurant.java` | Owns the `HappyHourPolicy`, registers the Host, allows one of each of the four roles, and no longer seats customers itself. |
| `domain/staff/Waiter.java` | `seatCustomer(customer, queue, table)` seats the manager's selected customer; `takeOrder` accepts the policy and applies Happy Hour after the customer's own bill calculation. |
| `domain/customer/Customer.java` | Renamed the waiting penalty to `applyWaitingLoss`; added `isWaitingForTableOrFood` for recovery and departure checks. |
| `application/GameConfig.java` | Difficulty uses the existing record fields; validation now accepts any strictly increasing single-arrival schedule within `maxTurns`, still with two tables. |
| `application/TurnController.java` | Host, Waiter and Chef decision handling, first-free-table allocation for seating, Happy Hour activation and validation, per-active-turn recovery, locked discounted orders, and duration decrement. |
| `application/contract/GameSnapshot.java`, `TurnChoicesDTO.java` | Snapshot exposes Host, Waiter and Chef actions plus happy-hour status/duration; manager choices carry the Host, Waiter and Chef decisions and the activation flag. |
| `ui/console/ConsoleUI.java` | Host prompt, Host-first ordering and Happy Hour prompt/status. |
| `ConsoleApp.java`, `visualization/App.java`, `ui/StartMenuScreen.java` | Difficulty argument/menu selection and the Easy/Medium/Hard `GameConfig` presets; Easy is the default. |
| `visualization/.../RestaurantScreen.java`, `JavaFXView.java` | Host card, Host-first gating, Chef gating, Happy Hour card and Stage 3 label. |
| `visualization/.../RestaurantScene.java` | Adds the Host actor; it meets guests on a free neighbouring tile, escorts them to the chair, then returns to its stand. The room shows the first three queued guests plus a `+N waiting` count; the customer card still lists the full queue. |

Menu helper `Menu.addItem` no longer copies its argument: a defensive copy would
flatten a `ComboMeal` into a plain `MenuItem`. No caller relied on the copy.

Seating follows the updated Phase 3 diagram, synced with the Stage 2 refactor:
the Waiter and the Host expose the same `seatCustomer(customer, queue, table)`
operation, the controller picks the first free table, and `Restaurant.seat(customer)`
was removed. The Waiter and the Host both seat the manager's selected customer;
the Host has its own once-per-turn decision and never consumes the Waiter's task.

### Stage 3 reproducible runs (seed 2026)

Build first with `.\mvnw.cmd -Pvisualization clean install` (Windows) or
`./mvnw -Pvisualization clean install`, then run one of:

```sh
java -cp core/target/classes edu.ntu.sc2002.ConsoleApp 2026 easy
java -cp core/target/classes edu.ntu.sc2002.ConsoleApp 2026 medium
java -cp core/target/classes edu.ntu.sc2002.ConsoleApp 2026 hard
```

The Host uses `1` plus a customer ID to seat and `2` to wait. The Waiter uses
`1` seat, `2` take order, `3` serve and `4` wait; the Chef uses `1` advance and
`2` wait. In every schedule below, activate Happy Hour on turn 1 where stated.

#### Easy (default, unchanged)

For a victory, activate Happy Hour on turn 1 (answer `1`), then use:

| Turn | Host task | Waiter task | Chef task |
| --- | --- | --- | --- |
| 1 | Seat C1 | Take order C1 | Advance C1 |
| 2 | Wait | Wait | Advance C1 |
| 3 | Wait | Serve C1 | Wait |
| 4 | Seat C2 | Take order C2 | Advance C2 |
| 5 | Wait | Wait | Advance C2 |
| 6 | Wait | Serve C2 | Wait |
| 7 | Seat C3 | Take order C3 | Advance C3 |
| 8 | Wait | Serve C3 | Wait |
| 9 | Wait | Wait | Wait |
| 10 | Seat C4 | Take order C4 | Advance C4 |
| 11 | Wait | Wait | Advance C4 |
| 12 | Wait | Serve C4 | Wait |
| 13 | Seat C5 | Take order C5 | Advance C5 |
| 14 | Wait | Wait | Advance C5 |
| 15 | Wait | Wait | Advance C5 |
| 16 | Seat C6 | Serve C5 | Wait |
| 17 | Wait | Take order C6 | Advance C6 |
| 18 | Wait | Wait | Advance C6 |

C1's seeded combo is Salad + Burger and its Happy Hour bill is $17.28 (menu
$21.60); C2 is the VIP on Pasta ($16.20), C4 is the Critic on Pasta ($18.00),
and C5/C6 choose Burger + Pasta ($29.70). Expected after turn 18: **VICTORY**,
served 5, paid 5, departures 0, revenue **$90.18**, average paid satisfaction
**85.2**, turns used 18.

For an Easy defeat, answer `2` to the Happy Hour prompt every turn, choose
Waiter **4** and Chef **2** on all 18 turns, and give the Host `1` + C1 on
turn 1, `1` + C2 on turn 4, `1` + C3 on turn 14, and `2` (wait) on every other
turn. Expected: **DEFEAT**, served 0, paid 0, departures 2, revenue **$0.00**,
average paid satisfaction **N/A**, turns used 18.

To check VIP-first stacking directly, use the Easy victory schedule but activate
Happy Hour on turn 4 instead; C2's Pasta is then billed at $12.96
($18.00 x 0.9 x 0.8) and the game still ends in victory.

#### Medium

Activate Happy Hour on turn 1, then use:

| Turn | Host task | Waiter task | Chef task |
| --- | --- | --- | --- |
| 1 | Seat C1 | Take order C1 | Advance C1 |
| 2 | Seat C2 | Take order C2 | Advance C2 |
| 3 | Wait | Wait | Advance C2 |
| 4 | Wait | Serve C2 | Advance C1 |
| 5 | Wait | Serve C1 | Wait |
| 6 | Wait | Wait | Wait |
| 7 | Wait | Seat C4 | Wait |
| 8 | Wait | Seat C3 | Wait |
| 9 | Wait | Take order C4 | Advance C4 |
| 10 | Wait | Take order C3 | Advance C4 |
| 11 | Wait | Serve C4 | Advance C3 |
| 12 | Wait | Serve C3 | Wait |
| 13 | Wait | Wait | Wait |
| 14 | Seat C7 | Take order C7 | Advance C7 |
| 15 | Seat C6 | Take order C6 | Advance C7 |
| 16 | Wait | Serve C7 | Advance C6 |

Expected: **VICTORY**, served 5, paid 4, departures 0, revenue **$57.24**,
average paid satisfaction **60.5**, turns used 16. The paid threshold is met
exactly, so ignoring the extra arrivals or serving the wrong pair fails.

For a Medium defeat, answer `2` to Happy Hour every turn and choose Host **2**,
Waiter **4** and Chef **2** on all 16 turns. Expected: **DEFEAT**, served 0,
paid 0, departures 3, revenue **$0.00**, average paid satisfaction **N/A**,
turns used 16.

#### Hard

Activate Happy Hour on turn 1, then use:

| Turn | Host task | Waiter task | Chef task |
| --- | --- | --- | --- |
| 1 | Seat C1 | Take order C1 | Advance C1 |
| 2 | Seat C2 | Take order C2 | Advance C2 |
| 3 | Wait | Wait | Advance C1 |
| 4 | Wait | Serve C1 | Advance C2 |
| 5 | Wait | Serve C2 | Wait |
| 6 | Wait | Wait | Wait |
| 7 | Wait | Seat C3 | Wait |
| 8 | Seat C6 | Take order C3 | Advance C3 |
| 9 | Wait | Serve C3 | Wait |
| 10 | Wait | Take order C6 | Advance C6 |
| 11 | Wait | Wait | Advance C6 |
| 12 | Seat C7 | Serve C6 | Wait |
| 13 | Wait | Take order C7 | Advance C7 |
| 14 | Wait | Wait | Advance C7 |

Expected: **VICTORY**, served 4, paid 4, departures 1, revenue **$57.78**,
average paid satisfaction **67.5**, turns used 14. This is intentionally tight:
exactly four paid customers, one departure and no spare turn if the wrong
customer is prioritised.

For a Hard defeat, answer `2` to Happy Hour every turn and choose Host **2**,
Waiter **4** and Chef **2** on all 14 turns. Expected: **DEFEAT**, served 0,
paid 0, departures 3, revenue **$0.00**, average paid satisfaction **N/A**,
turns used 14.

The GUI difficulty selector passes the same three `GameConfig` presets; Easy is
selected by default. Medium and Hard were verified through the CLI, and the Easy
GUI victory and defeat were driven through the real ComboBox and Button controls
with an external harness on seed 2026, reproducing the CLI totals. The harness
was kept outside the checkout and removed after the run. The top HUD wraps instead of clipping when the window is
narrow, and the Host meets a guest on a free neighbouring tile, escorts them to
the chair, then returns to its stand.

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
  two tables, its queue, menu and four staff (Waiter, Chef, Cashier and Host). Active customers
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
`Customer.applyWaitingLoss(): void` method (named `isApplySatisfactionLoss` in
the Stage 1 contract) applies the regular waiting penalty. `FoodTypes` defines SALAD, BURGER and PASTA and drives base-menu setup.
`Restaurant` no longer exposes the removed `getCustomers()`, `abandon()` or
`seat(customer)` APIs. Seating runs through the staff: the controller validates
the choice, picks the first free table, and `Waiter.seatCustomer(customer, queue,
table)` assigns and dequeues the manager's selected customer. The Stage 3 Host
exposes the same operation and receives its own once-per-turn `HostDecision`, so
it never consumes the Waiter's task. The controller still charges every manager
seating decision as its one Waiter task, so this division does not add another
Waiter action per turn.

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

## Lookup contract verification (2026-10-30)

`Restaurant.tableFor`, `Restaurant.oldestPaymentCandidate`,
`Table.getSeatedCustomer` and `Menu.getItemByName` return a value or throw when their
lookup precondition is unmet. The controller checks occupancy and payment
eligibility before querying. Waiting customers are not queried for a table;
seated abandonment captures its table before clearing membership. Payment uses
the customer's existing table before committing and releasing it.

The two single-use eating/payment and abandonment helpers are inlined into the
turn-resolution operation. Shared validation, owned-data read accessors and the
event/payment/departure buffers remain to preserve command behavior and
`lastResolution()` data. The supplied diagram is unchanged.

Both modules built offline. External checks passed 573 logic assertions and
compared every snapshot and command response for 1,800 turns across 100 seeds
with the preceding implementation; the traces were byte-identical. Offline CLI
victory, defeat, Pasta, priority seating, rejection/retry and three EOF paths also
produced byte-identical output. The API change is limited to absent domain
lookups throwing instead of returning null.

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
        try {
            room.getTables().get(0).getSeatedCustomer();
            throw new AssertionError("An empty table must reject customer lookup");
        } catch (IllegalStateException expected) {}
        try {
            room.oldestPaymentCandidate(1);
            throw new AssertionError("No payment candidate exists");
        } catch (IllegalStateException expected) {}
        try {
            room.getMenu().getItemByName("missing");
            throw new AssertionError("An unknown menu item must be rejected");
        } catch (IllegalArgumentException expected) {}
        var first = new RegularCustomer("C1", 1, 2027);
        var chosen = new RegularCustomer("C2", 2, 2028);
        room.registerArrival(first);
        room.registerArrival(chosen);
        try {
            room.tableFor(first);
            throw new AssertionError("A queued customer has no table");
        } catch (IllegalArgumentException expected) {}
        var waiter = room.getStaff(Waiter.class);
        assert waiter.seatCustomer(chosen, room.getWaitingQueue(), room.getTables().get(0));
        assert room.getTables().get(0).getSeatedCustomer() == chosen;
        assert room.getWaitingQueue().getQueuedCustomers().equals(List.of(first));
        assert waiter.seatCustomer(first, room.getWaitingQueue(), room.getTables().get(1));
        var game = new TurnController(GameConfig.stageOne(2026));
        for (int turn = 1; turn <= 18; turn++) {
            game.beginTurn();
            assert game.currentTurn() == turn && game.snapshot().turnsUsed() == turn - 1;
            game.processArrival();
            if (turn == 16) assert game.snapshot().waitingQueue().contains("C6");
            assert game.submitHostDecision(new HostDecision(HostTaskType.WAIT, null)).isSuccessful();
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

Use the victory/defeat schedules above in either frontend. In the GUI, activate
Happy Hour if desired, then select the Host task and Submit, select the Waiter
task and Submit, select the Chef task and Submit, then Resolve. The Waiter card
stays disabled until the Host has acted, and the Chef card stays disabled until
both the Host and Waiter have acted. Newly taken orders are available
immediately in the Chef dropdown.

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
