package model;

import java.awt.Color;

/** An achievement a student can unlock. Badges are derived, never stored. */
public class Badge {

    private final String name;
    private final String requirement;
    private final Color color;
    private final boolean earned;
    private final double progress;   // 0..1

    public Badge(String name, String requirement, Color color, boolean earned, double progress) {
        this.name = name;
        this.requirement = requirement;
        this.color = color;
        this.earned = earned;
        this.progress = Math.max(0, Math.min(1, progress));
    }

    public String getName() { return name; }
    public String getRequirement() { return requirement; }
    public Color getColor() { return color; }
    public boolean isEarned() { return earned; }
    public double getProgress() { return progress; }
}
