import javax.swing.*;
import java.awt.*;

/**
 * Improved avatar drawing: larger person, smoother rounded shoulders,
 * less boxy body, better symmetry inside the circular background.
 */
public class AvatarPanel extends JPanel {

    private final Color backgroundColor;
    private final Color figureColor;

    public AvatarPanel(Color backgroundColor, Color figureColor) {
        this.backgroundColor = backgroundColor;
        this.figureColor = figureColor;
        setPreferredSize(new Dimension(100, 100));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int size = Math.min(getWidth(), getHeight()) - 6;
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;

        // Outer circle
        g2.setColor(backgroundColor);
        g2.fillOval(x, y, size, size);

        // Person color
        g2.setColor(figureColor);

        // ----- NEW: Bigger head -----
        int headDiameter = (int) (size * 0.35);   // was 0.28
        int headX = x + (size - headDiameter) / 2;
        int headY = y + (int) (size * 0.16);      // slightly higher
        g2.fillOval(headX, headY, headDiameter, headDiameter);

        // ----- NEW: Rounded body (more circular, less boxy) -----
        int bodyWidth  = (int) (size * 0.56);     // wider
        int bodyHeight = (int) (size * 0.42);     // taller but rounded
        int bodyX = x + (size - bodyWidth) / 2;
        int bodyY = y + (int) (size * 0.45);

        g2.fillRoundRect(bodyX, bodyY, bodyWidth, bodyHeight,
                bodyWidth, bodyWidth);  // big arcs → very rounded

        g2.dispose();
    }
}
