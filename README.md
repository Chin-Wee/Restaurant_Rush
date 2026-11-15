# Restaurant Rush

SC2002 Group 5 turn-based restaurant game. This branch contains **Stage 1**.
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

## Base setting

Play all 18 turns with two tables and arrivals on turns 1, 4, 7, 10, 13 and 16.
Win with at least four paid customers, $45 in revenue and 60 average paid
satisfaction. The customer chooses the dish using `Random(seed + arrival ordinal)`
with arrival ordinals starting at one.

## Instant CLI tests

After building the CLI, run either command from the repository root in Bash or
Zsh. Each command plays all 18 turns and prints the final result if it matches.

**Victory**

```bash
(
  set -o pipefail
  printf '%s\n' \
    1 C1 2  2 C1 1 C1  3 C1 2 \
    1 C2 2  2 C2 1 C2  4 1 C2 \
    3 C2 2  1 C3 2  2 C3 1 C3 \
    3 C3 2  1 C4 2  2 C4 1 C4 \
    3 C4 2  1 C5 2  2 C5 1 C5 \
    1 C6 1 C5  3 C5 2  2 C6 1 C6 |
    java -cp core/target/classes edu.ntu.sc2002.ConsoleApp 2026 |
    grep -Fx 'VICTORY — served 5, paid 4, departures 0, revenue $51.00, average paid satisfaction 78.0, turns used 18'
)
```

**Defeat**

```bash
(
  set -o pipefail
  awk 'BEGIN { for (i = 0; i < 18; i++) print "4\n2" }' |
    java -cp core/target/classes edu.ntu.sc2002.ConsoleApp 2026 |
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

`TurnController` runs the turn sequence and publishes snapshots to both views.
`Restaurant` holds the tables, queue, menu and staff. `ScoreBoard` handles
the totals and win condition.

[Stage 1 class diagram](docs/uml/RestaurantRush_ClassDiagram_Phase1.drawio)

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
