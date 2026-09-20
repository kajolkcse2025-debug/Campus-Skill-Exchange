package model;

import util.Util;

/** A review left after a completed session, in either direction. */
public class Feedback implements Entity {

    private int id;
    private int sessionId;
    private int fromUserId;
    private int toUserId;
    private int rating = 5;        // 1..5
    private String comment = "";
    private String stamp = Util.now();
    private boolean forTeacher = true;

    public Feedback() { }

    public Feedback(int id, int sessionId, int fromUserId, int toUserId,
                    int rating, String comment, boolean forTeacher) {
        this.id = id;
        this.sessionId = sessionId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        setRating(rating);
        setComment(comment);
        this.forTeacher = forTeacher;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }

    public int getFromUserId() { return fromUserId; }
    public void setFromUserId(int fromUserId) { this.fromUserId = fromUserId; }

    public int getToUserId() { return toUserId; }
    public void setToUserId(int toUserId) { this.toUserId = toUserId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = Math.max(1, Math.min(5, rating)); }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = Util.clean(comment); }

    public String getStamp() { return stamp; }
    public void setStamp(String stamp) { this.stamp = stamp; }

    public boolean isForTeacher() { return forTeacher; }
    public void setForTeacher(boolean forTeacher) { this.forTeacher = forTeacher; }

    public String stars() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) sb.append(i <= rating ? '\u2605' : '\u2606');
        return sb.toString();
    }

    public String toLine() {
        return String.join("|", String.valueOf(id), String.valueOf(sessionId), String.valueOf(fromUserId),
                String.valueOf(toUserId), String.valueOf(rating), comment, stamp, String.valueOf(forTeacher));
    }

    public static Feedback fromLine(String line) {
        String[] p = line.split("\\|", -1);
        Feedback f = new Feedback();
        f.id = Integer.parseInt(p[0]);
        f.sessionId = Integer.parseInt(p[1]);
        f.fromUserId = Integer.parseInt(p[2]);
        f.toUserId = Integer.parseInt(p[3]);
        f.rating = Integer.parseInt(p[4]);
        f.comment = p[5];
        f.stamp = p[6];
        f.forTeacher = Boolean.parseBoolean(p[7]);
        return f;
    }
}
