package mines;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * Swing view and input adapter for a {@link GameBoard}.
 *
 * <p>The inherited Swing serialization mechanism is not supported because this
 * component deliberately owns a live model and a status callback.</p>
 */
@SuppressWarnings("serial")
public final class Board extends JPanel {
    private static final long serialVersionUID = 1L;

    static final int TILE_SIZE = 15;
    private static final int TILE_COUNT = 13;
    private static final int MINE_TILE = 9;
    private static final int COVERED_TILE = 10;
    private static final int FLAG_TILE = 11;
    private static final int WRONG_FLAG_TILE = 12;

    private final GameBoard game;
    private final Consumer<String> statusSink;
    private final transient Image[] images;

    public Board(GameBoard game, Consumer<String> statusSink) {
        this.game = Objects.requireNonNull(game, "game");
        this.statusSink = Objects.requireNonNull(statusSink, "statusSink");
        images = loadImages();

        setPreferredSize(new Dimension(game.columnCount() * TILE_SIZE, game.rowCount() * TILE_SIZE));
        setFocusable(true);
        addMouseListener(new BoardMouseListener());
        publishStatus(MoveOutcome.IGNORED);
    }

    public void startNewGame() {
        game.reset();
        publishStatus(MoveOutcome.IGNORED);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        for (int row = 0; row < game.rowCount(); row++) {
            for (int column = 0; column < game.columnCount(); column++) {
                CellView cell = game.cellAt(row, column);
                graphics.drawImage(images[spriteIndexFor(cell, game.state())], column * TILE_SIZE,
                        row * TILE_SIZE, this);
            }
        }
    }

    static int spriteIndexFor(CellView cell, GameState state) {
        if (!cell.covered()) {
            return cell.mine() ? MINE_TILE : cell.adjacentMines();
        }

        if (state != GameState.PLAYING) {
            if (cell.flagged()) {
                return cell.mine() ? FLAG_TILE : WRONG_FLAG_TILE;
            }
            return cell.mine() ? MINE_TILE : COVERED_TILE;
        }

        return cell.flagged() ? FLAG_TILE : COVERED_TILE;
    }

    private void handleMousePress(MouseEvent event) {
        if (game.state() != GameState.PLAYING) {
            startNewGame();
            return;
        }

        int column = event.getX() / TILE_SIZE;
        int row = event.getY() / TILE_SIZE;
        if (!game.contains(row, column)) {
            return;
        }

        MoveOutcome outcome = switch (event.getButton()) {
            case MouseEvent.BUTTON1 -> game.reveal(row, column);
            case MouseEvent.BUTTON3 -> game.toggleFlag(row, column);
            default -> MoveOutcome.IGNORED;
        };

        publishStatus(outcome);
        if (outcome != MoveOutcome.IGNORED) {
            repaint();
        }
    }

    private void publishStatus(MoveOutcome outcome) {
        if (outcome == MoveOutcome.FLAG_LIMIT_REACHED) {
            statusSink.accept("No flags left");
            return;
        }

        String message = switch (game.state()) {
            case PLAYING -> Integer.toString(game.remainingFlags());
            case WON -> "Game won";
            case LOST -> "Game lost";
        };
        statusSink.accept(message);
    }

    private Image[] loadImages() {
        Image[] loadedImages = new Image[TILE_COUNT];
        for (int index = 0; index < TILE_COUNT; index++) {
            String resourceName = "/" + index + ".gif";
            URL resource = Board.class.getResource(resourceName);
            if (resource == null) {
                throw new IllegalStateException("Missing board image: " + resourceName);
            }

            try {
                Image image = ImageIO.read(resource);
                if (image == null) {
                    throw new IllegalStateException("Unreadable board image: " + resourceName);
                }
                loadedImages[index] = image;
            } catch (IOException exception) {
                throw new IllegalStateException("Could not load board image: " + resourceName, exception);
            }
        }
        return loadedImages;
    }

    private final class BoardMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent event) {
            handleMousePress(event);
        }
    }
}
