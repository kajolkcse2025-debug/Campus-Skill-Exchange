package ui;

import model.User;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The shell: sidebar on the left, a title bar and the active page on the right. */
public class MainFrame extends JFrame {

    private final User user;
    private final CardLayout pages = new CardLayout();
    private final JPanel body = Theme.clear(pages);
    private final Map<String, JComponent> registry = new LinkedHashMap<>();
    private final List<NavItem> navItems = new ArrayList<>();

    private final JLabel pageTitle = Theme.label("", 21, Font.BOLD, Theme.TEXT);
    private final JLabel pageHint = Theme.label("", 12, Font.PLAIN, Theme.MUTED);
    private final CreditPill creditPill;

    public MainFrame(User user) {
        this.user = user;
        this.creditPill = new CreditPill();

        setTitle("Campus Skill Exchange  \u00b7  " + user.getName());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 700));
        setSize(1320, 820);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildContent(), BorderLayout.CENTER);
        setContentPane(root);

        show("dashboard");
    }

    public User user() { return user; }

    /** Lets a page send the user somewhere else, for example after a first skill is added. */
    public void show(String key) {
        JComponent page = registry.get(key);
        if (page instanceof Refreshable) ((Refreshable) page).refresh();
        pages.show(body, key);
        for (NavItem item : navItems) item.setSelected(item.key.equals(key));
        String[] copy = titleFor(key);
        pageTitle.setText(copy[0]);
        pageHint.setText(copy[1]);
        creditPill.refresh();
    }

    /** Called by pages after anything that moves credits. */
    public void refreshChrome() {
        creditPill.refresh();
    }

    private String[] titleFor(String key) {
        switch (key) {
            case "browse":      return new String[]{"Browse skills",
                    "Find someone on campus who can teach you this."};
            case "myskills":    return new String[]{"Skills you teach",
                    "List what you are good at and set a price in credits."};
            case "sessions":    return new String[]{"Sessions",
                    "Requests waiting on you, and everything you have booked."};
            case "credits":     return new String[]{"Credits",
                    "Every credit you have earned and spent."};
            case "leaderboard": return new String[]{"Leaderboard",
                    "Who is teaching the most this semester."};
            case "profile":     return new String[]{"Your profile",
                    "How the rest of campus sees you."};
            case "reports":     return new String[]{"Reports",
                    "Campus wide activity, credits and skill trends."};
            case "admin":       return new String[]{"Administration",
                    "Manage students, skills and sessions."};
            default:            return new String[]{"Hello, " + firstName(),
                    "Here is what is happening on campus today."};
        }
    }

    private String firstName() {
        String[] parts = user.getName().split("\\s+");
        return parts[0];
    }

    // ----------------------------------------------------------------- sidebar

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(Theme.PANEL);
        side.setPreferredSize(new Dimension(236, 100));
        side.setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel brand = Theme.clear(new BorderLayout(11, 0));
        brand.setBorder(new EmptyBorder(24, 20, 22, 20));
        brand.add(new Theme.IconBadge("spark", Theme.VIOLET, 36), BorderLayout.WEST);
        JPanel words = Theme.clear(new BorderLayout());
        words.add(Theme.label("Skill Exchange", 15, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        words.add(Theme.label("Sri Ramanujan College", 11, Font.PLAIN, Theme.FAINT), BorderLayout.SOUTH);
        brand.add(words, BorderLayout.CENTER);
        side.add(brand, BorderLayout.NORTH);

        JPanel nav = Theme.clear(null);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(new EmptyBorder(0, 12, 0, 12));

        addNav(nav, "dashboard", "Dashboard", "home");
        addNav(nav, "browse", "Browse skills", "search");
        addNav(nav, "myskills", "Skills you teach", "grid");
        addNav(nav, "sessions", "Sessions", "calendar");
        addNav(nav, "credits", "Credits", "coin");
        addNav(nav, "leaderboard", "Leaderboard", "trophy");
        addNav(nav, "profile", "Profile", "user");
        if (user.isAdmin()) {
            nav.add(sectionLabel("Staff"));
            addNav(nav, "reports", "Reports", "chart");
            addNav(nav, "admin", "Administration", "shield");
        }
        side.add(nav, BorderLayout.CENTER);
        side.add(buildAccountBox(), BorderLayout.SOUTH);
        return side;
    }

    private JComponent sectionLabel(String text) {
        JLabel l = Theme.label(text, 11, Font.BOLD, Theme.FAINT);
        l.setBorder(new EmptyBorder(16, 14, 6, 0));
        l.setAlignmentX(0f);
        return l;
    }

    private void addNav(JPanel nav, String key, String text, String icon) {
        NavItem item = new NavItem(key, text, icon);
        navItems.add(item);
        nav.add(item);
    }

    private JPanel buildAccountBox() {
        JPanel box = Theme.clear(new BorderLayout(10, 0));
        box.setBorder(new EmptyBorder(16, 18, 20, 16));

        box.add(new Theme.Avatar(user.initials(), 36, Theme.Avatar.toneFor(user.getName())),
                BorderLayout.WEST);

        JPanel who = Theme.clear(new BorderLayout());
        who.add(Theme.label(trim(user.getName(), 16), 13, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        who.add(Theme.label(user.isAdmin() ? "Student Affairs" : trim(user.getDepartment(), 18),
                11, Font.PLAIN, Theme.FAINT), BorderLayout.SOUTH);
        box.add(who, BorderLayout.CENTER);

        JPanel iconHolder = Theme.clear(new BorderLayout());
        LogoutButton logout = new LogoutButton();
        iconHolder.add(logout, BorderLayout.CENTER);
        box.add(iconHolder, BorderLayout.EAST);
        return box;
    }

    private void signOut() {
        if (!Dialogs.confirm(this, "Sign out?",
                "You will be returned to the sign in screen.", "Sign out", Theme.VIOLET)) return;
        dispose();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    private static String trim(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "\u2026";
    }

    // ----------------------------------------------------------------- content

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.BG);

        JPanel bar = Theme.clear(new BorderLayout());
        bar.setBorder(new EmptyBorder(26, 30, 18, 30));
        JPanel titles = Theme.clear(new BorderLayout(0, 3));
        titles.add(pageTitle, BorderLayout.NORTH);
        titles.add(pageHint, BorderLayout.SOUTH);
        bar.add(titles, BorderLayout.WEST);
        bar.add(creditPill, BorderLayout.EAST);
        content.add(bar, BorderLayout.NORTH);

        body.setBorder(new EmptyBorder(0, 30, 24, 30));
        register("dashboard", new DashboardPanel(this));
        register("browse", new BrowsePanel(this));
        register("myskills", new MySkillsPanel(this));
        register("sessions", new SessionsPanel(this));
        register("credits", new CreditsPanel(this));
        register("leaderboard", new LeaderboardPanel(this));
        register("profile", new ProfilePanel(this));
        if (user.isAdmin()) {
            register("reports", new ReportsPanel(this));
            register("admin", new AdminPanel(this));
        }
        content.add(body, BorderLayout.CENTER);
        return content;
    }

    private void register(String key, JComponent page) {
        registry.put(key, page);
        body.add(page, key);
    }

    // ------------------------------------------------------------ nav elements

    private class NavItem extends JPanel {
        private final String key;
        private final String text;
        private final String icon;
        private boolean selected;
        private boolean hover;

        NavItem(String key, String text, String icon) {
            this.key = key;
            this.text = text;
            this.icon = icon;
            setOpaque(false);
            setPreferredSize(new Dimension(200, 42));
            setMaximumSize(new Dimension(Short.MAX_VALUE, 42));
            setAlignmentX(0f);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { MainFrame.this.show(key); }
            });
        }

        void setSelected(boolean selected) {
            this.selected = selected;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.prepare(g);
            if (selected) {
                g2.setColor(Theme.alpha(Theme.VIOLET, 46));
                g2.fill(new RoundRectangle2D.Double(0, 2, getWidth(), getHeight() - 4, 11, 11));
                g2.setColor(Theme.VIOLET);
                g2.fill(new RoundRectangle2D.Double(0, 10, 3.5, getHeight() - 20, 3, 3));
            } else if (hover) {
                g2.setColor(Theme.alpha(Color.WHITE, 12));
                g2.fill(new RoundRectangle2D.Double(0, 2, getWidth(), getHeight() - 4, 11, 11));
            }
            Color tone = selected ? Theme.VIOLET_HI : (hover ? Theme.TEXT : Theme.MUTED);
            Icons.draw(g2, icon, 14, getHeight() / 2.0 - 9, 18, tone);
            g2.setFont(Theme.font(selected ? Font.BOLD : Font.PLAIN, 13));
            g2.setColor(selected ? Theme.TEXT : tone);
            g2.drawString(text, 42, getHeight() / 2 + 5);
            g2.dispose();
        }
    }

    private class LogoutButton extends JComponent {
        private boolean hover;

        LogoutButton() {
            setPreferredSize(new Dimension(32, 32));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Sign out");
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { signOut(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.prepare(g);
            if (hover) {
                g2.setColor(Theme.alpha(Theme.RED, 34));
                g2.fill(new RoundRectangle2D.Double(0, 0, 32, 32, 10, 10));
            }
            Icons.draw(g2, "logout", 7, 7, 18, hover ? Theme.RED : Theme.FAINT);
            g2.dispose();
        }
    }

    /** The balance, always visible, in the colour reserved for credits. */
    private class CreditPill extends JComponent {
        private String text = "";

        CreditPill() {
            setPreferredSize(new Dimension(150, 44));
            setToolTipText("Your credit balance");
            refresh();
        }

        void refresh() {
            text = user.isAdmin() ? "Staff account" : user.getCredits() + " credits";
            setPreferredSize(new Dimension(
                    getFontMetrics(Theme.font(Font.BOLD, 14)).stringWidth(text) + 62, 44));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.prepare(g);
            g2.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.AMBER, 46),
                    getWidth(), getHeight(), Theme.alpha(Theme.AMBER, 22)));
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 22, 22));
            g2.setColor(Theme.alpha(Theme.AMBER, 110));
            g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 2, getHeight() - 2, 22, 22));
            Icons.draw(g2, "coin", 14, getHeight() / 2.0 - 9, 18, Theme.AMBER);
            g2.setFont(Theme.font(Font.BOLD, 14));
            g2.setColor(Theme.AMBER);
            g2.drawString(text, 42, getHeight() / 2 + 5);
            g2.dispose();
        }
    }

    /** Shared helper so every page lays cards out on the same grid. */
    public static JPanel grid(int columns, int gap) {
        JPanel p = Theme.clear(new java.awt.GridLayout(0, columns, gap, gap));
        return p;
    }

    public static Component vspace(int height) {
        return javax.swing.Box.createVerticalStrut(height);
    }
}
