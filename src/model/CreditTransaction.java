package model;

import util.Util;

/** One line in a student's credit passbook. */
public class CreditTransaction implements Entity {

    public static final String SIGNUP = "Welcome bonus";
    public static final String TEACH = "Taught a session";
    public static final String LEARN = "Joined a session";
    public static final String BONUS = "Rating bonus";
    public static final String REFUND = "Refund";
    public static final String ADJUST = "Admin adjustment";

    private int id;
    private int userId;
    private int amount;          // positive = earned, negative = spent
    private int balanceAfter;
    private String type = SIGNUP;
    private String description = "";
    private String stamp = Util.now();

    public CreditTransaction() { }

    public CreditTransaction(int id, int userId, int amount, int balanceAfter,
                             String type, String description) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.type = type;
        this.description = Util.clean(description);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public int getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(int balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = Util.clean(description); }

    public String getStamp() { return stamp; }
    public void setStamp(String stamp) { this.stamp = stamp; }

    public boolean isEarned() { return amount > 0; }

    public String toLine() {
        return String.join("|", String.valueOf(id), String.valueOf(userId), String.valueOf(amount),
                String.valueOf(balanceAfter), type, description, stamp);
    }

    public static CreditTransaction fromLine(String line) {
        String[] p = line.split("\\|", -1);
        CreditTransaction t = new CreditTransaction();
        t.id = Integer.parseInt(p[0]);
        t.userId = Integer.parseInt(p[1]);
        t.amount = Integer.parseInt(p[2]);
        t.balanceAfter = Integer.parseInt(p[3]);
        t.type = p[4];
        t.description = p[5];
        t.stamp = p[6];
        return t;
    }
}
