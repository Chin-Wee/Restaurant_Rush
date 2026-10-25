# Stage 1 trace and parity audit

## Scope and verdict

Baseline reviewed: `678bcbe` on `feat/stage-1`. All **27 production Java types**, both original test classes, build configuration, README, test notes and supplied diagram were traced. New regression checks and actual CLI replay files accompany the corrections below.

Sources of authority:

- `SC2002_Restaurant_Rush_Group_Assignment.docx`, supplied from `/Users/cwee/Downloads/`, especially sections 2, 3, 4.1, 4.4 and 5.
- [Supplied class diagram](uml/RestaurantRush_ClassDiagram.drawio), labeled **Stage 3 Class Diagram**. Its bytes are unchanged.
- [Exhaustive declaration comparison](Stage1-Api-Differences.md), generated from the diagram XML and compiled classes, including visibility, fields, operations, enums and additional code declarations.

Word source SHA-256: `3edb556cdd1f1dd928370c05eaec8d4ea4afb22daf64321acddbbd3b22ed0148`. The original Word XML was read directly for this audit, rather than relying on the earlier pasted interpretation.

The assignment requires Stage 1 behavior and prohibits Stage 2/3 features at this tag. It requires the final class diagram to match **Stage 3**. Therefore missing later-stage features are not Stage 1 defects. Per the user's direction, the supplied UML is retained, no replacement diagram is created, and every literal API difference is marked rather than hidden. **Full literal UML parity is not claimed.**

## Findings against the assignment

| ID | Inconsistency at the baseline | Requirement | Disposition and evidence |
| --- | --- | --- | --- |
| G1 | Repeated `beginTurn()` calls before arrival could skip turn numbers and scheduled arrivals; arrival could also run at turn zero. `lastResolution()` accepted an unresolved start phase. | 2.1, 3.2: six scheduled arrivals and complete turns | Fixed with an active-turn guard. `turnProtocolCannotSkipArrivalsOrResolveBeforeWaiter` rejects each bad sequence without changing the turn. |
| G2 | Chef submission could resolve a turn before any Waiter decision. | 3.2: Waiter acts before Chef; explicit Wait is available | Fixed: Chef returns unsuccessful feedback until a valid Waiter action or Wait has been accepted. Same test verifies rejection, then valid retry. |
| G3 | Snapshots continued offering Waiter tasks after the Waiter had acted, and tasks remained visible after game over. | 3.1 and 3.4: available tasks and one task per turn | Fixed: availability follows protocol and task consumption. `availabilityFollowsAcceptedTaskAndInvalidRetryDoesNotMutate` verifies both. |
| G4 | Payment vacated the table but left the paid customer's old `tableId` in the displayed state. | 3.2 step 4: payment frees the table | Fixed: payment clears the inverse customer assignment too. Cashier timing test checks table occupancy, paid state, ID and revenue. |
| G5 | Public customer methods could overwrite an order, seat a terminal customer, or mark an unready customer paid. `Table.assign()` could set its occupant before a failed customer transition; `vacate()` allowed early release. | 3.4: no second order, no premature payment/eating; occupied until payment/leave | Fixed domain guards and mutation ordering. `terminalCustomerAndDuplicateOrderCannotBeReactivated` and the manager-priority test cover these failures. |
| G6 | Menu callers could mutate the backing list, removing the required dishes without using a domain operation. | 2.1: base menu always available; encapsulation | Fixed: menu collection is read-only to callers. Snapshot/menu immutability check verifies it. |
| G7 | Negative satisfaction adjustments and integer overflow could escape the 0–100 bounds. | 3.2 step 5; satisfaction invariant | Fixed: reject negative adjustments and clamp with wide arithmetic. `satisfactionBoundsAndCustomerChoicesAreDeterministic` checks extremes. |
| G8 | `GameConfig.numTables` was ignored; injected config and restaurant could disagree. | 2.1 and UML configuration responsibility | Fixed: configured construction uses the table count and injection rejects disagreement. Base remains two. Configured-table test verifies three when explicitly requested. |
| G9 | The customer's selected dish was reliably announced only in the final summary, after the Chef prompt; summary rendering depended on an `Arrival:` string prefix. | 2.2, 3.2: show food when ordered; same-turn Chef choice | Fixed: immutable command results carry events and errors; UI displays each new event once, including food before the Chef prompt. CLI replay checks ordering and no duplicate arrival/order announcements. |
| G10 | EOF and an invalid seed produced uncaught exceptions. | Learning outcome: robust input handling; 3.4: explain invalid input | Fixed: invalid seed gives usage feedback; EOF reports interruption without inventing an ending. CLI tests cover invalid numbers/targets, EOF and seed. |
| G11 | README still claimed the branch had a blank JavaFX screen and no engine. The general seed formula and exact CLI replays were not supplied. | 2.2, 4.1: documented reproducibility and runnable stage | Fixed README and test notes. Checked-in Victory/Defeat stdin files exercise the actual CLI. |
| P1 | The local `stage-1` tag was not published on `origin` when checked: `git ls-remote --tags origin refs/tags/stage-1 'refs/tags/stage-1^{}'` returned no entries. | 4.1 and 5: runnable stage tag on GitHub | **Publication gap remains.** Local source/tag validation does not prove remote publication. This review does not publish repository refs. |

No live Stage 2/3 features were found in production source. Earlier fixes for one Waiter action, separate served/paid counts and a snapshot-only UI are retained and covered by regression checks.

## UML inconsistencies and disposition

The [API inventory](Stage1-Api-Differences.md) marks every field and operation, not just the examples below. `MATCH` means matching name, parameter types, return type and visibility; generic element types are displayed but erased for comparison to the UML's raw lists. Generated record helpers and enum compiler helpers are excluded, and source additions are shown separately.

| Area | Literal difference from the supplied UML | Stage 1 disposition |
| --- | --- | --- |
| `GameController` | Diagram has `beginTurn(boolean)` and one `submitTurnChoices(WaiterDecision, ChefDecision, HostDecision)`; code has explicit begin/arrival and separate Waiter/Chef commands. | Retained staged protocol: no Happy Hour/Host argument in Stage 1, and Chef options refresh after a same-turn order. Command methods now return the UML's `TurnResolution` concept with success/error data. Original names/signatures remain marked differences. |
| `TurnResolution` | Customer references in `newArrival`/departures become immutable IDs; code adds final result and record accessors replace `get...` methods. | Retained to keep mutable entities outside the UI boundary. Success/error fields and arrival identity are now explicit. |
| `GameSnapshot` | Code adds customers, orders, choices and statistics; uses immutable string summaries and record accessors; Happy Hour fields are absent. | Required Stage 1 display data and safe boundary. Boxed `Double` now matches the diagram; absent paid average is null and displays N/A. |
| `WaiterDecision`, `ChefDecision` | Diagram targets live `Customer` objects and uses bean getters; code targets customer IDs and uses record accessors. | Retained for UI separation. These are API differences, not claimed exact matches. |
| `Customer` | Final UML is abstract and has subclasses; Stage 1 has one concrete Regular customer and an extra inverse table assignment. | Intentional Stage 1 specialization. Concrete regular loss/full-price operations, waiting predicate and locked-price order creation now match shared UML operations. |
| `MenuItem` | Final UML is abstract with `Dish`/`ComboMeal`; Stage 1 is a concrete record. | Intentional simple Stage 1 item. The three required items are not replaced by later-stage menu features. |
| `Waiter.takeOrder` | Diagram has a `BillCalculator` parameter; Stage 1 uses the Regular customer's full item price. | Literal difference retained; no pricing-policy object or discount feature is added solely for the final diagram. |
| `Restaurant.completePayment` | Baseline returned void despite UML's `PaymentRecord` return. | Corrected: the aggregate now creates the payment record, marks customer/order paid and releases the table; Cashier delegates to it. |
| `ScoreBoard` | Code adds `recordServed()` and read accessors not shown in the diagram. | Required to show service progress separately from paid-customer victory/average calculations. Diagram remains unchanged and addition is marked. |
| `WaitingQueue` | UML lists a `List`; code uses `ArrayDeque`, plus removal of a selected queued customer. | Standard queue implementation supports arbitrary manager seating priority. Collection/type/API differences are explicitly recorded. |
| Stage 2/3 types | `RegularCustomer`, VIP/Critic, `CustomerType`/factory, Host/task/decision, `Dish`/combo and Happy Hour/calculator are absent as separate types. | Marked staged exclusions, not implemented as placeholders. Stage 1 behavior stays Regular/full-price only. |
| Historical design notes | The old narrative names SimulationConfig/ManagerChoices/ResultEvaluator/HappyHourPolicy and a shared Random; the diagram and current source use different classes and per-arrival seeds. | Marked historical. The Word brief and actual diagram, not this stale narrative, govern this audit. |
| Constructors, helpers, enums | Explicit constructors, protocol guards, formatting helpers, standard record accessors and extra immutable view data are not all drawn. | Every explicit compiled declaration is listed as a code addition. Additions are not counted as gameplay failures. Shared enum values match. |

### Relationship trace

| UML relationship | Code trace | Status |
| --- | --- | --- |
| CLI depends on controller/snapshot/resolution/result | `App → ConsoleUI → GameController`; public responses are immutable records, IDs and values | Shared intent preserved; concrete API differences listed above |
| Controller owns simulation configuration/restaurant/scoreboard/phase | Corresponding private fields in `GameController`; extra guards represent active turn and consumed Waiter task | Preserved with additional implementation state |
| Restaurant owns queue, tables, menu and one staff instance per role | Constructor fields hold one queue/menu/Waiter/Chef/Cashier; base configuration constructs two tables | Stage 1 multiplicities preserved; Host excluded |
| Customer owns satisfaction and at most one order | One `Satisfaction` constructed per customer; guarded order placement creates one order | Preserved and now enforced against overwrite |
| Queue contains waiting customers; table has 0..1 occupant | Queue validates waiting state, table validates assignment, payment/abandonment removes occupancy | Preserved; manager may choose any queued customer |
| Order references one menu item and owns locked price/progress/timestamps | `Order` final item/price plus units/status/ready/served turn fields | Preserved |
| State, task and result dependencies | Customer→CustomerState, Order→OrderStatus, controller→TurnPhase/config/views, decisions→task enums, ScoreBoard→PaymentRecord/GameResult | Shared enum/value dependencies preserved; record/API differences inventoried |
| Final inheritance/pricing-policy relationships | No VIP/Critic/Host/combo/Happy Hour implementation or registration | Intentionally deferred by section 4.1; literal differences remain visible |

## Complete production code trace

Each row names an actual production type. The linked files and exhaustive API inventory cover every operation and field; this table explains their execution responsibilities.

| Type | Trace / requirement | Evidence |
| --- | --- | --- |
| [App](../src/main/java/edu/ntu/sc2002/App.java) | Parse optional integer seed; build required base config/controller; launch CLI | Invalid-seed test; actual offline replay commands |
| [GameConfig](../src/main/java/edu/ntu/sc2002/control/GameConfig.java) | 18 turns, two tables, arrivals 1/4/7/10/13/16, seed; validate supplied settings | Exact-base/configured-table tests |
| [GameController](../src/main/java/edu/ntu/sc2002/control/GameController.java) | Guard phase → arrival → validate/execute one Waiter → one Chef unit → eating → one payment → waiting loss/abandon → result | Protocol, invalid-choice, priority, Pasta and full-run tests |
| [TurnPhase](../src/main/java/edu/ntu/sc2002/control/TurnPhase.java) | START_OF_TURN, MANAGER_CHOICES, RESOLVING, GAME_OVER | Exact enum comparison and protocol tests |
| [WaiterTaskType](../src/main/java/edu/ntu/sc2002/control/WaiterTaskType.java) | SEAT, TAKE_ORDER, SERVE, WAIT | Enum comparison; live CLI inputs |
| [ChefTaskType](../src/main/java/edu/ntu/sc2002/control/ChefTaskType.java) | ADVANCE, WAIT | Enum comparison; live CLI inputs |
| [WaiterDecision](../src/main/java/edu/ntu/sc2002/control/WaiterDecision.java) | Immutable task plus target ID; malformed choices become control feedback | Invalid retry tests |
| [ChefDecision](../src/main/java/edu/ntu/sc2002/control/ChefDecision.java) | Immutable task plus order owner's ID | Same-turn priority and invalid Chef tests |
| [GameSnapshot](../src/main/java/edu/ntu/sc2002/control/GameSnapshot.java) | Read-only start state, legal choices, served/paid/departure metrics and nullable paid average | Snapshot immutability and CLI display tests |
| [TurnResolution](../src/main/java/edu/ntu/sc2002/control/TurnResolution.java) | Read-only accumulated events, arrival/departure IDs, payment, success/error and ending | Failed command snapshot equality; immediate food and event-once checks |
| [Restaurant](../src/main/java/edu/ntu/sc2002/entity/Restaurant.java) | Register arrivals; select free table; find oldest eligible payment; complete payment/abandon atomically | Queue/priority, cashier and abandonment tests |
| [WaitingQueue](../src/main/java/edu/ntu/sc2002/entity/WaitingQueue.java) | Enqueue/dequeue/read/remove selected waiting customer | Six-arrival and arbitrary seating-priority checks |
| [Table](../src/main/java/edu/ntu/sc2002/entity/Table.java) | At most one occupant; assignment only after valid customer transition; release only after pay/leave | Failed assignment leaves empty table; early vacancy rejected |
| [Customer](../src/main/java/edu/ntu/sc2002/entity/Customer.java) | Regular seeded food selection; guarded seating/order/service; one eating turn; delayed payment; waiting loss; terminal states | Pasta trace, deterministic selection, illegal transitions and late-arrival tests |
| [CustomerState](../src/main/java/edu/ntu/sc2002/entity/CustomerState.java) | WAITING, SEATED, EATING, READY_TO_PAY, PAID, LEFT | Enum comparison; flow and terminal-state checks |
| [Satisfaction](../src/main/java/edu/ntu/sc2002/entity/Satisfaction.java) | Initial 100; clamp loss/recovery to 0..100; reject negative adjustments | Pasta loss values and extreme-value checks |
| [Menu](../src/main/java/edu/ntu/sc2002/entity/Menu.java) | Salad/Burger/Pasta in stable selection order; immutable collection view | Exact menu and immutability checks |
| [MenuItem](../src/main/java/edu/ntu/sc2002/entity/MenuItem.java) | Immutable name, cents and positive preparation units | Exact required prices/units; constructor guard |
| [Order](../src/main/java/edu/ntu/sc2002/entity/Order.java) | PLACED → PREPARING → READY; strictly earlier ready turn before service; SERVED → PAID; cancel unserved order | Ready-date, Pasta, cancellation and locked-payment checks |
| [OrderStatus](../src/main/java/edu/ntu/sc2002/entity/OrderStatus.java) | PLACED, PREPARING, READY, SERVED, PAID, CANCELLED | Enum comparison and flow checks |
| [Waiter](../src/main/java/edu/ntu/sc2002/entity/Waiter.java) | Delegate seating; customer chooses full-price food; serve ready order | Manager priority and same-turn order-preparation checks |
| [Chef](../src/main/java/edu/ntu/sc2002/entity/Chef.java) | Advance selected order by one unit; controller permits one Chef submission | Pasta two-unit trace; duplicate submission rejected |
| [Cashier](../src/main/java/edu/ntu/sc2002/entity/Cashier.java) | Automatic one call per resolved turn; select oldest arrival eligible before this turn | Two ready customers paid on consecutive turns, oldest first |
| [PaymentRecord](../src/main/java/edu/ntu/sc2002/entity/PaymentRecord.java) | Immutable customer ID, locked cents, satisfaction captured at payment | Cashier and paid-average checks |
| [ScoreBoard](../src/main/java/edu/ntu/sc2002/entity/ScoreBoard.java) | Separate served/paid counts; record payments/departures; all three victory criteria | Exact thresholds, zero unpaid revenue and full-run outcomes |
| [GameResult](../src/main/java/edu/ntu/sc2002/entity/GameResult.java) | VICTORY or DEFEAT only after last turn through controller | No-early-result test and full 18-turn replays |
| [ConsoleUI](../src/main/java/edu/ntu/sc2002/ui/ConsoleUI.java) | Display only; read IDs/tasks; retry feedback; display chosen food before Chef; EOF interrupts cleanly | Five CLI tests and external Java process replays |

## Requirement-by-requirement verification

| Assignment requirement | Result / proving evidence |
| --- | --- |
| Base setting and menu (2.1) | PASS: exact config/menu assertions, six arrivals on exact turns, default two tables |
| Customers choose food (2.2) | PASS: seed per arrival = game seed + ordinal; Java Random selects from Salad/Burger/Pasta; no food choice command |
| Rules out of UI; integer cents; offline (2.2) | PASS: UI receives immutable snapshots/results; validation and eligibility in controller/domain; real Java processes replay stdin without runtime dependencies/network calls |
| Start display (3.1) | PASS: turn, revenue, N/A/paid average, queue, tables, each customer's satisfaction and order status, Waiter/Chef/Cashier availability checked in captured CLI output |
| Six ordered steps (3.2) | PASS: display before arrival; arrival once; Waiter before Chef; eating/payment before waiting loss; result only after turn 18 |
| One Waiter task / one Chef unit (3.2, 3.4) | PASS: accepted-task guard; second submission rejected; direct protocol regression checks |
| Manager controls priority; same-turn newly ordered Chef work (3.2, 4.1) | PASS: non-oldest target selected explicitly; first order left untouched; Chef can choose an order placed that turn |
| READY/SERVED timing (3.2) | PASS: ready turn must be strictly earlier; ready-on-this-turn service rejected without mutation |
| Full eating turn, next-turn payment, one payment, oldest first (3.2) | PASS: exact Pasta example plus two eligible-customer scenario |
| Waiting loss, abandonment and table lifecycle (3.2, 3.4) | PASS: regular -8 only while waiting/unserved; floor zero; unfinished order cancelled; queue/table removed; paid inverse ID cleared |
| Invalid input retries and no consumed task (3.4) | PASS: bad numeric input, wrong target, no table, second order, bad Chef order and role repetition checked |
| Unpaid/abandoned orders excluded from revenue (3.4) | PASS: all-WAIT/abandonment and deferred Pasta payment checks |
| Turn-16 arrival cannot pay / at most five paid (3.4) | PASS: C6 fastest-action scenario cannot pay by 18. Bound: even a one-unit dish requires seat 16 → order/prepare 17 → serve 18 → finish eating 19 → pay 20. Earlier five arrivals are the only possible payers. |
| End statistics and recorded paid average (3.5) | PASS: served count is separate from paid count; all required metrics displayed; no paid average renders N/A |
| Stage 1 only, independent runnable local tag, both endings and notes (4.1) | PASS locally: production source has no later-stage types/features; build + two actual CLI replay inputs; tag follows corrected commit. Remote publication remains P1. |
| Final class diagram matches final Stage 3 code (5) | NOT A STAGE 1 CLAIM: supplied diagram retained; all literal Stage 1 differences are documented in the exhaustive inventory, as requested. |

The Word brief also requests later stages, report/video/declarations and stage comparison evidence for the full group submission. Those are not implemented by this Stage 1 audit.

## Reproduce

Verified results for this revision: **22 tests, zero failures/errors/skips**. Both separately launched Java CLI processes replayed all 18 turns and six arrivals. Victory finished served 5 / paid 4 / departures 0 / $51.00 / paid average 78.0. Defeat finished served 0 / paid 0 / departures 2 / $0.00 / N/A. The compiled declaration inventory check passed: 135 matching declarations, 26 literal API differences, 40 later-stage declarations excluded and 141 code additions (including constructors, accessors and private helpers).

Run `./mvnw -q clean package`, then `python3 scripts/audit_stage1_api.py --check`. The first command executes the behavior and CLI tests; the second checks that the declaration inventory still matches the unchanged diagram and compiled code.

Replay both actual CLI runs with the commands and expected metrics in [Stage1-TestNotes.md](Stage1-TestNotes.md). API inventory freshness is not a zero-difference assertion. Remote publication is not established by local build success.
