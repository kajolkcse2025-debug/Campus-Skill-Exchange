package model;

import util.Util;

/** A member of the campus community: either a student or an administrator. */
public class User implements Entity {

    public static final String STUDENT = "STUDENT";
    public static final String ADMIN = "ADMIN";

    private int id;
    private String name = "";
    private String email = "";
    private String passwordHash = "";
    private String department = "";
    private String year = "";
    private String bio = "";
    private String role = STUDENT;
    private String joinedOn = Util.now();
    private int credits;
    private boolean active = true;

    public User() { }

    public User(int id, String name, String email, String plainPassword,
                String department, String year, String role, int credits) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = Util.sha256(plainPassword);
        this.department = department;
        this.year = year;
        this.role = role;
        this.credits = credits;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = Util.clean(name); }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = Util.clean(email); }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String hash) { this.passwordHash = hash; }
    public void setPassword(String plain) { this.passwordHash = Util.sha256(plain); }
    public boolean passwordMatches(String plain) { return passwordHash.equals(Util.sha256(plain)); }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = Util.clean(department); }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = Util.clean(year); }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = Util.clean(bio); }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isAdmin() { return ADMIN.equals(role); }

    public String getJoinedOn() { return joinedOn; }
    public void setJoinedOn(String joinedOn) { this.joinedOn = joinedOn; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = Math.max(0, credits); }
    public void addCredits(int delta) { setCredits(credits + delta); }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /** Two initials for the avatar circle. */
    public String initials() {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return "?";
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    public String toLine() {
        return String.join("|", String.valueOf(id), name, email, passwordHash, department,
                year, bio, role, joinedOn, String.valueOf(credits), String.valueOf(active));
    }

    public static User fromLine(String line) {
        String[] p = line.split("\\|", -1);
        User u = new User();
        u.id = Integer.parseInt(p[0]);
        u.name = p[1];
        u.email = p[2];
        u.passwordHash = p[3];
        u.department = p[4];
        u.year = p[5];
        u.bio = p[6];
        u.role = p[7];
        u.joinedOn = p[8];
        u.credits = Integer.parseInt(p[9]);
        u.active = Boolean.parseBoolean(p[10]);
        return u;
    }

    @Override
    public String toString() { return name; }
}
