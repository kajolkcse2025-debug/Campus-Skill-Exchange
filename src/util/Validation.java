package util;

// STATIC MEMBERS: utility class, no need to create an object to use it
public class Validation {

    public static boolean isNonEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    public static boolean isPositive(int number) {
        return number > 0;
    }
}
