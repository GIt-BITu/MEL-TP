package mines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class BoardTest {
    @Test
    void reportsInitialStatusAndUsesBoardDimensions() {
        GameBoard game = new GameBoard(new GameConfig(2, 3, 1), new FixedRandom(0));
        AtomicReference<String> status = new AtomicReference<>();

        Board board = new Board(game, status::set);

        assertEquals("1", status.get());
        assertEquals(new Dimension(45, 30), board.getPreferredSize());
    }

    @Test
    void translatesMouseInputIntoModelActions() {
        GameBoard game = new GameBoard(new GameConfig(3, 3, 1), new FixedRandom(0));
        AtomicReference<String> status = new AtomicReference<>();
        Board board = new Board(game, status::set);

        press(board, 1, 1, MouseEvent.BUTTON1);

        assertFalse(game.cellAt(0, 0).covered());
        assertFalse(game.cellAt(0, 0).mine());
        press(board, 16, 1, MouseEvent.BUTTON3);
        assertTrue(game.cellAt(0, 1).flagged());
        assertEquals("0", status.get());
        press(board, 31, 1, MouseEvent.BUTTON3);
        assertEquals("No flags left", status.get());
    }

    @Test
    void paintingDoesNotMutateGameStateOrStatus() {
        GameBoard game = new GameBoard(new GameConfig(2, 2, 1), new FixedRandom(0));
        AtomicReference<String> status = new AtomicReference<>();
        Board board = new Board(game, status::set);
        BufferedImage canvas = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = canvas.getGraphics();

        board.paint(graphics);
        board.paint(graphics);

        assertEquals(GameState.PLAYING, game.state());
        assertEquals("1", status.get());
    }

    @Test
    void resetsCompletedGamesWithoutApplyingTheResetClick() {
        GameBoard game = new GameBoard(new GameConfig(1, 2, 1), new FixedRandom(0));
        AtomicReference<String> status = new AtomicReference<>();
        Board board = new Board(game, status::set);
        game.reveal(0, 0);

        press(board, 1, 1, MouseEvent.BUTTON1);

        assertEquals(GameState.PLAYING, game.state());
        assertFalse(game.minesPlaced());
        assertTrue(game.cellAt(0, 0).covered());
        assertEquals("1", status.get());
    }

    @Test
    void mapsEveryCellAndGameStateToTheExpectedSprite() {
        CellView coveredSafe = new CellView(false, 0, true, false);
        CellView coveredMine = new CellView(true, 0, true, false);
        CellView flaggedSafe = new CellView(false, 0, true, true);
        CellView flaggedMine = new CellView(true, 0, true, true);
        CellView revealedNumber = new CellView(false, 4, false, false);
        CellView revealedMine = new CellView(true, 0, false, false);

        assertEquals(10, Board.spriteIndexFor(coveredSafe, GameState.PLAYING));
        assertEquals(11, Board.spriteIndexFor(flaggedMine, GameState.PLAYING));
        assertEquals(4, Board.spriteIndexFor(revealedNumber, GameState.PLAYING));
        assertEquals(9, Board.spriteIndexFor(revealedMine, GameState.LOST));
        assertEquals(9, Board.spriteIndexFor(coveredMine, GameState.LOST));
        assertEquals(11, Board.spriteIndexFor(flaggedMine, GameState.LOST));
        assertEquals(12, Board.spriteIndexFor(flaggedSafe, GameState.LOST));
    }

    private static void press(Board board, int x, int y, int button) {
        MouseEvent event = new MouseEvent(board, MouseEvent.MOUSE_PRESSED, 0, 0, x, y, 1, false, button);
        board.dispatchEvent(event);
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
