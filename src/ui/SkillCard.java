package ui;

import data.Database;
import model.Skill;
import model.User;
import service.FeedbackService;
import util.Util;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;

/** One skill presented as a card, used on the browse page. */
public class SkillCard extends Theme.Card {

    private static final Database DB = Database.get();
    private static final FeedbackService FEEDBACK = new FeedbackService();

    public SkillCard(Skill skill, String actionLabel, ActionListener action, boolean affordable) {
        super(new BorderLayout(0, 10));
        pad(16, 18, 16, 18);
        setPreferredSize(new Dimension(340, 212));

        User owner = DB.userById(skill.getOwnerId());
        String ownerName = owner == null ? "Removed user" : owner.getName();

        JPanel head = Theme.clear(new BorderLayout());
        JPanel chips = Theme.clear(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chips.add(new Theme.Chip(skill.getCategory(), Theme.categoryColor(skill.getCategory()), false));
        chips.add(new Theme.Chip(skill.getLevel(), Theme.MUTED, false));
        head.add(chips, BorderLayout.WEST);
        head.add(new CostTag(skill.getCreditCost(), affordable), BorderLayout.EAST);
        add(head, BorderLayout.NORTH);

        JPanel middle = Theme.clear(new BorderLayout(0, 6));
        middle.add(Theme.label(skill.getTitle(), 15, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        middle.add(Theme.wrapped(skill.getDescription(), 290, Theme.MUTED, 12), BorderLayout.CENTER);
        add(middle, BorderLayout.CENTER);

        JPanel foot = Theme.clear(new BorderLayout(10, 0));
        foot.setBorder(new EmptyBorder(4, 0, 0, 0));

        JPanel who = Theme.clear(new BorderLayout(9, 0));
        who.add(new Theme.Avatar(owner == null ? "?" : owner.initials(), 32,
                Theme.Avatar.toneFor(ownerName)), BorderLayout.WEST);

        JPanel names = Theme.clear(new BorderLayout(0, 2));
        names.add(Theme.label(ownerName, 12, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        double rating = FEEDBACK.skillRating(skill.getId());
        int reviews = FEEDBACK.forSkill(skill.getId()).size();
        if (reviews > 0) {
            JPanel stars = Theme.clear(new FlowLayout(FlowLayout.LEFT, 5, 0));
            stars.add(new Theme.Stars(rating, 11));
            stars.add(Theme.label(Util.round(rating) + " (" + reviews + ")", 11,
                    Font.PLAIN, Theme.FAINT));
            names.add(stars, BorderLayout.SOUTH);
        } else {
            names.add(Theme.label("No reviews yet", 11, Font.PLAIN, Theme.FAINT), BorderLayout.SOUTH);
        }
        who.add(names, BorderLayout.CENTER);
        foot.add(who, BorderLayout.CENTER);

        if (actionLabel != null) {
            Theme.Btn go = affordable ? Theme.solid(actionLabel) : Theme.ghost(actionLabel, Theme.FAINT);
            go.setEnabled(affordable);
            if (!affordable) go.setToolTipText("You need " + skill.getCreditCost() + " credits for this.");
            go.addActionListener(action);
            JPanel holder = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            holder.add(go);
            foot.add(holder, BorderLayout.EAST);
        }
        add(foot, BorderLayout.SOUTH);
    }

    /** The price, in the colour reserved for credits. */
    private static class CostTag extends javax.swing.JComponent {
        private final String text;
        private final boolean affordable;

        CostTag(int cost, boolean affordable) {
            this.text = cost + " cr";
            this.affordable = affordable;
            setPreferredSize(new Dimension(58, 24));
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = Theme.prepare(g);
            java.awt.Color tone = affordable ? Theme.AMBER : Theme.FAINT;
            g2.setColor(Theme.alpha(tone, 34));
            g2.fill(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth() - 1,
                    getHeight() - 1, 12, 12));
            g2.setFont(Theme.font(Font.BOLD, 12));
            g2.setColor(tone);
            int w = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, (getWidth() - w) / 2, getHeight() / 2 + 4);
            g2.dispose();
        }
    }
}
