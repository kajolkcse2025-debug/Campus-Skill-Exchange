package util;

// Custom exception -> EXCEPTION HANDLING
public class InsufficientCreditsException extends Exception {
    public InsufficientCreditsException(String message) {
        super(message);
    }
}
