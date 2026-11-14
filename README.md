# Restaurant Rush

SC2002 Group 5 turn-based restaurant game. This branch contains **Stage 1**.
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
with arrival ordinals starting at one.

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
| 13 | Serve C4 | Wait |
| 14 | Seat C5 | Wait |
| 15 | Take order C5 | Advance C5 |
| 16 | Seat C6 | Advance C5 |
| 17 | Serve C5 | Wait |
| 18 | Take order C6 | Advance C6 |

Expected **VICTORY** after 18 turns. Served 5, paid 4, departures 0, revenue
$51.00 and average paid satisfaction 78.0.

For **DEFEAT**, choose Waiter **4** and Chef **2** every turn. Expected 0 served,
0 paid, 2 departures, $0.00 revenue and N/A satisfaction after 18 turns.

## Design

`TurnController` runs the turn sequence and publishes snapshots to both views.
`Restaurant` holds the tables, queue, menu and staff. `ScoreBoard` handles
the totals and win condition.

[Stage 1 class diagram](docs/uml/RestaurantRush_ClassDiagram_Phase1.drawio)

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
