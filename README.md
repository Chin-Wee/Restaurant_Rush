# Restaurant Rush

SC2002 Group 5 turn-based restaurant game. This branch contains **Stage 3**.
The Java CLI is in `core` and the optional JavaFX interface is in `visualization`.

[Repository](https://github.com/Chin-Wee/Restaurant_Rush) · [Stage 1 branch](https://github.com/Chin-Wee/Restaurant_Rush/tree/feat/stage-1) · [Stage 2 branch](https://github.com/Chin-Wee/Restaurant_Rush/tree/feat/stage-2) · [Stage 3 branch](https://github.com/Chin-Wee/Restaurant_Rush/tree/feat/stage-3)

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

## Instant CLI tests

After building the CLI, run either command from the repository root in Bash or
Zsh. Each command plays all 18 turns and prints the final result if it matches.

**Victory**

```bash
(
  set -o pipefail
  printf '%s\n' \
    2 1 C1 2 C1 1 C1  2 2 4 1 C1  2 2 3 C1 2 \
    1 1 C2 2 C2 1 C2  2 4 1 C2  2 3 C2 2 \
    1 C3 2 C3 1 C3  2 3 C3 2  2 4 2 \
    1 C4 2 C4 1 C4  2 4 1 C4  2 3 C4 2 \
    1 C5 2 C5 1 C5  2 4 1 C5  2 4 1 C5 \
    1 C6 3 C5 2  2 2 C6 1 C6  2 4 1 C6 |
    java -cp core/target/classes edu.ntu.sc2002.ConsoleApp 2026 easy |
    grep -Fx 'VICTORY — served 5, paid 5, departures 0, revenue $91.26, average paid satisfaction 84.6, turns used 18'
)
```

**Defeat**

```bash
(
  set -o pipefail
  awk 'BEGIN { for (i = 0; i < 18; i++) print "2\n2\n4\n2" }' |
    java -cp core/target/classes edu.ntu.sc2002.ConsoleApp 2026 easy |
    grep -Fx 'DEFEAT — served 0, paid 0, departures 2, revenue $0.00, average paid satisfaction N/A, turns used 18'
)
```

## Submission video

[Watch the Group 5 submission](https://www.youtube.com/watch?v=evSWk2gFYU0) or jump to any of the seven required segments below.

| Required segment | Jump to | Presenter |
| --- | --- | --- |
| Launch the game and explain the objective | [1:00 (launch)](https://www.youtube.com/watch?v=evSWk2gFYU0&t=60s) · [2:29 (win conditions)](https://www.youtube.com/watch?v=evSWk2gFYU0&t=149s) | Chin Wee |
| Make manager choices under staff constraints | [1:19](https://www.youtube.com/watch?v=evSWk2gFYU0&t=79s) | Chin Wee |
| Follow a dish from READY to SERVED to payment | [1:43 (ready)](https://www.youtube.com/watch?v=evSWk2gFYU0&t=103s) · [1:47 (serve)](https://www.youtube.com/watch?v=evSWk2gFYU0&t=107s) · [2:20 (payment)](https://www.youtube.com/watch?v=evSWk2gFYU0&t=140s) | Chin Wee |
| Show an accelerated victory and defeat | [3:18 (victory)](https://www.youtube.com/watch?v=evSWk2gFYU0&t=198s) · [4:04 (defeat)](https://www.youtube.com/watch?v=evSWk2gFYU0&t=244s) | Chin Wee |
| Demonstrate Stage 2 customer behavior | [4:17 (customer types)](https://www.youtube.com/watch?v=evSWk2gFYU0&t=257s) · [5:01 (VIP bill)](https://www.youtube.com/watch?v=evSWk2gFYU0&t=301s) · [5:26 (Critic)](https://www.youtube.com/watch?v=evSWk2gFYU0&t=326s) | Bhavya |
| Demonstrate Stage 3 features | [5:40 (Host and combo meal)](https://www.youtube.com/watch?v=evSWk2gFYU0&t=340s) · [6:34 (Happy Hour)](https://www.youtube.com/watch?v=evSWk2gFYU0&t=394s) | Hecheng |
| Explain the stage branches, class diagrams, and sequence diagram | [0:36 (branches)](https://www.youtube.com/watch?v=evSWk2gFYU0&t=36s) · [7:34 (Stage 1 classes)](https://www.youtube.com/watch?v=evSWk2gFYU0&t=454s) · [9:50 (Stage 2 classes)](https://www.youtube.com/watch?v=evSWk2gFYU0&t=590s) · [10:19 (Stage 3 classes)](https://www.youtube.com/watch?v=evSWk2gFYU0&t=619s) · [11:00 (sequence)](https://www.youtube.com/watch?v=evSWk2gFYU0&t=660s) | Chin Wee, Lucas, Sweeda |

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

Counts come from the tagged submissions and cover production Java source files in
`core` and `visualization`, with one top-level type per file. The class counts in
parentheses exclude records, enums, and interfaces. Stage 1 is the baseline;
each later stage is compared with the previous tag.

| Tagged stage | Total files | Added files | Changed files | What changed |
| --- | ---: | ---: | ---: | --- |
| [Stage 1](https://github.com/Chin-Wee/Restaurant_Rush/tree/stage-1) | 37 (21 classes) | — | — | Playable baseline with a shared turn controller for the CLI and JavaFX views. |
| [Stage 2](https://github.com/Chin-Wee/Restaurant_Rush/tree/stage-2) | 42 (25 classes) | 5 (4 classes) | 10 (8 classes) | Made `Customer` abstract and added Regular, VIP, and Critic subclasses, a customer type enum, and a factory. |
| [Stage 3](https://github.com/Chin-Wee/Restaurant_Rush/tree/stage-3) | 48 (28 classes) | 6 (3 classes) | 37 (22 classes) | Added the Host, combo meals, and Happy Hour, and updated the turn flow and both views. |

In Stage 3, 21 of the 37 changed files differ only in comments or formatting.

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
| Changed `Customer.java`, `CustomerFactory.java`, `CustomerState.java`, `CustomerType.java`, `Order.java`, `OrderStatus.java`, `Table.java`, `WaitingQueue.java`, `PaymentRecord.java`, `Staff.java`, `Chef.java`, `Cashier.java`, `ScoreBoard.java` and `GameResult.java` | Document existing satisfaction recovery, lifecycle rules, and domain contracts. |
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
