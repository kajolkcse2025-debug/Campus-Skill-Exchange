package ui;

import model.User;
import service.AuthService;
import service.AuthService.RuleException;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.Random;

/** Sign in and registration, side by side with the campus skill network artwork. */
public class LoginFrame extends JFrame {

    private final AuthService auth = new AuthService();
    private final CardLayout cards = new CardLayout();
    private final JPanel forms = Theme.clear(cards);
    private final JLabel notice = Theme.label(" ", 12, Font.BOLD, Theme.RED);

    private Theme.Btn signInTab;
    private Theme.Btn joinTab;

    public LoginFrame() {
        setTitle("Campus Skill Exchange");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 620));
        setSize(1000, 660);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(new NetworkPanel(), BorderLayout.WEST);
        root.add(buildRight(), BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildRight() {
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Theme.BG);
        right.setBorder(new EmptyBorder(52, 46, 32, 46));

        JPanel head = Theme.clear(new BorderLayout(0, 6));
        head.add(Theme.label("Welcome back", 25, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        head.add(Theme.label("Sign in to trade skills with your campus.", 13, Font.PLAIN, Theme.MUTED),
                BorderLayout.CENTER);

        JPanel tabs = Theme.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tabs.setBorder(new EmptyBorder(22, 0, 16, 0));
        signInTab = Theme.solid("Sign in");
        joinTab = Theme.quiet("Create account");
        signInTab.addActionListener(e -> showCard("signin"));
        joinTab.addActionListener(e -> showCard("join"));
        tabs.add(signInTab);
        tabs.add(joinTab);
        head.add(tabs, BorderLayout.SOUTH);

        forms.add(buildSignIn(), "signin");
        forms.add(buildJoin(), "join");

        right.add(head, BorderLayout.NORTH);
        JPanel formHolder = Theme.clear(new BorderLayout());
        formHolder.add(forms, BorderLayout.NORTH);
        right.add(formHolder, BorderLayout.CENTER);

        JPanel foot = Theme.clear(new BorderLayout(0, 4));
        foot.setBorder(new EmptyBorder(14, 0, 0, 0));
        foot.add(notice, BorderLayout.NORTH);
        JPanel demo = Theme.clear(new java.awt.GridLayout(2, 1, 0, 2));
        demo.add(Theme.label("Student demo  \u00b7  aarav@campus.edu  /  pass123",
                12, Font.PLAIN, Theme.FAINT));
        demo.add(Theme.label("Admin demo  \u00b7  admin@campus.edu  /  admin123",
                12, Font.PLAIN, Theme.FAINT));
        foot.add(demo, BorderLayout.SOUTH);
        right.add(foot, BorderLayout.SOUTH);
        return right;
    }

    private void showCard(String name) {
        cards.show(forms, name);
        notice.setText(" ");
        boolean signIn = "signin".equals(name);
        signInTab.setTone(signIn ? Theme.VIOLET : Theme.MUTED);
        joinTab.setTone(signIn ? Theme.MUTED : Theme.VIOLET);
        remakeTabs(signIn);
    }

    /** Swing buttons keep their kind, so the look is swapped by tone and weight. */
    private void remakeTabs(boolean signIn) {
        signInTab.setFont(Theme.font(signIn ? Font.BOLD : Font.PLAIN, 13));
        joinTab.setFont(Theme.font(signIn ? Font.PLAIN : Font.BOLD, 13));
        signInTab.repaint();
        joinTab.repaint();
    }

    private JPanel buildSignIn() {
        JPanel card = Theme.clear(null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        Theme.HintField email = new Theme.HintField("you@campus.edu", 18);
        Theme.HintPassword password = new Theme.HintPassword("Your password", 18);

        card.add(field("Campus email", email));
        card.add(field("Password", password));

        Theme.Btn go = Theme.solid("Sign in");
        go.setAlignmentX(0f);
        go.setPreferredSize(new Dimension(160, 42));
        go.setMaximumSize(new Dimension(180, 42));
        go.addActionListener(e -> attemptLogin(email.getText(), new String(password.getPassword())));

        KeyAdapter enter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    attemptLogin(email.getText(), new String(password.getPassword()));
                }
            }
        };
        email.addKeyListener(enter);
        password.addKeyListener(enter);

        card.add(go);
        return card;
    }

    private JPanel buildJoin() {
        JPanel card = Theme.clear(null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        Theme.HintField name = new Theme.HintField("Full name", 18);
        Theme.HintField email = new Theme.HintField("you@campus.edu", 18);
        JComboBox<String> department = Theme.combo(new String[]{
            "Computer Science", "Information Tech", "Electronics", "Electrical", "Mechanical",
            "Civil", "Architecture", "Biotechnology", "Commerce", "Humanities"});
        JComboBox<String> year = Theme.combo(new String[]{
            "1st Year", "2nd Year", "3rd Year", "4th Year", "Postgraduate"});
        Theme.HintPassword password = new Theme.HintPassword("At least 6 characters", 18);
        Theme.HintPassword confirm = new Theme.HintPassword("Repeat password", 18);

        card.add(field("Full name", name));
        card.add(field("Campus email", email));

        JPanel pair = Theme.clear(new GridLayout(1, 2, 12, 0));
        pair.setAlignmentX(0f);
        pair.setMaximumSize(new Dimension(Short.MAX_VALUE, 70));
        pair.add(field("Department", department));
        pair.add(field("Year", year));
        card.add(pair);

        card.add(field("Password", password));
        card.add(field("Confirm password", confirm));

        JLabel welcome = Theme.label("New members start with 60 credits, enough for four sessions.",
                12, Font.PLAIN, Theme.AMBER);
        welcome.setAlignmentX(0f);
        welcome.setBorder(new EmptyBorder(0, 0, 14, 0));
        card.add(welcome);

        Theme.Btn go = Theme.solid("Create account");
        go.setAlignmentX(0f);
        go.setMaximumSize(new Dimension(190, 42));
        go.addActionListener(e -> {
            try {
                User user = auth.register(name.getText(), email.getText(),
                        new String(password.getPassword()), new String(confirm.getPassword()),
                        (String) department.getSelectedItem(), (String) year.getSelectedItem());
                open(user);
            } catch (RuleException ex) {
                notice.setText(ex.getMessage());
            }
        });
        card.add(go);
        return card;
    }

    private JPanel field(String caption, javax.swing.JComponent input) {
        JPanel row = Theme.clear(new BorderLayout(0, 6));
        row.setAlignmentX(0f);
        row.setBorder(new EmptyBorder(0, 0, 14, 0));
        row.add(Theme.label(caption, 12, Font.BOLD, Theme.MUTED), BorderLayout.NORTH);
        if (input instanceof JComboBox) input.setPreferredSize(new Dimension(180, 38));
        if (input instanceof JTextField) input.setPreferredSize(new Dimension(180, 40));
        row.add(input, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 74));
        return row;
    }

    private void attemptLogin(String email, String password) {
        try {
            open(auth.login(email, password));
        } catch (RuleException ex) {
            notice.setText(ex.getMessage());
        }
    }

    private void open(User user) {
        dispose();
        SwingUtilities.invokeLater(() -> new MainFrame(user).setVisible(true));
    }

    /**
     * The artwork: students as nodes, the skills they trade as the lines between them.
     * Drawn from a fixed seed so it looks the same every launch.
     */
    private static class NetworkPanel extends JPanel {

        private final double[][] nodes;
        private final String[] labels = {
            "Java", "Figma", "Guitar", "Spanish", "Skating", "Debate", "Photography", "Yoga", "Chess"
        };
        private final Color[] tones = {
            Theme.VIOLET, Theme.PINK, new Color(0xA78BFA), Theme.BLUE,
            Theme.GREEN, new Color(0xFB923C), Theme.TEAL, Theme.GREEN, new Color(0xFB923C)
        };

        NetworkPanel() {
            setPreferredSize(new Dimension(430, 600));
            setOpaque(false);
            Random r = new Random(7);
            nodes = new double[labels.length][2];
            for (int i = 0; i < labels.length; i++) {
                for (int attempt = 0; attempt < 400; attempt++) {
                    double x = 0.14 + r.nextDouble() * 0.58;
                    double y = 0.10 + r.nextDouble() * 0.64;
                    boolean clear = true;
                    for (int j = 0; j < i; j++) {
                        double dx = (x - nodes[j][0]) * 1.4;
                        double dy = y - nodes[j][1];
                        if (Math.sqrt(dx * dx + dy * dy) < 0.20) { clear = false; break; }
                    }
                    nodes[i][0] = x;
                    nodes[i][1] = y;
                    if (clear) break;
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.prepare(g);
            int w = getWidth(), h = getHeight();

            g2.setPaint(new GradientPaint(0, 0, new Color(0x241C56), w, h, new Color(0x141630)));
            g2.fillRect(0, 0, w, h);

            int area = (int) (h * 0.62);
            g2.setStroke(new java.awt.BasicStroke(1.1f));
            for (int i = 0; i < nodes.length; i++) {
                for (int j = i + 1; j < nodes.length; j++) {
                    double dx = nodes[i][0] - nodes[j][0];
                    double dy = nodes[i][1] - nodes[j][1];
                    if (Math.sqrt(dx * dx + dy * dy) > 0.34) continue;
                    g2.setColor(Theme.alpha(Theme.VIOLET_HI, 70));
                    g2.draw(new Line2D.Double(nodes[i][0] * w, nodes[i][1] * area,
                            nodes[j][0] * w, nodes[j][1] * area));
                }
            }

            g2.setFont(Theme.font(Font.BOLD, 12));
            for (int i = 0; i < nodes.length; i++) {
                double x = nodes[i][0] * w, y = nodes[i][1] * area;
                int size = 11;
                g2.setColor(Theme.alpha(tones[i], 55));
                g2.fill(new Ellipse2D.Double(x - size - 5, y - size - 5, (size + 5) * 2, (size + 5) * 2));
                g2.setColor(tones[i]);
                g2.fill(new Ellipse2D.Double(x - size, y - size, size * 2, size * 2));
                g2.setColor(Theme.TEXT);
                g2.drawString(labels[i], (float) (x + size + 7), (float) (y + 4));
            }

            int textTop = area + 40;
            g2.setColor(Theme.TEXT);
            g2.setFont(Theme.font(Font.BOLD, 27));
            g2.drawString("Campus Skill Exchange", 44, textTop);

            g2.setFont(Theme.font(Font.PLAIN, 15));
            g2.setColor(new Color(0xC9C7EC));
            g2.drawString("Teach what you know. Learn what you don't.", 44, textTop + 32);
            g2.setColor(Theme.alpha(Theme.AMBER, 230));
            g2.setFont(Theme.font(Font.PLAIN, 13));
            g2.drawString("Every hour you teach pays for an hour you learn.", 44, textTop + 58);
            g2.dispose();
        }
    }
}
