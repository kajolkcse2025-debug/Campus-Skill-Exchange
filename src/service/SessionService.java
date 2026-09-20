package service;

import data.Database;
import model.CreditTransaction;
import model.LearningSession;
import model.Skill;
import model.User;
import service.AuthService.RuleException;
import util.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The request to completion lifecycle, including the credit transfer. */
public class SessionService {

    private final Database db = Database.get();
    private final CreditService credits = new CreditService();

    public LearningSession request(User learner, Skill skill, String note, String mode,
                                   String preferredTime) throws RuleException {
        if (skill.getOwnerId() == learner.getId()) {
            throw new RuleException("This is your own skill.");
        }
        if (!skill.isActive()) {
            throw new RuleException("That skill is no longer offered.");
        }
        if (!credits.canAfford(learner, skill.getCreditCost())) {
            throw new RuleException("You need " + skill.getCreditCost() + " credits for this session. "
                    + "Teach something to earn more.");
        }
        for (LearningSession s : db.sessions()) {
            if (s.getLearnerId() == learner.getId() && s.getSkillId() == skill.getId() && s.isOpen()) {
                throw new RuleException("You already have an open request for this skill.");
            }
        }
        LearningSession session = new LearningSession(db.nextId(db.sessions()), skill.getId(),
                skill.getOwnerId(), learner.getId(), skill.getCreditCost());
        session.setNote(note);
        session.setMode(mode);
        session.setScheduledFor(preferredTime);
        db.sessions().add(session);
        db.save();
        return session;
    }

    public void accept(LearningSession session, String scheduledFor, String venue)
            throws RuleException {
        if (!LearningSession.PENDING.equals(session.getStatus())) {
            throw new RuleException("This request is not waiting for a reply.");
        }
        if (scheduledFor.trim().isEmpty()) {
            throw new RuleException("Set a date and time so your learner can plan.");
        }
        session.setStatus(LearningSession.ACCEPTED);
        session.setScheduledFor(scheduledFor);
        session.setVenue(venue);
        db.save();
    }

    public void reject(LearningSession session, String reason) throws RuleException {
        if (!LearningSession.PENDING.equals(session.getStatus())) {
            throw new RuleException("This request is not waiting for a reply.");
        }
        session.setStatus(LearningSession.REJECTED);
        session.setNote(reason.trim().isEmpty() ? "No reason given." : reason);
        db.save();
    }

    public void cancel(LearningSession session) throws RuleException {
        if (!session.isOpen()) throw new RuleException("This session is already closed.");
        session.setStatus(LearningSession.CANCELLED);
        db.save();
    }

    /**
     * Marks a session done and moves the credits: the learner pays the skill price,
     * the teacher receives it plus a flat teaching bonus.
     */
    public void complete(LearningSession session) throws RuleException {
        if (!LearningSession.ACCEPTED.equals(session.getStatus())) {
            throw new RuleException("Only a scheduled session can be marked complete.");
        }
        User learner = db.userById(session.getLearnerId());
        User teacher = db.userById(session.getTeacherId());
        if (learner == null || teacher == null) throw new RuleException("A participant no longer exists.");

        if (!credits.canAfford(learner, session.getCreditCost())) {
            throw new RuleException(learner.getName() + " no longer has enough credits. "
                    + "Ask them to top up by teaching, then try again.");
        }

        String title = db.skillTitle(session.getSkillId());
        credits.grant(learner, -session.getCreditCost(), CreditTransaction.LEARN,
                title + " with " + teacher.getName());
        credits.grant(teacher, session.getCreditCost(), CreditTransaction.TEACH,
                title + " for " + learner.getName());
        credits.grant(teacher, CreditRules.TEACH_BONUS, CreditTransaction.TEACH, "Teaching bonus");

        session.setStatus(LearningSession.COMPLETED);
        session.setCompletedOn(Util.now());
        db.save();
    }

    // ------------------------------------------------------------------ views

    public List<LearningSession> teaching(int teacherId) {
        return filter(s -> s.getTeacherId() == teacherId);
    }

    public List<LearningSession> learning(int learnerId) {
        return filter(s -> s.getLearnerId() == learnerId);
    }

    public List<LearningSession> pendingFor(int teacherId) {
        return filter(s -> s.getTeacherId() == teacherId
                && LearningSession.PENDING.equals(s.getStatus()));
    }

    public List<LearningSession> upcomingFor(int userId) {
        List<LearningSession> out = filter(s -> (s.getTeacherId() == userId || s.getLearnerId() == userId)
                && LearningSession.ACCEPTED.equals(s.getStatus()));
        out.sort(Comparator.comparing(LearningSession::getScheduledFor));
        return out;
    }

    public List<LearningSession> openFor(int skillId) {
        return filter(s -> s.getSkillId() == skillId && s.isOpen());
    }

    public int countTaught(int userId) {
        return filter(s -> s.getTeacherId() == userId
                && LearningSession.COMPLETED.equals(s.getStatus())).size();
    }

    public int countLearned(int userId) {
        return filter(s -> s.getLearnerId() == userId
                && LearningSession.COMPLETED.equals(s.getStatus())).size();
    }

    public List<LearningSession> completed() {
        return filter(s -> LearningSession.COMPLETED.equals(s.getStatus()));
    }

    private List<LearningSession> filter(java.util.function.Predicate<LearningSession> test) {
        List<LearningSession> out = new ArrayList<>();
        for (LearningSession s : db.sessions()) if (test.test(s)) out.add(s);
        out.sort(Comparator.comparing(LearningSession::getRequestedOn).reversed());
        return out;
    }
}
