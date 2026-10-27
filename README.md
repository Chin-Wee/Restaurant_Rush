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
