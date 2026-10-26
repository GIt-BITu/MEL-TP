package mines;

/** Result of a reveal or flag action. */
public enum MoveOutcome {
    REVEALED,
    FLAGGED,
    UNFLAGGED,
    WON,
    LOST,
    FLAG_LIMIT_REACHED,
    IGNORED
}
