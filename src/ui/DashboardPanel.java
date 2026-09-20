package ui;

import data.Database;
import model.Badge;
import model.LearningSession;
import model.User;
import service.CreditService;
import service.FeedbackService;
import service.LeaderboardService;
import service.ReportService;
import service.SessionService;
import util.Util;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDateTime;
import java.util.List;

/** The landing page: balance, standing, what is next and what campus is learning. */
public class DashboardPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final Database db = Database.get();
    private final SessionService sessions = new SessionService();
    private final CreditService credits = new CreditService();
    private final FeedbackService feedback = new FeedbackService();
    private final LeaderboardService board = new LeaderboardService();
    private final ReportService reports = new ReportService();

    private final JPanel column = Theme.clear(null);

    public DashboardPanel(MainFrame frame) {
        super(new BorderLayout());
        this.frame = frame;
        setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        add(Theme.vscroll(column), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        User me = frame.user();
        column.removeAll();

        column.add(hero(me));
        column.add(MainFrame.vspace(16));
        column.add(stats(me));
        column.add(MainFrame.vspace(16));
        column.add(middle(me));
        column.add(MainFrame.vspace(16));
        column.add(badges(me));
        column.add(MainFrame.vspace(8));

        column.revalidate();
        column.repaint();
    }

    // -------------------------------------------------------------------- hero

    private JPanel hero(User me) {
        Theme.GradientCard card = new Theme.GradientCard(new BorderLayout(24, 0),
                new Color(0x3B2C7A), new Color(0x1C2140));
        card.pad(24, 28, 24, 28);
        card.setMaximumSize(new Dimension(Short.MAX_VALUE, 186));
        card.setPreferredSize(new Dimension(900, 186));

        int pending = sessions.pendingFor(me.getId()).size();
        int upcoming = sessions.upcomingFor(me.getId()).size();

        JPanel left = Theme.clear(new BorderLayout(0, 10));
        JPanel heading = Theme.clear(new BorderLayout(0, 6));
        heading.add(Theme.label(greeting() + ", " + firstName(me) + ".", 24, Font.BOLD, Color.WHITE),
                BorderLayout.NORTH);
        heading.add(Theme.label(summaryLine(me, pending, upcoming), 13, Font.PLAIN,
                new Color(0xC6C4EA)), BorderLayout.SOUTH);
        left.add(heading, BorderLayout.NORTH);

        JPanel actions = Theme.clear(new FlowLayout(FlowLayout.LEFT, 10, 0));
        Theme.Btn browse = Theme.solid("Find a skill to learn");
        browse.addActionListener(e -> frame.show("browse"));
        Theme.Btn teach = Theme.ghost("Offer a skill", new Color(0xD8D4FF));
        teach.addActionListener(e -> frame.show("myskills"));
        actions.add(browse);
        actions.add(teach);
        if (pending > 0) {
            Theme.Btn requests = Theme.ghost(pending + (pending == 1 ? " request waiting"
                    : " requests waiting"), Theme.AMBER);
            requests.addActionListener(e -> frame.show("sessions"));
            actions.add(requests);
        }
        left.add(actions, BorderLayout.SOUTH);
        card.add(left, BorderLayout.CENTER);

        if (!me.isAdmin()) {
            int earned = credits.earnedBy(me.getId());
            int spent = credits.spentBy(me.getId());
            double kept = earned == 0 ? 0 : me.getCredits() / (double) earned;

            JPanel right = Theme.clear(new BorderLayout(0, 6));
            Charts.Ring ring = new Charts.Ring(kept, String.valueOf(me.getCredits()),
                    "credits", Theme.AMBER, 132);
            right.add(ring, BorderLayout.CENTER);
            right.add(Theme.label("Earned " + earned + "  \u00b7  Spent " + spent, 11,
                    Font.PLAIN, new Color(0xB9B6E4)), BorderLayout.SOUTH);
            ((javax.swing.JLabel) right.getComponent(1))
                    .setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            card.add(right, BorderLayout.EAST);
        }
        return card;
    }

    private String greeting() {
        int hour = LocalDateTime.now().getHour();
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }

    private String firstName(User me) {
        return me.getName().split("\\s+")[0];
    }

    private String summaryLine(User me, int pending, int upcoming) {
        if (me.isAdmin()) {
            return db.users().size() - 1 + " students, " + sessions.completed().size()
                    + " sessions completed, " + credits.inCirculation() + " credits in circulation.";
        }
        if (pending > 0) {
            return pending == 1
                    ? "One student is waiting for you to accept a session."
                    : pending + " students are waiting for you to accept a session.";
        }
        if (upcoming > 0) {
            return upcoming == 1 ? "You have one session coming up."
                    : "You have " + upcoming + " sessions coming up.";
        }
        int taught = sessions.countTaught(me.getId());
        return taught == 0
                ? "Nothing booked yet. Teaching one session pays for your next one."
                : "Nothing booked right now. Your credits are ready when you are.";
    }

    // ------------------------------------------------------------------- stats

    private JPanel stats(User me) {
        JPanel row = Theme.clear(new GridLayout(1, 4, 16, 0));
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 96));
        row.setPreferredSize(new Dimension(900, 96));

        double rating = feedback.teacherRating(me.getId());
        int rank = board.rankOf(me.getId());

        if (me.isAdmin()) {
            row.add(Theme.statCard(String.valueOf(db.users().size() - 1), "Students enrolled",
                    "user", Theme.VIOLET));
            row.add(Theme.statCard(String.valueOf(db.skills().size()), "Skills on offer",
                    "grid", Theme.PINK));
            row.add(Theme.statCard(String.valueOf(sessions.completed().size()), "Sessions completed",
                    "calendar", Theme.TEAL));
            row.add(Theme.statCard(String.valueOf(credits.inCirculation()), "Credits in circulation",
                    "coin", Theme.AMBER));
        } else {
            row.add(Theme.statCard(String.valueOf(sessions.countTaught(me.getId())),
                    "Sessions taught", "book", Theme.VIOLET));
            row.add(Theme.statCard(String.valueOf(sessions.countLearned(me.getId())),
                    "Sessions attended", "grid", Theme.TEAL));
            row.add(Theme.statCard(rating == 0 ? "\u2013" : Util.round(rating),
                    rating == 0 ? "No ratings yet" : "Teaching rating", "star", Theme.AMBER));
            row.add(Theme.statCard(rank == 0 ? "\u2013" : "#" + rank,
                    "Campus rank", "trophy", Theme.PINK));
        }
        return row;
    }

    // ------------------------------------------------------------ middle strip

    private JPanel middle(User me) {
        JPanel row = Theme.clear(new GridLayout(1, 2, 16, 0));
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 296));
        row.setPreferredSize(new Dimension(900, 296));
        row.add(nextUp(me));
        row.add(trending());
        return row;
    }

    private Theme.Card nextUp(User me) {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 12));
        JPanel head = Theme.clear(new BorderLayout());
        head.add(Theme.h2("Next up"), BorderLayout.WEST);
        Theme.Btn all = Theme.quiet("See all");
        all.addActionListener(e -> frame.show("sessions"));
        head.add(all, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel list = Theme.clear(null);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        List<LearningSession> upcoming = me.isAdmin()
                ? sessions.upcomingFor(-1) : sessions.upcomingFor(me.getId());
        if (upcoming.isEmpty()) {
            JPanel empty = Theme.clear(new BorderLayout(0, 8));
            empty.setBorder(new EmptyBorder(26, 0, 0, 0));
            empty.add(Theme.label("Nothing on the calendar.", 14, Font.BOLD, Theme.MUTED),
                    BorderLayout.NORTH);
            empty.add(Theme.label("Browse skills and send a request to get started.",
                    12, Font.PLAIN, Theme.FAINT), BorderLayout.CENTER);
            Theme.Btn go = Theme.ghost("Browse skills");
            go.addActionListener(e -> frame.show("browse"));
            JPanel holder = Theme.clear(new FlowLayout(FlowLayout.LEFT, 0, 8));
            holder.add(go);
            empty.add(holder, BorderLayout.SOUTH);
            list.add(empty);
        } else {
            int shown = 0;
            for (LearningSession s : upcoming) {
                if (shown++ >= 4) break;
                list.add(sessionRow(s, me));
                list.add(MainFrame.vspace(8));
            }
        }
        card.add(Theme.scroll(list), BorderLayout.CENTER);
        return card;
    }

    /** One upcoming session: a date block, the skill, and who you are meeting. */
    private JPanel sessionRow(LearningSession s, User me) {
        boolean teaching = s.getTeacherId() == me.getId();
        String other = db.userName(teaching ? s.getLearnerId() : s.getTeacherId());

        JPanel row = Theme.clear(new BorderLayout(12, 0));
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 58));
        row.setPreferredSize(new Dimension(300, 58));
        row.add(new DateBlock(s.getScheduledFor()), BorderLayout.WEST);

        JPanel middle = Theme.clear(new BorderLayout(0, 3));
        middle.setBorder(new EmptyBorder(9, 0, 0, 0));
        middle.add(Theme.label(db.skillTitle(s.getSkillId()), 13, Font.BOLD, Theme.TEXT),
                BorderLayout.NORTH);
        middle.add(Theme.label((teaching ? "Teaching " : "Learning from ") + other
                + "  \u00b7  " + s.getMode(), 11, Font.PLAIN, Theme.MUTED), BorderLayout.CENTER);
        row.add(middle, BorderLayout.CENTER);

        JPanel chipHolder = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 0, 16));
        chipHolder.add(new Theme.Chip(teaching ? "Teaching" : "Learning",
                teaching ? Theme.VIOLET : Theme.TEAL, false));
        row.add(chipHolder, BorderLayout.EAST);
        return row;
    }

    private Theme.Card trending() {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 14));
        JPanel head = Theme.clear(new BorderLayout(0, 3));
        head.add(Theme.h2("What campus is learning"), BorderLayout.NORTH);
        head.add(Theme.label("Completed sessions, all time", 11, Font.PLAIN, Theme.FAINT),
                BorderLayout.SOUTH);
        card.add(head, BorderLayout.NORTH);

        Charts.Bars bars = new Charts.Bars(reports.popularSkills(6));
        bars.labelWidth(160).emptyMessage("No sessions have been completed yet.");
        bars.tones(title -> {
            model.Skill found = null;
            for (model.Skill s : db.skills()) if (s.getTitle().equals(title)) found = s;
            return found == null ? Theme.VIOLET : Theme.categoryColor(found.getCategory());
        });
        card.add(bars, BorderLayout.CENTER);
        return card;
    }

    // ------------------------------------------------------------------ badges

    private JPanel badges(User me) {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 14));
        card.setMaximumSize(new Dimension(Short.MAX_VALUE, 190));
        card.setPreferredSize(new Dimension(900, 190));

        List<Badge> all = board.badgesFor(me);
        int earned = board.badgeCount(me);

        JPanel head = Theme.clear(new BorderLayout());
        head.add(Theme.h2("Achievements"), BorderLayout.WEST);
        head.add(Theme.label(earned + " of " + all.size() + " unlocked", 12, Font.PLAIN, Theme.MUTED),
                BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel strip = Theme.clear(new GridLayout(1, all.size(), 6, 0));
        for (Badge b : all) strip.add(new BadgeView(b, true));
        card.add(strip, BorderLayout.CENTER);
        return card;
    }

    /** The day and month block on a scheduled session. */
    private static class DateBlock extends JPanel {
        private final String day;
        private final String month;
        private final String time;

        DateBlock(String stamp) {
            LocalDateTime when = Util.parse(stamp);
            day = String.format("%02d", when.getDayOfMonth());
            month = when.format(java.time.format.DateTimeFormatter.ofPattern("MMM"));
            time = when.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            setOpaque(false);
            setPreferredSize(new Dimension(54, 56));
            setToolTipText(Util.pretty(stamp));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.prepare(g);
            g2.setColor(Theme.PANEL_HI);
            g2.fill(new RoundRectangle2D.Double(0, 2, 52, 52, 13, 13));
            g2.setColor(Theme.alpha(Theme.VIOLET, 110));
            g2.draw(new RoundRectangle2D.Double(0.5, 2.5, 51, 51, 13, 13));

            g2.setFont(Theme.font(Font.BOLD, 18));
            g2.setColor(Theme.TEXT);
            int w = g2.getFontMetrics().stringWidth(day);
            g2.drawString(day, (52 - w) / 2, 27);

            g2.setFont(Theme.font(Font.PLAIN, 10));
            g2.setColor(Theme.MUTED);
            int mw = g2.getFontMetrics().stringWidth(month + " " + time);
            g2.drawString(month + " " + time, (52 - mw) / 2, 42);
            g2.dispose();
        }
    }
}
