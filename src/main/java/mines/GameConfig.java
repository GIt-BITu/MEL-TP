package mines;

/** Immutable dimensions and mine count for a game. */
public record GameConfig(int rows, int columns, int mineCount) {
    public static final GameConfig INTERMEDIATE = new GameConfig(16, 16, 40);

    public GameConfig {
        if (rows <= 0) {
            throw new IllegalArgumentException("rows must be positive");
        }
        if (columns <= 0) {
            throw new IllegalArgumentException("columns must be positive");
        }

        long cellCount = (long) rows * columns;
        if (cellCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("board is too large");
        }
        if (mineCount < 0 || mineCount >= cellCount) {
            throw new IllegalArgumentException("mineCount must leave at least one safe cell");
        }
    }

    public int cellCount() {
        return rows * columns;
    }
}
