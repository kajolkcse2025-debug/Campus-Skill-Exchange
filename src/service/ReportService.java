package service;

import data.Database;
import model.CreditTransaction;
import model.Feedback;
import model.LearningSession;
import model.Skill;
import model.User;
import util.Util;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the campus reports and the numbers behind the charts. */
public class ReportService {

    private final Database db = Database.get();
    private final SessionService sessions = new SessionService();
    private final FeedbackService feedback = new FeedbackService();
    private final CreditService credits = new CreditService();

    // --------------------------------------------------------------- chart data

    /** Completed sessions per skill, highest first, capped at {@code limit} entries. */
    public Map<String, Integer> popularSkills(int limit) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (LearningSession s : sessions.completed()) {
            String title = db.skillTitle(s.getSkillId());
            counts.merge(title, 1, Integer::sum);
        }
        return topOf(counts, limit);
    }

    public Map<String, Integer> sessionsByCategory() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (LearningSession s : sessions.completed()) {
            Skill skill = db.skillById(s.getSkillId());
            if (skill != null) counts.merge(skill.getCategory(), 1, Integer::sum);
        }
        return topOf(counts, 99);
    }

    /** Completed sessions for each of the last {@code months} months, oldest first. */
    public Map<String, Integer> sessionsByMonth(int months) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        LocalDate cursor = LocalDate.now().withDayOfMonth(1).minusMonths(months - 1L);
        for (int i = 0; i < months; i++) {
            counts.put(cursor.format(java.time.format.DateTimeFormatter.ofPattern("MMM")), 0);
            cursor = cursor.plusMonths(1);
        }
        LocalDate from = LocalDate.now().withDayOfMonth(1).minusMonths(months - 1L);
        for (LearningSession s : sessions.completed()) {
            LocalDate done = Util.dateOf(s.getCompletedOn());
            if (done.isBefore(from)) continue;
            String key = done.format(java.time.format.DateTimeFormatter.ofPattern("MMM"));
            if (counts.containsKey(key)) counts.merge(key, 1, Integer::sum);
        }
        return counts;
    }

    private Map<String, Integer> topOf(Map<String, Integer> counts, int limit) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(counts.entrySet());
        list.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        Map<String, Integer> out = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(limit, list.size()); i++) {
            out.put(list.get(i).getKey(), list.get(i).getValue());
        }
        return out;
    }

    // ------------------------------------------------------------- text reports

    public String userActivityReport() {
        StringBuilder sb = header("User activity report");
        sb.append(String.format("%-24s %-18s %8s %8s %8s %8s%n",
                "Student", "Department", "Taught", "Learned", "Rating", "Credits"));
        sb.append(line());
        List<User> students = students();
        students.sort(Comparator.comparingInt((User u) -> sessions.countTaught(u.getId())).reversed());
        for (User u : students) {
            double rating = feedback.teacherRating(u.getId());
            sb.append(String.format("%-24s %-18s %8d %8d %8s %8d%n",
                    cut(u.getName(), 24), cut(u.getDepartment(), 18),
                    sessions.countTaught(u.getId()), sessions.countLearned(u.getId()),
                    rating == 0 ? "-" : Util.round(rating), u.getCredits()));
        }
        sb.append(line());
        sb.append(String.format("%d students, %d completed sessions in total.%n",
                students.size(), sessions.completed().size()));
        return sb.toString();
    }

    public String creditReport() {
        StringBuilder sb = header("Credit report");
        int earnedAll = 0, spentAll = 0;
        for (CreditTransaction t : db.transactions()) {
            if (t.getAmount() > 0) earnedAll += t.getAmount();
            else spentAll -= t.getAmount();
        }
        sb.append(String.format("Credits in circulation : %d%n", credits.inCirculation()));
        sb.append(String.format("Total ever earned      : %d%n", earnedAll));
        sb.append(String.format("Total ever spent       : %d%n", spentAll));
        sb.append(String.format("Transactions recorded  : %d%n%n", db.transactions().size()));

        sb.append(String.format("%-24s %10s %10s %10s%n", "Student", "Earned", "Spent", "Balance"));
        sb.append(line());
        List<User> students = students();
        students.sort(Comparator.comparingInt((User u) -> credits.earnedBy(u.getId())).reversed());
        for (User u : students) {
            sb.append(String.format("%-24s %10d %10d %10d%n", cut(u.getName(), 24),
                    credits.earnedBy(u.getId()), credits.spentBy(u.getId()), u.getCredits()));
        }
        return sb.toString();
    }

    public String popularSkillsReport() {
        StringBuilder sb = header("Popular skills analysis");
        sb.append(String.format("%-34s %-16s %10s %8s%n",
                "Skill", "Category", "Sessions", "Rating"));
        sb.append(line());
        List<Skill> all = new ArrayList<>(db.skills());
        all.sort(Comparator.comparingInt((Skill s) -> sessions.openFor(s.getId()).size()).reversed());
        Map<String, Integer> popular = popularSkills(99);
        all.sort(Comparator.comparingInt((Skill s) ->
                popular.getOrDefault(s.getTitle(), 0)).reversed());
        for (Skill s : all) {
            double rating = feedback.skillRating(s.getId());
            sb.append(String.format("%-34s %-16s %10d %8s%n", cut(s.getTitle(), 34),
                    cut(s.getCategory(), 16), popular.getOrDefault(s.getTitle(), 0),
                    rating == 0 ? "-" : Util.round(rating)));
        }
        sb.append(line());
        Map<String, Integer> byCategory = sessionsByCategory();
        sb.append("Sessions by category\n");
        for (Map.Entry<String, Integer> e : byCategory.entrySet()) {
            sb.append(String.format("  %-20s %s (%d)%n", e.getKey(),
                    bar(e.getValue(), byCategory), e.getValue()));
        }
        return sb.toString();
    }

    public String studentReport(User user) {
        StringBuilder sb = header("Activity report for " + user.getName());
        sb.append(String.format("Department  : %s, %s%n", user.getDepartment(), user.getYear()));
        sb.append(String.format("Member since: %s%n", Util.pretty(user.getJoinedOn())));
        sb.append(String.format("Balance     : %d credits%n", user.getCredits()));
        sb.append(String.format("Earned      : %d    Spent: %d%n",
                credits.earnedBy(user.getId()), credits.spentBy(user.getId())));
        double rating = feedback.teacherRating(user.getId());
        sb.append(String.format("Taught      : %d sessions    Learned: %d sessions%n",
                sessions.countTaught(user.getId()), sessions.countLearned(user.getId())));
        sb.append(String.format("Rating      : %s from %d reviews%n%n",
                rating == 0 ? "not rated yet" : Util.round(rating) + " / 5",
                feedback.reviewCount(user.getId())));

        sb.append("Recent sessions\n").append(line());
        List<LearningSession> mine = new ArrayList<>();
        mine.addAll(sessions.teaching(user.getId()));
        mine.addAll(sessions.learning(user.getId()));
        mine.sort(Comparator.comparing(LearningSession::getRequestedOn).reversed());
        int shown = 0;
        for (LearningSession s : mine) {
            if (shown++ >= 12) break;
            sb.append(String.format("%-34s %-10s %-10s %s%n",
                    cut(db.skillTitle(s.getSkillId()), 34),
                    s.getTeacherId() == user.getId() ? "teaching" : "learning",
                    s.getStatus().toLowerCase(), Util.pretty(s.getRequestedOn())));
        }
        if (shown == 0) sb.append("No sessions yet.\n");

        sb.append("\nReviews received\n").append(line());
        List<Feedback> reviews = feedback.receivedBy(user.getId());
        if (reviews.isEmpty()) sb.append("No reviews yet.\n");
        for (Feedback f : reviews) {
            sb.append(String.format("%s  %s%n", f.stars(), f.getComment()));
        }
        return sb.toString();
    }

    /** Writes a report next to the app in ./reports and returns the file. */
    public File export(String fileName, String contents) throws Exception {
        File folder = new File("reports");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new Exception("Could not create the reports folder.");
        }
        File file = new File(folder, fileName);
        try (PrintWriter out = new PrintWriter(file, "UTF-8")) {
            out.print(contents);
        }
        return file;
    }

    // ------------------------------------------------------------------ helpers

    private List<User> students() {
        List<User> out = new ArrayList<>();
        for (User u : db.users()) if (!u.isAdmin()) out.add(u);
        return out;
    }

    private StringBuilder header(String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("Campus Skill Exchange\n");
        sb.append(title).append('\n');
        sb.append("Generated ").append(Util.pretty(Util.now())).append('\n');
        sb.append(line());
        return sb;
    }

    private String line() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 76; i++) sb.append('-');
        return sb.append('\n').toString();
    }

    private String bar(int value, Map<String, Integer> all) {
        int max = 1;
        for (int v : all.values()) max = Math.max(max, v);
        int width = (int) Math.round(value * 24.0 / max);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width; i++) sb.append('#');
        return sb.toString();
    }

    private String cut(String text, int width) {
        if (text.length() <= width) return text;
        return text.substring(0, width - 1) + "\u2026";
    }
}
