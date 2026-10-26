package mines;

/** Immutable cell state exposed by the game model. */
public record CellView(boolean mine, int adjacentMines, boolean covered, boolean flagged) {
}
