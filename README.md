# Jeu Mines

A small, desktop Minesweeper game built with Java Swing. It starts with an
intermediate board: 16 rows, 16 columns, and 40 mines.

The game logic is deliberately independent of Swing so that rules can be
tested without a graphical desktop.

## Requirements

- JDK 17 or newer
- Maven 3.9 or newer
- A graphical desktop session to launch the application

## Build, test, and run

```sh
mvn verify
java -jar target/jeu-mines-1.0.0.jar
```

Left-click reveals a cell. Right-click adds or removes a flag. The first
revealed cell is guaranteed not to be a mine. Start a new game with the button
in the window, or click the completed board to reset it.

`mvn verify` compiles the code, runs the JUnit suite in headless mode, produces
a JaCoCo coverage report at `target/site/jacoco/index.html`, and enforces the
minimum coverage threshold.

## Structure

```text
src/main/java/mines/       application and game model
src/main/resources/        cell sprites
src/test/java/mines/       unit and Swing-component tests
.github/workflows/         CI and platform application-image packaging
```

Each workflow run publishes exactly three separately downloadable artifacts:

- `jeu-mines-windows`, containing `JeuMines.exe`;
- `jeu-mines-linux`, containing `JeuMines/bin/JeuMines`; and
- `jeu-mines-macos`, containing the `JeuMines.app` bundle.

GitHub Actions packages each artifact as its own downloadable archive. Every
job verifies the expected native launcher exists before uploading its artifact.

## Documentation

- [`Docs/MAINTAINER_GUIDE.md`](Docs/MAINTAINER_GUIDE.md) explains the current
  architecture, quality gates, and safe extension points.
- [`Docs/REFACTORING_SUMMARY.md`](Docs/REFACTORING_SUMMARY.md) records the
  broken starting state and the fixes delivered by the refactor.
- [`Docs/REPOSITORY_AUDIT.md`](Docs/REPOSITORY_AUDIT.md) preserves the original
  pre-refactor audit.

## Licence and provenance

This project is derived from Jan Bodnar's ZetCode Minesweeper example and is
distributed under the included BSD 2-Clause License. The original tutorial and
source repository are linked in `Docs/REPOSITORY_AUDIT.md`.
