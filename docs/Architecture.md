# Stage 1 package boundaries

`src/main/java/edu/ntu/sc2002/` is the source root.

| Package | Classes | Responsibility |
| --- | --- | --- |
| root | `App` | Creates configured game and starts CLI |
| `ui` | `ConsoleUI` | Displays controller snapshots, reads decisions, retries rejected choices; no game rules |
| `control` | `GameController`, `GameConfig`, `TurnPhase`, `WaiterDecision`, `ChefDecision`, `WaiterTaskType`, `ChefTaskType`, `GameSnapshot`, `TurnResolution` | Owns the 18-turn sequence, arrivals, decision validation, and orchestration |
| `entity` | `Restaurant`, `Customer`, `Satisfaction`, `Order`, `OrderStatus`, `WaitingQueue`, `Table`, `Menu`, `MenuItem`, `Waiter`, `Chef`, `Cashier`, `ScoreBoard`, `PaymentRecord`, `CustomerState`, `GameResult` | Restaurant state and Stage 1 business rules |

Table.occupant is the sole seating authority. Customer stores no table ID or reference; Table calls the package-private markSeated transition, and snapshots derive locations through Restaurant.tableFor. Customer recovery and the discount-dispatch hook are deferred until their respective later stages. Stage 1 bills the item price directly.

Cashier policy: oldest means the smallest arrival ordinal among customers eligible to pay before this turn. Readiness age does not override arrival age. The brief leaves this term ambiguous; the policy is documented and tested with both equal and different readiness times. The controller still collects at most one payment per turn.

The checked-in class diagram describes Stage 3. Stage 1 implements the core subset with a concrete Regular `Customer` and simple concrete `MenuItem`. Stage 2/3 types such as VIP/Critic customers, Host, combos and Happy Hour stay absent.

## Dependency direction

`App → ConsoleUI → GameController → entity`. The UI passes typed decisions and displays immutable snapshots/results; it cannot access mutable entities or the ScoreBoard and does not calculate eligibility, satisfaction, revenue, or outcomes. Each command returns `TurnResolution` with success/error data. The controller rejects duplicate/out-of-order turn operations and consumes only successful choices. Service count increments when an order is served; paid count, revenue, and recorded paid satisfaction increment at payment. All model money uses integer cents.

Flow: begin → start display → scheduled arrival → accepted Waiter choice → display chosen food → refreshed Chef choices → accepted Chef choice → eating → one eligible payment → waiting loss/abandonment → statistics → next turn or final result. `readyTurn` and `readyToPayTurn` must be strictly earlier than service/payment turns. Snapshots use null for no paid average; CLI renders N/A.

The assignment's six-step Stage 1 loop is the game-specific reference. Keep presentation mechanics out of it. If a visual client is added later, put JavaFX screens and scene movement in `ui`, retain the controller/entity boundary, and map immutable snapshots to the visual view model. Do not make animation completion advance an order or turn.

The supplied Stage 3 UML is retained unchanged at the user's request. All shared API differences, staged exclusions, extra declarations, and relationship dispositions are marked in [the API inventory](Stage1-Api-Differences.md) and [parity audit](Stage1-Parity-Audit.md). In particular, `recordServed()`, ID-based commands, immutable summaries, record accessors and split Waiter/Chef submission differ from the final diagram. Full literal diagram parity remains a separate reconciliation decision; no replacement diagram is created.

`docs/uml/` holds the diagrams and their design notes. Runtime assets belong in `src/main/resources/`; asset processing tools belong in `scripts/`. Tests mirror production packages under `src/test/java/`.
