package service;

import data.Database;
import model.CreditTransaction;
import model.Feedback;
import model.LearningSession;
import model.User;
import service.AuthService.RuleException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Reviews left after a session, and the ratings derived from them. */
public class FeedbackService {

    private final Database db = Database.get();
    private final CreditService credits = new CreditService();

    public Feedback submit(LearningSession session, User from, int rating, String comment)
            throws RuleException {
        if (!LearningSession.COMPLETED.equals(session.getStatus())) {
            throw new RuleException("You can review a session once it is complete.");
        }
        if (alreadyReviewed(session.getId(), from.getId())) {
            throw new RuleException("You have already reviewed this session.");
        }
        boolean fromLearner = session.getLearnerId() == from.getId();
        if (!fromLearner && session.getTeacherId() != from.getId()) {
            throw new RuleException("You were not part of this session.");
        }
        int toId = fromLearner ? session.getTeacherId() : session.getLearnerId();

        Feedback f = new Feedback(db.nextId(db.feedbacks()), session.getId(), from.getId(),
                toId, rating, comment, fromLearner);
        db.feedbacks().add(f);

        if (fromLearner && rating == 5) {
            User teacher = db.userById(toId);
            if (teacher != null) {
                credits.grant(teacher, CreditRules.FIVE_STAR_BONUS, CreditTransaction.BONUS,
                        "Five star review from " + from.getName());
            }
        }
        db.save();
        return f;
    }

    public boolean alreadyReviewed(int sessionId, int fromUserId) {
        for (Feedback f : db.feedbacks()) {
            if (f.getSessionId() == sessionId && f.getFromUserId() == fromUserId) return true;
        }
        return false;
    }

    /** Average rating a student has received as a teacher. 0 when unrated. */
    public double teacherRating(int userId) {
        int sum = 0, count = 0;
        for (Feedback f : db.feedbacks()) {
            if (f.getToUserId() == userId && f.isForTeacher()) {
                sum += f.getRating();
                count++;
            }
        }
        return count == 0 ? 0 : (double) sum / count;
    }

    public int reviewCount(int userId) {
        int count = 0;
        for (Feedback f : db.feedbacks()) {
            if (f.getToUserId() == userId && f.isForTeacher()) count++;
        }
        return count;
    }

    public List<Feedback> receivedBy(int userId) {
        List<Feedback> out = new ArrayList<>();
        for (Feedback f : db.feedbacks()) if (f.getToUserId() == userId) out.add(f);
        out.sort(Comparator.comparing(Feedback::getStamp).reversed());
        return out;
    }

    public List<Feedback> writtenBy(int userId) {
        List<Feedback> out = new ArrayList<>();
        for (Feedback f : db.feedbacks()) if (f.getFromUserId() == userId) out.add(f);
        out.sort(Comparator.comparing(Feedback::getStamp).reversed());
        return out;
    }

    public List<Feedback> forSkill(int skillId) {
        List<Feedback> out = new ArrayList<>();
        for (Feedback f : db.feedbacks()) {
            LearningSession s = db.sessionById(f.getSessionId());
            if (s != null && s.getSkillId() == skillId && f.isForTeacher()) out.add(f);
        }
        return out;
    }

    public double skillRating(int skillId) {
        List<Feedback> list = forSkill(skillId);
        if (list.isEmpty()) return 0;
        int sum = 0;
        for (Feedback f : list) sum += f.getRating();
        return (double) sum / list.size();
    }
}
