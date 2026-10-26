# Refactoring summary

This document records the starting condition of the repository and the changes
made to turn it into a maintainable, testable Java application.

## Before and after

| Starting problem | Fix and current benefit |
|---|---|
| Two source files mixed game rules, painting, sprite loading, status updates, random placement, and input handling. | `GameBoard` owns rules; `Board` is a Swing adapter; `Mines` composes the window. A rule, UI, or layout change now has a clear home. |
| A cell was an integer combining mine content, visibility, and flag state through arithmetic offsets. | `Cell` uses named fields and `CellView` exposes immutable state. Code no longer relies on broad numeric comparisons. |
| A win could display as a loss on the next repaint because painting changed game state. | `GameState` distinguishes `PLAYING`, `WON`, and `LOST`; `paintComponent` renders only and never changes rules or status. |
| Flood-fill treated flags as ordinary covered cells, clearing them without restoring the counter. | Iterative flood-fill skips flagged cells. Flag state and the remaining flag count stay synchronized. |
| Empty-cell expansion was recursive and could grow the call stack on a larger board. | Flood-fill uses an iterative queue. |
| A first left-click could reveal a mine. | Mines are placed after the first reveal and never on the selected cell. |
| Clicking a completed game reset it and also applied the same click to the new game. | A completed-game click only resets; the next click is a deliberate move. A visible `New game` button is also available. |
| The board overwrote `paint`, omitted `super.paint`, had no preferred size, and relied on fixed frame dimensions. | It uses `paintComponent`, calls the Swing superclass, derives preferred size from `GameConfig`, and the frame uses `pack()`. |
| Swing was created directly from `main`, outside the Event Dispatch Thread. | `Mines.main` uses `EventQueue.invokeLater`. |
| Missing GIFs failed with an unhelpful null-pointer exception. | Resource loading reports the exact missing, unreadable, or un-loadable sprite. |
| There was no test suite, build tool, coverage measurement, or CI. | Maven, JUnit 5, JaCoCo, and GitHub Actions are now in place. `mvn clean verify` runs 15 tests, reports 89.04% line coverage, and enforces an 85% minimum. |
| Source and build output were intermingled; stale Java 6 and Java 16 classes, duplicated sprites, Finder files, and IDE metadata were tracked. | Standard Maven source/resource/test directories are authoritative. Generated output and machine-specific metadata are removed and ignored. |
| The only declared build target was Java 6, which modern compilers reject. | The project compiles with Java 17 and fails the build on compiler warnings via `-Xlint:all -Werror`. |
| There was no README, no reproducible run command, and no licence file despite identifiable upstream provenance. | `README.md`, `LICENSE`, the historical audit, and the maintainer guide document build/run, provenance, and maintenance expectations. |
| There was no release packaging. | The workflow verifies and produces three independent, downloadable native app images: Windows, Linux, and macOS. |
| VS Code/Eclipse configuration named different projects and required an undeclared Eclipse Metrics plugin. | Stale IDE-specific configuration was removed. Maven is the single build definition. |

## New quality boundaries

- Game rules are testable without a graphical desktop.
- Rendering cannot mutate a game outcome.
- Tests inject deterministic randomness when exact mine layouts matter.
- `GameConfig` makes additional difficulty levels a configuration change rather
  than a rewrite of game logic or geometry.
- `MoveOutcome` gives the UI an explicit, testable response to every action.
- Java compilation, coverage, unit tests, and package creation are repeatable
  locally and in CI.

## Deliberately out of scope

The work did not add product features that were absent rather than broken:
timers, saved games, high scores, sound, custom themes, or online play. Native
app images are not code-signed or notarized because that requires owner-managed
platform credentials. Those features can be added without changing core rules
by following the extension guidance in `MAINTAINER_GUIDE.md`.
