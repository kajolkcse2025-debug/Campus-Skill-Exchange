package service;

import data.Database;
import model.Badge;
import model.CreditTransaction;
import model.LearningSession;
import model.Skill;
import model.User;
import util.Util;

import java.awt.Color;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Rankings and achievement badges. */
public class LeaderboardService {

    private final Database db = Database.get();
    private final SessionService sessions = new SessionService();
    private final FeedbackService feedback = new FeedbackService();
    private final CreditService credits = new CreditService();
    private final SkillService skills = new SkillService();

    /** One row of the leaderboard. */
    public static class Row {
        public int rank;
        public User user;
        public int creditsEarned;
        public int taught;
        public int learned;
        public double rating;
        public int score;
    }

    /** Ranked by a score that rewards teaching most, then ratings, then learning. */
    public List<Row> ranking(boolean thisMonthOnly) {
        List<Row> rows = new ArrayList<>();
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);

        for (User u : db.users()) {
            if (u.isAdmin() || !u.isActive()) continue;
            Row r = new Row();
            r.user = u;
            r.rating = feedback.teacherRating(u.getId());

            if (thisMonthOnly) {
                int earned = 0;
                for (CreditTransaction t : db.transactions()) {
                    if (t.getUserId() == u.getId() && t.getAmount() > 0
                            && !Util.dateOf(t.getStamp()).isBefore(monthStart)) {
                        earned += t.getAmount();
                    }
                }
                r.creditsEarned = earned;
                int taught = 0, learned = 0;
                for (LearningSession s : sessions.completed()) {
                    if (Util.dateOf(s.getCompletedOn()).isBefore(monthStart)) continue;
                    if (s.getTeacherId() == u.getId()) taught++;
                    if (s.getLearnerId() == u.getId()) learned++;
                }
                r.taught = taught;
                r.learned = learned;
            } else {
                r.creditsEarned = credits.earnedBy(u.getId());
                r.taught = sessions.countTaught(u.getId());
                r.learned = sessions.countLearned(u.getId());
            }

            r.score = r.taught * 20 + r.learned * 5 + (int) Math.round(r.rating * 10)
                    + r.creditsEarned / 2;
            rows.add(r);
        }

        rows.sort(Comparator.comparingInt((Row r) -> r.score).reversed()
                .thenComparing(r -> r.user.getName()));
        for (int i = 0; i < rows.size(); i++) rows.get(i).rank = i + 1;
        return rows;
    }

    public int rankOf(int userId) {
        for (Row r : ranking(false)) if (r.user.getId() == userId) return r.rank;
        return 0;
    }

    /** Every badge in the system, marked earned or not for this student. */
    public List<Badge> badgesFor(User user) {
        int taught = sessions.countTaught(user.getId());
        int learned = sessions.countLearned(user.getId());
        int offered = 0;
        for (Skill s : skills.ownedBy(user.getId())) if (s.isActive()) offered++;
        double rating = feedback.teacherRating(user.getId());
        int reviews = feedback.reviewCount(user.getId());
        int earned = credits.earnedBy(user.getId());
        int rank = rankOf(user.getId());

        List<Badge> out = new ArrayList<>();
        out.add(new Badge("First Session", "Teach your first session",
                new Color(0x22D3EE), taught >= 1, taught / 1.0));
        out.add(new Badge("Mentor", "Teach 5 sessions",
                new Color(0x7C5CFF), taught >= 5, taught / 5.0));
        out.add(new Badge("Skill Master", "Teach 10 sessions at 4.5 stars or better",
                new Color(0xFFB74D), taught >= 10 && rating >= 4.5, taught / 10.0));
        out.add(new Badge("Curious Mind", "Complete 3 sessions as a learner",
                new Color(0x2DD4BF), learned >= 3, learned / 3.0));
        out.add(new Badge("Multi-talented", "Offer 3 skills at once",
                new Color(0xF472B6), offered >= 3, offered / 3.0));
        out.add(new Badge("Crowd Favourite", "Hold a 4.8 rating across 5 reviews",
                new Color(0xFBBF24), reviews >= 5 && rating >= 4.8,
                reviews == 0 ? 0 : Math.min(1, reviews / 5.0) * (rating / 5.0)));
        out.add(new Badge("Credit Magnet", "Earn 150 credits in total",
                new Color(0x34D399), earned >= 150, earned / 150.0));
        out.add(new Badge("Top Contributor", "Finish in the campus top 3",
                new Color(0xF87171), rank > 0 && rank <= 3, rank == 0 ? 0 : 3.0 / rank));
        return out;
    }

    public int badgeCount(User user) {
        int n = 0;
        for (Badge b : badgesFor(user)) if (b.isEarned()) n++;
        return n;
    }
}
