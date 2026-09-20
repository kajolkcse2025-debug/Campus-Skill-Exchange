package ui;

import data.Database;
import model.Feedback;
import model.LearningSession;
import model.User;
import service.AuthService.RuleException;
import service.FeedbackService;
import service.SessionService;
import util.Util;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Requests waiting on you, sessions you teach and sessions you attend. */
public class SessionsPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final Database db = Database.get();
    private final SessionService sessions = new SessionService();
    private final FeedbackService feedback = new FeedbackService();

    private final CardLayout tabs = new CardLayout();
    private final JPanel pages = Theme.clear(tabs);
    private final JPanel tabStrip = Theme.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));

    private final JPanel requestList = Theme.clear(new GridLayout(0, 2, 16, 16));
    private final SessionTable teaching = new SessionTable(true);
    private final SessionTable learning = new SessionTable(false);

    private String active = "requests";

    public SessionsPanel(MainFrame frame) {
        super(new BorderLayout(0, 16));
        this.frame = frame;
        setOpaque(false);

        JPanel requestsPage = Theme.clear(new BorderLayout());
        JPanel holder = Theme.clear(new BorderLayout());
        holder.add(requestList, BorderLayout.NORTH);
        requestsPage.add(Theme.vscroll(holder), BorderLayout.CENTER);

        pages.add(requestsPage, "requests");
        pages.add(teaching, "teaching");
        pages.add(learning, "learning");

        add(tabStrip, BorderLayout.NORTH);
        add(pages, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        buildTabs();
        buildRequests();
        teaching.load(sessions.teaching(frame.user().getId()));
        learning.load(sessions.learning(frame.user().getId()));
        tabs.show(pages, active);
    }

    private void buildTabs() {
        tabStrip.removeAll();
        int pending = sessions.pendingFor(frame.user().getId()).size();
        tabStrip.add(tab("requests", pending == 0 ? "Requests" : "Requests (" + pending + ")"));
        tabStrip.add(tab("teaching", "Teaching"));
        tabStrip.add(tab("learning", "Learning"));
        tabStrip.revalidate();
        tabStrip.repaint();
    }

    private Theme.Btn tab(String key, String label) {
        boolean selected = key.equals(active);
        Theme.Btn button = selected ? Theme.solid(label) : Theme.quiet(label);
        button.addActionListener(e -> {
            active = key;
            refresh();
        });
        return button;
    }

    // ---------------------------------------------------------------- requests

    private void buildRequests() {
        requestList.removeAll();
        List<LearningSession> pending = sessions.pendingFor(frame.user().getId());

        if (pending.isEmpty()) {
            requestList.setLayout(new BorderLayout());
            Theme.Card empty = new Theme.Card(new BorderLayout(0, 8));
            empty.pad(44, 28, 44, 28);
            empty.add(Theme.label("No one is waiting on you.", 17, Font.BOLD, Theme.TEXT),
                    BorderLayout.NORTH);
            empty.add(Theme.label("Requests from learners will appear here as soon as they arrive.",
                    13, Font.PLAIN, Theme.MUTED), BorderLayout.CENTER);
            requestList.add(empty, BorderLayout.CENTER);
        } else {
            requestList.setLayout(new GridLayout(0, 2, 16, 16));
            for (LearningSession s : pending) requestList.add(requestCard(s));
            if (pending.size() % 2 == 1) requestList.add(Theme.clear(new BorderLayout()));
        }
        requestList.revalidate();
        requestList.repaint();
    }

    private Theme.Card requestCard(LearningSession session) {
        User learner = db.userById(session.getLearnerId());
        String learnerName = learner == null ? "Removed user" : learner.getName();

        Theme.Card card = new Theme.Card(new BorderLayout(0, 12));
        card.pad(18, 18, 16, 18);
        card.setPreferredSize(new Dimension(430, 222));

        JPanel head = Theme.clear(new BorderLayout(11, 0));
        head.add(new Theme.Avatar(learner == null ? "?" : learner.initials(), 40,
                Theme.Avatar.toneFor(learnerName)), BorderLayout.WEST);
        JPanel who = Theme.clear(new BorderLayout(0, 3));
        who.add(Theme.label(learnerName, 14, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        who.add(Theme.label((learner == null ? "" : learner.getDepartment() + ", "
                + learner.getYear()), 11, Font.PLAIN, Theme.FAINT), BorderLayout.SOUTH);
        head.add(who, BorderLayout.CENTER);
        head.add(Theme.label("+" + session.getCreditCost() + " cr", 14, Font.BOLD, Theme.AMBER),
                BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel body = Theme.clear(new BorderLayout(0, 6));
        body.add(Theme.label("wants to learn " + db.skillTitle(session.getSkillId()),
                13, Font.BOLD, Theme.VIOLET_HI), BorderLayout.NORTH);
        String note = session.getNote().isEmpty() ? "No note left." : "\u201c" + session.getNote() + "\u201d";
        body.add(Theme.wrapped(note, 360, Theme.MUTED, 12), BorderLayout.CENTER);
        body.add(Theme.label("Suggested: " + (session.getScheduledFor().isEmpty()
                ? "no preference" : Util.pretty(session.getScheduledFor()))
                + "  \u00b7  " + session.getMode(), 11, Font.PLAIN, Theme.FAINT), BorderLayout.SOUTH);
        card.add(body, BorderLayout.CENTER);

        JPanel actions = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        Theme.Btn decline = Theme.ghost("Decline", Theme.RED);
        decline.addActionListener(e -> decline(session));
        Theme.Btn accept = Theme.solid("Accept and schedule", Theme.TEAL);
        accept.addActionListener(e -> accept(session));
        actions.add(decline);
        actions.add(accept);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private void accept(LearningSession session) {
        Dialogs.Form form = new Dialogs.Form(this, "Schedule the session",
                "Confirm when and where you will meet " + db.userName(session.getLearnerId()) + ".");
        form.submitLabel("Confirm").tone(Theme.TEAL);

        String suggested = session.getScheduledFor().isEmpty()
                ? LocalDateTime.now().plusDays(2).withHour(16).withMinute(0).format(Util.TS)
                : session.getScheduledFor();
        JTextField when = form.addField("Date and time", "yyyy-MM-dd HH:mm", suggested);
        JTextField venue = form.addField("Where", "e.g. Central Library, Room 3",
                session.getVenue());
        form.addNote("They asked for: " + (session.getNote().isEmpty() ? "nothing in particular"
                : session.getNote()));

        form.validate(() -> when.getText().trim().isEmpty() ? "Set a date and time." : null);
        form.onSubmit(() -> {
            try {
                sessions.accept(session, when.getText(), venue.getText());
                refresh();
            } catch (RuleException ex) {
                Dialogs.error(this, "Could not schedule that", ex.getMessage());
            }
        });
        form.show();
    }

    private void decline(LearningSession session) {
        Dialogs.Form form = new Dialogs.Form(this, "Decline this request",
                "A short reason helps them ask someone else, or ask you again later.");
        form.submitLabel("Decline").tone(Theme.RED);
        JTextArea reason = form.addArea("Reason", "", 3);
        form.onSubmit(() -> {
            try {
                sessions.reject(session, reason.getText());
                refresh();
            } catch (RuleException ex) {
                Dialogs.error(this, "Could not decline that", ex.getMessage());
            }
        });
        form.show();
    }

    // ------------------------------------------------------------------ tables

    /** A table of sessions with the actions that apply to the selected row. */
    private class SessionTable extends JPanel {

        private final boolean asTeacher;
        private final DefaultTableModel model;
        private final JTable table;
        private final List<LearningSession> rows = new ArrayList<>();

        private final Theme.Btn complete = Theme.solid("Mark complete", Theme.TEAL);
        private final Theme.Btn cancel = Theme.ghost("Cancel session", Theme.RED);
        private final Theme.Btn review = Theme.solid("Leave a review");
        private final javax.swing.JLabel hint = Theme.label("", 12, Font.PLAIN, Theme.MUTED);

        SessionTable(boolean asTeacher) {
            super(new BorderLayout(0, 12));
            this.asTeacher = asTeacher;
            setOpaque(false);

            model = new DefaultTableModel(new Object[]{
                    "Skill", asTeacher ? "Learner" : "Teacher", "Status", "When", "Credits"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            table = new JTable(model);
            Theme.styleTable(table);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getSelectionModel().addListSelectionListener(e -> updateActions());

            Theme.Card card = new Theme.Card(new BorderLayout(0, 10));
            card.pad(6, 6, 6, 6);
            card.add(Theme.scroll(table), BorderLayout.CENTER);
            add(card, BorderLayout.CENTER);

            JPanel bar = Theme.clear(new BorderLayout(12, 0));
            bar.setBorder(new EmptyBorder(0, 4, 0, 4));
            bar.add(hint, BorderLayout.CENTER);

            JPanel buttons = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            if (asTeacher) buttons.add(complete);
            buttons.add(cancel);
            buttons.add(review);
            bar.add(buttons, BorderLayout.EAST);
            add(bar, BorderLayout.SOUTH);

            complete.addActionListener(e -> completeSelected());
            cancel.addActionListener(e -> cancelSelected());
            review.addActionListener(e -> reviewSelected());
        }

        void load(List<LearningSession> list) {
            rows.clear();
            rows.addAll(list);
            model.setRowCount(0);
            for (LearningSession s : list) {
                String when = LearningSession.COMPLETED.equals(s.getStatus())
                        ? Util.pretty(s.getCompletedOn())
                        : (s.getScheduledFor().isEmpty() ? "To be confirmed"
                                                         : Util.pretty(s.getScheduledFor()));
                model.addRow(new Object[]{
                        db.skillTitle(s.getSkillId()),
                        db.userName(asTeacher ? s.getLearnerId() : s.getTeacherId()),
                        s.getStatus(),
                        when,
                        (asTeacher ? "+" : "\u2212") + s.getCreditCost()});
            }
            updateActions();
        }

        private LearningSession selected() {
            int view = table.getSelectedRow();
            if (view < 0) return null;
            return rows.get(table.convertRowIndexToModel(view));
        }

        private void updateActions() {
            LearningSession s = selected();
            if (s == null) {
                complete.setEnabled(false);
                cancel.setEnabled(false);
                review.setEnabled(false);
                hint.setText(rows.isEmpty()
                        ? (asTeacher ? "Nothing booked with you yet."
                                     : "You have not booked a session yet.")
                        : "Select a session to act on it.");
                return;
            }
            boolean accepted = LearningSession.ACCEPTED.equals(s.getStatus());
            boolean completed = LearningSession.COMPLETED.equals(s.getStatus());
            boolean reviewed = feedback.alreadyReviewed(s.getId(), frame.user().getId());

            complete.setEnabled(accepted);
            cancel.setEnabled(s.isOpen());
            review.setEnabled(completed && !reviewed);

            if (completed && reviewed) hint.setText("You have already reviewed this session.");
            else if (completed) hint.setText("Leave a review so others know what to expect.");
            else if (accepted) hint.setText(asTeacher
                    ? "Credits move when you mark this complete."
                    : "Your teacher marks this complete once you have met.");
            else hint.setText("Waiting for a reply.");
        }

        private void completeSelected() {
            LearningSession s = selected();
            if (s == null) return;
            if (!Dialogs.confirm(this, "Mark this session complete?",
                    db.userName(s.getLearnerId()) + " pays " + s.getCreditCost()
                    + " credits and you receive " + (s.getCreditCost()
                    + service.CreditRules.TEACH_BONUS) + " with the teaching bonus.",
                    "Mark complete", Theme.TEAL)) return;
            try {
                sessions.complete(s);
                frame.refreshChrome();
                refresh();
                Dialogs.success(this, "Nicely done",
                        "You earned " + (s.getCreditCost() + service.CreditRules.TEACH_BONUS)
                        + " credits. Ask your learner to leave a review.");
            } catch (RuleException ex) {
                Dialogs.error(this, "Could not complete that", ex.getMessage());
            }
        }

        private void cancelSelected() {
            LearningSession s = selected();
            if (s == null) return;
            if (!Dialogs.confirm(this, "Cancel this session?",
                    "No credits move. The other student will see it as cancelled.",
                    "Cancel session", Theme.RED)) return;
            try {
                sessions.cancel(s);
                refresh();
            } catch (RuleException ex) {
                Dialogs.error(this, "Could not cancel that", ex.getMessage());
            }
        }

        private void reviewSelected() {
            LearningSession s = selected();
            if (s == null) return;
            String other = db.userName(asTeacher ? s.getLearnerId() : s.getTeacherId());

            Dialogs.Form form = new Dialogs.Form(this, "Review this session",
                    asTeacher ? "How was " + other + " to teach?"
                              : "How was " + other + " as a teacher?");
            form.submitLabel("Post review");
            Dialogs.StarPicker stars = form.addStars("Rating", 5);
            JTextArea comment = form.addArea("What should others know?", "", 4);
            if (!asTeacher) {
                form.addNote("Five stars sends your teacher a "
                        + service.CreditRules.FIVE_STAR_BONUS + " credit bonus.");
            }
            form.validate(() -> comment.getText().trim().length() < 5
                    ? "Add a line or two so the review is useful." : null);
            form.onSubmit(() -> {
                try {
                    Feedback f = feedback.submit(s, frame.user(), stars.getRating(), comment.getText());
                    refresh();
                    frame.refreshChrome();
                    Dialogs.success(this, "Review posted",
                            f.getRating() == 5 && !asTeacher
                                ? other + " received a bonus for your five stars."
                                : "Thanks, this helps the next student choose.");
                } catch (RuleException ex) {
                    Dialogs.error(this, "Could not post that", ex.getMessage());
                }
            });
            form.show();
        }
    }
}
