package mines;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Application entry point and Swing window composition. */
public final class Mines {
    private static final String TITLE = "Minesweeper";

    private Mines() {
    }

    public static void main(String[] arguments) {
        EventQueue.invokeLater(Mines::showWindow);
    }

    static JPanel createContent() {
        GameBoard game = new GameBoard(GameConfig.INTERMEDIATE);
        JLabel statusLabel = new JLabel();
        Board board = new Board(game, statusLabel::setText);
        JButton newGameButton = new JButton("New game");

        newGameButton.addActionListener(event -> board.startNewGame());

        JPanel controls = new JPanel(new BorderLayout());
        controls.add(statusLabel, BorderLayout.WEST);
        controls.add(newGameButton, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout());
        content.add(controls, BorderLayout.NORTH);
        content.add(board, BorderLayout.CENTER);
        return content;
    }

    private static void showWindow() {
        JFrame frame = new JFrame(TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(createContent());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}
