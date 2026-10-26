package mines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class GameBoardTest {
    @Test
    void startsCoveredAndPlacesMinesOnlyAfterTheFirstReveal() {
        GameBoard board = new GameBoard(new GameConfig(4, 5, 4), new FixedRandom(0));

        assertEquals(GameState.PLAYING, board.state());
        assertEquals(4, board.remainingFlags());
        assertFalse(board.minesPlaced());
        assertEquals(0, countMines(board));

        MoveOutcome outcome = board.reveal(0, 0);

        assertEquals(MoveOutcome.REVEALED, outcome);
        assertTrue(board.minesPlaced());
        assertFalse(board.cellAt(0, 0).mine());
        assertFalse(board.cellAt(0, 0).covered());
        assertEquals(4, countMines(board));
        assertAdjacentCountsAreCorrect(board);
    }

    @Test
    void togglesFlagsAndEnforcesTheFlagLimit() {
        GameBoard board = new GameBoard(new GameConfig(2, 2, 2), new FixedRandom(0));

        assertEquals(MoveOutcome.FLAGGED, board.toggleFlag(0, 0));
        assertEquals(MoveOutcome.FLAGGED, board.toggleFlag(0, 1));
        assertEquals(0, board.remainingFlags());
        assertEquals(MoveOutcome.FLAG_LIMIT_REACHED, board.toggleFlag(1, 0));
        assertEquals(MoveOutcome.UNFLAGGED, board.toggleFlag(0, 1));
        assertEquals(1, board.remainingFlags());
        assertFalse(board.cellAt(0, 1).flagged());
    }

    @Test
    void floodFillPreservesFlagsAndTheirCounter() {
        GameBoard board = new GameBoard(new GameConfig(3, 3, 1), new FixedRandom(0));
        assertEquals(MoveOutcome.FLAGGED, board.toggleFlag(1, 1));

        assertEquals(MoveOutcome.REVEALED, board.reveal(2, 2));

        assertTrue(board.cellAt(1, 1).covered());
        assertTrue(board.cellAt(1, 1).flagged());
        assertEquals(0, board.remainingFlags());
        assertEquals(MoveOutcome.UNFLAGGED, board.toggleFlag(1, 1));
        assertEquals(MoveOutcome.WON, board.reveal(1, 1));
    }

    @Test
    void losesWhenAPlacedMineIsRevealedAndIgnoresFurtherMoves() {
        GameBoard board = new GameBoard(new GameConfig(2, 2, 1), new FixedRandom(0));
        board.reveal(1, 1);
        Position mine = firstMine(board);

        assertEquals(MoveOutcome.LOST, board.reveal(mine.row(), mine.column()));
        assertEquals(GameState.LOST, board.state());
        assertEquals(MoveOutcome.IGNORED, board.toggleFlag(0, 1));
        assertEquals(MoveOutcome.IGNORED, board.reveal(0, 1));
    }

    @Test
    void winsAfterEverySafeCellIsRevealed() {
        GameBoard board = new GameBoard(new GameConfig(2, 2, 1), new FixedRandom(0));
        MoveOutcome outcome = MoveOutcome.IGNORED;

        for (int row = 0; row < board.rowCount() && board.state() == GameState.PLAYING; row++) {
            for (int column = 0; column < board.columnCount() && board.state() == GameState.PLAYING; column++) {
                if (!board.minesPlaced() || !board.cellAt(row, column).mine()) {
                    outcome = board.reveal(row, column);
                }
            }
        }

        assertEquals(MoveOutcome.WON, outcome);
        assertEquals(GameState.WON, board.state());
        assertEquals(3, board.revealedSafeCells());
    }

    @Test
    void resetRestoresAFreshGame() {
        GameBoard board = new GameBoard(new GameConfig(2, 2, 1), new FixedRandom(0));
        board.toggleFlag(0, 0);
        board.reveal(1, 1);

        board.reset();

        assertEquals(GameState.PLAYING, board.state());
        assertEquals(1, board.remainingFlags());
        assertEquals(0, board.revealedSafeCells());
        assertFalse(board.minesPlaced());
        assertTrue(board.cellAt(0, 0).covered());
        assertFalse(board.cellAt(0, 0).flagged());
    }

    @Test
    void validatesCoordinates() {
        GameBoard board = new GameBoard(new GameConfig(2, 2, 1), new FixedRandom(0));

        assertFalse(board.contains(-1, 0));
        assertFalse(board.contains(0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> board.cellAt(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.reveal(2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.toggleFlag(0, 2));
    }

    private static int countMines(GameBoard board) {
        int mines = 0;
        for (int row = 0; row < board.rowCount(); row++) {
            for (int column = 0; column < board.columnCount(); column++) {
                if (board.cellAt(row, column).mine()) {
                    mines++;
                }
            }
        }
        return mines;
    }

    private static void assertAdjacentCountsAreCorrect(GameBoard board) {
        for (int row = 0; row < board.rowCount(); row++) {
            for (int column = 0; column < board.columnCount(); column++) {
                CellView cell = board.cellAt(row, column);
                if (!cell.mine()) {
                    assertEquals(countNeighboringMines(board, row, column), cell.adjacentMines(),
                            "adjacent count at (" + row + ", " + column + ")");
                }
            }
        }
    }

    private static int countNeighboringMines(GameBoard board, int row, int column) {
        int mines = 0;
        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
                if (board.contains(row + rowOffset, column + columnOffset)
                        && board.cellAt(row + rowOffset, column + columnOffset).mine()) {
                    mines++;
                }
            }
        }
        return mines;
    }

    private static Position firstMine(GameBoard board) {
        for (int row = 0; row < board.rowCount(); row++) {
            for (int column = 0; column < board.columnCount(); column++) {
                if (board.cellAt(row, column).mine()) {
                    return new Position(row, column);
                }
            }
        }
        throw new AssertionError("Expected a mine");
    }

    private record Position(int row, int column) {
    }

    private static final class FixedRandom extends Random {
        private final int value;

        private FixedRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return Math.floorMod(value, bound);
        }
    }
}
