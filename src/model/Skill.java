package model;

import util.Util;

/** A skill a student offers to teach. */
public class Skill implements Entity {

    public static final String[] CATEGORIES = {
        "Programming", "Design", "Languages", "Music & Dance",
        "Sports & Fitness", "Photography", "Business", "Academics", "Life Skills"
    };
    public static final String[] LEVELS = { "Beginner", "Intermediate", "Advanced" };

    private int id;
    private int ownerId;
    private String title = "";
    private String category = CATEGORIES[0];
    private String level = LEVELS[0];
    private String description = "";
    private int creditCost = 10;
    private boolean active = true;
    private String createdOn = Util.now();

    public Skill() { }

    public Skill(int id, int ownerId, String title, String category, String level,
                 String description, int creditCost) {
        this.id = id;
        this.ownerId = ownerId;
        setTitle(title);
        this.category = category;
        this.level = level;
        setDescription(description);
        this.creditCost = creditCost;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = Util.clean(title); }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = Util.clean(description); }

    public int getCreditCost() { return creditCost; }
    public void setCreditCost(int creditCost) { this.creditCost = Math.max(1, creditCost); }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getCreatedOn() { return createdOn; }
    public void setCreatedOn(String createdOn) { this.createdOn = createdOn; }

    public String toLine() {
        return String.join("|", String.valueOf(id), String.valueOf(ownerId), title, category,
                level, description, String.valueOf(creditCost), String.valueOf(active), createdOn);
    }

    public static Skill fromLine(String line) {
        String[] p = line.split("\\|", -1);
        Skill s = new Skill();
        s.id = Integer.parseInt(p[0]);
        s.ownerId = Integer.parseInt(p[1]);
        s.title = p[2];
        s.category = p[3];
        s.level = p[4];
        s.description = p[5];
        s.creditCost = Integer.parseInt(p[6]);
        s.active = Boolean.parseBoolean(p[7]);
        s.createdOn = p[8];
        return s;
    }

    @Override
    public String toString() { return title; }
}
