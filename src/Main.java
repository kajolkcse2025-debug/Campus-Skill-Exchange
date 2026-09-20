import data.Database;
import model.Skill;
import model.User;
import service.CreditService;
import service.FeedbackService;
import service.LeaderboardService;
import service.SessionService;
import service.SkillService;
import ui.LoginFrame;
import ui.Theme;

import javax.swing.SwingUtilities;

/**
 * Campus Skill Exchange &amp; Credit System.
 *
 * <p>Run with no arguments to open the desktop application. Run with
 * {@code --selftest} to exercise the service layer without opening a window.</p>
 */
public final class Main {

    public static void main(String[] args) {
        if (args.length > 0 && "--selftest".equals(args[0])) {
            selfTest();
            return;
        }
        Theme.install();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    private static void selfTest() {
        Database db = Database.get();
        SkillService skills = new SkillService();
        SessionService sessions = new SessionService();
        CreditService credits = new CreditService();
        FeedbackService feedback = new FeedbackService();
        LeaderboardService board = new LeaderboardService();

        System.out.println("users        : " + db.users().size());
        System.out.println("skills       : " + db.skills().size());
        System.out.println("sessions     : " + db.sessions().size());
        System.out.println("transactions : " + db.transactions().size());
        System.out.println("feedback     : " + db.feedbacks().size());
        System.out.println("circulating  : " + credits.inCirculation());

        boolean ok = true;
        for (User u : db.users()) {
            if (u.getCredits() < 0) {
                System.out.println("FAIL negative balance for " + u.getName());
                ok = false;
            }
        }
        for (Skill s : skills.allActive()) {
            if (s.getCreditCost() <= 0) {
                System.out.println("FAIL bad price on " + s.getTitle());
                ok = false;
            }
        }

        System.out.println("\nTop of the leaderboard");
        for (LeaderboardService.Row row : board.ranking(false).subList(0, 5)) {
            System.out.printf("  %d. %-22s score %-5d taught %d  rating %.1f%n",
                    row.rank, row.user.getName(), row.score, row.taught, row.rating);
        }

        User first = db.users().get(1);
        System.out.println("\n" + first.getName()
                + " taught " + sessions.countTaught(first.getId())
                + ", learned " + sessions.countLearned(first.getId())
                + ", rating " + String.format("%.2f", feedback.teacherRating(first.getId()))
                + ", badges " + board.badgeCount(first));

        System.out.println("\nself test " + (ok ? "PASSED" : "FAILED"));
    }
}
