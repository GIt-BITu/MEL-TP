package mines;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GameConfigTest {
    @Test
    void acceptsAConfigurationWithAtLeastOneSafeCell() {
        GameConfig configuration = new GameConfig(2, 3, 5);

        assertEquals(6, configuration.cellCount());
        assertEquals(5, configuration.mineCount());
    }

    @Test
    void rejectsInvalidDimensionsAndMineCounts() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(1, 1, -1));
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(50_000, 50_000, 1));
        assertDoesNotThrow(() -> new GameConfig(1, 1, 0));
    }
}
