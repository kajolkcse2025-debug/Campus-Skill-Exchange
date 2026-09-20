package ui;

import model.Badge;
import model.Feedback;
import model.User;
import service.AuthService;
import service.AuthService.RuleException;
import service.CreditService;
import service.FeedbackService;
import service.LeaderboardService;
import service.SessionService;
import util.Util;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

/** Your public profile on the left, the things you can change on the right. */
public class ProfilePanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final AuthService auth = new AuthService();
    private final SessionService sessions = new SessionService();
    private final FeedbackService feedback = new FeedbackService();
    private final CreditService credits = new CreditService();
    private final LeaderboardService board = new LeaderboardService();

    private final JPanel left = Theme.clear(new BorderLayout());
    private final JPanel right = Theme.clear(null);

    public ProfilePanel(MainFrame frame) {
        super(new BorderLayout(16, 0));
        this.frame = frame;
        setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        left.setPreferredSize(new Dimension(420, 100));
        add(left, BorderLayout.WEST);
        add(Theme.vscroll(right), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        left.removeAll();
        left.add(Theme.vscroll(profileCard()), BorderLayout.CENTER);
        left.revalidate();
        left.repaint();

        right.removeAll();
        right.add(editCard());
        right.add(MainFrame.vspace(16));
        right.add(passwordCard());
        right.add(MainFrame.vspace(16));
        right.add(reviewsCard());
        right.revalidate();
        right.repaint();
    }

    private Theme.Card profileCard() {
        User me = frame.user();
        Theme.Card card = new Theme.Card(new BorderLayout(0, 16));
        card.pad(24, 22, 22, 22);

        JPanel head = Theme.clear(new BorderLayout(14, 0));
        head.add(new Theme.Avatar(me.initials(), 64, Theme.Avatar.toneFor(me.getName())),
                BorderLayout.WEST);
        JPanel who = Theme.clear(new BorderLayout(0, 5));
        who.setBorder(new EmptyBorder(6, 0, 0, 0));
        who.add(Theme.label(me.getName(), 19, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        who.add(Theme.label(me.getDepartment() + "  \u00b7  " + me.getYear(), 12,
                Font.PLAIN, Theme.MUTED), BorderLayout.CENTER);
        who.add(Theme.label(me.getEmail(), 11, Font.PLAIN, Theme.FAINT), BorderLayout.SOUTH);
        head.add(who, BorderLayout.CENTER);
        card.add(head, BorderLayout.NORTH);

        JPanel middle = Theme.clear(null);
        middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));

        JTextArea bio = Theme.wrapped(me.getBio().isEmpty()
                ? "No bio yet. Add a line about what you like teaching."
                : me.getBio(), 350, me.getBio().isEmpty() ? Theme.FAINT : Theme.MUTED, 13);
        bio.setAlignmentX(0f);
        middle.add(bio);
        middle.add(MainFrame.vspace(16));

        double rating = feedback.teacherRating(me.getId());
        JPanel ratingRow = Theme.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        ratingRow.setAlignmentX(0f);
        ratingRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 26));
        ratingRow.add(new Theme.Stars(rating, 15));
        ratingRow.add(Theme.label(rating == 0 ? "No ratings yet"
                : Util.round(rating) + " from " + feedback.reviewCount(me.getId()) + " reviews",
                12, Font.PLAIN, Theme.MUTED));
        middle.add(ratingRow);
        middle.add(MainFrame.vspace(16));

        JPanel stats = Theme.clear(new GridLayout(2, 2, 10, 10));
        stats.setAlignmentX(0f);
        stats.setMaximumSize(new Dimension(Short.MAX_VALUE, 180));
        stats.add(Theme.statCard(String.valueOf(sessions.countTaught(me.getId())),
                "Sessions taught", "book", Theme.VIOLET));
        stats.add(Theme.statCard(String.valueOf(sessions.countLearned(me.getId())),
                "Sessions attended", "grid", Theme.TEAL));
        stats.add(Theme.statCard(String.valueOf(credits.earnedBy(me.getId())),
                "Credits earned", "coin", Theme.AMBER));
        stats.add(Theme.statCard("#" + Math.max(1, board.rankOf(me.getId())),
                "Campus rank", "trophy", Theme.PINK));
        middle.add(stats);
        middle.add(MainFrame.vspace(16));

        List<Badge> badges = board.badgesFor(me);
        JPanel earned = Theme.clear(new GridLayout(0, 4, 4, 8));
        earned.setAlignmentX(0f);
        int shown = 0;
        for (Badge b : badges) {
            if (!b.isEarned()) continue;
            earned.add(new BadgeView(b, false));
            shown++;
        }
        if (shown > 0) {
            middle.add(Theme.label("Badges", 12, Font.BOLD, Theme.MUTED));
            middle.add(MainFrame.vspace(8));
            middle.add(earned);
        }
        card.add(middle, BorderLayout.CENTER);

        card.add(Theme.label("Member since " + Util.prettyDate(me.getJoinedOn()), 11,
                Font.PLAIN, Theme.FAINT), BorderLayout.SOUTH);
        return card;
    }

    private Theme.Card editCard() {
        User me = frame.user();
        Theme.Card card = new Theme.Card(new BorderLayout(0, 14));
        card.pad(20, 22, 18, 22);
        card.setMaximumSize(new Dimension(Short.MAX_VALUE, 330));

        JPanel head = Theme.clear(new BorderLayout(0, 4));
        head.add(Theme.h2("Edit your details"), BorderLayout.NORTH);
        head.add(Theme.label("This is what learners see before they book you.", 12,
                Font.PLAIN, Theme.MUTED), BorderLayout.SOUTH);
        card.add(head, BorderLayout.NORTH);

        JPanel form = Theme.clear(null);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        Theme.HintField name = new Theme.HintField("Full name", 20);
        name.setText(me.getName());
        JComboBox<String> department = Theme.combo(new String[]{
            "Computer Science", "Information Tech", "Electronics", "Electrical", "Mechanical",
            "Civil", "Architecture", "Biotechnology", "Commerce", "Humanities", "Student Affairs"});
        department.setSelectedItem(me.getDepartment());
        JComboBox<String> year = Theme.combo(new String[]{
            "1st Year", "2nd Year", "3rd Year", "4th Year", "Postgraduate", "Staff"});
        year.setSelectedItem(me.getYear());
        JTextArea bio = new JTextArea(me.getBio());

        form.add(labelled("Full name", name));
        JPanel pair = Theme.clear(new GridLayout(1, 2, 12, 0));
        pair.setAlignmentX(0f);
        pair.setMaximumSize(new Dimension(Short.MAX_VALUE, 66));
        pair.add(labelled("Department", department));
        pair.add(labelled("Year", year));
        form.add(pair);
        form.add(labelled("Short bio", Theme.area(bio, 3)));
        card.add(form, BorderLayout.CENTER);

        Theme.Btn save = Theme.solid("Save changes");
        save.addActionListener(e -> {
            try {
                auth.updateProfile(me, name.getText(), (String) department.getSelectedItem(),
                        (String) year.getSelectedItem(), bio.getText());
                refresh();
                Dialogs.success(this, "Profile updated", "Your details are live on campus.");
            } catch (RuleException ex) {
                Dialogs.error(this, "Could not save that", ex.getMessage());
            }
        });
        JPanel actions = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.add(save);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private Theme.Card passwordCard() {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 14));
        card.pad(20, 22, 18, 22);
        card.setMaximumSize(new Dimension(Short.MAX_VALUE, 250));
        card.add(Theme.h2("Change your password"), BorderLayout.NORTH);

        Theme.HintPassword current = new Theme.HintPassword("Current password", 18);
        Theme.HintPassword next = new Theme.HintPassword("New password", 18);
        Theme.HintPassword confirm = new Theme.HintPassword("Repeat new password", 18);

        JPanel form = Theme.clear(new GridLayout(1, 3, 12, 0));
        form.add(labelled("Current", current));
        form.add(labelled("New", next));
        form.add(labelled("Repeat", confirm));
        card.add(form, BorderLayout.CENTER);

        Theme.Btn save = Theme.solid("Update password");
        save.addActionListener(e -> {
            try {
                auth.changePassword(frame.user(), new String(current.getPassword()),
                        new String(next.getPassword()), new String(confirm.getPassword()));
                current.setText("");
                next.setText("");
                confirm.setText("");
                Dialogs.success(this, "Password updated", "Use the new one next time you sign in.");
            } catch (RuleException ex) {
                Dialogs.error(this, "Could not update that", ex.getMessage());
            }
        });
        JPanel actions = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.add(save);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private Theme.Card reviewsCard() {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 14));
        card.pad(20, 22, 18, 22);
        card.add(Theme.h2("What people said about you"), BorderLayout.NORTH);

        List<Feedback> received = feedback.receivedBy(frame.user().getId());
        JPanel list = Theme.clear(null);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        if (received.isEmpty()) {
            list.add(Theme.label("No reviews yet. They appear after your first completed session.",
                    12, Font.PLAIN, Theme.FAINT));
        } else {
            int shown = 0;
            for (Feedback f : received) {
                if (shown++ >= 6) break;
                list.add(reviewRow(f));
                list.add(MainFrame.vspace(12));
            }
        }
        card.add(list, BorderLayout.CENTER);
        card.setMaximumSize(new Dimension(Short.MAX_VALUE, 120 + received.size() * 64));
        return card;
    }

    private JPanel reviewRow(Feedback f) {
        data.Database db = data.Database.get();
        String from = db.userName(f.getFromUserId());

        JPanel row = Theme.clear(new BorderLayout(11, 0));
        row.setAlignmentX(0f);
        row.add(new Theme.Avatar(from.substring(0, 1), 32, Theme.Avatar.toneFor(from)),
                BorderLayout.WEST);

        JPanel body = Theme.clear(new BorderLayout(0, 4));
        JPanel top = Theme.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.add(Theme.label(from, 12, Font.BOLD, Theme.TEXT));
        top.add(new Theme.Stars(f.getRating(), 12));
        top.add(Theme.label(Util.pretty(f.getStamp()), 11, Font.PLAIN, Theme.FAINT));
        body.add(top, BorderLayout.NORTH);
        body.add(Theme.wrapped(f.getComment(), 520, Theme.MUTED, 12), BorderLayout.CENTER);
        row.add(body, BorderLayout.CENTER);
        return row;
    }

    private JPanel labelled(String caption, javax.swing.JComponent input) {
        JPanel row = Theme.clear(new BorderLayout(0, 6));
        row.setAlignmentX(0f);
        row.setBorder(new EmptyBorder(0, 0, 12, 0));
        row.add(Theme.label(caption, 12, Font.BOLD, Theme.MUTED), BorderLayout.NORTH);
        if (input instanceof JComboBox || input instanceof javax.swing.text.JTextComponent) {
            input.setPreferredSize(new Dimension(180, 38));
        }
        row.add(input, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE,
                input.getPreferredSize().height + 30));
        return row;
    }
}
