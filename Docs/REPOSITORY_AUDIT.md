# JeuMines pre-refactor repository audit

Audit snapshot: 2026-10-26, before the refactor. This document preserves the
baseline findings that motivated the work. The repository's current structure,
quality gates, packaging process, and extension guidance are documented in
[`MAINTAINER_GUIDE.md`](MAINTAINER_GUIDE.md).

## Executive assessment

The project can compile from source and the core board-generation logic creates a 16 x 16 board with 40 mines. It is a useful learning example, but is not ready to maintain or distribute reliably. The most important defects are a win state that turns into a displayed loss after the next paint, and flood-fill logic that silently removes flags while leaving the mine counter incorrect. The repository has no build script, test suite, README, licence file, or repeatable packaging process. It also commits generated classes, copied assets, and macOS metadata.

The application has no network, database, authentication, persistence, or third-party runtime dependencies. I found no immediate application-security exposure. The principal risks are correctness, legal provenance, reproducibility, and maintenance cost.

## Complete inspection scope

At the start of the inspection there were 115 filesystem files: 47 worktree files (including the ignored `.agents` prompt) and 68 local `.git` files. `HEAD` contains 45 entries; the index contains 46 because the pre-existing `.gitignore` addition is staged but uncommitted. That staged change was preserved. Git integrity verification (`git fsck --full`) reported no corruption.

Every text file was read in full. Each binary was parsed using the appropriate reader rather than treated as text: all GIFs were decoded and visually inspected, all Java classes were inspected as class files/bytecode metadata, `.DS_Store` files were inspected as Finder metadata, and the Git index and all 44 loose Git objects were integrity-checked and typed. Binary content cannot meaningfully be read “word by word”; no binary was silently treated as source code.

The 44 loose Git objects are 33 blobs, 10 trees, and 1 commit. They reconstruct the one `Initial commit` on branch `master`; there is no configured remote. The `.git/hooks` directory contains only Git's 13 disabled shell samples and one disabled Perl sample, not project hooks. The local Codex capture ref is tooling metadata, not application code.

### File catalogue

| Area | Files inspected | What they are / maintainer relevance |
|---|---|---|
| Application source | `src/mines/Mines.java`, `src/mines/Board.java` | Entire functional implementation: window setup, board generation, game state, painting, and mouse handling. |
| Entrypoint output | `bin/mines/Mines.class` | Java 6 class-file output (major version 50), compiled from `Mines.java`. |
| Board output | `bin/mines/Board.class`, `bin/mines/Board$MinesAdapter.class` | Java 6 class-file output; runtime code if `bin` is used. |
| Misplaced output | `src/mines/mines/Board.class`, `src/mines/mines/Board$MinesAdapter.class` | Java 16 class-file output (major version 60) inside the source tree, with no matching `Mines.class`; stale/generated material. |
| Runtime tiles | `images/0.gif` through `images/12.gif` | Thirteen 15 x 15 GIF87a sprites: blank, 1–8, mine, cover, flag, wrong flag. |
| Duplicated tiles | `bin/0.gif` through `bin/12.gif` | Byte-for-byte copies of `images/0.gif` through `images/12.gif`, needed by the checked-in `bin` runtime but generated/duplicated. |
| Eclipse configuration | `.classpath`, `.project`, `.settings/org.eclipse.jdt.core.prefs` | Eclipse Java project. It declares `src` and `images` source roots, output `bin`, Java SE 1.6, and an Eclipse Metrics builder/nature. |
| VS Code configuration | `.vscode/extensions.json`, `.vscode/launch.json`, `.vscode/project.json` | Recommends a complexity extension; debugger starts `mines.Mines` but names project `Demo11`; project metadata says `demo11`, both inconsistent with Eclipse project `JeuMines`. |
| Git ignore | `.gitignore` | A pre-existing staged addition. It ignores `.agents/*` only, leaving all normal Java build artifacts unignored. |
| Finder metadata | `.DS_Store`, `bin/.DS_Store`, `bin/mines/.DS_Store`, `images/.DS_Store`, `src/.DS_Store`, `src/mines/.DS_Store` | Per-directory macOS UI state; not used by the game, all should be removed from version control. |
| Agent prompt | `.agents/skills/refactor/PROMPT.md` | Ignored, non-product instructions for an agent. It has no effect on Java build/runtime. |
| Git administrative files | `.git/HEAD`, config, index, refs, logs, description, `COMMIT_EDITMSG`, info/exclude, hooks, 44 objects | Standard local repository state. Git configuration has no remote or custom hooks; the commit history consists of one initial commit. |

## What the program does

`mines.Mines` extends `JFrame`. Its constructor sets a 250 x 290 fixed window, adds a status `JLabel` at the south border, and adds a `Board` in the center. `main` constructs it immediately.

`mines.Board` extends `JPanel` and owns both game logic and the presentation layer:

```text
Mines JFrame
├── status JLabel
└── Board JPanel
    ├── int[] field           board model and cell-state encoding
    ├── Image[13] img         loaded GIF sprites
    ├── random mine placement
    ├── recursive empty-cell flood fill
    ├── paint(Graphics)       state-dependent rendering and win/loss label
    └── MinesAdapter          left/right mouse actions
```

The board is hard-coded to 16 rows, 16 columns, 40 mines, and 15-pixel tiles. `newGame()` fills the `int[256]` board as covered, repeatedly selects random positions until it has placed 40 distinct mines, and increments the adjacent-cell count around each mine. It shows `40` in the status label.

One integer stores both a cell's content and its visibility:

| Stored value | Meaning |
|---:|---|
| `0`–`8` | Revealed empty/number cell |
| `9` | Revealed mine |
| `10`–`18` | Covered empty/number cell (`content + 10`) |
| `19` | Covered mine |
| `20`–`28` | Flagged covered empty/number cell (`content + 20`) |
| `29` | Flagged covered mine |

Left-click removes the cover; revealing a zero starts a recursive eight-neighbour flood fill. Right-click toggles a flag while `mines_left` remains positive. A click after a finished game creates a fresh board and then continues to process that same click. Rendering selects one of the 13 sprites; on a loss it reveals mines and identifies wrong flags.

## Technology and prerequisites

- Java SE, Swing, and AWT only; no Maven, Gradle, Ant, JAR manifest, dependency manager, or external Java library.
- Eclipse project files target Java SE 1.6. The source itself uses only old Java syntax, but modern JDKs no longer accept `-source 1.6 -target 1.6` (verified with JDK 25: both options are rejected). Compile with a supported current target, or retain an older toolchain only if Java 6 bytecode is a real requirement.
- A graphical desktop session is required. Launching in this audit's headless Linux session fails at `JFrame` construction with `java.awt.AWTError: Can't connect to X11 window server`; this is an environment limitation, not a source compile failure.
- The 13 GIFs must be at the root of the runtime classpath. Eclipse achieves this through the `images` source folder; `bin` currently contains copied resources.
- Eclipse Metrics must be installed or its obsolete builder/nature removed to avoid IDE setup errors. The recommended VS Code extension is optional.

On a graphical POSIX desktop, a clean source run is:

```sh
mkdir -p out
javac -d out src/mines/Board.java src/mines/Mines.java
java -cp out:images mines.Mines
```

Use `;` rather than `:` as the classpath separator on Windows. `java -cp bin mines.Mines` can run the checked-in output, but should not be treated as the project build process.

## Verification performed

| Check | Result |
|---|---|
| Source compile | `javac -Xlint:all` compiled both Java sources successfully. |
| Compiler diagnostics | Three warnings: the `Image[]` is non-serializable in a serializable Swing component, and both constructors may expose `this` before initialization. |
| Resource loading | A headless `Board` construction passed when all GIFs were added to the classpath. With compiled classes alone it deterministically throws `NullPointerException` while constructing `ImageIcon`, proving the unguarded resource dependency. |
| Board generation | A generated board had 256 cells and exactly 40 stored covered mines. |
| GUI launch | Not visually exercised because this environment has no usable X11 display. |
| Git health | `git fsck --full` passed; only one historical commit is present. |
| Tests | No automated tests, test framework, CI definition, quality gate, or coverage configuration exists. The checks above were temporary audit probes, not committed tests. |

## Findings, ordered by priority

### P1 — user-visible correctness defects

1. **Winning is overwritten as losing on a subsequent paint.** In `src/mines/Board.java:257-261`, the first paint with no covered cells sets `inGame = false` and the label to `Game won`. Every later paint reaches `else if (!inGame)` and writes `Game lost`. A controlled paint test produced exactly `Game won`, then `Game lost`. Rendering must not infer and mutate terminal game state; store a distinct `WON`/`LOST` state instead.

2. **Flood fill removes flags and desynchronizes the mine counter.** `find_empty_cells` at `src/mines/Board.java:141-212` treats every value above `MINE_CELL` as covered. That includes a flagged value (`20`–`29`), so it subtracts 10 and clears its flag. `mines_left` is modified only by right-click handling at `src/mines/Board.java:287-302`, not by this path. Audit probe: a flagged neighbour changed from `20` to `10`, while the counter stayed `40`. Flood-fill must not alter flagged cells, or it must use a single state transition that also updates the counter.

3. **Painting violates Swing's rendering contract and contains game logic.** `Board` overrides public `paint(Graphics)` at `src/mines/Board.java:216` rather than protected `paintComponent(Graphics)`, and never calls `super.paint`. It both draws the board and mutates `inGame`/status text. This is the direct design cause of finding 1 and can create repaint artifacts or future UI integration problems. Move drawing to `paintComponent`, call `super.paintComponent(g)`, and calculate terminal transitions in event/model code.

### P1 — release and governance blockers

4. **No licence file despite clear third-party provenance.** `Mines.java:8` cites a ZetCode tutorial. The current upstream tutorial identifies a GitHub source repository, whose repository metadata declares BSD-2-Clause. This checkout contains neither a licence nor attribution text beyond the URL comment. Do not assume the current upstream licence automatically applies to this copy: identify the exact source revision and asset provenance, then add the applicable licence and required notices before redistribution. Relevant references: [tutorial](https://zetcode.com/javagames/minesweeper/) and [upstream repository](https://github.com/janbodnar/Java-Minesweeper-Game).

5. **The checked-in runtime is not reproducible from one authoritative source tree.** The repository tracks Java 6 classes in `bin`, Java 16 `Board` classes beneath `src/mines/mines`, the same GIF set twice, and six `.DS_Store` files. There is no build command to regenerate them. This invites source/binary drift and platform-specific commits. The stray source-tree classes also omit the entrypoint class, so that directory is not independently runnable.

### P2 — design, usability, and reliability

6. **Build configuration is obsolete and inconsistent.** Eclipse declares Java 1.6; modern compilers reject that target. The VS Code launch and project metadata refer to `Demo11`/`demo11`, whereas `.project` names `JeuMines`. The Eclipse Metrics plugin is declared but not supplied. Adopt a supported Java LTS baseline and one build tool, then regenerate IDE metadata if it is retained.

7. **Asset loading is fragile and fails unclearly.** `src/mines/Board.java:49-53` assumes every resource is root-visible and passes a potentially null URL directly to `ImageIcon`. A missing sprite causes `NullPointerException` rather than a descriptive startup error. Package resources under a dedicated resource directory and validate each load with its name in the error.

8. **The model is inseparable from Swing.** `Board` owns random generation, encoded game state, recursion, status-label updates, images, drawing, and input. This prevents simple unit tests and makes a change such as difficulty selection touch unrelated concerns. Extract a UI-independent `GameBoard`/`Cell` model and let a Swing panel render it.

9. **The numeric state encoding is brittle.** Content, cover, and flag state are represented by arithmetic offsets. Conditions such as `field[cell] > MINE_CELL` are broad enough to cause finding 2. Use a `Cell` type (mine, adjacentCount, covered, flagged) or at least named bit flags and focused predicates.

10. **Hard-coded geometry is layout-dependent.** `Mines.java:13-14` fixes the frame at 250 x 290; `Board.java:36-38` separately fixes board dimensions. `Board` has no preferred size, and `Mines` does not call `pack()`. It happens to fit the 240-pixel grid with the current look-and-feel but is not robust across scaling, fonts, or future controls. Set `Board`'s preferred size from its dimensions and pack the frame.

11. **Swing is initialized outside the Event Dispatch Thread.** `Mines.main` calls `new Mines()` directly at `src/mines/Mines.java:34-36`. Use `EventQueue.invokeLater` for predictable Swing creation and future thread safety.

12. **Game rules and feedback need clarification.** The first left-click may immediately reveal a mine; many Minesweeper implementations guarantee a safe first click. Clicking after a loss/win resets the board and acts on the same click. The board has no menu, explicit new-game control, keyboard support, accessibility labels, timer, difficulty controls, or save/high-score functionality. These may be deliberate scope limits, but should be written down as product decisions.

13. **Recursive flood-fill limits future scaling.** The fixed 16 x 16 board is safe in practice, but a large custom board can use a deep call stack. An iterative queue/stack traversal is safer and easier to test.

14. **Code conventions make changes harder.** The 330-line `Board` has duplicated eight-neighbour logic, mixed tabs/spaces, snake-case `mines_left`, public methods that should be private/internal, no `@Override`, minimal comments, and no style enforcement. The source has a single external-origin comment, not maintainer documentation.

## Maintainability and repository hygiene

The code is easy to read at first because it has only two classes, but modification risk is high: one class has too many responsibilities and has no regression tests. Generated output in version control conceals whether an observed behavior comes from source or an old class file. At audit start, the only worktree change was the pre-existing staged `.gitignore` addition; this audit intentionally changed no implementation or Git staging.

Recommended `.gitignore` coverage after a deliberate cleanup should include at least `bin/`, `out/`, `target/`, `*.class`, `.DS_Store`, IDE user state, and any local agent directories that are not meant to be shared. Keep the source GIFs once, under a canonical resources directory; do not keep copied output copies.

## Recommended remediation sequence

1. Resolve provenance and add `LICENSE`/attribution before redistributing any code or tiles.
2. Choose a supported Java baseline (for example Java 17 or 21), add a minimal build definition and a README with exact run instructions, and make the build package resources.
3. Delete generated classes, duplicated output resources, and `.DS_Store` from version control; expand `.gitignore`. Keep no runtime artifact without a reproducible build.
4. Add focused model tests for mine count, adjacent counts, reveal, flag toggling, flood-fill, first-click behavior, win, and loss.
5. Extract the game model, replace arithmetic state encoding, add an explicit terminal-state enum, then fix the two P1 correctness defects under test.
6. Convert rendering to `paintComponent`, use `pack()`/preferred size, validate resource loads, and initialize Swing on the EDT.
7. Resolve or remove stale Eclipse/VS Code configuration and decide which usability features are in scope.

## Bottom line for a new maintainer

Use `src/mines/*.java` and `images/*.gif` as the only plausible source of truth, but first put them under a real build, test, and licence boundary. Do not edit or trust the checked-in `.class` files as authoritative. Fix the win/loss state and flag/flood-fill defects before presenting the game as functionally complete.
