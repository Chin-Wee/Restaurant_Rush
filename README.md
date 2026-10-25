# Restaurant Rush

Blank JavaFX application.

## Run on macOS

Install JDK 17, then run from the project directory:

```sh
./mvnw javafx:run
```

The first run downloads Maven and the JavaFX dependencies.

## Architecture

`App` wires the window; `ui/RestaurantScreen` owns the blank JavaFX view. This branch contains no game engine or visualization assets.

See [package boundaries](docs/Architecture.md) for the shared target layout. Only create future application/domain files when implementing their UML responsibilities. The visualization branch implements its own UI, presentation state and preview packages separately.

Diagrams and their existing notes are in [docs/uml](docs/uml/): [class diagram](docs/uml/RestaurantRush_ClassDiagram.drawio), [sequence diagram](docs/uml/RestaurantRush_SequenceDiagram.drawio), and [design notes](docs/uml/DesignNotes.md).
