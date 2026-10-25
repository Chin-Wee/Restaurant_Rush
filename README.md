# Restaurant Rush

Stage 1 command-line restaurant game. The Stage 1 model and six-step turn loop live in `entity` and `control`; `ConsoleUI` only formats snapshots and reads choices.

## Run on macOS

Install JDK 17, then compile and run from the project directory:

```sh
./mvnw -q package
java -cp target/classes edu.ntu.sc2002.App
```

Pass an optional seed to reproduce customer food choices, for example `java -cp target/classes edu.ntu.sc2002.App 2026`.

## Stage 1 checks

Run `./mvnw -q test`. `StageOneGameTest` verifies the assignment's Pasta trace and validation/payment rules. `StageOneRunTest` runs complete 18-turn Victory and Defeat scenarios. See [reproducible Stage 1 checks](docs/Stage1-TestNotes.md) for seeds and decisions.

Stage 1 contains no VIP, Critic, Host, combos, customer subclasses or Happy Hour. Behavior follows sections 2–4.1 of the supplied assignment brief. Customer food choice uses `new Random(gameSeed + arrivalOrdinal).nextInt(3)` over Salad, Burger, Pasta in that order; the manager never selects food.

## Architecture

`App` creates the configured controller and CLI. `control` sequences turns and validates commands; `entity` owns game state and rules; `ui/ConsoleUI` reads choices and renders immutable snapshots/results. The runnable game needs only Java 17 at runtime, with no JavaFX or network service.

See [the complete Stage 1 trace](docs/Stage1-Parity-Audit.md), [every API difference](docs/Stage1-Api-Differences.md), and [package boundaries](docs/Architecture.md). The supplied UML is the final Stage 3 design and is retained unchanged; this branch does not claim literal API parity with it.

After compilation, regenerate the declaration inventory with `python3 scripts/audit_stage1_api.py`, or check freshness with `python3 scripts/audit_stage1_api.py --check`. This marks mismatches rather than asserting none exist. Build tooling may download Maven/test dependencies on first use; the compiled CLI runs offline.

Diagrams and their existing notes are in [docs/uml](docs/uml/): [class diagram](docs/uml/RestaurantRush_ClassDiagram.drawio), [sequence diagram](docs/uml/RestaurantRush_SequenceDiagram.drawio), and [design notes](docs/uml/DesignNotes.md).
