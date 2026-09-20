package ui;

import data.Database;
import model.LearningSession;
import model.Skill;
import model.User;
import service.AuthService.RuleException;
import service.SessionService;
import service.SkillService;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.util.List;

/** Search the campus catalogue and ask someone to teach you. */
public class BrowsePanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final Database db = Database.get();
    private final SkillService skills = new SkillService();
    private final SessionService sessions = new SessionService();

    private final Theme.HintField search = new Theme.HintField("Search a skill, a topic or a name", 26);
    private final JComboBox<String> category;
    private final JComboBox<String> level;
    private final JLabel count = Theme.label("", 12, Font.PLAIN, Theme.MUTED);
    private final JPanel results = Theme.clear(new GridLayout(0, 3, 16, 16));

    public BrowsePanel(MainFrame frame) {
        super(new BorderLayout(0, 16));
        this.frame = frame;
        setOpaque(false);

        String[] categories = new String[Skill.CATEGORIES.length + 1];
        categories[0] = "All";
        System.arraycopy(Skill.CATEGORIES, 0, categories, 1, Skill.CATEGORIES.length);
        category = Theme.combo(categories);

        String[] levels = {"All", "Beginner", "Intermediate", "Advanced"};
        level = Theme.combo(levels);

        add(filterBar(), BorderLayout.NORTH);

        JPanel holder = Theme.clear(new BorderLayout());
        holder.add(results, BorderLayout.NORTH);
        add(Theme.vscroll(holder), BorderLayout.CENTER);
    }

    private Theme.Card filterBar() {
        Theme.Card bar = new Theme.Card(new BorderLayout(14, 0));
        bar.pad(14, 16, 14, 16);
        bar.setMaximumSize(new Dimension(Short.MAX_VALUE, 68));

        search.setPreferredSize(new Dimension(320, 40));
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { rebuild(); }
            public void removeUpdate(DocumentEvent e) { rebuild(); }
            public void changedUpdate(DocumentEvent e) { rebuild(); }
        });
        bar.add(search, BorderLayout.WEST);

        JPanel right = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        category.setPreferredSize(new Dimension(160, 38));
        level.setPreferredSize(new Dimension(140, 38));
        category.addActionListener(e -> rebuild());
        level.addActionListener(e -> rebuild());

        Theme.Btn clear = Theme.quiet("Clear");
        clear.addActionListener(e -> {
            search.setText("");
            category.setSelectedIndex(0);
            level.setSelectedIndex(0);
            rebuild();
        });

        right.add(count);
        right.add(category);
        right.add(level);
        right.add(clear);
        bar.add(right, BorderLayout.CENTER);
        return bar;
    }

    @Override
    public void refresh() {
        rebuild();
    }

    private void rebuild() {
        User me = frame.user();
        results.removeAll();

        List<Skill> found = skills.search(search.getText(), (String) category.getSelectedItem(),
                (String) level.getSelectedItem(), me.getId());

        count.setText(found.size() == 1 ? "1 skill" : found.size() + " skills");

        if (found.isEmpty()) {
            results.setLayout(new BorderLayout());
            Theme.Card empty = new Theme.Card(new BorderLayout(0, 8));
            empty.pad(40, 28, 40, 28);
            empty.add(Theme.label("Nothing matches that search.", 16, Font.BOLD, Theme.TEXT),
                    BorderLayout.NORTH);
            empty.add(Theme.label("Try a broader word, or clear the filters and browse everything.",
                    13, Font.PLAIN, Theme.MUTED), BorderLayout.CENTER);
            results.add(empty, BorderLayout.CENTER);
        } else {
            results.setLayout(new GridLayout(0, 3, 16, 16));
            for (Skill s : found) {
                boolean affordable = me.isAdmin() || me.getCredits() >= s.getCreditCost();
                results.add(new SkillCard(s, me.isAdmin() ? null : "Request session",
                        e -> requestSession(s), affordable));
            }
            int filler = (3 - found.size() % 3) % 3;
            for (int i = 0; i < filler; i++) results.add(Theme.clear(new BorderLayout()));
        }
        results.revalidate();
        results.repaint();
    }

    /** Opens the booking form, then records the request. */
    private void requestSession(Skill skill) {
        User me = frame.user();
        User teacher = db.userById(skill.getOwnerId());

        Dialogs.Form form = new Dialogs.Form(this, "Request a session",
                skill.getTitle() + " with " + (teacher == null ? "" : teacher.getName())
                + ". This costs " + skill.getCreditCost() + " credits, taken when the session is marked complete.");
        form.submitLabel("Send request");

        JComboBox<String> mode = form.addCombo("How would you like to meet?",
                LearningSession.MODES, LearningSession.MODES[0]);
        JTextField when = form.addField("When suits you?", "e.g. Saturday afternoon",
                LocalDateTime.now().plusDays(3).withHour(16).withMinute(0)
                        .format(util.Util.TS));
        JTextArea note = form.addArea("Anything your teacher should know?", "", 3);
        form.addNote("You have " + me.getCredits() + " credits. Your teacher sets the final "
                + "time when they accept.");

        form.validate(() -> {
            if (when.getText().trim().isEmpty()) return "Suggest a time that works for you.";
            return null;
        });

        form.onSubmit(() -> {
            try {
                sessions.request(me, skill, note.getText(), (String) mode.getSelectedItem(),
                        when.getText());
                Dialogs.success(this, "Request sent",
                        (teacher == null ? "Your teacher" : teacher.getName().split("\\s+")[0])
                        + " will see this under their requests. You will find it under Sessions.");
                frame.refreshChrome();
                rebuild();
            } catch (RuleException ex) {
                Dialogs.error(this, "Could not send that", ex.getMessage());
            }
        });
        form.show();
    }
}
