# Package boundaries

`src/main/java/edu/ntu/sc2002/` is the Java package root. `App` only wires the application and selects its entry screen.

| Package | Responsibility and files |
| --- | --- |
| `ui` | JavaFX boundaries: `StartMenuScreen`, `RestaurantScreen`; optional `ui.console.ConsoleUI` |
| `ui.scene` | `RestaurantScene`: room layers, sprites, frame interpolation and visual A* routes |
| `ui.preview` | `RestaurantUiFixtures`, `ScenePreviewDriver`: sample snapshots and scripted/random scene testing |
| `presentation` | Immutable `RestaurantUiState` and its nested commands/summaries; later `RestaurantUiAdapter` maps the engine to this contract |
| `application` | Future `GameController`, `GameConfig`: turn sequencing and configuration |
| `application.contract` | Future `GameSnapshot`, `TurnResolution`, `TurnPhase`, `WaiterDecision`, `ChefDecision`, `HostDecision`, `WaiterTaskType`, `ChefTaskType`, `HostTaskType` |
| `domain` | Future `Restaurant`, `WaitingQueue`, `Table`, `Order`, `OrderStatus`, `Satisfaction`, `ScoreBoard`, `PaymentRecord`, `GameResult` |
| `domain.customer` | Future `Customer`, `RegularCustomer`, `VIPCustomer`, `CriticCustomer`, `CustomerFactory`, `CustomerState`, `CustomerType` |
| `domain.menu` | Future `Menu`, `MenuItem`, `Dish`, `ComboMeal` |
| `domain.staff` | Future `Waiter`, `Chef`, `Cashier`, `Host` |
| `domain.pricing` | Future `BillCalculator`, `HappyHour` |

Future packages are documented here, not populated with placeholder classes. Each listed future type becomes its own Java file when implemented. The UML is under `docs/uml/`; its contents describe intended logic, not implemented behavior.

## Dependency direction

UI emits typed commands to a `Consumer`. The future presentation adapter translates IDs into controller decisions and maps immutable engine snapshots back into `RestaurantUiState`. It is the only bridge between the two models. Keep mutable domain entities out of UI snapshots. The adapter belongs to `presentation`, not `application`.

`application` depends on `domain`. Neither imports JavaFX, `ui`, or `presentation`. Game rules and eligibility come from the engine; the screen only renders supplied choices and feedback. Domain money uses integer cents; formatting belongs to presentation/UI. Keep basic null, finite-number and payload-shape validation at the boundary, without duplicating satisfaction limits or Happy Hour duration rules.

`ui.scene` does not import preview or gameplay code. `ui.preview` drives visual routes through scene methods, and `App` explicitly attaches it. Completing a route never completes an order, seats a domain customer, takes payment or advances a turn. The existing production reference is Stardew Valley's room layers with a fixed HUD.

Presentation `UiPhase` and `ResultSummary` intentionally differ from UML `TurnPhase` and `GameResult`. Before connecting the engine, reconcile the UML's combined `submitTurnChoices(...)` with the UI's Waiter-first acceptance and refreshed Chef choices. Do not implement those rules in the adapter.

Assets stay under `src/main/resources/` (existing original packs may remain under `Assets/`). Asset tools stay under `scripts/`. Future tests mirror the package paths under `src/test/java/` so domain/application checks can run without JavaFX.
