package ui;

import model.CreditTransaction;
import model.User;
import service.CreditRules;
import service.CreditService;
import util.Util;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

/** The credit passbook: balance, what you earned, what you spent and every line. */
public class CreditsPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final CreditService credits = new CreditService();

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Date", "Activity", "Detail", "Amount", "Balance"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JPanel summary = Theme.clear(new GridLayout(1, 3, 16, 0));

    public CreditsPanel(MainFrame frame) {
        super(new BorderLayout(0, 16));
        this.frame = frame;
        setOpaque(false);

        summary.setPreferredSize(new Dimension(900, 168));
        add(summary, BorderLayout.NORTH);

        JTable table = new JTable(model);
        Theme.styleTable(table);
        table.getColumnModel().getColumn(0).setMinWidth(140);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(0).setMaxWidth(170);
        table.getColumnModel().getColumn(3).setMaxWidth(110);
        table.getColumnModel().getColumn(4).setMaxWidth(110);

        Theme.Card card = new Theme.Card(new BorderLayout(0, 10));
        card.pad(16, 16, 10, 16);
        JPanel head = Theme.clear(new BorderLayout());
        head.add(Theme.h2("Your credit history"), BorderLayout.WEST);
        head.add(Theme.label("Newest first", 12, Font.PLAIN, Theme.FAINT), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);
        card.add(Theme.scroll(table), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        User me = frame.user();
        int earned = credits.earnedBy(me.getId());
        int spent = credits.spentBy(me.getId());

        summary.removeAll();
        summary.add(balanceCard(me, earned));
        summary.add(flowCard(earned, spent));
        summary.add(rulesCard());
        summary.revalidate();
        summary.repaint();

        model.setRowCount(0);
        List<CreditTransaction> history = credits.historyOf(me.getId());
        for (CreditTransaction t : history) {
            model.addRow(new Object[]{
                    Util.pretty(t.getStamp()),
                    t.getType(),
                    t.getDescription(),
                    (t.getAmount() > 0 ? "+" : "\u2212") + Math.abs(t.getAmount()),
                    String.valueOf(t.getBalanceAfter())});
        }
    }

    private Theme.Card balanceCard(User me, int earned) {
        Theme.GradientCard card = new Theme.GradientCard(new BorderLayout(18, 0),
                new Color(0x4A3A16), new Color(0x231F2E));
        card.pad(20, 22, 20, 22);

        JPanel text = Theme.clear(new BorderLayout(0, 6));
        text.add(Theme.label("Balance", 13, Font.BOLD, new Color(0xE8D9B8)), BorderLayout.NORTH);
        text.add(Theme.label(String.valueOf(me.getCredits()), 40, Font.BOLD, Theme.AMBER),
                BorderLayout.CENTER);
        text.add(Theme.label("About " + affordable(me) + " more sessions", 12, Font.PLAIN,
                new Color(0xCBBFA4)), BorderLayout.SOUTH);
        card.add(text, BorderLayout.CENTER);

        double kept = earned == 0 ? 0 : me.getCredits() / (double) earned;
        card.add(new Charts.Ring(kept, Math.round(kept * 100) + "%", "kept", Theme.AMBER, 104),
                BorderLayout.EAST);
        return card;
    }

    private String affordable(User me) {
        return String.valueOf(me.getCredits() / 12);
    }

    private Theme.Card flowCard(int earned, int spent) {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 14));
        card.pad(20, 22, 20, 22);
        card.add(Theme.h2("Earned and spent"), BorderLayout.NORTH);

        JPanel rows = Theme.clear(new GridLayout(2, 1, 0, 14));
        rows.add(flowRow("Earned by teaching", earned, Theme.GREEN));
        rows.add(flowRow("Spent on learning", spent, Theme.AMBER));
        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    private JPanel flowRow(String caption, int value, Color tone) {
        JPanel row = Theme.clear(new BorderLayout(0, 5));
        JPanel top = Theme.clear(new BorderLayout());
        top.add(Theme.label(caption, 12, Font.PLAIN, Theme.MUTED), BorderLayout.WEST);
        top.add(Theme.label(String.valueOf(value), 14, Font.BOLD, tone), BorderLayout.EAST);
        row.add(top, BorderLayout.NORTH);
        return row;
    }

    private Theme.Card rulesCard() {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 12));
        card.pad(20, 22, 20, 22);
        card.add(Theme.h2("How credits work"), BorderLayout.NORTH);

        JPanel lines = Theme.clear(new GridLayout(4, 1, 0, 8));
        lines.add(rule("Teach a session", "+ the price you set"));
        lines.add(rule("Teaching bonus", "+" + CreditRules.TEACH_BONUS + " every session"));
        lines.add(rule("Five star review", "+" + CreditRules.FIVE_STAR_BONUS + " to the teacher"));
        lines.add(rule("Join a session", "\u2212 the price of that skill"));
        card.add(lines, BorderLayout.CENTER);
        return card;
    }

    private JPanel rule(String left, String right) {
        JPanel row = Theme.clear(new BorderLayout());
        row.add(Theme.label(left, 12, Font.PLAIN, Theme.MUTED), BorderLayout.WEST);
        row.add(Theme.label(right, 12, Font.BOLD,
                right.startsWith("+") ? Theme.GREEN : Theme.AMBER), BorderLayout.EAST);
        return row;
    }
}
