package mines;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * UI-independent Minesweeper game rules.
 *
 * <p>Mines are placed only on the first reveal so that the initial reveal is safe.
 * A board instance is reusable through {@link #reset()}.</p>
 */
public final class GameBoard {
    private final GameConfig configuration;
    private final Random random;

    private Cell[][] cells;
    private GameState state;
    private int remainingFlags;
    private int revealedSafeCells;
    private boolean minesPlaced;

    public GameBoard(GameConfig configuration) {
        this(configuration, new Random());
    }

    public GameBoard(GameConfig configuration, Random random) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.random = Objects.requireNonNull(random, "random");
        reset();
    }

    public void reset() {
        cells = new Cell[configuration.rows()][configuration.columns()];
        for (int row = 0; row < rowCount(); row++) {
            for (int column = 0; column < columnCount(); column++) {
                cells[row][column] = new Cell();
            }
        }

        state = GameState.PLAYING;
        remainingFlags = configuration.mineCount();
        revealedSafeCells = 0;
        minesPlaced = false;
    }

    public MoveOutcome reveal(int row, int column) {
        Cell selected = cellAtInternal(row, column);
        if (state != GameState.PLAYING || !selected.canReveal()) {
            return MoveOutcome.IGNORED;
        }

        if (!minesPlaced) {
            placeMinesExcluding(row, column);
        }

        if (selected.isMine()) {
            selected.reveal();
            state = GameState.LOST;
            return MoveOutcome.LOST;
        }

        revealSafeRegion(row, column);
        if (revealedSafeCells == safeCellCount()) {
            state = GameState.WON;
            return MoveOutcome.WON;
        }
        return MoveOutcome.REVEALED;
    }

    public MoveOutcome toggleFlag(int row, int column) {
        Cell selected = cellAtInternal(row, column);
        if (state != GameState.PLAYING || !selected.isCovered()) {
            return MoveOutcome.IGNORED;
        }

        if (selected.isFlagged()) {
            selected.toggleFlag();
            remainingFlags++;
            return MoveOutcome.UNFLAGGED;
        }
        if (remainingFlags == 0) {
            return MoveOutcome.FLAG_LIMIT_REACHED;
        }

        selected.toggleFlag();
        remainingFlags--;
        return MoveOutcome.FLAGGED;
    }

    public CellView cellAt(int row, int column) {
        return cellAtInternal(row, column).view();
    }

    public boolean contains(int row, int column) {
        return row >= 0 && row < rowCount() && column >= 0 && column < columnCount();
    }

    public int rowCount() {
        return configuration.rows();
    }

    public int columnCount() {
        return configuration.columns();
    }

    public int mineCount() {
        return configuration.mineCount();
    }

    public int remainingFlags() {
        return remainingFlags;
    }

    public int revealedSafeCells() {
        return revealedSafeCells;
    }

    public boolean minesPlaced() {
        return minesPlaced;
    }

    public GameState state() {
        return state;
    }

    private int safeCellCount() {
        return configuration.cellCount() - configuration.mineCount();
    }

    private Cell cellAtInternal(int row, int column) {
        if (!contains(row, column)) {
            throw new IndexOutOfBoundsException("Cell outside board: (" + row + ", " + column + ")");
        }
        return cells[row][column];
    }

    private void placeMinesExcluding(int safeRow, int safeColumn) {
        List<Coordinate> candidates = new ArrayList<>(configuration.cellCount() - 1);
        for (int row = 0; row < rowCount(); row++) {
            for (int column = 0; column < columnCount(); column++) {
                if (row != safeRow || column != safeColumn) {
                    candidates.add(new Coordinate(row, column));
                }
            }
        }

        for (int placedMines = 0; placedMines < mineCount(); placedMines++) {
            Coordinate mine = candidates.remove(random.nextInt(candidates.size()));
            cellAtInternal(mine.row(), mine.column()).placeMine();
            incrementAdjacentCounts(mine);
        }
        minesPlaced = true;
    }

    private void incrementAdjacentCounts(Coordinate mine) {
        for (Coordinate neighbor : neighborsOf(mine.row(), mine.column())) {
            Cell neighborCell = cellAtInternal(neighbor.row(), neighbor.column());
            if (!neighborCell.isMine()) {
                neighborCell.incrementAdjacentMines();
            }
        }
    }

    private void revealSafeRegion(int startRow, int startColumn) {
        Deque<Coordinate> pending = new ArrayDeque<>();
        pending.add(new Coordinate(startRow, startColumn));

        while (!pending.isEmpty()) {
            Coordinate current = pending.removeFirst();
            Cell cell = cellAtInternal(current.row(), current.column());
            if (!cell.canReveal() || cell.isMine()) {
                continue;
            }

            cell.reveal();
            revealedSafeCells++;
            if (cell.adjacentMines() == 0) {
                pending.addAll(neighborsOf(current.row(), current.column()));
            }
        }
    }

    private List<Coordinate> neighborsOf(int row, int column) {
        List<Coordinate> neighbors = new ArrayList<>(8);
        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
                if (rowOffset == 0 && columnOffset == 0) {
                    continue;
                }

                int neighborRow = row + rowOffset;
                int neighborColumn = column + columnOffset;
                if (contains(neighborRow, neighborColumn)) {
                    neighbors.add(new Coordinate(neighborRow, neighborColumn));
                }
            }
        }
        return neighbors;
    }

    private record Coordinate(int row, int column) {
    }
}
