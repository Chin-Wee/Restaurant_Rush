# Restaurant Rush

JavaFX pixel-art restaurant game. The current build opens with an animated start menu; Start Game switches to a blank gameplay placeholder.

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

## Asset credits and licenses

- City 2 pixel-art backgrounds are from Craftpix. The supplied free license is for non-commercial use only; keep the included `Assets/craftpix-net-322807-free-city-backgrounds-pixel-art/License.txt` with the project.
- Friendly Scribbles font by @kmlgames (used with attribution).
