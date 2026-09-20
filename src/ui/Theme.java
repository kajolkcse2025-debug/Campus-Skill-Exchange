package ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.List;

/** Colours, type and the custom painted widgets the whole app is built from. */
public final class Theme {

    // Palette: deep indigo base, violet for skills, amber reserved for credits.
    public static final Color BG        = new Color(0x12131F);
    public static final Color PANEL     = new Color(0x1A1C2B);
    public static final Color PANEL_HI  = new Color(0x22243A);
    public static final Color LINE      = new Color(0x2C2F48);
    public static final Color TEXT      = new Color(0xECEEF8);
    public static final Color MUTED     = new Color(0x9096B4);
    public static final Color FAINT     = new Color(0x6B7194);
    public static final Color VIOLET    = new Color(0x7C5CFF);
    public static final Color VIOLET_HI = new Color(0x9C82FF);
    public static final Color AMBER     = new Color(0xFFB74D);
    public static final Color TEAL      = new Color(0x2DD4BF);
    public static final Color GREEN     = new Color(0x4ADE80);
    public static final Color RED       = new Color(0xF87171);
    public static final Color PINK      = new Color(0xF472B6);
    public static final Color BLUE      = new Color(0x60A5FA);

    private static final String FAMILY = pickFamily();

    private Theme() { }

    private static String pickFamily() {
        String[] wanted = {"Inter", "Segoe UI", "SF Pro Text", "Helvetica Neue",
                           "Ubuntu", "Noto Sans", "DejaVu Sans", "Arial"};
        List<String> available = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String w : wanted) if (available.contains(w)) return w;
        return Font.SANS_SERIF;
    }

    public static Font font(int style, int size) { return new Font(FAMILY, style, size); }
    public static Font mono(int size) { return new Font(Font.MONOSPACED, Font.PLAIN, size); }

    /** Dark defaults for the Swing widgets that are not hand painted. */
    public static void install() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) { }
        UIManager.put("control", PANEL);
        UIManager.put("info", PANEL);
        UIManager.put("nimbusBase", new Color(0x1E2033));
        UIManager.put("nimbusBlueGrey", new Color(0x2A2D45));
        UIManager.put("nimbusLightBackground", PANEL);
        UIManager.put("nimbusFocus", VIOLET);
        UIManager.put("nimbusSelectionBackground", VIOLET);
        UIManager.put("nimbusSelectedText", Color.WHITE);
        UIManager.put("nimbusDisabledText", FAINT);
        UIManager.put("text", TEXT);
        UIManager.put("Table.background", PANEL);
        UIManager.put("Table.alternateRowColor", PANEL);
        UIManager.put("TabbedPane.contentOpaque", Boolean.FALSE);
        UIManager.put("OptionPane.background", PANEL);
        UIManager.put("Panel.background", PANEL);
        UIManager.put("defaultFont", font(Font.PLAIN, 13));
    }

    /** A hue per skill category, so the same subject always looks the same. */
    public static Color categoryColor(String category) {
        switch (category) {
            case "Programming":      return new Color(0x7C5CFF);
            case "Design":           return new Color(0xF472B6);
            case "Languages":        return new Color(0x60A5FA);
            case "Music & Dance":    return new Color(0xA78BFA);
            case "Sports & Fitness": return new Color(0x4ADE80);
            case "Photography":      return new Color(0x2DD4BF);
            case "Business":         return new Color(0xFFB74D);
            case "Academics":        return new Color(0x38BDF8);
            default:                 return new Color(0xFB923C);
        }
    }

    public static Color statusColor(String status) {
        switch (status) {
            case "COMPLETED": return TEAL;
            case "ACCEPTED":  return BLUE;
            case "PENDING":   return AMBER;
            case "REJECTED":  return RED;
            case "CANCELLED": return FAINT;
            default:          return MUTED;
        }
    }

    // ------------------------------------------------------------------ labels

    public static JLabel label(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(style, size));
        l.setForeground(color);
        return l;
    }

    public static JLabel h1(String text) { return label(text, 26, Font.BOLD, TEXT); }
    public static JLabel h2(String text) { return label(text, 17, Font.BOLD, TEXT); }
    public static JLabel body(String text) { return label(text, 13, Font.PLAIN, TEXT); }
    public static JLabel muted(String text) { return label(text, 12, Font.PLAIN, MUTED); }

    /** Wraps long text at a pixel width without the HTML sizing surprises. */
    public static JTextArea wrapped(String text, int width, Color color, int size) {
        JTextArea a = new JTextArea(text);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setEditable(false);
        a.setFocusable(false);
        a.setOpaque(false);
        a.setForeground(color);
        a.setFont(font(Font.PLAIN, size));
        a.setBorder(null);
        a.setSize(width, Short.MAX_VALUE);
        return a;
    }

    // -------------------------------------------------------------- containers

    /** A rounded panel; the building block for every card in the app. */
    public static class Card extends JPanel {
        private Color fill = PANEL;
        private Color stroke = LINE;
        private int radius = 16;

        public Card() { this(new java.awt.BorderLayout()); }

        public Card(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setBorder(new EmptyBorder(18, 18, 18, 18));
        }

        public Card fill(Color c) { this.fill = c; repaint(); return this; }
        public Card stroke(Color c) { this.stroke = c; repaint(); return this; }
        public Card radius(int r) { this.radius = r; repaint(); return this; }
        public Card pad(int t, int l, int b, int r) { setBorder(new EmptyBorder(t, l, b, r)); return this; }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = prepare(g);
            if (fill != null) {
                g2.setColor(fill);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, radius, radius));
            }
            if (stroke != null) {
                g2.setColor(stroke);
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 2, getHeight() - 2, radius, radius));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** A card with a diagonal two colour wash, used for the one hero area. */
    public static class GradientCard extends Card {
        private final Color from;
        private final Color to;

        public GradientCard(LayoutManager layout, Color from, Color to) {
            super(layout);
            this.from = from;
            this.to = to;
            stroke(null);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = prepare(g);
            g2.setPaint(new GradientPaint(0, 0, from, getWidth(), getHeight(), to));
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 18, 18));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Transparent panel, for grouping without adding a surface. */
    public static JPanel clear(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    // ----------------------------------------------------------------- buttons

    public static class Btn extends JButton {
        public enum Kind { SOLID, GHOST, QUIET, DANGER }

        private final Kind kind;
        private Color tone;

        public Btn(String text, Kind kind, Color tone) {
            super(text);
            this.kind = kind;
            this.tone = tone;
            setFont(font(Font.BOLD, 13));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 18, 10, 18));
            setForeground(kind == Kind.SOLID ? Color.WHITE : tone);
        }

        /** Fluent click handler so buttons can be built inline. */
        public Btn onClick(Runnable action) {
            addActionListener(e -> action.run());
            return this;
        }

        public void setTone(Color tone) {
            this.tone = tone;
            if (kind != Kind.SOLID) setForeground(tone);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = prepare(g);
            boolean hover = getModel().isRollover() && isEnabled();
            boolean down = getModel().isPressed() && isEnabled();
            int arc = getHeight();
            RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

            if (!isEnabled()) {
                g2.setColor(PANEL_HI);
                g2.fill(shape);
                setForeground(FAINT);
            } else if (kind == Kind.SOLID) {
                Color a = hover ? lighten(tone, 0.18f) : tone;
                Color b = hover ? lighten(tone, 0.02f) : darken(tone, 0.16f);
                if (down) { a = darken(a, 0.12f); b = darken(b, 0.12f); }
                g2.setPaint(new GradientPaint(0, 0, a, 0, getHeight(), b));
                g2.fill(shape);
                setForeground(Color.WHITE);
            } else if (kind == Kind.GHOST || kind == Kind.DANGER) {
                if (hover) {
                    g2.setColor(alpha(tone, down ? 46 : 30));
                    g2.fill(shape);
                }
                g2.setColor(alpha(tone, hover ? 190 : 120));
                g2.setStroke(new BasicStroke(1.3f));
                g2.draw(new RoundRectangle2D.Double(0.6, 0.6, getWidth() - 2, getHeight() - 2, arc, arc));
                setForeground(hover ? lighten(tone, 0.2f) : tone);
            } else {
                if (hover) {
                    g2.setColor(alpha(tone, down ? 46 : 26));
                    g2.fill(shape);
                }
                setForeground(hover ? lighten(tone, 0.2f) : MUTED);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static Btn solid(String text) { return new Btn(text, Btn.Kind.SOLID, VIOLET); }
    public static Btn solid(String text, Color tone) { return new Btn(text, Btn.Kind.SOLID, tone); }
    public static Btn ghost(String text) { return new Btn(text, Btn.Kind.GHOST, VIOLET); }
    public static Btn ghost(String text, Color tone) { return new Btn(text, Btn.Kind.GHOST, tone); }
    public static Btn quiet(String text) { return new Btn(text, Btn.Kind.QUIET, MUTED); }
    public static Btn danger(String text) { return new Btn(text, Btn.Kind.DANGER, RED); }

    // ------------------------------------------------------------------ inputs

    /** Text field with a rounded well and placeholder text. */
    public static class HintField extends JTextField {
        private final String hint;

        public HintField(String hint, int columns) {
            super(columns);
            this.hint = hint;
            setOpaque(false);
            setForeground(TEXT);
            setCaretColor(VIOLET);
            setFont(font(Font.PLAIN, 13));
            setBorder(new EmptyBorder(10, 14, 10, 14));
            setSelectionColor(alpha(VIOLET, 90));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = prepare(g);
            g2.setColor(PANEL_HI);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
            g2.setColor(isFocusOwner() ? VIOLET : LINE);
            g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 2, getHeight() - 2, 12, 12));
            g2.dispose();
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) paintHint(g, hint, getHeight());
        }
    }

    public static class HintPassword extends JPasswordField {
        private final String hint;

        public HintPassword(String hint, int columns) {
            super(columns);
            this.hint = hint;
            setOpaque(false);
            setForeground(TEXT);
            setCaretColor(VIOLET);
            setFont(font(Font.PLAIN, 13));
            setBorder(new EmptyBorder(10, 14, 10, 14));
            setEchoChar('\u2022');
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = prepare(g);
            g2.setColor(PANEL_HI);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
            g2.setColor(isFocusOwner() ? VIOLET : LINE);
            g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 2, getHeight() - 2, 12, 12));
            g2.dispose();
            super.paintComponent(g);
            if (getPassword().length == 0 && !isFocusOwner()) paintHint(g, hint, getHeight());
        }
    }

    private static void paintHint(Graphics g, String hint, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(FAINT);
        g2.setFont(font(Font.PLAIN, 13));
        g2.drawString(hint, 14, height / 2 + 5);
        g2.dispose();
    }

    /** Multi line input inside the same rounded well. */
    public static JScrollPane area(JTextArea target, int rows) {
        target.setRows(rows);
        target.setLineWrap(true);
        target.setWrapStyleWord(true);
        target.setFont(font(Font.PLAIN, 13));
        target.setForeground(TEXT);
        target.setBackground(PANEL_HI);
        target.setCaretColor(VIOLET);
        target.setBorder(new EmptyBorder(10, 12, 10, 12));

        JScrollPane sp = new JScrollPane(target);
        sp.setBorder(BorderFactory.createLineBorder(LINE));
        sp.getViewport().setBackground(PANEL_HI);
        sp.setBackground(PANEL_HI);
        styleScroll(sp);
        return sp;
    }

    public static <T> JComboBox<T> combo(T[] items) {
        JComboBox<T> box = new JComboBox<>(items);
        box.setFont(font(Font.PLAIN, 13));
        box.setForeground(TEXT);
        box.setBackground(PANEL_HI);
        box.setBorder(new EmptyBorder(2, 6, 2, 6));
        box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        box.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton b = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = prepare(g);
                        g2.setColor(MUTED);
                        int cx = getWidth() / 2, cy = getHeight() / 2;
                        g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.drawLine(cx - 4, cy - 2, cx, cy + 2);
                        g2.drawLine(cx, cy + 2, cx + 4, cy - 2);
                        g2.dispose();
                    }
                };
                b.setBorder(null);
                b.setContentAreaFilled(false);
                return b;
            }
        });
        box.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                                                          int index, boolean selected, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                l.setBorder(new EmptyBorder(7, 11, 7, 11));
                l.setFont(font(Font.PLAIN, 13));
                l.setForeground(selected ? Color.WHITE : TEXT);
                l.setBackground(selected ? VIOLET : PANEL_HI);
                l.setOpaque(true);
                return l;
            }
        });
        return box;
    }

    // ------------------------------------------------------------------ tables

    public static void styleTable(JTable table) {
        table.setFont(font(Font.PLAIN, 13));
        table.setForeground(TEXT);
        table.setBackground(PANEL);
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(0x2E2A55));
        table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(font(Font.BOLD, 12));
        header.setForeground(MUTED);
        header.setBackground(PANEL);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LINE));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(10, 36));
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.LEFT);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                l.setBorder(new EmptyBorder(0, 12, 0, 12));
                l.setFont(font(Font.PLAIN, 13));
                if (!selected) {
                    l.setBackground(row % 2 == 0 ? PANEL : new Color(0x1E2031));
                    l.setForeground(TEXT);
                }
                String text = String.valueOf(value);
                if (text.matches("[+\\u2212-]\\d+")) {        // credit amounts read as gain or spend
                    boolean gain = text.charAt(0) == '+';
                    l.setForeground(selected ? Color.WHITE : (gain ? GREEN : AMBER));
                    l.setFont(font(Font.BOLD, 13));
                } else if (text.matches("[A-Z]{6,9}")) {   // status words get their own colour
                    l.setForeground(selected ? Color.WHITE : statusColor(text));
                    l.setFont(font(Font.BOLD, 12));
                    l.setText(text.charAt(0) + text.substring(1).toLowerCase());
                }
                return l;
            }
        });
    }

    public static JScrollPane scroll(JComponent view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setUI(new javax.swing.plaf.basic.BasicScrollPaneUI());   // Nimbus paints a panel here
        sp.setBorder(null);
        sp.setViewportBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        styleScroll(sp);
        return sp;
    }

    /**
     * A vertical-only scroll pane: the view is stretched to the viewport width so
     * card grids re-flow instead of being clipped by a horizontal scrollbar.
     */
    public static JScrollPane vscroll(JComponent view) {
        JPanel tracker = new TrackingPanel(view);
        JScrollPane sp = scroll(tracker);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return sp;
    }

    private static class TrackingPanel extends JPanel implements javax.swing.Scrollable {
        TrackingPanel(JComponent view) {
            super(new java.awt.BorderLayout());
            setOpaque(false);
            add(view, java.awt.BorderLayout.CENTER);
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(java.awt.Rectangle r, int o, int d) { return 18; }
        @Override public int getScrollableBlockIncrement(java.awt.Rectangle r, int o, int d) { return 120; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    public static void styleScroll(JScrollPane sp) {
        sp.getVerticalScrollBar().setUI(new SlimScrollBar());
        sp.getHorizontalScrollBar().setUI(new SlimScrollBar());
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(9, 0));
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 9));
    }

    private static class SlimScrollBar extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() { trackColor = BG; }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }
        @Override protected JButton createDecreaseButton(int o) { return zero(); }

        private JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setBorder(null);
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, java.awt.Rectangle r) {
            Graphics2D g2 = prepare(g);
            g2.setColor(BG);
            g2.fillRect(r.x, r.y, r.width, r.height);
            g2.dispose();
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, java.awt.Rectangle r) {
            Graphics2D g2 = prepare(g);
            g2.setColor(isDragging || isThumbRollover() ? VIOLET : new Color(0x3A3E60));
            g2.fill(new RoundRectangle2D.Double(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6));
            g2.dispose();
        }
    }

    // ------------------------------------------------------------ small pieces

    /** Coloured pill used for categories, levels and statuses. */
    public static class Chip extends JComponent {
        private String text;
        private Color tone;
        private boolean filled;

        public Chip(String text, Color tone, boolean filled) {
            this.text = text;
            this.tone = tone;
            this.filled = filled;
            setFont(font(Font.BOLD, 11));
        }

        public void set(String text, Color tone) {
            this.text = text;
            this.tone = tone;
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            int w = getFontMetrics(font(Font.BOLD, 11)).stringWidth(text) + 22;
            return new Dimension(w, 22);
        }

        @Override
        public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = prepare(g);
            g2.setColor(filled ? tone : alpha(tone, 38));
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 11, 11));
            if (!filled) {
                g2.setColor(alpha(tone, 90));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 2, getHeight() - 2, 11, 11));
            }
            g2.setFont(font(Font.BOLD, 11));
            g2.setColor(filled ? Color.WHITE : tone);
            int tw = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, (getWidth() - tw) / 2, getHeight() / 2 + 4);
            g2.dispose();
        }
    }

    /** Circular initials avatar, coloured from the name so it stays stable. */
    public static class Avatar extends JComponent {
        private final String initials;
        private final int size;
        private final Color tone;

        public Avatar(String initials, int size, Color tone) {
            this.initials = initials;
            this.size = size;
            this.tone = tone;
            setPreferredSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
        }

        public static Color toneFor(String name) {
            Color[] tones = {VIOLET, TEAL, PINK, BLUE, AMBER, GREEN, new Color(0xA78BFA)};
            return tones[Math.abs(name.hashCode()) % tones.length];
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = prepare(g);
            g2.setPaint(new GradientPaint(0, 0, lighten(tone, 0.18f), size, size, darken(tone, 0.25f)));
            g2.fill(new Ellipse2D.Double(0, 0, size, size));
            g2.setFont(font(Font.BOLD, Math.max(11, size / 2 - 3)));
            g2.setColor(Color.WHITE);
            int w = g2.getFontMetrics().stringWidth(initials);
            g2.drawString(initials, (size - w) / 2, size / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
            g2.dispose();
        }
    }

    /** Five stars with the filled portion showing a rating out of five. */
    public static class Stars extends JComponent {
        private double rating;
        private final int size;

        public Stars(double rating, int size) {
            this.rating = rating;
            this.size = size;
            setPreferredSize(new Dimension(size * 5 + 8, size + 4));
            setMaximumSize(getPreferredSize());
        }

        public void setRating(double rating) { this.rating = rating; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = prepare(g);
            for (int i = 0; i < 5; i++) {
                double cx = i * size + size / 2.0;
                double cy = size / 2.0 + 2;
                g2.setColor(new Color(0x343852));
                g2.fill(Icons.star(cx, cy, size / 2.0, size / 4.6, 5));
                double fill = Math.max(0, Math.min(1, rating - i));
                if (fill > 0) {
                    java.awt.Shape old = g2.getClip();
                    g2.clipRect((int) (cx - size / 2.0), 0, (int) Math.ceil(size * fill), getHeight());
                    g2.setColor(AMBER);
                    g2.fill(Icons.star(cx, cy, size / 2.0, size / 4.6, 5));
                    g2.setClip(old);
                }
            }
            g2.dispose();
        }
    }

    /** A labelled figure. The big number is the point, the label supports it. */
    public static Card statCard(String value, String caption, String icon, Color tone) {
        Card card = new Card(new java.awt.BorderLayout(0, 2));
        card.pad(16, 18, 16, 18);

        JPanel top = clear(new java.awt.BorderLayout());
        JLabel number = label(value, 27, Font.BOLD, TEXT);
        top.add(number, java.awt.BorderLayout.WEST);
        top.add(new IconBadge(icon, tone), java.awt.BorderLayout.EAST);

        card.add(top, java.awt.BorderLayout.CENTER);
        card.add(label(caption, 12, Font.PLAIN, MUTED), java.awt.BorderLayout.SOUTH);
        return card;
    }

    /** Rounded tinted square holding one icon. */
    public static class IconBadge extends JComponent {
        private final String icon;
        private final Color tone;
        private final int size;

        public IconBadge(String icon, Color tone) { this(icon, tone, 34); }

        public IconBadge(String icon, Color tone, int size) {
            this.icon = icon;
            this.tone = tone;
            this.size = size;
            setPreferredSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = prepare(g);
            g2.setColor(alpha(tone, 40));
            g2.fill(new RoundRectangle2D.Double(0, 0, size, size, 11, 11));
            Icons.draw(g2, icon, size * 0.26, size * 0.26, size * 0.48, tone);
            g2.dispose();
        }
    }

    // ----------------------------------------------------------------- colours

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    public static Color lighten(Color c, float amount) {
        return new Color(
                Math.min(255, (int) (c.getRed() + 255 * amount)),
                Math.min(255, (int) (c.getGreen() + 255 * amount)),
                Math.min(255, (int) (c.getBlue() + 255 * amount)));
    }

    public static Color darken(Color c, float amount) {
        return new Color(
                Math.max(0, (int) (c.getRed() * (1 - amount))),
                Math.max(0, (int) (c.getGreen() * (1 - amount))),
                Math.max(0, (int) (c.getBlue() * (1 - amount))));
    }

    public static Graphics2D prepare(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }
}
