# Restaurant Rush

SC2002 Group 5 turn-based restaurant game. This branch contains **Stage 2**.
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

## Reproduce a victory and defeat

Run the CLI with seed `2026`. Waiter options are **1** seat, **2** take order,
**3** serve and **4** wait. Chef options are **1** advance one preparation unit
and **2** wait. Enter the customer ID when prompted.

For the victory, follow this schedule:

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
| 13 | Wait | Advance C4 |
| 14 | Serve C4 | Wait |
| 15 | Seat C5 | Wait |
| 16 | Take order C5 | Advance C5 |
| 17 | Wait | Advance C5 |
| 18 | Serve C5 | Wait |

Expected **VICTORY** after 18 turns. Served 5, paid 4, departures 0, revenue
$52.20 and average paid satisfaction 74.3. The VIP pays $16.20 for Pasta.
The Critic chooses Pasta and pays $18.00.

For **DEFEAT**, choose Waiter **4** and Chef **2** every turn. Expected 0 served,
0 paid, 2 departures, $0.00 revenue and N/A satisfaction after 18 turns.

## Design

`CustomerFactory` chooses the customer subclass at arrival. Each subclass
defines its own waiting loss and price. `CriticCustomer` also overrides menu
selection. The controller uses the shared `Customer` API during service.

[Stage 2 class diagram](docs/uml/RestaurantRush_ClassDiagram_Phase2.drawio) ·
[Stage 2 AI chat history](ai-history/stage-2/chat-01.md)

## Stage transition

### Stage 1 to Stage 2

| Files | Change |
| --- | --- |
| Added `RegularCustomer.java`, `VIPCustomer.java`, `CriticCustomer.java`, `CustomerType.java` and `CustomerFactory.java` | Put customer behavior in subclasses and select the subclass at arrival. |
| Changed `Customer.java`, `Waiter.java`, `GameConfig.java` and `TurnController.java` | Dispatch dish choice, decay and pricing through the customer. Configure typed arrivals and lock the price when taking the order. |
| Changed `GameSnapshot.java`, `ConsoleApp.java`, `ConsoleUI.java`, `App.java`, `RestaurantScreen.java` and `RestaurantScene.java` | Launch the Stage 2 configuration and display customer types and bills in both interfaces. |

[Compare Stage 1 and Stage 2](https://github.com/Chin-Wee/Restaurant_Rush/compare/stage-1..stage-2)

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
