# Restaurant Rush — Stage 3 AI-Assisted Development History

- Consolidated on 2026-11-14
- SC2002 Restaurant Rush, Stage 3
- [Class diagram](../../docs/uml/RestaurantRush_ClassDiagram_Phase3.drawio)

> This is a curated history of the substantive prompts, decisions, replies and
> verification results. Tool calls, raw build logs and repetitive prompt text are
> omitted. The commands and outcomes below describe the development checks.

The team supplied the Phase 3 diagram and chose the seating, Host and difficulty
behavior. AI assistance covered implementation and refactoring, UML comparisons,
and CLI and GUI test inputs. The team corrected the diagram during review.

---

## 1. Project goal and final state

The Stage 3 work added and integrated:

- **Host** as a fourth staff role, with a manager `HostChoiceDTO` decision.
- **Combo meals** in the menu.
- **Happy Hour** with a `Restaurant`-owned `HappyHourPolicy`.
- **Difficulty presets** (Easy / Medium / Hard) using only existing `GameConfig`
  data.
- CLI and JavaFX support for Host, Happy Hour and difficulty.
- Visual polish for queue layout, staff movement and title-screen styling.

The final implementation is the main Stage 3 project. The class diagram is the
updated Phase 3 diagram, and the code follows its modeled members.

---

## 2. Key design decisions

### 2.1 Seating

The final design follows the updated diagram:

```text
Host:   seatCustomer(customer, queue, table): boolean
Waiter: seatCustomer(customer, queue, table): boolean
```

- The controller chooses the first free table.
- The Waiter and Host seat an explicitly selected customer.
- `Restaurant.seat(customer)` is not used.

### 2.2 Host decision

After initially following an older diagram with automatic Host seating, the
updated diagram added:

```text
HostChoiceDTO(taskType: HostTaskType, targetCustomerId: String)
HostTaskType: SEAT, WAIT
TurnController.submitHostDecision(HostChoiceDTO)
TurnController.hostTaskUsed
GameSnapshot.hostActions
TurnChoicesDTO.hostChoice
```

The Host became a manager decision, not automatic seating.

### 2.3 Happy Hour

The final diagram added:

```text
HappyHourPolicy
HappyHourStatus: AVAILABLE, ACTIVE, EXHAUSTED
Restaurant.happyHourPolicy
Restaurant.getHappyHourPolicy()
TurnController.activateHappyHour()
GameSnapshot.happyHourStatus
GameSnapshot.happyHourRemainingTurns
TurnChoicesDTO.activateHappyHour
Waiter.takeOrder(customer, menu, happyHourPolicy)
```

The manager may activate it once per game before the Host acts. It lasts for the
activation turn and the next, applies 20% off after the customer's own discount,
and recovers 15 satisfaction for waiting/seated customers at the start of each
active turn.

### 2.4 Difficulty

Difficulty is data-only and does not add class-diagram members:

| Difficulty | Turns | Arrival turns | Customer mix |
| --- | ---: | --- | --- |
| Easy | 18 | 1, 4, 7, 10, 13, 16 | Regular, VIP, Regular, Critic, Regular, Regular |
| Medium | 16 | 1, 2, 4, 6, 8, 10, 12, 14 | Regular, VIP, Regular, Critic, Regular, Critic, VIP, Regular |
| Hard | 14 | 1, 2, 3, 5, 6, 8, 9, 11, 12 | Regular, Critic, VIP, Critic, Regular, Critic, VIP, Critic, Regular |

Easy is the default and preserves the original Stage 3 schedule. Multiple
customers on the same turn are not supported; arrival turns must be strictly
increasing.

### 2.5 Diagram alignment

The updated diagram had a few internal contradictions that the user later fixed:

- `HostChoiceDTO.getTaskType()` must return `HostTaskType`, not `ChefTaskType`.
- `TurnChoicesDTO` constructor must include `hostChoice`.
- `GameSnapshot` constructor must include `hostActions`.
- `GameSnapshot` field order should match the constructor order:
  `hostActions`, `happyHourStatus`, `happyHourRemainingTurns`.

The final code follows the corrected field lists. The diagram file itself was
not modified by the implementation work.

---

## 3. Chronological prompt and response log

### Phase A — Initial Stage 3 refactor

**User request (paraphrased):** Refactor the Stage 3 project to match the
updated seating design, keep Stage 3 features, leave Stage 2 untouched, keep the
diagram and code in sync, and update the README.

**Work done:**

- `Waiter.seatCustomer(customer, queue, table)` implemented.
- `Host.seatCustomer(customer, queue, table)` implemented.
- `Restaurant.seat(customer)` removed.
- `TurnController` chooses the first free table and calls the staff seating
  operation.
- README updated. See Phase F for the team’s separate diagram corrections.
- Offline build passed.

**Outcome:** Stage 3 code compiled; seed-2026 victory and defeat runs remained
reachable.

---

### Phase B — CLI and GUI completion

**User:** “I think there is a cli version of the game to test if it works, how do
i test it out?”

**Reply:** Provided the Windows build and CLI run commands, explained the prompt
order, and supplied scripted victory/defeat inputs.

**User:** “Don’t update anything, I just want to know if the GUI version is done.”

**Reply:** Reported that the GUI code was complete and compiled, but that the
post-change GUI flow had not yet been runtime-verified.

**User:** “Are you able to implement the gui portion now that the cli is done …
and also I just want to confirm again if the code matches the updated class
diagram.”

**Work done:**

- Host card, Host-first gating, Happy Hour card and phase flow added to the GUI.
- Host actor added to the room.
- Actual JavaFX controls driven through a full victory/defeat harness.
- Class-diagram comparison documented.

---

### Phase C — Host decision and the first diagram confusion

**User:** Noted that the uploaded SVG/diagram had Host decision support and that
Host should not be automatic.

**Work done:**

- Added `HostDecision`/`HostTaskType`-style plumbing.
- Added Host choices to `GameSnapshot`.
- Added Host choice to `TurnChoicesDTO`.
- Added `submitHostDecision` and `hostTaskUsed`.
- Added GUI Host card and CLI Host prompt.
- Verified both outcome paths.

**Later correction:** The uploaded SVG was not the authoritative diagram; the
team’s updated Phase 3 diagram was the correct reference.

---

### Phase D — Difficulty design iteration

**User:** Asked whether Stage 2 belonged to “medium” and Stage 3 to “hard”, and
whether difficulty affects anything.

**Reply:** Difficulty buttons were cosmetic; both GUI and CLI used
`GameConfig.stageTwo(seed)` regardless of the selected difficulty. There was no
runtime difficulty mapping.

**User:** Asked whether difficulty could vary without changing the class
diagram.

**Reply:** Yes — use different existing `GameConfig` values
(`maxTurns`, `arrivalSchedule`, `customerTypes`, `randomSeed`). No new class or
method is required. Adding a `Difficulty` enum or `stageThree` factory would
change the diagram.

**User:** Requested Easy/Medium/Hard presets, with Easy as the base.

**Work done:**

- Relaxed `GameConfig` validation.
- Added private `configFor(...)` presets in `App`/`ConsoleApp`.
- Wired title-screen difficulty selection.
- Added CLI `[seed] [easy|medium|hard]`.
- Verified Medium and Hard victory/defeat paths.

---

### Phase E — Consolidating the authoritative implementation

**User:** “Change of plans … implement the stage 3 things inside and follow the
class diagram strictly without making any changes to the class diagram.”

**Important discovery:** The updated diagram matched the team’s Phase 3
diagram. The Stage 3 work was therefore consolidated into the authoritative
project source.

**Work done:**

- Added `Host`, `ComboMeal`, `HappyHourPolicy`, `HappyHourStatus`.
- Updated `Menu` to add all three base-item combo pairs.
- Allowed `Host` in `Restaurant.staffList`.
- Added Host choice plumbing and Happy Hour plumbing.
- Added `HostChoiceDTO` and `HostTaskType`.
- Added Host/Happy Hour fields to `GameSnapshot` and `TurnChoicesDTO`.
- Added `submitHostDecision`, `activateHappyHour`, `hostTaskUsed`.
- Updated `Waiter.takeOrder` to accept `HappyHourPolicy`.
- Added Host and Manager cards to the GUI.
- Added Host/Waiter actors and queue display.
- Updated README.

**Verification:**

```text
VICTORY — served 5, paid 5, departures 0, revenue $90.18,
          average paid satisfaction 85.2, turns used 18

DEFEAT  — served 0, paid 0, departures 2, revenue $0.00,
          average paid satisfaction N/A, turns used 18
```

---

### Phase F — Diagram corrections

**User:** Asked whether the code and diagram matched strictly.

**Reply:** The code matched the corrected diagram at the modeled level. The
updated diagram had four issues, which the user then fixed:

1. `HostChoiceDTO.getTaskType()` return type.
2. Missing `hostChoice` in the `TurnChoicesDTO` constructor.
3. Missing `hostActions` in the `GameSnapshot` constructor.
4. Happy Hour field order mismatch.

After the user’s diagram update, the modeled members matched.

**Caveat documented:** Read accessors, record-generated methods, private helper
methods and UI internals are treated as implementation details not drawn in the
diagram. Examples include `TurnController.money(...)`, `Customer.order()`,
`Order.lockedPriceCents()`, and record accessors.

---

### Phase G — Visual polish

1. **Top HUD** changed from a non-wrapping `HBox` to a wrapping `FlowPane`.
2. **Customer card label** changed from `Cashier: EATING` to `State: EATING`.
3. **Queue** displays the front three guests plus `+N waiting`.
4. **Queue direction/layout**:
   - Front is on the right, toward the Host/counter.
   - Visible slots are columns 4, 2 and 0.
   - New guests arrive at the back/left.
   - Seated customers cause the remaining queue to shift forward/right.
   - Idle queue guests face right.
5. **Host rest**:
   - Host stands at `(5, 4)`.
   - Host faces down at rest.
   - Host returns to the stand after escorting a guest.
6. **Waiter**:
   - Returns to `(12, 7)` after serving or taking an order.
   - Faces left at rest.
7. **Seating actor**:
   - Host-seated guest → Host animation.
   - Waiter-seated guest → Waiter animation.
   - Waiter serving/taking order → only Waiter animates.
8. **Title screen**:
   - Credits: `Group 5 presents`.
   - Medium colour: amber `#E6B450`.
   - Easy highlighted by default.

---

### Phase H — Difficulty wiring in the final implementation

**User:** Confirmed they wanted the difficulty presets wired into the main
version.

**Work done:**

- `GameConfig` validation now accepts variable turn counts and strictly
  increasing single-arrival schedules, while keeping two tables.
- `App` maps Easy/Medium/Hard to `GameConfig` presets.
- `ConsoleApp` accepts `[seed] [easy|medium|hard]`.
- `StartMenuScreen` passes the selected difficulty to the launcher.
- Easy is the default.

**Verified outcomes:**

```text
Easy
VICTORY — served 5, paid 5, departures 0, revenue $90.18, avg 85.2, turns 18
DEFEAT  — served 0, paid 0, departures 2, revenue $0.00, avg N/A, turns 18

Medium
VICTORY — served 5, paid 4, departures 0, revenue $57.24, avg 60.5, turns 16
DEFEAT  — served 0, paid 0, departures 3, revenue $0.00, avg N/A, turns 16

Hard
VICTORY — served 4, paid 4, departures 1, revenue $57.78, avg 67.5, turns 14
DEFEAT  — served 0, paid 0, departures 3, revenue $0.00, avg N/A, turns 14
```

All runs had zero invalid choices.

---

### Final CLI alignment

Codex helped align the CLI summary with the JavaFX turn boundary in
[5a08ca1](https://github.com/Chin-Wee/Restaurant_Rush/commit/5a08ca1). The CLI
displays the post-arrival state and prints the previous turn’s summary from
the saved resolution and snapshot. The final turn’s summary is printed before
the result.

The release review reran the seed 2026 CLI victory and defeat at all three
difficulties. It also verified the Easy schedule with Happy Hour activated on
turn 4. The [published Easy run](../../README.md#reproduce-a-victory-and-defeat)
uses that timing to demonstrate the combined VIP and Happy Hour price.

---

## 4. Final file map

### Added

```text
core/src/main/java/edu/ntu/sc2002/domain/staff/Host.java
core/src/main/java/edu/ntu/sc2002/domain/menu/ComboMeal.java
core/src/main/java/edu/ntu/sc2002/domain/HappyHourPolicy.java
core/src/main/java/edu/ntu/sc2002/domain/HappyHourStatus.java
core/src/main/java/edu/ntu/sc2002/application/contract/HostChoiceDTO.java
core/src/main/java/edu/ntu/sc2002/application/contract/HostTaskType.java
```

### Modified

```text
core/src/main/java/edu/ntu/sc2002/domain/menu/MenuItem.java
core/src/main/java/edu/ntu/sc2002/domain/menu/Menu.java
core/src/main/java/edu/ntu/sc2002/domain/Restaurant.java
core/src/main/java/edu/ntu/sc2002/domain/staff/Waiter.java
core/src/main/java/edu/ntu/sc2002/application/GameConfig.java
core/src/main/java/edu/ntu/sc2002/application/TurnController.java
core/src/main/java/edu/ntu/sc2002/application/contract/GameSnapshot.java
core/src/main/java/edu/ntu/sc2002/application/contract/TurnChoicesDTO.java
core/src/main/java/edu/ntu/sc2002/ui/console/ConsoleUI.java
core/src/main/java/edu/ntu/sc2002/ConsoleApp.java
visualization/src/main/java/edu/ntu/sc2002/App.java
visualization/src/main/java/edu/ntu/sc2002/ui/StartMenuScreen.java
visualization/src/main/java/edu/ntu/sc2002/ui/RestaurantScreen.java
visualization/src/main/java/edu/ntu/sc2002/presentation/RestaurantUiState.java
visualization/src/main/java/edu/ntu/sc2002/presentation/JavaFXView.java
visualization/src/main/java/edu/ntu/sc2002/ui/scene/RestaurantScene.java
README.md
```

Commit [1fcae89](https://github.com/Chin-Wee/Restaurant_Rush/commit/1fcae89)
records the team’s diagram corrections.

---

## 5. Verification summary

### Build

```powershell
# run from the project root
.\mvnw.cmd -q -o -Pvisualization clean install
```

### CLI

```powershell
java -cp core\target\classes edu.ntu.sc2002.ConsoleApp 2026 easy
java -cp core\target\classes edu.ntu.sc2002.ConsoleApp 2026 medium
java -cp core\target\classes edu.ntu.sc2002.ConsoleApp 2026 hard
```

### Focused visual checks

- Host faces down at rest.
- Waiter returns to `(12, 7)` and faces left.
- Waiter-seated guest uses the Waiter animation; Host remains at its stand.
- Queue front is at column 4, second at 2, third at 0.
- Queue guests face right when idle.
- Front-three queue plus `+N waiting` count.

### GUI harness outcomes

```text
GUI victory: served=5 paid=5 departures=0 revenue=9018 avgPaid=85.2 turns=18
GUI defeat:  served=0 paid=0 departures=2 revenue=0 turns=18
GUI STAGE 3 CHECKS PASSED
```

---

## 6. Run commands

### GUI

```powershell
# run from the project root
.\mvnw.cmd -Pvisualization clean install
.\mvnw.cmd -f visualization\pom.xml javafx:run
```

Direct launch with a difficulty:

```powershell
.\mvnw.cmd -o -f visualization\pom.xml javafx:run -Djavafx.args="--gameDirect=true --seed=2026 --difficulty=hard"
```

### CLI

```powershell
java -cp core\target\classes edu.ntu.sc2002.ConsoleApp 2026 medium
```

The CLI prompt order is:

```text
Happy Hour (when available) → Host → Waiter → Chef
```

---

## 7. Known notes and caveats

- The class diagram in `docs/uml/` is the updated reference.
- Difficulty uses only existing `GameConfig` fields; no diagram members were
  added.
- Only one customer arrives per turn; arrival turns are strictly increasing.
- Read accessors, record-generated methods, private helpers and UI-only classes
  are treated as implementation details not represented in the class diagram.
- Build artifacts under `target/` are ignored by `.gitignore` and should not be
  uploaded manually.
