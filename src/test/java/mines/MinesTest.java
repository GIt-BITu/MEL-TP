package mines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

class MinesTest {
    @Test
    void createsWindowContentWithBoardAndNewGameControl() {
        JPanel content = Mines.createContent();

        assertEquals(2, content.getComponentCount());
        assertInstanceOf(Board.class, findComponent(content, Board.class));
        assertInstanceOf(JButton.class, findComponent(content, JButton.class));
    }

    private static Component findComponent(Component component, Class<? extends Component> expectedType) {
        if (expectedType.isInstance(component)) {
            return component;
        }
        if (component instanceof JPanel panel) {
            for (Component child : panel.getComponents()) {
                Component found = findComponent(child, expectedType);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
