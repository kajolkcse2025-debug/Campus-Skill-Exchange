package model;

import util.Util;

/** A request from a learner to be taught a skill, through to completion. */
public class LearningSession implements Entity {

    public static final String PENDING = "PENDING";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String REJECTED = "REJECTED";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    public static final String[] MODES = { "In person", "Online", "Hybrid" };

    private int id;
    private int skillId;
    private int teacherId;
    private int learnerId;
    private String status = PENDING;
    private String requestedOn = Util.now();
    private String scheduledFor = "";
    private String completedOn = "";
    private String mode = MODES[0];
    private String venue = "";
    private String note = "";
    private int creditCost;

    public LearningSession() { }

    public LearningSession(int id, int skillId, int teacherId, int learnerId, int creditCost) {
        this.id = id;
        this.skillId = skillId;
        this.teacherId = teacherId;
        this.learnerId = learnerId;
        this.creditCost = creditCost;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSkillId() { return skillId; }
    public void setSkillId(int skillId) { this.skillId = skillId; }

    public int getTeacherId() { return teacherId; }
    public void setTeacherId(int teacherId) { this.teacherId = teacherId; }

    public int getLearnerId() { return learnerId; }
    public void setLearnerId(int learnerId) { this.learnerId = learnerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRequestedOn() { return requestedOn; }
    public void setRequestedOn(String requestedOn) { this.requestedOn = requestedOn; }

    public String getScheduledFor() { return scheduledFor; }
    public void setScheduledFor(String scheduledFor) { this.scheduledFor = Util.clean(scheduledFor); }

    public String getCompletedOn() { return completedOn; }
    public void setCompletedOn(String completedOn) { this.completedOn = completedOn; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = Util.clean(venue); }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = Util.clean(note); }

    public int getCreditCost() { return creditCost; }
    public void setCreditCost(int creditCost) { this.creditCost = creditCost; }

    public boolean isOpen() { return PENDING.equals(status) || ACCEPTED.equals(status); }

    public String toLine() {
        return String.join("|", String.valueOf(id), String.valueOf(skillId), String.valueOf(teacherId),
                String.valueOf(learnerId), status, requestedOn, scheduledFor, completedOn,
                mode, venue, note, String.valueOf(creditCost));
    }

    public static LearningSession fromLine(String line) {
        String[] p = line.split("\\|", -1);
        LearningSession s = new LearningSession();
        s.id = Integer.parseInt(p[0]);
        s.skillId = Integer.parseInt(p[1]);
        s.teacherId = Integer.parseInt(p[2]);
        s.learnerId = Integer.parseInt(p[3]);
        s.status = p[4];
        s.requestedOn = p[5];
        s.scheduledFor = p[6];
        s.completedOn = p[7];
        s.mode = p[8];
        s.venue = p[9];
        s.note = p[10];
        s.creditCost = Integer.parseInt(p[11]);
        return s;
    }
}
