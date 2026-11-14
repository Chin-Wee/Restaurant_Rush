# Restaurant Rush

SC2002 Group 5 turn-based restaurant game. This branch contains **Stage 3**.
The Java CLI is in `core` and the optional JavaFX interface is in `visualization`.

[Repository](https://github.com/Chin-Wee/Restaurant_Rush) · [Stage 1](https://github.com/Chin-Wee/Restaurant_Rush/tree/stage-1) · [Stage 2](https://github.com/Chin-Wee/Restaurant_Rush/tree/stage-2) · [Stage 3](https://github.com/Chin-Wee/Restaurant_Rush/tree/stage-3)

## Setup

Install **JDK 17** and run the commands from the repository root. The Maven Wrapper
downloads Maven and dependencies on the first build. The game runs offline once
those dependencies are cached.

## Run the CLI

macOS/Linux:

```sh
./mvnw clean install
java -cp core/target/classes edu.ntu.sc2002.ConsoleApp 2026
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean install
java -cp core\target\classes edu.ntu.sc2002.ConsoleApp 2026
```

## Run the JavaFX GUI

macOS/Linux:

```sh
./mvnw -Pvisualization clean install
./mvnw -f visualization/pom.xml javafx:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd -Pvisualization clean install
.\mvnw.cmd -f visualization/pom.xml javafx:run
```

The CLI accepts an optional seed and difficulty after the class name, for example
`ConsoleApp 2026 hard`. The title screen's **Menu** button selects difficulty.
Easy is the 18-turn base setting. Medium runs 16 turns and Hard runs 14, with
earlier arrivals and more VIPs or Critics.

## Base setting

Play all 18 turns with two tables and arrivals on turns 1, 4, 7, 10, 13 and 16.
Win with at least four paid customers, $45 in revenue and 60 average paid
satisfaction. The customer chooses the dish using `Random(seed + arrival ordinal)`
unless their customer type has its own preference.

The turn-4 arrival (C2) is a VIP and the turn-10 arrival (C4) is a Critic. The
other four are Regulars.

| Customer | Waiting loss per turn | Bill | Order choice |
| --- | ---: | --- | --- |
| Regular | 8 | Full price | Seeded |
| VIP | 5 | 10% off, rounded to the nearest cent | Seeded |
| Critic | 12 | Full price | Pasta, with the highest-priced menu item as fallback |

The Host can seat one queued customer before the Waiter acts. The menu adds
Salad + Burger ($21.60 / 2 units), Salad + Pasta ($24.30 / 3 units) and
Burger + Pasta ($29.70 / 3 units) to the seeded choices. A combo costs 90% of
its components' combined price and takes their combined preparation time.

Activate Happy Hour before the Host acts. It lasts that turn and the next,
restores up to 15 satisfaction to eligible customers at the start of each active
turn, and applies 20% off after the customer discount. The order locks the final
bill when the Waiter takes it.

## Reproduce a victory and defeat

Run the CLI with seed `2026`. Waiter options are **1** seat, **2** take order,
**3** serve and **4** wait. Chef options are **1** advance one preparation unit
and **2** wait. Enter the customer ID when prompted.

Run Easy. Host options are **1** seat and **2** wait. For the victory, decline
Happy Hour on turns 1–3 and activate it on turn 4. Then follow this schedule:

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

Expected **VICTORY** after 18 turns. Served 5, paid 5, departures 0, revenue
$91.26 and average paid satisfaction 84.6. C1 orders the Salad + Burger combo.
C2 orders Pasta during Happy Hour for $12.96 and pays that price on turn 8.

For **DEFEAT**, decline Happy Hour and choose Host **2**, Waiter **4** and
Chef **2** every turn. Expected 0 served, 0 paid, 2 departures, $0.00 revenue
and N/A satisfaction after 18 turns.

## Submission video

| Required segment | Time | Presenter |
| --- | --- | --- |
| Launch the submitted game and explain the objective | 0:00–2:00 | Chin Wee |
| Make manager choices under staff constraints | 0:00–2:00 | Chin Wee |
| Follow a dish from READY to SERVED to payment | 0:00–2:00 | Chin Wee |
| Show an accelerated victory and defeat | 2:00–4:00 | Chin Wee |
| Demonstrate a Stage 2 VIP or Critic behavior | 4:00–5:30 | Bhavya |
| Demonstrate a Stage 3 feature | 5:30–7:00 | Hecheng |
| Compare stage tags and changes, then explain the diagrams | 2:00–4:00 (tags), 7:00–11:00 (class diagram and SOLID), 11:00–15:00 (sequence diagram) | Chin Wee, Lucas, Sweeda |

## Design

| Abstraction | Responsibility |
| --- | --- |
| `ConsoleUI` and `JavaFXView` | Collect decisions and display game state. |
| `TurnController` | Run the turn sequence, validate commands and publish snapshots. |
| `Restaurant` | Own the tables, queue, menu, staff and Happy Hour policy. |
| `Customer` and `Order` | Keep customer state, order timing and locked prices consistent. |
| `ScoreBoard` | Track payments and departures and evaluate the end result. |

`Customer` subclasses implement pricing and waiting behavior through the same
API. A type switch in the controller would duplicate these rules across turns.
The factory keeps the one necessary mapping from customer type to subclass.

[Stage 1 class diagram](https://github.com/Chin-Wee/Restaurant_Rush/blob/stage-1/docs/uml/RestaurantRush_ClassDiagram_Phase1.drawio) ·
[Stage 2 class diagram](https://github.com/Chin-Wee/Restaurant_Rush/blob/stage-2/docs/uml/RestaurantRush_ClassDiagram_Phase2.drawio) ·
[Stage 3 class diagram](docs/uml/RestaurantRush_ClassDiagram_Phase3.drawio) ·
[Sequence diagram](docs/uml/Stage3_Sequence.md) ·
[Stage 3 AI chat history](ai-history/stage-3/chat-01.md) ·
[Stage 2 AI chat history](https://github.com/Chin-Wee/Restaurant_Rush/blob/stage-2/ai-history/stage-2/chat-01.md)

## Stage transitions

### Stage 1 to Stage 2

| Files | Change |
| --- | --- |
| Added `RegularCustomer.java`, `VIPCustomer.java`, `CriticCustomer.java`, `CustomerType.java` and `CustomerFactory.java` | Put customer behavior in subclasses and select the subclass at arrival. |
| Changed `Customer.java`, `Waiter.java`, `GameConfig.java` and `TurnController.java` | Dispatch dish choice, decay and pricing through the customer. Configure typed arrivals and lock the price when taking the order. |
| Changed `GameSnapshot.java`, `ConsoleApp.java`, `ConsoleUI.java`, `App.java`, `RestaurantScreen.java` and `RestaurantScene.java` | Launch the Stage 2 configuration and display customer types and bills in both interfaces. |

The Stage 2 branch adds its AI chat history and replaces the Stage 1 class
diagram with the Stage 2 diagram.

[Stage 2 feature commit](https://github.com/Chin-Wee/Restaurant_Rush/commit/e0df5d0)

### Stage 2 to Stage 3

| Files | Change |
| --- | --- |
| Added `Host.java`, `HostChoiceDTO.java` and `HostTaskType.java` | Give the manager a separate seating task before the Waiter acts. |
| Added `ComboMeal.java`, `HappyHourPolicy.java` and `HappyHourStatus.java` | Represent composed menu items and the once-per-game pricing policy. |
| Changed `TurnController.java`, `GameConfig.java`, `GameSnapshot.java`, `TurnChoicesDTO.java`, `TurnPhase.java`, `ChefChoiceDTO.java`, `WaiterChoiceDTO.java`, `ChefTaskType.java` and `WaiterTaskType.java` | Carry the Host choice and Happy Hour state through the existing turn boundary. Document the command types and phases. |
| Changed `Restaurant.java`, `Waiter.java`, `Menu.java` and `MenuItem.java` | Let the Host and Waiter seat the chosen customer, add combo items and apply Happy Hour when taking an order. Moved selected seating out of `Restaurant.seat`. |
| Changed `Customer.java`, `CustomerFactory.java`, `CustomerState.java`, `CustomerType.java`, `Order.java`, `OrderStatus.java`, `Table.java`, `WaitingQueue.java`, `PaymentRecord.java`, `Staff.java`, `Chef.java`, `Cashier.java`, `ScoreBoard.java` and `GameResult.java` | Add capped satisfaction recovery to Customer and document responsibilities, lifecycle rules and existing contracts. |
| Changed `ConsoleApp.java`, `ConsoleUI.java`, `App.java`, `GameView.java`, `JavaFXView.java`, `RestaurantUiState.java`, `RestaurantScreen.java`, `StartMenuScreen.java`, `UiEffects.java` and `RestaurantScene.java` | Add difficulty selection and show Host, queue and Happy Hour state. Document the view boundary and controls. |

The Stage 3 branch adds the final class diagram, the sequence diagram and its AI
chat history. The Stage 2 diagram and chat remain available at the Stage 2 tag.

[Stage 3 feature commit](https://github.com/Chin-Wee/Restaurant_Rush/commit/a090535)

## API documentation

Generate Javadoc with `./mvnw -Pvisualization javadoc:aggregate` and open
`target/reports/apidocs/index.html`. On Windows, use `.\mvnw.cmd` in place of
`./mvnw`.

## Asset credits

- City 2 backgrounds by Craftpix (non-commercial).
- Modern Interiors Free and Kitchen and More tiles by LimeZu. The Modern Interiors
  license is included with the assets.
- Friendly Scribbles font by @kmlgames.
- “sunny” by Potsu. Menu button sounds from the 400 Sounds Pack.

The GUI packages its selected assets from `visualization/Assets`. The character
sheets used by `visualization/scripts/SplitCharacterSheets.java` are kept beside
the packaged assets.
