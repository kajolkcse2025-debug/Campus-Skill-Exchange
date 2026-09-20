package ui;

import javax.swing.JComponent;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Charts painted directly with Graphics2D, so there is no charting library to install. */
public final class Charts {

    private Charts() { }

    /** Horizontal bars, one per entry, ordered as the map is ordered. */
    public static class Bars extends JComponent {

        private Map<String, Integer> data = new LinkedHashMap<>();
        private Function<String, Color> toneOf = key -> Theme.VIOLET;
        private int labelWidth = 150;
        private String emptyMessage = "Nothing to show yet.";

        public Bars(Map<String, Integer> data) { setData(data); }

        public Bars tones(Function<String, Color> toneOf) { this.toneOf = toneOf; return this; }
        public Bars labelWidth(int w) { this.labelWidth = w; return this; }
        public Bars emptyMessage(String m) { this.emptyMessage = m; return this; }

        public void setData(Map<String, Integer> data) {
            this.data = data == null ? new LinkedHashMap<>() : data;
            setPreferredSize(new Dimension(360, Math.max(60, this.data.size() * 30)));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.prepare(g);
            if (data.isEmpty()) {
                g2.setFont(Theme.font(Font.PLAIN, 12));
                g2.setColor(Theme.FAINT);
                g2.drawString(emptyMessage, 2, 24);
                g2.dispose();
                return;
            }
            int max = 1;
            for (int v : data.values()) max = Math.max(max, v);

            int rowHeight = Math.min(30, Math.max(22, getHeight() / data.size()));
            int barHeight = rowHeight - 12;
            int barLeft = labelWidth + 8;
            int barSpace = Math.max(40, getWidth() - barLeft - 34);
            int y = 2;

            g2.setFont(Theme.font(Font.PLAIN, 12));
            FontMetrics fm = g2.getFontMetrics();
            for (Map.Entry<String, Integer> e : data.entrySet()) {
                String name = e.getKey();
                while (fm.stringWidth(name) > labelWidth - 6 && name.length() > 4) {
                    name = name.substring(0, name.length() - 2) + "\u2026";
                }
                g2.setColor(Theme.MUTED);
                g2.drawString(name, 0, y + barHeight - 2);

                double fraction = e.getValue() / (double) max;
                int width = (int) Math.round(barSpace * fraction);

                g2.setColor(new Color(0x262940));
                g2.fill(new RoundRectangle2D.Double(barLeft, y, barSpace, barHeight, barHeight, barHeight));

                Color tone = toneOf.apply(e.getKey());
                g2.setPaint(new GradientPaint(barLeft, 0, Theme.darken(tone, 0.12f),
                        barLeft + Math.max(width, 1), 0, Theme.lighten(tone, 0.1f)));
                g2.fill(new RoundRectangle2D.Double(barLeft, y, Math.max(width, barHeight),
                        barHeight, barHeight, barHeight));

                g2.setFont(Theme.font(Font.BOLD, 12));
                g2.setColor(Theme.TEXT);
                g2.drawString(String.valueOf(e.getValue()), barLeft + barSpace + 8, y + barHeight - 2);
                g2.setFont(Theme.font(Font.PLAIN, 12));

                y += rowHeight;
            }
            g2.dispose();
        }
    }

    /** Vertical columns with a baseline, used for the months trend. */
    public static class Columns extends JComponent {

        private Map<String, Integer> data;
        private final Color tone;

        public Columns(Map<String, Integer> data, Color tone) {
            this.data = data;
            this.tone = tone;
            setPreferredSize(new Dimension(320, 150));
        }

        public void setData(Map<String, Integer> data) { this.data = data; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.prepare(g);
            int max = 1;
            for (int v : data.values()) max = Math.max(max, v);

            int bottom = getHeight() - 22;
            int top = 12;
            int usable = bottom - top;

            g2.setStroke(new BasicStroke(1f));
            for (int i = 0; i <= 2; i++) {
                int gy = bottom - (usable * i / 2);
                g2.setColor(new Color(0x242740));
                g2.draw(new Line2D.Double(0, gy, getWidth(), gy));
            }

            int n = Math.max(1, data.size());
            int slot = getWidth() / n;
            int width = Math.min(34, slot - 12);
            int x = 0;

            g2.setFont(Theme.font(Font.PLAIN, 11));
            for (Map.Entry<String, Integer> e : data.entrySet()) {
                int height = (int) Math.round(usable * (e.getValue() / (double) max));
                int bx = x + (slot - width) / 2;
                int by = bottom - height;

                g2.setPaint(new GradientPaint(0, by, Theme.lighten(tone, 0.12f), 0, bottom,
                        Theme.alpha(tone, 70)));
                g2.fill(new RoundRectangle2D.Double(bx, by, width, Math.max(height, 3), 8, 8));

                g2.setColor(Theme.MUTED);
                int lw = g2.getFontMetrics().stringWidth(e.getKey());
                g2.drawString(e.getKey(), x + (slot - lw) / 2, getHeight() - 5);

                if (e.getValue() > 0) {
                    g2.setFont(Theme.font(Font.BOLD, 11));
                    g2.setColor(Theme.TEXT);
                    String v = String.valueOf(e.getValue());
                    int vw = g2.getFontMetrics().stringWidth(v);
                    g2.drawString(v, x + (slot - vw) / 2, by - 4);
                    g2.setFont(Theme.font(Font.PLAIN, 11));
                }
                x += slot;
            }
            g2.dispose();
        }
    }

    /**
     * The credit medallion: a ring showing how much of what you earned you still hold,
     * with the balance in the middle.
     */
    public static class Ring extends JComponent {

        private double fraction;
        private String centre;
        private String caption;
        private final Color tone;
        private final int size;

        public Ring(double fraction, String centre, String caption, Color tone, int size) {
            this.fraction = Math.max(0, Math.min(1, fraction));
            this.centre = centre;
            this.caption = caption;
            this.tone = tone;
            this.size = size;
            setPreferredSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
        }

        public void set(double fraction, String centre, String caption) {
            this.fraction = Math.max(0, Math.min(1, fraction));
            this.centre = centre;
            this.caption = caption;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.prepare(g);
            double thickness = size * 0.11;
            double pad = thickness / 2 + 2;

            g2.setColor(Theme.alpha(Color.BLACK, 60));
            g2.fill(new Ellipse2D.Double(pad, pad, size - pad * 2, size - pad * 2));

            g2.setStroke(new BasicStroke((float) thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(0x2E3252));
            g2.draw(new Ellipse2D.Double(pad, pad, size - pad * 2, size - pad * 2));

            g2.setPaint(new GradientPaint(0, 0, Theme.lighten(tone, 0.2f), size, size, tone));
            g2.draw(new Arc2D.Double(pad, pad, size - pad * 2, size - pad * 2,
                    90, -360 * fraction, Arc2D.OPEN));

            g2.setFont(Theme.font(Font.BOLD, (int) (size * 0.26)));
            g2.setColor(Theme.TEXT);
            int w = g2.getFontMetrics().stringWidth(centre);
            g2.drawString(centre, (size - w) / 2, (int) (size / 2 + size * 0.06));

            g2.setFont(Theme.font(Font.PLAIN, Math.max(10, (int) (size * 0.095))));
            g2.setColor(Theme.MUTED);
            int cw = g2.getFontMetrics().stringWidth(caption);
            g2.drawString(caption, (size - cw) / 2, (int) (size / 2 + size * 0.22));
            g2.dispose();
        }
    }

    /** A thin progress track, used on locked badges. */
    public static class Progress extends JComponent {

        private double fraction;
        private final Color tone;

        public Progress(double fraction, Color tone) {
            this.fraction = fraction;
            this.tone = tone;
            setPreferredSize(new Dimension(120, 6));
            setMaximumSize(new Dimension(Short.MAX_VALUE, 6));
        }

        public void setFraction(double f) { this.fraction = f; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.prepare(g);
            g2.setColor(new Color(0x2A2D47));
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 6, 6));
            g2.setColor(tone);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() * Math.max(0, Math.min(1, fraction)),
                    getHeight(), 6, 6));
            g2.dispose();
        }
    }
}
