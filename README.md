# Campus Skill Exchange & Credit System

A desktop application where students on a campus teach each other. Every hour you
teach earns credits, and those credits are what you spend to learn something from
someone else. No money changes hands, only time.

Written in plain **Java 21 + Swing**. No external libraries, no database server,
no build tool required.

---

## Running it

```
javac -d out $(find src -name "*.java")     # compile
java -cp out Main                            # run
```

Or use the helper scripts:

* Linux / macOS: `sh run.sh`
* Windows: `run.bat`

A headless check of the service layer (no window opens):

```
java -cp out Main --selftest
```

The first run creates a `data/` folder and fills it with a demo campus:
11 accounts, 20 skills, 37 sessions and the credit history behind them.
Delete `data/` to start again from the seed.

### Demo logins

| Account | Email | Password |
|---|---|---|
| Student (top of the leaderboard) | `aarav@campus.edu` | `pass123` |
| Any other student | `divya@campus.edu`, `rahul@campus.edu`, … | `pass123` |
| Administrator | `admin@campus.edu` | `admin123` |

---

## The eight modules

| Module | Where it lives | What it does |
|---|---|---|
| User management | `service/AuthService`, `ui/LoginFrame`, `ui/ProfilePanel` | Registration, sign in, profile editing, password change |
| Skill management | `service/SkillService`, `ui/MySkillsPanel`, `ui/BrowsePanel` | Offer a skill, edit it, retire or re-offer it, search by text, category and level |
| Learning sessions | `service/SessionService`, `ui/SessionsPanel` | Request, accept with a date and venue, decline, cancel, mark complete |
| Credit system | `service/CreditService`, `service/CreditRules`, `ui/CreditsPanel` | Earning, spending, refunds and a full passbook of every movement |
| Feedback & rating | `service/FeedbackService` | Star reviews in both directions, teacher averages, per-skill ratings |
| Leaderboard & achievements | `service/LeaderboardService`, `ui/LeaderboardPanel` | All-time and monthly ranking, podium, eight unlockable badges |
| Reports | `service/ReportService`, `ui/ReportsPanel` | User activity, credit and popular-skill reports, saved to `reports/` |
| Database | `data/Database`, `data/Seed` | Loads and saves every entity, hands out ids, seeds a demo campus |

---

## How credits work

| Event | Credits |
|---|---|
| Joining the platform | **+60** welcome bonus |
| Teaching a session | **+** the price the teacher set (5 to 25) |
| Teaching bonus, every completed session | **+5** |
| Receiving a five star review | **+3** |
| Attending a session | **−** the price of that skill |
| A scheduled session is cancelled | full refund to the learner |

A learner cannot send a request they cannot afford, and the charge only happens
when the session is actually marked complete, so nobody pays for a session that
never happened.

## Badges

First Session, Mentor, Skill Master, Curious Mind, Multi-talented,
Crowd Favourite, Credit Magnet and Top Contributor. Badges are never stored —
they are recalculated from the records every time a page is opened, so they can
never drift out of step with reality.

---

## Design of the code

```
src/
  Main.java              entry point and --selftest
  util/Util              hashing, date handling, small helpers
  model/                 User, Skill, LearningSession, CreditTransaction, Feedback, Badge
  data/Database          in-memory lists backed by pipe-delimited text files
  data/Seed              the demo campus
  service/               all the rules: auth, skills, sessions, credits, feedback,
                         leaderboard, reports
  ui/                    Swing screens, the theme and the hand-painted charts
```

Three points worth making in a viva:

1. **The UI never touches the rules.** Panels call services; services validate and
   then write to the database. Anything a user could reasonably get wrong is
   raised as `AuthService.RuleException` and shown as a readable message.
2. **Storage is isolated.** Only `Database.load()` and `Database.save()` know that
   persistence is a text file. Swapping in JDBC means rewriting those two methods
   and nothing else. The file format is one record per line with `|` separators;
   the separator is stripped from user input on the way in.
3. **Nothing is drawn from an image file.** The ring gauge, bar and column charts,
   badges, avatars, medals, icons and the login artwork are all painted with
   `Graphics2D`, so the project is a single self-contained source tree.

### Credit integrity

Every change to a balance goes through `CreditService.grant`, which writes a
`CreditTransaction` recording the amount and the balance immediately after.
The passbook therefore reconstructs any balance at any point in time, and the
self test verifies no account can go negative.

---

## Screenshots

See the `screenshots/` folder: login, dashboard, browse, my skills, sessions,
credits, leaderboard, profile and the two administrator pages.
