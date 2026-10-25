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

Stage 1 contains no VIP, Critic, Host, combos, customer subclasses or Happy Hour. The draw.io class diagram is labeled Stage 3; its Stage 1 subset is implemented from `docs/uml/DesignNotes.md` and the attached Stage 1 requirements.

## Architecture

`App` wires the window; `ui/RestaurantScreen` owns the blank JavaFX view. This branch contains no game engine or visualization assets.

See [package boundaries](docs/Architecture.md) for the shared target layout. Only create future application/domain files when implementing their UML responsibilities. The visualization branch implements its own UI, presentation state and preview packages separately.

Diagrams and their existing notes are in [docs/uml](docs/uml/): [class diagram](docs/uml/RestaurantRush_ClassDiagram.drawio), [sequence diagram](docs/uml/RestaurantRush_SequenceDiagram.drawio), and [design notes](docs/uml/DesignNotes.md).
