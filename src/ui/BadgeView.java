package ui;

import model.Badge;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

/** A badge as a medallion: bright when earned, outlined with progress when not. */
public class BadgeView extends JPanel {

    public BadgeView(Badge badge, boolean showProgress) {
        super(new BorderLayout(0, 7));
        setOpaque(false);
        setBorder(new EmptyBorder(4, 4, 4, 4));

        JPanel top = Theme.clear(new BorderLayout());
        top.add(new Medallion(badge), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JPanel text = Theme.clear(new BorderLayout(0, 3));
        javax.swing.JLabel name = Theme.label(badge.getName(), 12, Font.BOLD,
                badge.isEarned() ? Theme.TEXT : Theme.FAINT);
        name.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        text.add(name, BorderLayout.NORTH);

        if (showProgress) {
            javax.swing.JTextArea req = Theme.wrapped(badge.getRequirement(), 120,
                    badge.isEarned() ? Theme.MUTED : Theme.FAINT, 11);
            req.setBorder(new EmptyBorder(0, 6, 0, 6));
            text.add(req, BorderLayout.CENTER);
            if (!badge.isEarned()) {
                JPanel track = Theme.clear(new BorderLayout());
                track.setBorder(new EmptyBorder(6, 14, 0, 14));
                track.add(new Charts.Progress(badge.getProgress(), badge.getColor()));
                text.add(track, BorderLayout.SOUTH);
            }
        }
        add(text, BorderLayout.CENTER);
        setToolTipText(badge.getRequirement());
    }

    /** The painted disc. */
    private static class Medallion extends JComponent {
        private final Badge badge;
        private static final int SIZE = 56;

        Medallion(Badge badge) {
            this.badge = badge;
            setPreferredSize(new Dimension(SIZE, SIZE));
            setMinimumSize(new Dimension(SIZE, SIZE));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.prepare(g);
            int x = (getWidth() - SIZE) / 2;
            Color tone = badge.getColor();

            if (badge.isEarned()) {
                g2.setColor(Theme.alpha(tone, 45));
                g2.fill(new Ellipse2D.Double(x - 3, -3, SIZE + 6, SIZE + 6));
                g2.setPaint(new GradientPaint(x, 0, Theme.lighten(tone, 0.16f),
                        x + SIZE, SIZE, Theme.darken(tone, 0.28f)));
                g2.fill(new Ellipse2D.Double(x, 0, SIZE, SIZE));
                g2.setColor(Theme.alpha(Color.WHITE, 120));
                g2.setStroke(new java.awt.BasicStroke(1.2f));
                g2.draw(new Ellipse2D.Double(x + 5, 5, SIZE - 10, SIZE - 10));
                g2.setColor(Color.WHITE);
                g2.fill(Icons.star(x + SIZE / 2.0, SIZE / 2.0, 13, 5.6, 5));
            } else {
                g2.setColor(new Color(0x23263C));
                g2.fill(new Ellipse2D.Double(x, 0, SIZE, SIZE));
                g2.setColor(new Color(0x343852));
                g2.setStroke(new java.awt.BasicStroke(1.4f));
                g2.draw(new Ellipse2D.Double(x + 0.7, 0.7, SIZE - 1.4, SIZE - 1.4));
                g2.setColor(new Color(0x3C4160));
                g2.fill(Icons.star(x + SIZE / 2.0, SIZE / 2.0, 13, 5.6, 5));
            }
            g2.dispose();
        }
    }
}
