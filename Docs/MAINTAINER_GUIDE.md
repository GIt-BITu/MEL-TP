# Maintainer guide

## Current status

The project is a Java 17 Swing application built by Maven. `mvn clean verify`
is the authoritative local quality gate: it compiles with all `javac` warnings
treated as errors, executes the JUnit 5 suite headlessly, creates the runnable
JAR, generates JaCoCo coverage, and enforces at least 85% line coverage. The
current suite exercises 15 tests and covers 89.04% of executable source lines.

GitHub Actions repeats `mvn verify` on Linux, Windows, and macOS. Each matrix
job invokes `jpackage` on its own operating system, which is required because
native launchers cannot be reliably cross-compiled by `jpackage`. The uploaded
artifacts contain a self-contained app image: a Windows `.exe`, a Linux native
launcher, or a macOS `.app` bundle and launcher.

## Architecture

```text
Mines
└── Swing composition: frame, status label, new-game button
    └── Board
        ├── renders immutable CellView values using the GIF sprites
        ├── translates mouse clicks to model actions
        └── publishes status text; never changes rules while painting
            └── GameBoard
                ├── owns game state, mine placement, flags, reveal and win/loss rules
                ├── returns MoveOutcome for each user action
                └── exposes CellView snapshots, never its mutable cells
                    └── Cell (package-private mutable implementation detail)
```

`GameConfig` is an immutable record containing rows, columns, and mine count.
`GameState` captures `PLAYING`, `WON`, and `LOST`; `MoveOutcome` records the
effect of a reveal/flag attempt. This avoids conflating UI rendering with
domain state and removes the old integer-offset cell encoding.

## Game rules and invariants

- The default board is 16 × 16 with 40 mines.
- Mines are placed on the first reveal, never on the selected cell; the first
  reveal is therefore safe.
- Right-click toggles a flag on a covered cell until the flag allowance is
  exhausted. Revealed cells and completed games do not accept flag changes.
- Flood fill is iterative, so larger boards do not risk recursive stack growth.
  It does not reveal or remove flags.
- The game is won only when every safe cell is revealed. A loss occurs only
  when a mine is revealed. Repainting is pure and cannot change either state.
- A board click after a win/loss resets the game and does not also apply that
  click; the visible `New game` button does the same explicitly.

## Change guide

### Add a difficulty

Create a named `GameConfig` constant or construct one at composition time,
then provide a control in `Mines`. `Board` derives its preferred size from the
model dimensions, so it needs no geometry changes. Add tests for the new
configuration and its expected game behavior.

### Add or modify a game rule

Change `GameBoard` and its tests first. Every user action must return the
appropriate `MoveOutcome`, leave the model valid, and keep `GameState`
transitions explicit. Do not add rule changes to `Board.paintComponent` or any
other rendering code.

### Change the appearance or controls

Modify `Board` (sprites, rendering, input) or `Mines` (window composition).
Keep `GameBoard` free of Swing types. Add component tests for status text,
input translation, and sprite selection when visual behavior changes.

### Make randomness deterministic in tests

Use the package-visible `GameBoard(GameConfig, Random)` constructor. Production
code uses the public constructor, which supplies its own `Random`. A fixed
test random source makes mine placement reproducible without exposing mutable
board internals.

## Commands

```sh
# Compile, run all quality checks, produce target/jeu-mines-1.0.0.jar
mvn clean verify

# Start the desktop application after a build
java -jar target/jeu-mines-1.0.0.jar

# View line and branch coverage
open target/site/jacoco/index.html
```

Use `xdg-open` on most Linux desktops or `start` on Windows in place of `open`.
The application requires a graphical desktop session; tests do not.

## Repository conventions

- Keep product source in `src/main/java`, resources in `src/main/resources`,
  and tests in `src/test/java`.
- Do not commit `target/`, `dist/`, class files, IDE metadata, or `.DS_Store`.
- Keep the 13 GIFs at the root of the application classpath unless the loading
  code and tests are deliberately changed together.
- Preserve the BSD 2-Clause licence and existing ZetCode attribution when
  redistributing source or native application images.
- Use small commits with imperative, lower-case summaries such as `add expert
  config` or `fix flag counter`.
