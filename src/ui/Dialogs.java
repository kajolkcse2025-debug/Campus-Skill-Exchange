package ui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Styled replacements for JOptionPane, plus a small form builder. */
public final class Dialogs {

    private Dialogs() { }

    public static void info(Component parent, String title, String text) {
        message(parent, title, text, Theme.BLUE, "book");
    }

    public static void success(Component parent, String title, String text) {
        message(parent, title, text, Theme.TEAL, "shield");
    }

    public static void error(Component parent, String title, String text) {
        message(parent, title, text, Theme.RED, "spark");
    }

    public static void message(Component parent, String title, String text, Color tone, String icon) {
        JDialog dialog = shell(parent, title);
        Theme.Card card = new Theme.Card(new BorderLayout(16, 0));
        card.pad(22, 22, 18, 22);

        card.add(new Theme.IconBadge(icon, tone, 40), BorderLayout.WEST);

        JPanel body = Theme.clear(new BorderLayout(0, 8));
        body.add(Theme.label(title, 16, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        body.add(Theme.wrapped(text, 320, Theme.MUTED, 13), BorderLayout.CENTER);

        Theme.Btn close = Theme.solid("Got it", tone);
        close.addActionListener(e -> dialog.dispose());
        JPanel actions = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.add(close);
        body.add(actions, BorderLayout.SOUTH);

        card.add(body, BorderLayout.CENTER);
        finish(dialog, card, 430);
    }

    public static boolean confirm(Component parent, String title, String text,
                                  String confirmLabel, Color tone) {
        JDialog dialog = shell(parent, title);
        final boolean[] answer = {false};

        Theme.Card card = new Theme.Card(new BorderLayout(16, 0));
        card.pad(22, 22, 18, 22);
        card.add(new Theme.IconBadge("shield", tone, 40), BorderLayout.WEST);

        JPanel body = Theme.clear(new BorderLayout(0, 8));
        body.add(Theme.label(title, 16, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        body.add(Theme.wrapped(text, 320, Theme.MUTED, 13), BorderLayout.CENTER);

        Theme.Btn cancel = Theme.quiet("Cancel");
        cancel.addActionListener(e -> dialog.dispose());
        Theme.Btn go = Theme.solid(confirmLabel, tone);
        go.addActionListener(e -> {
            answer[0] = true;
            dialog.dispose();
        });

        JPanel actions = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(cancel);
        actions.add(go);
        body.add(actions, BorderLayout.SOUTH);

        card.add(body, BorderLayout.CENTER);
        finish(dialog, card, 440);
        return answer[0];
    }

    private static JDialog shell(Component parent, String title) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, JDialog.DEFAULT_MODALITY_TYPE);
        dialog.setResizable(false);
        return dialog;
    }

    private static void finish(JDialog dialog, JComponent content, int width) {
        JPanel backing = new JPanel(new BorderLayout());
        backing.setBackground(Theme.BG);
        backing.setBorder(new EmptyBorder(10, 10, 10, 10));
        backing.add(content, BorderLayout.CENTER);
        dialog.setContentPane(backing);
        dialog.pack();
        dialog.setSize(Math.max(width, dialog.getWidth()), dialog.getHeight());
        dialog.setLocationRelativeTo(dialog.getOwner());
        dialog.setVisible(true);
    }

    // ------------------------------------------------------------- form dialog

    /** A stacked label-over-input form in a modal, with inline validation. */
    public static class Form {

        private final JDialog dialog;
        private final JPanel fields = Theme.clear(null);
        private final JLabel errorLabel = Theme.label(" ", 12, Font.PLAIN, Theme.RED);
        private final List<Runnable> beforeSubmit = new ArrayList<>();
        private Supplier<String> validator = () -> null;
        private boolean submitted;
        private String submitLabel = "Save";
        private Color tone = Theme.VIOLET;
        private final String title;
        private final String subtitle;

        public Form(Component parent, String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
            dialog = shell(parent, title);
            fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        }

        public Form submitLabel(String label) { this.submitLabel = label; return this; }
        public Form tone(Color tone) { this.tone = tone; return this; }
        public Form validate(Supplier<String> validator) { this.validator = validator; return this; }

        private void addRow(String caption, JComponent input) {
            JPanel row = Theme.clear(new BorderLayout(0, 6));
            row.setBorder(new EmptyBorder(0, 0, 14, 0));
            row.setAlignmentX(0f);
            if (caption != null) {
                row.add(Theme.label(caption, 12, Font.BOLD, Theme.MUTED), BorderLayout.NORTH);
            }
            row.add(input, BorderLayout.CENTER);
            row.setMaximumSize(new Dimension(Short.MAX_VALUE,
                    input.getPreferredSize().height + (caption == null ? 14 : 34)));
            fields.add(row);
        }

        public JTextField addField(String caption, String hint, String value) {
            Theme.HintField field = new Theme.HintField(hint, 20);
            field.setText(value);
            addRow(caption, field);
            return field;
        }

        public JTextArea addArea(String caption, String value, int rows) {
            JTextArea area = new JTextArea(value);
            addRow(caption, Theme.area(area, rows));
            return area;
        }

        public <T> JComboBox<T> addCombo(String caption, T[] items, T selected) {
            JComboBox<T> box = Theme.combo(items);
            if (selected != null) box.setSelectedItem(selected);
            box.setPreferredSize(new Dimension(200, 34));
            addRow(caption, box);
            return box;
        }

        public StarPicker addStars(String caption, int initial) {
            StarPicker picker = new StarPicker(initial);
            addRow(caption, picker);
            return picker;
        }

        public void addNote(String text) {
            JTextArea note = Theme.wrapped(text, 360, Theme.FAINT, 12);
            addRow(null, note);
        }

        public void onSubmit(Runnable action) { beforeSubmit.add(action); }

        /** Shows the modal; returns true when the user submitted and validation passed. */
        public boolean show() {
            Theme.Card card = new Theme.Card(new BorderLayout(0, 14));
            card.pad(22, 22, 18, 22);

            JPanel head = Theme.clear(new BorderLayout(0, 4));
            head.add(Theme.label(title, 18, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
            if (subtitle != null && !subtitle.isEmpty()) {
                head.add(Theme.wrapped(subtitle, 380, Theme.MUTED, 13), BorderLayout.CENTER);
            }
            card.add(head, BorderLayout.NORTH);
            card.add(fields, BorderLayout.CENTER);

            JPanel footer = Theme.clear(new BorderLayout(0, 8));
            footer.add(errorLabel, BorderLayout.NORTH);

            Theme.Btn cancel = Theme.quiet("Cancel");
            cancel.addActionListener(e -> dialog.dispose());
            Theme.Btn save = Theme.solid(submitLabel, tone);
            save.addActionListener(e -> {
                String problem = validator.get();
                if (problem != null) {
                    errorLabel.setText(problem);
                    return;
                }
                submitted = true;
                for (Runnable r : beforeSubmit) r.run();
                dialog.dispose();
            });

            JPanel actions = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actions.add(cancel);
            actions.add(save);
            footer.add(actions, BorderLayout.SOUTH);
            card.add(footer, BorderLayout.SOUTH);

            JPanel backing = new JPanel(new BorderLayout());
            backing.setBackground(Theme.BG);
            backing.setBorder(new EmptyBorder(10, 10, 10, 10));
            backing.add(card, BorderLayout.CENTER);

            dialog.setContentPane(backing);
            dialog.pack();
            dialog.setSize(Math.max(460, dialog.getWidth()), Math.min(dialog.getHeight(), 700));
            dialog.setLocationRelativeTo(dialog.getOwner());
            dialog.setVisible(true);
            return submitted;
        }
    }

    /** Five clickable stars. */
    public static class StarPicker extends JComponent {

        private int rating;
        private int hovered;

        public StarPicker(int initial) {
            this.rating = initial;
            setPreferredSize(new Dimension(190, 36));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { rating = starAt(e.getX()); repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = 0; repaint(); }
            });
            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) { hovered = starAt(e.getX()); repaint(); }
            });
        }

        private int starAt(int x) { return Math.max(1, Math.min(5, x / 34 + 1)); }

        public int getRating() { return rating; }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.prepare(g);
            int shown = hovered > 0 ? hovered : rating;
            for (int i = 0; i < 5; i++) {
                g2.setColor(i < shown ? Theme.AMBER : new Color(0x343852));
                g2.fill(Icons.star(i * 34 + 15, 17, 14, 6.2, 5));
            }
            g2.setFont(Theme.font(Font.PLAIN, 12));
            g2.setColor(Theme.MUTED);
            g2.drawString(label(shown), 180, 22);
            g2.dispose();
        }

        private String label(int stars) {
            switch (stars) {
                case 1: return "Poor";
                case 2: return "Below par";
                case 3: return "Fine";
                case 4: return "Good";
                default: return "Excellent";
            }
        }
    }

}
