# Stage 1 package boundaries

`src/main/java/edu/ntu/sc2002/` is the source root.

| Package | Classes | Responsibility |
| --- | --- | --- |
| root | `App` | Creates configured game and starts CLI |
| `ui` | `ConsoleUI` | Displays controller snapshots, reads decisions, retries rejected choices; no game rules |
| `control` | `GameController`, `GameConfig`, `TurnPhase`, `WaiterDecision`, `ChefDecision`, `WaiterTaskType`, `ChefTaskType`, `GameSnapshot`, `TurnResolution` | Owns the 18-turn sequence, arrivals, decision validation, and orchestration |
| `entity` | `Restaurant`, `Customer`, `Satisfaction`, `Order`, `OrderStatus`, `WaitingQueue`, `Table`, `Menu`, `MenuItem`, `Waiter`, `Chef`, `Cashier`, `ScoreBoard`, `PaymentRecord`, `CustomerState`, `GameResult` | Restaurant state and Stage 1 business rules |

The checked-in class diagram describes Stage 3. Stage 1 implements the core subset with a concrete Regular `Customer` and simple concrete `MenuItem`. Stage 2/3 types such as VIP/Critic customers, Host, combos and Happy Hour stay absent.

## Dependency direction

`App → ConsoleUI → GameController → entity`. The UI passes typed decisions and displays immutable snapshots; it cannot access mutable entities or the ScoreBoard and does not calculate eligibility, satisfaction, revenue, or outcomes. The controller validates a choice before applying it, then resolves Waiter before Chef, then eating/payment, waiting loss/abandonment, and end statistics. Service count increments when an order is served; paid count, revenue, and recorded paid satisfaction increment at payment. Money is integer cents in the model and formatted only at the UI boundary.

The assignment's six-step Stage 1 loop is the game-specific reference. Keep presentation mechanics out of it. If a visual client is added later, put JavaFX screens and scene movement in `ui`, retain the controller/entity boundary, and map immutable snapshots to the visual view model. Do not make animation completion advance an order or turn.

The checked-in Stage 3 UML's `ScoreBoard` operations do not yet include `recordServed()`. Add it to the final diagram when reconciling the Stage 3 design: the task spec reports served and paid customers as distinct progress measures.

`docs/uml/` holds the diagrams and their design notes. Runtime assets belong in `src/main/resources/`; asset processing tools belong in `scripts/`. Tests mirror production packages under `src/test/java/`.
