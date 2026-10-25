# Restaurant Rush

JavaFX pixel-art restaurant game. The current build opens with an animated start menu; Start Game opens the restaurant UI preview. Displayed game values are fixtures; character movement is scene testing only. No turn engine is connected.

## Run on macOS

Install JDK 17, then run from the project directory:

```sh
./mvnw javafx:run
```

The first run downloads Maven and the JavaFX dependencies.

## Asset credits and licenses

- City 2 pixel-art backgrounds are from Craftpix. The supplied free license is for non-commercial use only; keep the included `Assets/craftpix-net-322807-free-city-backgrounds-pixel-art/License.txt` with the project.
- Character sprites are from the Modern tiles free pack. The included `Assets/Modern tiles_Free/LICENSE.txt` limits the pack to non-commercial use; its copy beside the split sprites is `src/main/resources/characters/LICENSE.txt`.
- Friendly Scribbles font by @kmlgames (used with attribution).

## Architecture

See [package boundaries](docs/Architecture.md) and the [UML](docs/uml/RestaurantRush_ClassDiagram.drawio).
