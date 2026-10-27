# Restaurant Rush

JavaFX pixel-art restaurant game for SC2002.

## Setup

Install **JDK 17** no need to install Maven or JavaFX handled on project

- **Mac:** `brew install openjdk@17`
- **Windows:** download [Adoptium Temurin 17](https://adoptium.net/temurin/releases/?os=windows&version=17), tick "Add to PATH" during install

Check it works:

```
java -version
```

Should show `17.x.x`.

## Run (GUI)

**Mac / Linux:**

```sh
./mvnw -Pvisualization clean install
./mvnw -f visualization/pom.xml javafx:run
```

**Windows:**

```powershell
.\mvnw.cmd -Pvisualization clean install
.\mvnw.cmd -f visualization\pom.xml javafx:run
```

## Run (CLI)

```sh
./mvnw clean install
java -cp core/target/classes edu.ntu.sc2002.ConsoleApp
```

## Stage 1 reproducible runs

Run `java -cp core/target/classes edu.ntu.sc2002.ConsoleApp 2026` after building.
Waiter choices: 1 seat, 2 take order, 3 serve, 4 wait. Chef choices: 1 advance one
preparation unit, 2 wait. Enter the customer ID when prompted.
Customers choose from the menu using Java `Random(seed + arrival ordinal)`;
arrival ordinals start at 1. Automatic payment prioritizes arrival order among
customers eligible to pay.

For a victory, use this schedule:

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

Expected after turn 18: **VICTORY**, served 5, paid 4, departures 0, revenue
**$51.00**, average paid satisfaction **78.0**.

For a defeat with the same seed, choose Waiter **4** and Chef **2** on all 18 turns.
Expected: **DEFEAT**, served 0, paid 0, departures 2, revenue **$0.00**, average
paid satisfaction **N/A**. Both runs use all 18 turns.

## Javadoc

Javadoc comes with JDK 17. Generate the core API documentation from the repository root:

```sh
./mvnw javadoc:aggregate
```

Include the visualization API with `./mvnw -Pvisualization javadoc:aggregate`.
On Windows, use `.\mvnw.cmd` in place of `./mvnw`.
Open `target/reports/apidocs/index.html`. Generated documentation stays ignored by Git.

## Project structure

```
Restaurant_Rush/
├── core/           ← game engine, domain model
└── visualization/  ← JavaFX UI
```

## Asset credits

- City 2 backgrounds — Craftpix (non-commercial)
- Character sprites — Modern tiles free pack (non-commercial)
- Friendly Scribbles font — @kmlgames
