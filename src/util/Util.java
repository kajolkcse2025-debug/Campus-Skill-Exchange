package util;

import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Small shared helpers: hashing, timestamps and text sanitising. */
public final class Util {

    public static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter PRETTY = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    public static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy");

    private Util() { }

    public static String now() {
        return LocalDateTime.now().format(TS);
    }

    public static String daysAgo(int days) {
        return LocalDateTime.now().minusDays(days).format(TS);
    }

    public static String daysAhead(int days) {
        return LocalDateTime.now().plusDays(days).withHour(16).withMinute(0).format(TS);
    }

    public static LocalDateTime parse(String stamp) {
        try {
            return LocalDateTime.parse(stamp, TS);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    public static String pretty(String stamp) {
        return parse(stamp).format(PRETTY);
    }

    public static String monthOf(String stamp) {
        return parse(stamp).format(MONTH);
    }

    /** "12 Jun 2026" without the clock time. */
    public static String prettyDate(String stamp) {
        return parse(stamp).format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    public static LocalDate dateOf(String stamp) {
        return parse(stamp).toLocalDate();
    }

    /** Strips the field delimiter and newlines so a value is safe to persist. */
    public static String clean(String raw) {
        if (raw == null) return "";
        return raw.replace("|", "/").replace("\r", " ").replace("\n", " ").trim();
    }

    public static String sha256(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(plain.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing unavailable", e);
        }
    }

    public static boolean isEmail(String value) {
        return value != null && value.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]{2,}$");
    }

    public static int toInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    public static String round(double value) {
        return String.format("%.1f", value);
    }
}
