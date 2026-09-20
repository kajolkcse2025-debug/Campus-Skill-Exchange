package ui;

import data.Database;
import model.LearningSession;
import model.Skill;
import model.User;
import service.CreditService;
import service.FeedbackService;
import service.ReportService;
import service.SessionService;
import service.SkillService;
import util.Util;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * The administrator console: campus-wide numbers on top, then three managed
 * tables (students, skills, sessions) behind a small tab strip.
 */
public class AdminPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final Database db = Database.get();
    private final CreditService credits = new CreditService();
    private final SkillService skills = new SkillService();
    private final SessionService sessions = new SessionService();
    private final FeedbackService feedback = new FeedbackService();
    private final ReportService reports = new ReportService();

    private final JPanel stats = Theme.clear(new GridLayout(1, 4, 14, 0));
    private final CardLayout pages = new CardLayout();
    private final JPanel body = new JPanel(pages);
    private final List<Theme.Btn> tabs = new ArrayList<>();

    private final List<User> studentRows = new ArrayList<>();
    private final List<Skill> skillRows = new ArrayList<>();

    private final DefaultTableModel studentModel = readOnly(
            "Student", "Email", "Department", "Credits", "Taught", "Learned", "Rating", "Status");
    private final DefaultTableModel skillModel = readOnly(
            "Skill", "Owner", "Category", "Level", "Cost", "Sessions", "Status");
    private final DefaultTableModel sessionModel = readOnly(
            "Skill", "Teacher", "Learner", "Status", "Scheduled", "Cost");

    private JTable studentTable;
    private JTable skillTable;

    public AdminPanel(MainFrame frame) {
        super(new BorderLayout(0, 16));
        this.frame = frame;
        setOpaque(false);

        stats.setPreferredSize(new Dimension(900, 92));
        add(stats, BorderLayout.NORTH);

        JPanel centre = Theme.clear(new BorderLayout(0, 12));
        centre.add(tabStrip(), BorderLayout.NORTH);
        body.setOpaque(false);
        body.add(studentsPage(), "students");
        body.add(skillsPage(), "skills");
        body.add(sessionsPage(), "sessions");
        centre.add(body, BorderLayout.CENTER);
        add(centre, BorderLayout.CENTER);

        select(0);
    }

    // ------------------------------------------------------------------- tabs

    private JPanel tabStrip() {
        JPanel strip = Theme.clear(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        String[] names = {"Students", "Skills", "Sessions"};
        String[] keys = {"students", "skills", "sessions"};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            Theme.Btn tab = Theme.quiet(names[i]).onClick(() -> {
                select(index);
                pages.show(body, keys[index]);
            });
            tabs.add(tab);
            strip.add(tab);
        }
        return strip;
    }

    private void select(int index) {
        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).setTone(i == index ? Theme.VIOLET_HI : Theme.MUTED);
            tabs.get(i).setFont(Theme.font(i == index ? Font.BOLD : Font.PLAIN, 13));
        }
    }

    // ------------------------------------------------------------------ pages

    private JPanel studentsPage() {
        studentTable = new JTable(studentModel);
        Theme.styleTable(studentTable);
        studentTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        studentTable.getColumnModel().getColumn(1).setMinWidth(190);
        for (int i = 3; i < 8; i++) studentTable.getColumnModel().getColumn(i).setMaxWidth(110);

        JPanel actions = Theme.clear(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        actions.add(Theme.ghost("Adjust credits", Theme.AMBER).onClick(this::adjustCredits));
        actions.add(Theme.ghost("Suspend / restore", Theme.PINK).onClick(this::toggleStudent));
        actions.add(Theme.quiet("View report").onClick(this::studentReport));

        return tableCard("Students on the platform",
                "Select a row, then choose an action.", studentTable, actions);
    }

    private JPanel skillsPage() {
        skillTable = new JTable(skillModel);
        Theme.styleTable(skillTable);

        JPanel actions = Theme.clear(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        actions.add(Theme.ghost("Retire / restore", Theme.TEAL).onClick(this::toggleSkill));

        return tableCard("Every skill on offer",
                "Retired skills stay in the records but stop appearing in Browse.",
                skillTable, actions);
    }

    private JPanel sessionsPage() {
        JTable table = new JTable(sessionModel);
        Theme.styleTable(table);
        return tableCard("All learning sessions",
                "A read only log of every request made on campus.", table, null);
    }

    private JPanel tableCard(String title, String caption, JTable table, JComponent actions) {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 12));
        card.pad(18, 18, 14, 18);

        JPanel head = Theme.clear(new BorderLayout(0, 3));
        head.add(Theme.h2(title), BorderLayout.NORTH);
        head.add(Theme.label(caption, 12, Font.PLAIN, Theme.MUTED), BorderLayout.SOUTH);
        card.add(head, BorderLayout.NORTH);
        card.add(Theme.scroll(table), BorderLayout.CENTER);
        if (actions != null) card.add(actions, BorderLayout.SOUTH);

        JPanel wrap = Theme.clear(new BorderLayout());
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    // ---------------------------------------------------------------- actions

    private User selectedStudent() {
        int row = studentTable.getSelectedRow();
        if (row < 0 || row >= studentRows.size()) {
            Dialogs.info(this, "Pick a student", "Select a row in the table first.");
            return null;
        }
        return studentRows.get(row);
    }

    private void adjustCredits() {
        User student = selectedStudent();
        if (student == null) return;

        Dialogs.Form form = new Dialogs.Form(this, "Adjust credits",
                student.getName() + " currently holds " + student.getCredits() + " credits.");
        form.tone(Theme.AMBER).submitLabel("Apply");
        JTextField amount = form.addField("Amount", "e.g. 20 or -10", "");
        JTextField reason = form.addField("Reason", "Why this adjustment?", "Admin adjustment");
        form.addNote("Positive numbers add credits, negative numbers take them away. "
                + "The change is written to the student's credit history.");
        form.validate(() -> {
            int value = Util.toInt(amount.getText().trim(), 0);
            if (value == 0) return "Enter a non zero whole number.";
            if (student.getCredits() + value < 0) return "That would push the balance below zero.";
            if (reason.getText().trim().isEmpty()) return "Give a short reason.";
            return null;
        });
        form.onSubmit(() -> {
            credits.grant(student, Util.toInt(amount.getText().trim(), 0), "ADJUST",
                    reason.getText().trim());
            db.save();
            frame.refreshChrome();
            refresh();
            Dialogs.success(this, "Credits updated",
                    student.getName() + " now holds " + student.getCredits() + " credits.");
        });
        form.show();
    }

    private void toggleStudent() {
        User student = selectedStudent();
        if (student == null) return;
        if (student.isAdmin()) {
            Dialogs.info(this, "Not allowed", "Administrator accounts cannot be suspended.");
            return;
        }
        boolean suspending = student.isActive();
        String title = suspending ? "Suspend this student?" : "Restore this student?";
        String text = suspending
                ? student.getName() + " will not be able to sign in until restored."
                : student.getName() + " will be able to sign in again.";
        if (!Dialogs.confirm(this, title, text, suspending ? "Suspend" : "Restore",
                suspending ? Theme.RED : Theme.GREEN)) return;

        student.setActive(!suspending);
        db.save();
        refresh();
    }

    private void studentReport() {
        User student = selectedStudent();
        if (student == null) return;
        Dialogs.info(this, student.getName(), reports.studentReport(student));
    }

    private void toggleSkill() {
        int row = skillTable.getSelectedRow();
        if (row < 0 || row >= skillRows.size()) {
            Dialogs.info(this, "Pick a skill", "Select a row in the table first.");
            return;
        }
        Skill skill = skillRows.get(row);
        try {
            if (skill.isActive()) {
                skills.retire(skill);
            } else {
                skills.restore(skill);
            }
            refresh();
        } catch (Exception e) {
            Dialogs.error(this, "Could not update", String.valueOf(e.getMessage()));
        }
    }

    // ---------------------------------------------------------------- refresh

    @Override
    public void refresh() {
        stats.removeAll();
        long students = db.users().stream().filter(u -> !u.isAdmin()).count();
        long completed = db.sessions().stream()
                .filter(s -> LearningSession.COMPLETED.equals(s.getStatus())).count();
        stats.add(Theme.statCard(String.valueOf(students), "Students registered", "user", Theme.BLUE));
        stats.add(Theme.statCard(String.valueOf(db.skills().size()), "Skills offered", "grid", Theme.VIOLET));
        stats.add(Theme.statCard(String.valueOf(completed), "Sessions completed", "calendar", Theme.TEAL));
        stats.add(Theme.statCard(String.valueOf(credits.inCirculation()), "Credits in circulation",
                "coin", Theme.AMBER));
        stats.revalidate();
        stats.repaint();

        studentRows.clear();
        studentModel.setRowCount(0);
        for (User u : db.users()) {
            if (u.isAdmin()) continue;
            studentRows.add(u);
            double rating = feedback.teacherRating(u.getId());
            studentModel.addRow(new Object[]{
                    u.getName(), u.getEmail(), u.getDepartment(),
                    String.valueOf(u.getCredits()),
                    String.valueOf(sessions.countTaught(u.getId())),
                    String.valueOf(sessions.countLearned(u.getId())),
                    rating == 0 ? "\u2014" : String.format("%.1f", rating),
                    u.isActive() ? "ACTIVE" : "SUSPENDED"});
        }

        skillRows.clear();
        skillModel.setRowCount(0);
        for (Skill s : db.skills()) {
            skillRows.add(s);
            long count = db.sessions().stream().filter(x -> x.getSkillId() == s.getId()).count();
            skillModel.addRow(new Object[]{
                    s.getTitle(), db.userName(s.getOwnerId()), s.getCategory(), s.getLevel(),
                    String.valueOf(s.getCreditCost()), String.valueOf(count),
                    s.isActive() ? "ACTIVE" : "RETIRED"});
        }

        sessionModel.setRowCount(0);
        List<LearningSession> all = new ArrayList<>(db.sessions());
        all.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        for (LearningSession s : all) {
            sessionModel.addRow(new Object[]{
                    db.skillTitle(s.getSkillId()),
                    db.userName(s.getTeacherId()),
                    db.userName(s.getLearnerId()),
                    s.getStatus(),
                    s.getScheduledFor() == null || s.getScheduledFor().isEmpty()
                            ? "\u2014" : Util.pretty(s.getScheduledFor()),
                    String.valueOf(s.getCreditCost())});
        }
    }

    private static DefaultTableModel readOnly(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    @SuppressWarnings("unused")
    private static final Color UNUSED = Theme.LINE;
}
