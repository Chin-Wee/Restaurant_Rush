# Restaurant Rush visualization development

The graded versions are available as [stage tags](https://github.com/Chin-Wee/Restaurant_Rush/tags).
This branch contains the original Stage 1 JavaFX development work.

Requires JDK 17 or later. Graded Stage 1 code is in `core/`; the optional JavaFX client is in `visualization/`.

Build and run the CLI:

```sh
./mvnw -q clean package
java -cp core/target/classes edu.ntu.sc2002.ConsoleApp 2026
```

Build and run the visual client:

```sh
./mvnw -q -Pvisualization install
./mvnw -q -f visualization/pom.xml javafx:run
```

Generate API documentation using the JDK's Javadoc tool through Maven:

```sh
./mvnw -q -Pvisualization javadoc:aggregate
```

Generated documentation and build output stay under `target/`; exclude all `target/` folders and `graphify-out/` from the submission archive.

Asset credits: Craftpix city art and Modern tiles character art are non-commercial; retain their licenses in `visualization/Assets/` and `visualization/src/main/resources/characters/`. Friendly Scribbles font by @kmlgames, used with attribution.
