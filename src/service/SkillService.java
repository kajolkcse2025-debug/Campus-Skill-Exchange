package service;

import data.Database;
import model.Skill;
import model.User;
import service.AuthService.RuleException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Adding, editing, retiring and searching skills. */
public class SkillService {

    private final Database db = Database.get();
    private final SessionService sessions = new SessionService();

    public Skill add(User owner, String title, String category, String level,
                     String description, int cost) throws RuleException {
        validate(title, description, cost);
        for (Skill s : db.skills()) {
            if (s.getOwnerId() == owner.getId() && s.isActive()
                    && s.getTitle().equalsIgnoreCase(title.trim())) {
                throw new RuleException("You already teach a skill with that name.");
            }
        }
        Skill skill = new Skill(db.nextId(db.skills()), owner.getId(), title, category,
                level, description, cost);
        db.skills().add(skill);
        db.save();
        return skill;
    }

    public void update(Skill skill, String title, String category, String level,
                       String description, int cost) throws RuleException {
        validate(title, description, cost);
        skill.setTitle(title);
        skill.setCategory(category);
        skill.setLevel(level);
        skill.setDescription(description);
        skill.setCreditCost(cost);
        db.save();
    }

    private void validate(String title, String description, int cost) throws RuleException {
        if (title.trim().length() < 3) throw new RuleException("Give the skill a clear name.");
        if (description.trim().length() < 10) {
            throw new RuleException("Describe what a learner will walk away with.");
        }
        if (cost < CreditRules.MIN_PRICE || cost > CreditRules.MAX_PRICE) {
            throw new RuleException("Price a session between " + CreditRules.MIN_PRICE
                    + " and " + CreditRules.MAX_PRICE + " credits.");
        }
    }

    /** Skills are retired rather than deleted so past sessions still read correctly. */
    public void retire(Skill skill) throws RuleException {
        if (!sessions.openFor(skill.getId()).isEmpty()) {
            throw new RuleException("Close the open requests for this skill first.");
        }
        skill.setActive(false);
        db.save();
    }

    public void restore(Skill skill) {
        skill.setActive(true);
        db.save();
    }

    public List<Skill> ownedBy(int ownerId) {
        List<Skill> out = new ArrayList<>();
        for (Skill s : db.skills()) if (s.getOwnerId() == ownerId) out.add(s);
        return out;
    }

    /**
     * Browse with optional filters. Pass "All" for category or level to ignore them,
     * and an empty string for text to match everything.
     */
    public List<Skill> search(String text, String category, String level, int excludeOwnerId) {
        String q = text == null ? "" : text.trim().toLowerCase();
        List<Skill> out = new ArrayList<>();
        for (Skill s : db.skills()) {
            if (!s.isActive() || s.getOwnerId() == excludeOwnerId) continue;
            User owner = db.userById(s.getOwnerId());
            if (owner == null || !owner.isActive()) continue;
            if (!"All".equals(category) && !s.getCategory().equals(category)) continue;
            if (!"All".equals(level) && !s.getLevel().equals(level)) continue;
            if (!q.isEmpty()) {
                String hay = (s.getTitle() + " " + s.getDescription() + " "
                        + s.getCategory() + " " + owner.getName()).toLowerCase();
                if (!hay.contains(q)) continue;
            }
            out.add(s);
        }
        out.sort(Comparator.comparing(Skill::getTitle));
        return out;
    }

    public List<Skill> allActive() {
        return search("", "All", "All", -1);
    }
}
