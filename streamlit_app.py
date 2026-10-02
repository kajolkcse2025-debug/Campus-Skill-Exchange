"""Campus Skill Exchange & Credit System - Streamlit web edition.

A Python/Streamlit port of the Java Swing application in ./src. It keeps the same
domain model, the same business rules (CreditRules, SessionService, ...) and reads
the same pipe-delimited data files (./data_seed), so the numbers match the report:
11 accounts, 20 skills, 37 sessions, 105 credit transactions, 35 reviews.

Each visitor works on a private in-memory copy of the demo campus, so nothing a
visitor does affects anyone else, and refreshing the browser tab resets it.
"""
import hashlib
from datetime import datetime, date, time, timedelta
from pathlib import Path

import pandas as pd
import streamlit as st

# ----------------------------------------------------------------- constants
SEED_DIR = Path(__file__).parent / "data_seed"
TS = "%Y-%m-%d %H:%M"
CATEGORIES = ["Programming", "Design", "Languages", "Music & Dance", "Sports & Fitness",
              "Photography", "Business", "Academics", "Life Skills"]
LEVELS = ["Beginner", "Intermediate", "Advanced"]
MODES = ["In person", "Online", "Hybrid"]
DEPARTMENTS = ["Computer Science", "Information Tech", "Electronics", "Electrical", "Mechanical",
               "Civil", "Architecture", "Biotechnology", "Commerce", "Humanities"]
YEARS = ["1st Year", "2nd Year", "3rd Year", "4th Year", "Postgraduate"]

# CreditRules.java
WELCOME, TEACH_BONUS, FIVE_STAR_BONUS = 60, 5, 3
MIN_PRICE, MAX_PRICE = 5, 25

PENDING, ACCEPTED, REJECTED, COMPLETED, CANCELLED = (
    "PENDING", "ACCEPTED", "REJECTED", "COMPLETED", "CANCELLED")

FIELDS = {
    "users": ["id", "name", "email", "passwordHash", "department", "year", "bio", "role",
              "joinedOn", "credits", "active"],
    "skills": ["id", "ownerId", "title", "category", "level", "description", "creditCost",
               "active", "createdOn"],
    "sessions": ["id", "skillId", "teacherId", "learnerId", "status", "requestedOn",
                 "scheduledFor", "completedOn", "mode", "venue", "note", "creditCost"],
    "credits": ["id", "userId", "amount", "balanceAfter", "type", "description", "stamp"],
    "feedback": ["id", "sessionId", "fromUserId", "toUserId", "rating", "comment", "stamp",
                 "forTeacher"],
}
INT_FIELDS = {"id", "ownerId", "creditCost", "skillId", "teacherId", "learnerId", "userId",
              "amount", "balanceAfter", "sessionId", "fromUserId", "toUserId", "rating",
              "credits"}
BOOL_FIELDS = {"active", "forTeacher"}


class RuleError(Exception):
    """A business rule was broken; the message is safe to show to the student."""


# ----------------------------------------------------------------- utilities
def now() -> str:
    return datetime.now().strftime(TS)


def sha256(plain: str) -> str:
    return hashlib.sha256(plain.encode("utf-8")).hexdigest()


def clean(raw) -> str:
    return (raw or "").replace("|", "/").replace("\r", " ").replace("\n", " ").strip()


def parse(stamp: str) -> datetime:
    try:
        return datetime.strptime(stamp, TS)
    except (ValueError, TypeError):
        return datetime.now()


def pretty(stamp: str) -> str:
    return parse(stamp).strftime("%d %b %Y, %H:%M") if stamp else "-"


def is_email(v: str) -> bool:
    import re
    return bool(v and re.match(r"^[\w.+-]+@[\w-]+\.[\w.]{2,}$", v))


def initials(name: str) -> str:
    parts = name.split()
    if not parts:
        return "?"
    return (parts[0][0] + parts[-1][0]).upper() if len(parts) > 1 else parts[0][0].upper()


# ----------------------------------------------------------------- database
def _parse_value(field, raw):
    if field in INT_FIELDS:
        return int(raw)
    if field in BOOL_FIELDS:
        return raw.strip().lower() == "true"
    return raw


def load_seed() -> dict:
    db = {}
    names = {"users": "users.txt", "skills": "skills.txt", "sessions": "sessions.txt",
             "credits": "credits.txt", "feedback": "feedback.txt"}
    for table, fname in names.items():
        rows = []
        path = SEED_DIR / fname
        if path.exists():
            for line in path.read_text(encoding="utf-8").splitlines():
                if not line.strip():
                    continue
                parts = line.split("|")
                cols = FIELDS[table]
                if len(parts) < len(cols):
                    continue
                rows.append({c: _parse_value(c, parts[i]) for i, c in enumerate(cols)})
        db[table] = rows
    return db


def get_db() -> dict:
    if "db" not in st.session_state:
        st.session_state.db = load_seed()
    return st.session_state.db


def next_id(rows) -> int:
    return max([r["id"] for r in rows], default=0) + 1


def user_by_id(db, uid):
    return next((u for u in db["users"] if u["id"] == uid), None)


def user_by_email(db, email):
    e = (email or "").strip().lower()
    return next((u for u in db["users"] if u["email"].lower() == e), None)


def skill_by_id(db, sid):
    return next((s for s in db["skills"] if s["id"] == sid), None)


def user_name(db, uid):
    u = user_by_id(db, uid)
    return u["name"] if u else "Removed user"


def skill_title(db, sid):
    s = skill_by_id(db, sid)
    return s["title"] if s else "Removed skill"


# ----------------------------------------------------------------- services
def grant(db, user, amount, ttype, description):
    """CreditService.grant - the single place a balance can change."""
    user["credits"] = max(0, user["credits"] + amount)
    db["credits"].append({"id": next_id(db["credits"]), "userId": user["id"], "amount": amount,
                          "balanceAfter": user["credits"], "type": ttype,
                          "description": clean(description), "stamp": now()})


def earned_by(db, uid):
    return sum(t["amount"] for t in db["credits"] if t["userId"] == uid and t["amount"] > 0)


def spent_by(db, uid):
    return -sum(t["amount"] for t in db["credits"] if t["userId"] == uid and t["amount"] < 0)


def in_circulation(db):
    return sum(u["credits"] for u in db["users"] if u["role"] != "ADMIN")


def register(db, name, email, password, confirm, department, year):
    if len(name.strip()) < 3:
        raise RuleError("Enter your full name.")
    if not is_email(email):
        raise RuleError("That email address is not valid.")
    if user_by_email(db, email):
        raise RuleError("An account already uses that email.")
    if len(password) < 6:
        raise RuleError("Use at least 6 characters for the password.")
    if password != confirm:
        raise RuleError("The two passwords do not match.")
    user = {"id": next_id(db["users"]), "name": clean(name), "email": clean(email).lower(),
            "passwordHash": sha256(password), "department": department, "year": year,
            "bio": "", "role": "STUDENT", "joinedOn": now(), "credits": 0, "active": True}
    db["users"].append(user)
    grant(db, user, WELCOME, "Welcome bonus", "Joined the skill exchange")
    return user


def login(db, email, password):
    user = user_by_email(db, email)
    if not user or user["passwordHash"] != sha256(password):
        raise RuleError("Email or password is incorrect.")
    if not user["active"]:
        raise RuleError("This account is suspended. Contact Student Affairs.")
    return user


def validate_skill(title, description, cost):
    if len(title.strip()) < 3:
        raise RuleError("Give the skill a clear name.")
    if len(description.strip()) < 10:
        raise RuleError("Describe what a learner will walk away with.")
    if cost < MIN_PRICE or cost > MAX_PRICE:
        raise RuleError(f"Price a session between {MIN_PRICE} and {MAX_PRICE} credits.")


def add_skill(db, owner, title, category, level, description, cost):
    validate_skill(title, description, cost)
    for s in db["skills"]:
        if s["ownerId"] == owner["id"] and s["active"] and s["title"].lower() == title.strip().lower():
            raise RuleError("You already teach a skill with that name.")
    db["skills"].append({"id": next_id(db["skills"]), "ownerId": owner["id"],
                         "title": clean(title), "category": category, "level": level,
                         "description": clean(description), "creditCost": cost,
                         "active": True, "createdOn": now()})


def sessions_where(db, test):
    out = [s for s in db["sessions"] if test(s)]
    out.sort(key=lambda s: s["requestedOn"], reverse=True)
    return out


def is_open(s):
    return s["status"] in (PENDING, ACCEPTED)


def open_for_skill(db, sid):
    return sessions_where(db, lambda s: s["skillId"] == sid and is_open(s))


def retire_skill(db, skill):
    if open_for_skill(db, skill["id"]):
        raise RuleError("Close the open requests for this skill first.")
    skill["active"] = False


def search_skills(db, text, category, level, exclude_owner):
    q = (text or "").strip().lower()
    out = []
    for s in db["skills"]:
        if not s["active"] or s["ownerId"] == exclude_owner:
            continue
        owner = user_by_id(db, s["ownerId"])
        if not owner or not owner["active"]:
            continue
        if category != "All" and s["category"] != category:
            continue
        if level != "All" and s["level"] != level:
            continue
        if q and q not in f'{s["title"]} {s["description"]} {s["category"]} {owner["name"]}'.lower():
            continue
        out.append(s)
    out.sort(key=lambda s: s["title"])
    return out


def request_session(db, learner, skill, note, mode, preferred):
    if skill["ownerId"] == learner["id"]:
        raise RuleError("This is your own skill.")
    if not skill["active"]:
        raise RuleError("That skill is no longer offered.")
    if learner["credits"] < skill["creditCost"]:
        raise RuleError(f'You need {skill["creditCost"]} credits for this session. '
                        "Teach something to earn more.")
    for s in db["sessions"]:
        if s["learnerId"] == learner["id"] and s["skillId"] == skill["id"] and is_open(s):
            raise RuleError("You already have an open request for this skill.")
    db["sessions"].append({"id": next_id(db["sessions"]), "skillId": skill["id"],
                           "teacherId": skill["ownerId"], "learnerId": learner["id"],
                           "status": PENDING, "requestedOn": now(), "scheduledFor": clean(preferred),
                           "completedOn": "", "mode": mode, "venue": "", "note": clean(note),
                           "creditCost": skill["creditCost"]})


def accept_session(s, scheduled_for, venue):
    if s["status"] != PENDING:
        raise RuleError("This request is not waiting for a reply.")
    if not scheduled_for.strip():
        raise RuleError("Set a date and time so your learner can plan.")
    s.update(status=ACCEPTED, scheduledFor=scheduled_for, venue=clean(venue))


def reject_session(s, reason):
    if s["status"] != PENDING:
        raise RuleError("This request is not waiting for a reply.")
    s.update(status=REJECTED, note=clean(reason) or "No reason given.")


def cancel_session(s):
    if not is_open(s):
        raise RuleError("This session is already closed.")
    s["status"] = CANCELLED


def complete_session(db, s):
    if s["status"] != ACCEPTED:
        raise RuleError("Only a scheduled session can be marked complete.")
    learner, teacher = user_by_id(db, s["learnerId"]), user_by_id(db, s["teacherId"])
    if not learner or not teacher:
        raise RuleError("A participant no longer exists.")
    if learner["credits"] < s["creditCost"]:
        raise RuleError(f'{learner["name"]} no longer has enough credits. '
                        "Ask them to top up by teaching, then try again.")
    title = skill_title(db, s["skillId"])
    grant(db, learner, -s["creditCost"], "Joined a session", f'{title} with {teacher["name"]}')
    grant(db, teacher, s["creditCost"], "Taught a session", f'{title} for {learner["name"]}')
    grant(db, teacher, TEACH_BONUS, "Taught a session", "Teaching bonus")
    s.update(status=COMPLETED, completedOn=now())


def count_taught(db, uid):
    return len(sessions_where(db, lambda s: s["teacherId"] == uid and s["status"] == COMPLETED))


def count_learned(db, uid):
    return len(sessions_where(db, lambda s: s["learnerId"] == uid and s["status"] == COMPLETED))


def already_reviewed(db, sid, uid):
    return any(f["sessionId"] == sid and f["fromUserId"] == uid for f in db["feedback"])


def submit_feedback(db, s, user, rating, comment):
    if s["status"] != COMPLETED:
        raise RuleError("You can review a session once it is complete.")
    if already_reviewed(db, s["id"], user["id"]):
        raise RuleError("You have already reviewed this session.")
    from_learner = s["learnerId"] == user["id"]
    if not from_learner and s["teacherId"] != user["id"]:
        raise RuleError("You were not part of this session.")
    to_id = s["teacherId"] if from_learner else s["learnerId"]
    db["feedback"].append({"id": next_id(db["feedback"]), "sessionId": s["id"],
                           "fromUserId": user["id"], "toUserId": to_id,
                           "rating": max(1, min(5, rating)), "comment": clean(comment),
                           "stamp": now(), "forTeacher": from_learner})
    if from_learner and rating == 5:
        teacher = user_by_id(db, to_id)
        if teacher:
            grant(db, teacher, FIVE_STAR_BONUS, "Rating bonus", f'Five star review from {user["name"]}')


def teacher_rating(db, uid):
    r = [f["rating"] for f in db["feedback"] if f["toUserId"] == uid and f["forTeacher"]]
    return sum(r) / len(r) if r else 0.0


def review_count(db, uid):
    return sum(1 for f in db["feedback"] if f["toUserId"] == uid and f["forTeacher"])


def skill_rating(db, sid):
    r = []
    for f in db["feedback"]:
        if not f["forTeacher"]:
            continue
        s = next((x for x in db["sessions"] if x["id"] == f["sessionId"]), None)
        if s and s["skillId"] == sid:
            r.append(f["rating"])
    return sum(r) / len(r) if r else 0.0


def ranking(db, this_month_only=False):
    month_start = date.today().replace(day=1)
    rows = []
    for u in db["users"]:
        if u["role"] == "ADMIN" or not u["active"]:
            continue
        rating = teacher_rating(db, u["id"])
        if this_month_only:
            earned = sum(t["amount"] for t in db["credits"] if t["userId"] == u["id"]
                         and t["amount"] > 0 and parse(t["stamp"]).date() >= month_start)
            done = [s for s in db["sessions"] if s["status"] == COMPLETED
                    and parse(s["completedOn"]).date() >= month_start]
            taught = sum(1 for s in done if s["teacherId"] == u["id"])
            learned = sum(1 for s in done if s["learnerId"] == u["id"])
        else:
            earned, taught, learned = earned_by(db, u["id"]), count_taught(db, u["id"]), count_learned(db, u["id"])
        score = taught * 20 + learned * 5 + round(rating * 10) + earned // 2
        rows.append({"user": u, "earned": earned, "taught": taught, "learned": learned,
                     "rating": rating, "score": score})
    rows.sort(key=lambda r: (-r["score"], r["user"]["name"]))
    for i, r in enumerate(rows, 1):
        r["rank"] = i
    return rows


def rank_of(db, uid):
    return next((r["rank"] for r in ranking(db) if r["user"]["id"] == uid), 0)


def badges_for(db, user):
    uid = user["id"]
    taught, learned = count_taught(db, uid), count_learned(db, uid)
    offered = sum(1 for s in db["skills"] if s["ownerId"] == uid and s["active"])
    rating, reviews = teacher_rating(db, uid), review_count(db, uid)
    earned, rank = earned_by(db, uid), rank_of(db, uid)
    return [
        ("First Session", "Teach your first session", taught >= 1, min(1, taught / 1)),
        ("Mentor", "Teach 5 sessions", taught >= 5, min(1, taught / 5)),
        ("Skill Master", "Teach 10 sessions at 4.5 stars or better", taught >= 10 and rating >= 4.5, min(1, taught / 10)),
        ("Curious Mind", "Complete 3 sessions as a learner", learned >= 3, min(1, learned / 3)),
        ("Multi-talented", "Offer 3 skills at once", offered >= 3, min(1, offered / 3)),
        ("Crowd Favourite", "Hold a 4.8 rating across 5 reviews", reviews >= 5 and rating >= 4.8,
         0 if reviews == 0 else min(1, reviews / 5) * (rating / 5)),
        ("Credit Magnet", "Earn 150 credits in total", earned >= 150, min(1, earned / 150)),
        ("Top Contributor", "Finish in the campus top 3", 0 < rank <= 3, 0 if rank == 0 else min(1, 3 / rank)),
    ]


# ----------------------------------------------------------------- UI helpers
CSS = """
<style>
.block-container {padding-top: 2rem; max-width: 1200px;}
.hero {background: linear-gradient(135deg,#0e3d7a 0%,#16a0b2 100%); color:white;
       padding: 1.4rem 1.6rem; border-radius: 16px; margin-bottom: 1rem;}
.hero h1 {margin:0; font-size: 1.7rem;} .hero p {margin:.2rem 0 0 0; opacity:.9;}
.card {border:1px solid #cfe3f3; background:#f4faff; border-radius:14px; padding:.9rem 1.1rem;
       margin-bottom:.7rem;}
.card h4 {margin:0 0 .15rem 0; color:#0e3d7a;}
.pill {display:inline-block; padding:1px 10px; border-radius:999px; font-size:.75rem;
       background:#0e3d7a; color:#fff; margin-right:6px;}
.pill.teal {background:#16a0b2;} .pill.gold {background:#d99a14;} .pill.gray {background:#6b7785;}
.pill.green {background:#2eaa6e;} .pill.red {background:#d64545;}
.muted {color:#5a626e; font-size:.9rem;}
</style>
"""

STATUS_PILL = {PENDING: "gold", ACCEPTED: "teal", COMPLETED: "green", REJECTED: "red", CANCELLED: "gray"}


def pill(text, kind=""):
    return f'<span class="pill {kind}">{text}</span>'


def stars(n):
    n = int(round(n))
    return "\u2605" * n + "\u2606" * (5 - n)


def flash():
    """Show a one-shot message saved before a rerun."""
    msg = st.session_state.pop("flash", None)
    if msg:
        kind, text = msg
        (st.success if kind == "ok" else st.error)(text)


def set_flash(kind, text):
    st.session_state.flash = (kind, text)


def run_action(fn, ok_message):
    """Run a service call, remember its result as a flash message, then rerun."""
    try:
        fn()
        set_flash("ok", ok_message)
    except RuleError as e:
        set_flash("err", str(e))
    st.rerun()


# ----------------------------------------------------------------- pages
def page_login(db):
    st.markdown('<div class="hero"><h1>\U0001F393 Campus Skill Exchange</h1>'
                "<p>Teach what you know, earn credits, spend them to learn something new. "
                "No money \u2013 only time.</p></div>", unsafe_allow_html=True)
    flash()
    tab_in, tab_up = st.tabs(["Sign in", "Create account"])
    with tab_in:
        email = st.text_input("Email", key="li_email", placeholder="aarav@campus.edu")
        pw = st.text_input("Password", type="password", key="li_pw")
        if st.button("Sign in", type="primary"):
            try:
                st.session_state.uid = login(db, email, pw)["id"]
                st.rerun()
            except RuleError as e:
                st.error(str(e))
        st.info("**Demo logins** \u2013 Student: `aarav@campus.edu` / `pass123` \u00b7 "
                "Other students: `divya@campus.edu`, `rahul@campus.edu` \u2026 / `pass123` \u00b7 "
                "Administrator: `admin@campus.edu` / `admin123`")
    with tab_up:
        name = st.text_input("Full name", key="su_name")
        email2 = st.text_input("Email", key="su_email")
        c1, c2 = st.columns(2)
        dept = c1.selectbox("Department", DEPARTMENTS, key="su_dept")
        year = c2.selectbox("Year", YEARS, key="su_year")
        p1 = c1.text_input("Password (min 6 characters)", type="password", key="su_p1")
        p2 = c2.text_input("Confirm password", type="password", key="su_p2")
        if st.button("Create account"):
            try:
                u = register(db, name, email2, p1, p2, dept, year)
                st.session_state.uid = u["id"]
                set_flash("ok", f"Welcome, {u['name']}! You start with {WELCOME} credits.")
                st.rerun()
            except RuleError as e:
                st.error(str(e))


def page_dashboard(db, me):
    st.markdown(f'<div class="hero"><h1>Hello, {me["name"].split()[0]} \U0001F44B</h1>'
                f'<p>{me["department"]} \u00b7 {me["year"]}</p></div>', unsafe_allow_html=True)
    flash()
    rating = teacher_rating(db, me["id"])
    c = st.columns(5)
    c[0].metric("Credits", me["credits"])
    c[1].metric("Sessions taught", count_taught(db, me["id"]))
    c[2].metric("Sessions attended", count_learned(db, me["id"]))
    c[3].metric("Teaching rating", f"{rating:.1f}" if rating else "-")
    c[4].metric("Campus rank", f"#{rank_of(db, me['id'])}")
    pend = sessions_where(db, lambda s: s["teacherId"] == me["id"] and s["status"] == PENDING)
    if pend:
        st.warning(f"{len(pend)} student(s) are waiting for you to accept a session \u2013 see **Sessions**.")
    left, right = st.columns(2)
    with left:
        st.subheader("Next up")
        up = sessions_where(db, lambda s: me["id"] in (s["teacherId"], s["learnerId"]) and s["status"] == ACCEPTED)
        up.sort(key=lambda s: s["scheduledFor"])
        if not up:
            st.caption("Nothing scheduled yet.")
        for s in up:
            teaching = s["teacherId"] == me["id"]
            other = user_name(db, s["learnerId"] if teaching else s["teacherId"])
            st.markdown(f'<div class="card"><h4>{skill_title(db, s["skillId"])}</h4>'
                        f'{pill("Teaching" if teaching else "Learning", "teal" if not teaching else "")}'
                        f'<span class="muted">{"with " + other} \u00b7 {s["scheduledFor"]} \u00b7 {s["mode"]}</span></div>',
                        unsafe_allow_html=True)
    with right:
        st.subheader("What campus is learning")
        counts = {}
        for s in db["sessions"]:
            if s["status"] == COMPLETED:
                t = skill_title(db, s["skillId"])
                counts[t] = counts.get(t, 0) + 1
        top = sorted(counts.items(), key=lambda kv: -kv[1])[:6]
        if top:
            st.bar_chart(pd.DataFrame(top, columns=["Skill", "Sessions"]).set_index("Skill"))
    st.subheader("Achievements")
    cols = st.columns(4)
    for i, (name, req, earned, prog) in enumerate(badges_for(db, me)):
        with cols[i % 4]:
            icon = "\U0001F3C5" if earned else "\U0001F512"
            st.markdown(f'<div class="card"><h4>{icon} {name}</h4>'
                        f'<span class="muted">{req}</span></div>', unsafe_allow_html=True)
            st.progress(float(prog))


def page_browse(db, me):
    st.header("Browse skills")
    flash()
    c1, c2, c3 = st.columns([3, 2, 2])
    text = c1.text_input("Search", placeholder="python, guitar, French \u2026")
    cat = c2.selectbox("Category", ["All"] + CATEGORIES)
    lvl = c3.selectbox("Level", ["All"] + LEVELS)
    results = search_skills(db, text, cat, lvl, me["id"])
    st.caption(f"{len(results)} skill(s) found")
    for s in results:
        owner = user_by_id(db, s["ownerId"])
        r = skill_rating(db, s["id"])
        rated = f"{stars(r)} {r:.1f}" if r else "not rated yet"
        with st.container(border=True):
            a, b = st.columns([4, 1])
            a.markdown(f'**{s["title"]}**  \n'
                       f'{pill(s["category"])}{pill(s["level"], "teal")}'
                       f'<span class="muted">by {owner["name"]} \u00b7 {rated}</span>',
                       unsafe_allow_html=True)
            a.write(s["description"])
            b.metric("Cost", f'{s["creditCost"]} cr')
            with st.expander("Request a session"):
                mode = st.selectbox("Mode", MODES, key=f"m{s['id']}")
                pref = st.text_input("Preferred time", key=f"t{s['id']}", placeholder="Weekdays after 4pm")
                note = st.text_area("Note to the teacher", key=f"n{s['id']}", height=70)
                if st.button("Send request", key=f"r{s['id']}", type="primary"):
                    run_action(lambda: request_session(db, me, s, note, mode, pref),
                               f'Request sent to {owner["name"]}.')


def page_myskills(db, me):
    st.header("My skills")
    flash()
    mine = [s for s in db["skills"] if s["ownerId"] == me["id"]]
    for s in mine:
        with st.container(border=True):
            a, b = st.columns([4, 1])
            a.markdown(f'**{s["title"]}**  {pill(s["category"])}{pill(s["level"], "teal")}'
                       f'{pill("Active", "green") if s["active"] else pill("Retired", "gray")}',
                       unsafe_allow_html=True)
            a.write(s["description"])
            a.caption(f'Taught {len([x for x in db["sessions"] if x["skillId"] == s["id"] and x["status"] == COMPLETED])} time(s)'
                      f' \u00b7 {s["creditCost"]} credits per session')
            if s["active"]:
                if b.button("Retire", key=f"ret{s['id']}"):
                    run_action(lambda: retire_skill(db, s), "Skill retired.")
            elif b.button("Re-offer", key=f"res{s['id']}"):
                s["active"] = True
                set_flash("ok", "Skill is being offered again.")
                st.rerun()
    if not mine:
        st.caption("You are not teaching anything yet.")
    st.subheader("Offer a new skill")
    title = st.text_input("Title", key="ns_t")
    c1, c2, c3 = st.columns(3)
    cat = c1.selectbox("Category", CATEGORIES, key="ns_c")
    lvl = c2.selectbox("Level", LEVELS, key="ns_l")
    cost = c3.number_input("Credits per session", MIN_PRICE, MAX_PRICE, 10, key="ns_p")
    desc = st.text_area("What will a learner walk away with?", key="ns_d", height=80)
    if st.button("Add skill", type="primary"):
        run_action(lambda: add_skill(db, me, title, cat, lvl, desc, int(cost)), "Skill added.")


def session_card(db, me, s):
    teaching = s["teacherId"] == me["id"]
    other = user_name(db, s["learnerId"] if teaching else s["teacherId"])
    with st.container(border=True):
        st.markdown(f'**{skill_title(db, s["skillId"])}** '
                    f'{pill(s["status"].title(), STATUS_PILL[s["status"]])}'
                    f'{pill("Teaching" if teaching else "Learning", "gray")}  \n'
                    f'<span class="muted">{"Learner" if teaching else "Teacher"}: {other} \u00b7 '
                    f'{s["creditCost"]} credits \u00b7 {s["mode"]} \u00b7 requested {pretty(s["requestedOn"])}</span>',
                    unsafe_allow_html=True)
        if s["scheduledFor"]:
            st.caption(f'When: {s["scheduledFor"]}' + (f' \u00b7 Where: {s["venue"]}' if s["venue"] else ""))
        if s["note"]:
            st.caption(f'Note: {s["note"]}')
        k = f's{s["id"]}'
        if s["status"] == PENDING and teaching:
            c1, c2 = st.columns(2)
            d = c1.date_input("Date", date.today() + timedelta(days=2), key=k + "d")
            t = c2.time_input("Time", time(16, 0), key=k + "t")
            venue = st.text_input("Venue / link", key=k + "v", placeholder="Central Library, Room 3")
            b1, b2, _ = st.columns([1, 1, 4])
            if b1.button("Accept", key=k + "a", type="primary"):
                run_action(lambda: accept_session(s, datetime.combine(d, t).strftime(TS), venue),
                           "Session scheduled.")
            if b2.button("Decline", key=k + "x"):
                run_action(lambda: reject_session(s, ""), "Request declined.")
        elif s["status"] == ACCEPTED:
            b1, b2, _ = st.columns([1.4, 1, 4])
            if teaching and b1.button("Mark complete", key=k + "c", type="primary"):
                run_action(lambda: complete_session(db, s), "Session completed and credits transferred.")
            if b2.button("Cancel", key=k + "k"):
                run_action(lambda: cancel_session(s), "Session cancelled.")
        elif s["status"] == PENDING and not teaching:
            if st.button("Cancel request", key=k + "k"):
                run_action(lambda: cancel_session(s), "Request cancelled.")
        elif s["status"] == COMPLETED and not already_reviewed(db, s["id"], me["id"]):
            with st.expander("Leave a review"):
                rating = st.slider("Rating", 1, 5, 5, key=k + "rt")
                comment = st.text_input("Comment", key=k + "cm")
                if st.button("Submit review", key=k + "sr"):
                    run_action(lambda: submit_feedback(db, s, me, rating, comment), "Thanks for your review!")


def page_sessions(db, me):
    st.header("Sessions")
    flash()
    t1, t2 = st.tabs(["Teaching", "Learning"])
    with t1:
        rows = sessions_where(db, lambda s: s["teacherId"] == me["id"])
        st.caption(f"{len(rows)} session(s)") if rows else st.caption("No one has asked to learn from you yet.")
        for s in rows:
            session_card(db, me, s)
    with t2:
        rows = sessions_where(db, lambda s: s["learnerId"] == me["id"])
        st.caption(f"{len(rows)} session(s)") if rows else st.caption("Browse skills to request your first session.")
        for s in rows:
            session_card(db, me, s)


def page_credits(db, me):
    st.header("Credit passbook")
    c = st.columns(3)
    c[0].metric("Balance", f'{me["credits"]} credits')
    c[1].metric("Earned", earned_by(db, me["id"]))
    c[2].metric("Spent", spent_by(db, me["id"]))
    hist = sorted([t for t in db["credits"] if t["userId"] == me["id"]], key=lambda t: t["stamp"])
    if hist:
        st.line_chart(pd.DataFrame({"Balance": [t["balanceAfter"] for t in hist]},
                                   index=[pretty(t["stamp"]) for t in hist]))
        df = pd.DataFrame([{"When": pretty(t["stamp"]), "Type": t["type"], "Details": t["description"],
                            "Amount": f'{t["amount"]:+d}', "Balance": t["balanceAfter"]}
                           for t in reversed(hist)])
        st.dataframe(df, width="stretch", hide_index=True)
    st.caption(f"Rules: welcome bonus {WELCOME} \u00b7 teaching bonus +{TEACH_BONUS} \u00b7 "
               f"five-star review +{FIVE_STAR_BONUS} \u00b7 session price {MIN_PRICE}\u2013{MAX_PRICE} credits.")


def page_leaderboard(db, me):
    st.header("Campus leaderboard")
    scope = st.radio("Show", ["All time", "This month"], horizontal=True)
    rows = ranking(db, scope == "This month")
    medals = {1: "\U0001F947", 2: "\U0001F948", 3: "\U0001F949"}
    top = st.columns(3)
    for col, r in zip(top, rows[:3]):
        col.markdown(f'<div class="card" style="text-align:center"><h2 style="margin:0">{medals[r["rank"]]}</h2>'
                     f'<h4>{r["user"]["name"]}</h4><span class="muted">{r["score"]} pts \u00b7 '
                     f'taught {r["taught"]}</span></div>', unsafe_allow_html=True)
    df = pd.DataFrame([{"Rank": r["rank"], "Student": r["user"]["name"], "Department": r["user"]["department"],
                        "Taught": r["taught"], "Learned": r["learned"],
                        "Rating": round(r["rating"], 1) if r["rating"] else None,
                        "Credits earned": r["earned"], "Score": r["score"]} for r in rows])
    st.dataframe(df, width="stretch", hide_index=True)
    st.caption("Score = taught \u00d7 20 + learned \u00d7 5 + rating \u00d7 10 + credits earned \u00f7 2")


def page_profile(db, me):
    st.header("Profile")
    flash()
    c1, c2 = st.columns(2)
    name = c1.text_input("Full name", me["name"])
    dept = c2.selectbox("Department", DEPARTMENTS,
                        index=DEPARTMENTS.index(me["department"]) if me["department"] in DEPARTMENTS else 0)
    year_opts = YEARS + ["Staff"]
    year = c1.selectbox("Year", year_opts, index=year_opts.index(me["year"]) if me["year"] in year_opts else 0)
    bio = st.text_area("Bio", me["bio"], height=80)
    if st.button("Save profile", type="primary"):
        def save():
            if len(name.strip()) < 3:
                raise RuleError("Enter your full name.")
            me.update(name=clean(name), department=dept, year=year, bio=clean(bio))
        run_action(save, "Profile saved.")
    st.subheader("Change password")
    cur = st.text_input("Current password", type="password")
    n1 = st.text_input("New password", type="password")
    n2 = st.text_input("Confirm new password", type="password")
    if st.button("Change password"):
        def change():
            if me["passwordHash"] != sha256(cur):
                raise RuleError("Your current password is wrong.")
            if len(n1) < 6:
                raise RuleError("Use at least 6 characters for the new password.")
            if n1 != n2:
                raise RuleError("The two new passwords do not match.")
            me["passwordHash"] = sha256(n1)
        run_action(change, "Password changed.")


def table_report(db, kind):
    students = [u for u in db["users"] if u["role"] != "ADMIN"]
    if kind == "activity":
        rows = [{"Student": u["name"], "Department": u["department"], "Taught": count_taught(db, u["id"]),
                 "Learned": count_learned(db, u["id"]),
                 "Rating": round(teacher_rating(db, u["id"]), 1) or None, "Credits": u["credits"]}
                for u in students]
        return pd.DataFrame(rows).sort_values("Taught", ascending=False)
    if kind == "credits":
        rows = [{"Student": u["name"], "Earned": earned_by(db, u["id"]), "Spent": spent_by(db, u["id"]),
                 "Balance": u["credits"]} for u in students]
        return pd.DataFrame(rows).sort_values("Earned", ascending=False)
    pop = {}
    for s in db["sessions"]:
        if s["status"] == COMPLETED:
            pop[s["skillId"]] = pop.get(s["skillId"], 0) + 1
    rows = [{"Skill": s["title"], "Category": s["category"], "Sessions": pop.get(s["id"], 0),
             "Rating": round(skill_rating(db, s["id"]), 1) or None} for s in db["skills"]]
    return pd.DataFrame(rows).sort_values("Sessions", ascending=False)


def page_admin(db, me):
    st.header("Administrator console")
    flash()
    students = [u for u in db["users"] if u["role"] != "ADMIN"]
    c = st.columns(5)
    c[0].metric("Students", len(students))
    c[1].metric("Skills", len(db["skills"]))
    c[2].metric("Sessions", len(db["sessions"]))
    c[3].metric("Transactions", len(db["credits"]))
    c[4].metric("Credits in circulation", in_circulation(db))
    negative = [u for u in students if u["credits"] < 0]
    st.success("Ledger check passed: no account has a negative balance.") if not negative else st.error("Negative balance found!")

    t_rep, t_stu, t_sk = st.tabs(["Reports", "Students", "Skills"])
    with t_rep:
        kind = st.radio("Report", ["activity", "credits", "popular"], horizontal=True,
                        format_func=lambda k: {"activity": "User activity", "credits": "Credits",
                                               "popular": "Popular skills"}[k])
        df = table_report(db, kind)
        st.dataframe(df, width="stretch", hide_index=True)
        st.download_button("Download CSV", df.to_csv(index=False), f"{kind}_report.csv", "text/csv")
        cat = {}
        for s in db["sessions"]:
            if s["status"] == COMPLETED:
                sk = skill_by_id(db, s["skillId"])
                if sk:
                    cat[sk["category"]] = cat.get(sk["category"], 0) + 1
        st.subheader("Completed sessions by category")
        st.bar_chart(pd.Series(cat, name="Sessions"))
    with t_stu:
        st.dataframe(pd.DataFrame([{"Name": u["name"], "Email": u["email"], "Department": u["department"],
                                    "Credits": u["credits"], "Status": "Active" if u["active"] else "Suspended"}
                                   for u in students]), width="stretch", hide_index=True)
        who = st.selectbox("Student", students, format_func=lambda u: u["name"])
        a, b = st.columns(2)
        with a:
            amt = st.number_input("Adjust credits by", -100, 100, 0)
            why = st.text_input("Reason", "Admin adjustment")
            if st.button("Apply adjustment"):
                def adj():
                    if amt == 0:
                        raise RuleError("Enter a non-zero amount.")
                    grant(db, who, int(amt), "Admin adjustment", why)
                run_action(adj, "Credits updated.")
        with b:
            if st.button("Restore account" if not who["active"] else "Suspend account"):
                who["active"] = not who["active"]
                set_flash("ok", "Account updated.")
                st.rerun()
    with t_sk:
        st.dataframe(pd.DataFrame([{"Skill": s["title"], "Owner": user_name(db, s["ownerId"]),
                                    "Category": s["category"], "Level": s["level"],
                                    "Credits": s["creditCost"], "Active": s["active"]}
                                   for s in db["skills"]]), width="stretch", hide_index=True)
        sk = st.selectbox("Skill", db["skills"], format_func=lambda s: s["title"])
        if st.button("Restore skill" if not sk["active"] else "Retire skill"):
            if sk["active"]:
                run_action(lambda: retire_skill(db, sk), "Skill retired.")
            else:
                sk["active"] = True
                set_flash("ok", "Skill restored.")
                st.rerun()


# ----------------------------------------------------------------- main
def main():
    st.set_page_config(page_title="Campus Skill Exchange", page_icon="\U0001F393", layout="wide")
    st.markdown(CSS, unsafe_allow_html=True)
    db = get_db()
    me = user_by_id(db, st.session_state.get("uid", -1))
    if not me:
        page_login(db)
        return

    pages = {"Dashboard": page_dashboard, "Browse skills": page_browse, "My skills": page_myskills,
             "Sessions": page_sessions, "Credits": page_credits, "Leaderboard": page_leaderboard,
             "Profile": page_profile}
    if me["role"] == "ADMIN":
        pages = {"Admin console": page_admin, "Leaderboard": page_leaderboard, "Profile": page_profile}
    with st.sidebar:
        st.markdown(f"### \U0001F393 Skill Exchange\n**{me['name']}**  \n{me['department']}")
        if me["role"] != "ADMIN":
            st.metric("Credits", me["credits"])
        choice = st.radio("Go to", list(pages), label_visibility="collapsed")
        st.divider()
        if st.button("Sign out"):
            st.session_state.pop("uid", None)
            st.rerun()
        if st.button("Reset demo data"):
            st.session_state.pop("db", None)
            st.session_state.pop("uid", None)
            st.rerun()
        st.caption("Demo mode: your changes live only in this browser session.")
    pages[choice](db, me)


main()
