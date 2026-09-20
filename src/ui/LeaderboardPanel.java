package ui;

import model.Badge;
import model.User;
import service.LeaderboardService;
import util.Util;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.Ellipse2D;
import java.util.List;

/** Who is contributing the most, all time or this month, plus your own badges. */
public class LeaderboardPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final LeaderboardService board = new LeaderboardService();

    private final JPanel podium = Theme.clear(new GridLayout(1, 3, 16, 0));
    private final JPanel tabs = Theme.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Rank", "Student", "Department", "Taught", "Attended", "Rating", "Earned"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JPanel badgeStrip = Theme.clear(new GridLayout(1, 8, 6, 0));

    private boolean monthly;

    public LeaderboardPanel(MainFrame frame) {
        super(new BorderLayout(0, 16));
        this.frame = frame;
        setOpaque(false);

        JPanel north = Theme.clear(new BorderLayout(0, 16));
        north.add(tabs, BorderLayout.NORTH);
        podium.setPreferredSize(new Dimension(900, 178));
        north.add(podium, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        JTable table = new JTable(model);
        Theme.styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(70);

        Theme.Card tableCard = new Theme.Card(new BorderLayout(0, 10));
        tableCard.pad(16, 16, 10, 16);
        tableCard.add(Theme.h2("Full ranking"), BorderLayout.NORTH);
        tableCard.add(Theme.scroll(table), BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        Theme.Card badges = new Theme.Card(new BorderLayout(0, 12));
        badges.pad(16, 18, 14, 18);
        badges.setPreferredSize(new Dimension(900, 214));
        badges.add(Theme.h2("Your badges"), BorderLayout.NORTH);
        badges.add(badgeStrip, BorderLayout.CENTER);
        add(badges, BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        buildTabs();

        List<LeaderboardService.Row> rows = board.ranking(monthly);

        podium.removeAll();
        int[] order = {1, 0, 2};        // second, first, third, so the winner sits in the middle
        for (int slot : order) {
            if (slot < rows.size()) podium.add(podiumCard(rows.get(slot)));
            else podium.add(Theme.clear(new BorderLayout()));
        }
        podium.revalidate();
        podium.repaint();

        model.setRowCount(0);
        for (LeaderboardService.Row r : rows) {
            model.addRow(new Object[]{
                    "#" + r.rank,
                    r.user.getName() + (r.user.getId() == frame.user().getId() ? "  (you)" : ""),
                    r.user.getDepartment(),
                    r.taught,
                    r.learned,
                    r.rating == 0 ? "\u2013" : Util.round(r.rating),
                    r.creditsEarned});
        }

        badgeStrip.removeAll();
        List<Badge> badges = board.badgesFor(frame.user());
        for (Badge b : badges) badgeStrip.add(new BadgeView(b, true));
        badgeStrip.revalidate();
        badgeStrip.repaint();
    }

    private void buildTabs() {
        tabs.removeAll();
        Theme.Btn allTime = monthly ? Theme.quiet("All time") : Theme.solid("All time");
        Theme.Btn thisMonth = monthly ? Theme.solid(Util.monthOf(Util.now()))
                                      : Theme.quiet(Util.monthOf(Util.now()));
        allTime.addActionListener(e -> {
            monthly = false;
            refresh();
        });
        thisMonth.addActionListener(e -> {
            monthly = true;
            refresh();
        });
        tabs.add(allTime);
        tabs.add(thisMonth);
        tabs.revalidate();
        tabs.repaint();
    }

    private Theme.Card podiumCard(LeaderboardService.Row row) {
        boolean winner = row.rank == 1;
        boolean me = row.user.getId() == frame.user().getId();

        Theme.Card card = winner
                ? new Theme.GradientCard(new BorderLayout(0, 8), new Color(0x4A3A16), new Color(0x241F32))
                : new Theme.Card(new BorderLayout(0, 8));
        card.pad(winner ? 16 : 24, 18, 16, 18);
        if (me && !winner) card.stroke(Theme.VIOLET);

        JPanel head = Theme.clear(new BorderLayout(12, 0));
        head.add(new Medal(row.rank), BorderLayout.WEST);

        JPanel who = Theme.clear(new BorderLayout(0, 4));
        who.add(Theme.label(row.user.getName() + (me ? "  (you)" : ""), 15, Font.BOLD,
                Theme.TEXT), BorderLayout.NORTH);
        who.add(Theme.label(row.user.getDepartment() + "  \u00b7  " + row.user.getYear(),
                11, Font.PLAIN, Theme.FAINT), BorderLayout.SOUTH);
        head.add(who, BorderLayout.CENTER);
        card.add(head, BorderLayout.NORTH);

        JPanel facts = Theme.clear(new GridLayout(1, 3, 6, 0));
        facts.add(fact(String.valueOf(row.taught), "taught"));
        facts.add(fact(row.rating == 0 ? "\u2013" : Util.round(row.rating), "rating"));
        facts.add(fact(String.valueOf(row.creditsEarned), "earned"));
        card.add(facts, BorderLayout.SOUTH);
        return card;
    }

    private JPanel fact(String value, String caption) {
        JPanel p = Theme.clear(new BorderLayout(0, 2));
        p.add(Theme.label(value, 17, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        p.add(Theme.label(caption, 11, Font.PLAIN, Theme.MUTED), BorderLayout.SOUTH);
        return p;
    }

    /** Gold, silver and bronze discs carrying the rank. */
    private static class Medal extends javax.swing.JComponent {
        private final int rank;
        private static final int SIZE = 46;

        Medal(int rank) {
            this.rank = rank;
            setPreferredSize(new Dimension(SIZE, SIZE));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.prepare(g);
            Color tone;
            if (rank == 1) tone = new Color(0xFFC94D);
            else if (rank == 2) tone = new Color(0xC7CCE0);
            else tone = new Color(0xD08A52);

            g2.setColor(Theme.alpha(tone, 50));
            g2.fill(new Ellipse2D.Double(0, 0, SIZE, SIZE));
            g2.setPaint(new java.awt.GradientPaint(0, 0, Theme.lighten(tone, 0.14f),
                    SIZE, SIZE, Theme.darken(tone, 0.3f)));
            g2.fill(new Ellipse2D.Double(4, 4, SIZE - 8, SIZE - 8));

            g2.setFont(Theme.font(Font.BOLD, 17));
            g2.setColor(new Color(0x231B07));
            String text = String.valueOf(rank);
            int w = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, (SIZE - w) / 2, SIZE / 2 + 6);
            g2.dispose();
        }
    }
}
