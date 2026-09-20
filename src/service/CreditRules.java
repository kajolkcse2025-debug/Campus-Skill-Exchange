package service;

/** Every number the credit economy depends on, in one place. */
public final class CreditRules {

    /** Credits a new student starts with. */
    public static final int WELCOME = 60;
    /** Extra credits a teacher gets on top of the skill price. */
    public static final int TEACH_BONUS = 5;
    /** Awarded to a teacher when a learner leaves five stars. */
    public static final int FIVE_STAR_BONUS = 3;
    /** Credits returned to a learner if a scheduled session is cancelled. */
    public static final int CANCEL_REFUND_PERCENT = 100;
    /** Cheapest and dearest a skill may be priced. */
    public static final int MIN_PRICE = 5;
    public static final int MAX_PRICE = 25;

    private CreditRules() { }
}
