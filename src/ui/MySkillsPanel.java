package ui;

import data.Database;
import model.LearningSession;
import model.Skill;
import service.AuthService.RuleException;
import service.CreditRules;
import service.FeedbackService;
import service.SessionService;
import service.SkillService;
import util.Util;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

/** Manage the skills you offer: add, edit, retire and bring back. */
public class MySkillsPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final Database db = Database.get();
    private final SkillService skills = new SkillService();
    private final SessionService sessions = new SessionService();
    private final FeedbackService feedback = new FeedbackService();

    private final JPanel list = Theme.clear(new GridLayout(0, 2, 16, 16));

    public MySkillsPanel(MainFrame frame) {
        super(new BorderLayout(0, 16));
        this.frame = frame;
        setOpaque(false);

        add(header(), BorderLayout.NORTH);
        JPanel holder = Theme.clear(new BorderLayout());
        holder.add(list, BorderLayout.NORTH);
        add(Theme.vscroll(holder), BorderLayout.CENTER);
    }

    private Theme.Card header() {
        Theme.Card bar = new Theme.Card(new BorderLayout(16, 0));
        bar.pad(16, 20, 16, 16);
        bar.setMaximumSize(new Dimension(Short.MAX_VALUE, 78));

        JPanel text = Theme.clear(new BorderLayout(0, 4));
        text.add(Theme.label("Teaching is how you earn", 15, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        text.add(Theme.label("You keep the session price plus a " + CreditRules.TEACH_BONUS
                + " credit bonus, and " + CreditRules.FIVE_STAR_BONUS
                + " more for every five star review.", 12, Font.PLAIN, Theme.MUTED), BorderLayout.SOUTH);
        bar.add(text, BorderLayout.CENTER);

        Theme.Btn add = Theme.solid("Add a skill");
        add.addActionListener(e -> openForm(null));
        JPanel holder = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        holder.add(add);
        bar.add(holder, BorderLayout.EAST);
        return bar;
    }

    @Override
    public void refresh() {
        list.removeAll();
        List<Skill> mine = skills.ownedBy(frame.user().getId());

        if (mine.isEmpty()) {
            list.setLayout(new BorderLayout());
            Theme.Card empty = new Theme.Card(new BorderLayout(0, 10));
            empty.pad(44, 28, 44, 28);
            empty.add(Theme.label("You are not teaching anything yet.", 17, Font.BOLD, Theme.TEXT),
                    BorderLayout.NORTH);
            empty.add(Theme.label("Pick one thing you could explain to a friend in an hour. "
                    + "That is enough to start.", 13, Font.PLAIN, Theme.MUTED), BorderLayout.CENTER);
            Theme.Btn add = Theme.solid("Add your first skill");
            add.addActionListener(e -> openForm(null));
            JPanel holder = Theme.clear(new FlowLayout(FlowLayout.LEFT, 0, 10));
            holder.add(add);
            empty.add(holder, BorderLayout.SOUTH);
            list.add(empty, BorderLayout.CENTER);
        } else {
            list.setLayout(new GridLayout(0, 2, 16, 16));
            for (Skill s : mine) list.add(card(s));
            if (mine.size() % 2 == 1) list.add(Theme.clear(new BorderLayout()));
        }
        list.revalidate();
        list.repaint();
    }

    private Theme.Card card(Skill skill) {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 10));
        card.pad(16, 18, 14, 18);
        card.setPreferredSize(new Dimension(420, 214));
        if (!skill.isActive()) card.fill(new java.awt.Color(0x181A28));

        JPanel head = Theme.clear(new BorderLayout());
        JPanel chips = Theme.clear(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chips.add(new Theme.Chip(skill.getCategory(), Theme.categoryColor(skill.getCategory()), false));
        chips.add(new Theme.Chip(skill.getLevel(), Theme.MUTED, false));
        if (!skill.isActive()) chips.add(new Theme.Chip("Retired", Theme.FAINT, false));
        head.add(chips, BorderLayout.WEST);
        head.add(Theme.label(skill.getCreditCost() + " credits", 13, Font.BOLD, Theme.AMBER),
                BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel middle = Theme.clear(new BorderLayout(0, 6));
        middle.add(Theme.label(skill.getTitle(), 16, Font.BOLD,
                skill.isActive() ? Theme.TEXT : Theme.MUTED), BorderLayout.NORTH);
        middle.add(Theme.wrapped(skill.getDescription(), 360, Theme.MUTED, 12), BorderLayout.CENTER);
        card.add(middle, BorderLayout.CENTER);

        int taught = 0, earnedCredits = 0, waiting = 0;
        for (LearningSession s : db.sessions()) {
            if (s.getSkillId() != skill.getId()) continue;
            if (LearningSession.COMPLETED.equals(s.getStatus())) {
                taught++;
                earnedCredits += s.getCreditCost() + CreditRules.TEACH_BONUS;
            }
            if (LearningSession.PENDING.equals(s.getStatus())) waiting++;
        }
        double rating = feedback.skillRating(skill.getId());

        JPanel foot = Theme.clear(new BorderLayout(10, 0));
        foot.setBorder(new EmptyBorder(6, 0, 0, 0));

        StringBuilder facts = new StringBuilder();
        facts.append(taught).append(taught == 1 ? " session taught" : " sessions taught");
        if (taught > 0) facts.append("  \u00b7  ").append(earnedCredits).append(" credits earned");
        if (rating > 0) facts.append("  \u00b7  ").append(Util.round(rating)).append(" stars");
        if (waiting > 0) facts.append("  \u00b7  ").append(waiting).append(" waiting");
        foot.add(Theme.label(facts.toString(), 11, Font.PLAIN, Theme.FAINT), BorderLayout.CENTER);

        JPanel actions = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        Theme.Btn edit = Theme.quiet("Edit");
        edit.addActionListener(e -> openForm(skill));
        actions.add(edit);

        if (skill.isActive()) {
            Theme.Btn retire = Theme.ghost("Retire", Theme.RED);
            retire.addActionListener(e -> retire(skill));
            actions.add(retire);
        } else {
            Theme.Btn restore = Theme.ghost("Offer again", Theme.TEAL);
            restore.addActionListener(e -> {
                skills.restore(skill);
                refresh();
            });
            actions.add(restore);
        }
        foot.add(actions, BorderLayout.EAST);
        card.add(foot, BorderLayout.SOUTH);
        return card;
    }

    private void retire(Skill skill) {
        if (!Dialogs.confirm(this, "Retire this skill?",
                "It disappears from browse straight away. Your past sessions and reviews stay. "
                + "You can offer it again later.", "Retire", Theme.RED)) return;
        try {
            skills.retire(skill);
            refresh();
        } catch (RuleException ex) {
            Dialogs.error(this, "Not yet", ex.getMessage());
        }
    }

    /** One form for both adding and editing. */
    private void openForm(Skill existing) {
        boolean editing = existing != null;
        Dialogs.Form form = new Dialogs.Form(this,
                editing ? "Edit skill" : "Add a skill",
                editing ? "Changes show up straight away for anyone browsing."
                        : "Describe it the way you would to a classmate who asked for help.");
        form.submitLabel(editing ? "Save changes" : "Add skill");

        JTextField title = form.addField("Skill name", "e.g. Python for Beginners",
                editing ? existing.getTitle() : "");
        JComboBox<String> category = form.addCombo("Category", Skill.CATEGORIES,
                editing ? existing.getCategory() : Skill.CATEGORIES[0]);
        JComboBox<String> level = form.addCombo("Level you teach at", Skill.LEVELS,
                editing ? existing.getLevel() : Skill.LEVELS[0]);
        JTextArea description = form.addArea("What will a learner walk away with?",
                editing ? existing.getDescription() : "", 4);
        JTextField cost = form.addField("Credits per session",
                CreditRules.MIN_PRICE + " to " + CreditRules.MAX_PRICE,
                String.valueOf(editing ? existing.getCreditCost() : 10));

        form.validate(() -> {
            if (title.getText().trim().length() < 3) return "Give the skill a clear name.";
            if (description.getText().trim().length() < 10) {
                return "Add a line about what a learner will walk away with.";
            }
            int value = Util.toInt(cost.getText(), -1);
            if (value < CreditRules.MIN_PRICE || value > CreditRules.MAX_PRICE) {
                return "Price it between " + CreditRules.MIN_PRICE + " and "
                        + CreditRules.MAX_PRICE + " credits.";
            }
            return null;
        });

        form.onSubmit(() -> {
            try {
                int value = Util.toInt(cost.getText(), 10);
                if (editing) {
                    skills.update(existing, title.getText(), (String) category.getSelectedItem(),
                            (String) level.getSelectedItem(), description.getText(), value);
                } else {
                    skills.add(frame.user(), title.getText(), (String) category.getSelectedItem(),
                            (String) level.getSelectedItem(), description.getText(), value);
                }
                refresh();
            } catch (RuleException ex) {
                Dialogs.error(this, "Could not save that", ex.getMessage());
            }
        });
        form.show();
    }
}
