package service;

import data.Database;
import model.CreditTransaction;
import model.User;
import util.Util;

/** Registration, login and profile edits. */
public class AuthService {

    private final Database db = Database.get();
    private final CreditService credits = new CreditService();

    /** Thrown when input fails a rule the student can fix. */
    public static class RuleException extends Exception {
        public RuleException(String message) { super(message); }
    }

    public User register(String name, String email, String password, String confirm,
                         String department, String year) throws RuleException {
        if (name.trim().length() < 3) throw new RuleException("Enter your full name.");
        if (!Util.isEmail(email)) throw new RuleException("That email address is not valid.");
        if (db.userByEmail(email) != null) throw new RuleException("An account already uses that email.");
        if (password.length() < 6) throw new RuleException("Use at least 6 characters for the password.");
        if (!password.equals(confirm)) throw new RuleException("The two passwords do not match.");
        if (department.trim().isEmpty()) throw new RuleException("Choose your department.");

        User user = new User(db.nextId(db.users()), Util.clean(name), Util.clean(email).toLowerCase(),
                password, department, year, User.STUDENT, 0);
        db.users().add(user);
        credits.grant(user, CreditRules.WELCOME, CreditTransaction.SIGNUP,
                "Joined the skill exchange");
        db.save();
        return user;
    }

    public User login(String email, String password) throws RuleException {
        User user = db.userByEmail(email);
        if (user == null || !user.passwordMatches(password)) {
            throw new RuleException("Email or password is incorrect.");
        }
        if (!user.isActive()) {
            throw new RuleException("This account is suspended. Contact Student Affairs.");
        }
        return user;
    }

    public void updateProfile(User user, String name, String department, String year, String bio)
            throws RuleException {
        if (name.trim().length() < 3) throw new RuleException("Enter your full name.");
        user.setName(name);
        user.setDepartment(department);
        user.setYear(year);
        user.setBio(bio);
        db.save();
    }

    public void changePassword(User user, String current, String next, String confirm)
            throws RuleException {
        if (!user.passwordMatches(current)) throw new RuleException("Your current password is wrong.");
        if (next.length() < 6) throw new RuleException("Use at least 6 characters for the new password.");
        if (!next.equals(confirm)) throw new RuleException("The two new passwords do not match.");
        user.setPassword(next);
        db.save();
    }
}
