package service;

import data.Database;
import model.CreditTransaction;
import model.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Moves credits and keeps the passbook in step with every balance. */
public class CreditService {

    private final Database db = Database.get();

    /** Applies a change and records it. Negative amounts are spends. */
    public CreditTransaction grant(User user, int amount, String type, String description) {
        user.addCredits(amount);
        CreditTransaction t = new CreditTransaction(db.nextId(db.transactions()), user.getId(),
                amount, user.getCredits(), type, description);
        db.transactions().add(t);
        db.save();
        return t;
    }

    public boolean canAfford(User user, int cost) {
        return user.getCredits() >= cost;
    }

    public List<CreditTransaction> historyOf(int userId) {
        List<CreditTransaction> out = new ArrayList<>();
        for (CreditTransaction t : db.transactions()) {
            if (t.getUserId() == userId) out.add(t);
        }
        out.sort(Comparator.comparing(CreditTransaction::getStamp).reversed());
        return out;
    }

    public int earnedBy(int userId) {
        int total = 0;
        for (CreditTransaction t : db.transactions()) {
            if (t.getUserId() == userId && t.getAmount() > 0) total += t.getAmount();
        }
        return total;
    }

    public int spentBy(int userId) {
        int total = 0;
        for (CreditTransaction t : db.transactions()) {
            if (t.getUserId() == userId && t.getAmount() < 0) total -= t.getAmount();
        }
        return total;
    }

    /** Credits held by every student right now. */
    public int inCirculation() {
        int total = 0;
        for (User u : db.users()) if (!u.isAdmin()) total += u.getCredits();
        return total;
    }
}
