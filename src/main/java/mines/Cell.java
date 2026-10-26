package mines;

/** Mutable internal state for one board cell. */
final class Cell {
    private boolean mine;
    private int adjacentMines;
    private boolean covered = true;
    private boolean flagged;

    boolean isMine() {
        return mine;
    }

    void placeMine() {
        mine = true;
    }

    int adjacentMines() {
        return adjacentMines;
    }

    void incrementAdjacentMines() {
        adjacentMines++;
    }

    boolean isCovered() {
        return covered;
    }

    boolean isFlagged() {
        return flagged;
    }

    boolean canReveal() {
        return covered && !flagged;
    }

    void reveal() {
        covered = false;
    }

    void toggleFlag() {
        flagged = !flagged;
    }

    CellView view() {
        return new CellView(mine, adjacentMines, covered, flagged);
    }
}
