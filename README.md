# CourseReg — An Online Course Registration System

A 3-role course registration system that grew into a small LMS: **students**
browse the catalog, request to enroll/drop, work through course content and
quizzes, earn certificates and badges, complete side quests, and get course
recommendations toward a career path. An **admin** manages the course
catalog, lesson content, quizzes, side quests, and career paths. A
**registrar** approves or rejects every enroll/drop request and is the only
one who can remove a course.

Java + Spring Boot backend using hand-written JDBC (no JPA/Hibernate), MySQL,
and a plain HTML/CSS/JS frontend (no framework, no build step).

## Why it's built this way

- **Spring Boot**, not plain Servlets/JSP — modern, in-demand, and gives us
  REST + Spring Security for free.
- **Raw JDBC**, not Spring Data JPA — every query in `dao/` is a hand-written
  `Connection` / `PreparedStatement` / `ResultSet`, using Spring Boot's
  auto-configured (HikariCP-pooled) `DataSource`.
- **Seat limits are enforced with real transactions**, not just a counter
  column. When the registrar *approves* an enrollment, `EnrollmentService`
  opens a transaction, takes a row lock on the course (`SELECT ... FOR
  UPDATE`), re-checks the seat count, and only then increments it — so two
  concurrent approvals for the last seat can't both succeed.
- **Certificate eligibility and admin analytics share one rule**, computed in
  `ProgressService`, so "what counts as done" can never drift between what
  the admin sees and what actually unlocks a certificate.
- **Badges and side quests deliberately don't duplicate progress tracking.**
  Badges are computed on the fly from data that already exists (no "badges"
  table). Side quests are the one genuinely separate, lightweight system —
  self-reported by the student, worth points, no grading — because they're
  meant for pure engagement, not verified completion.
- **Vanilla JS frontend**, not React — no build step; open any `.js` file and
  read it top to bottom. Icons are small inline SVGs (see `ICONS` in
  `js/api.js`), not a CDN icon font, so nothing can fail to load and the app
  still works offline.

## Project layout

```
database/    — schema + one migration file per feature, run in order (see below)
backend/     — Spring Boot Maven project (raw JDBC throughout)
frontend/    — static HTML/CSS/JS, talks to the backend over REST
```

## Prerequisites

- JDK 17+ (including current JDKs like 21 or 25)
- Maven 3.8+
- MySQL 8+ running locally

> **Spring Boot version note.** This targets **Spring Boot 4.1.0**, which
> officially supports Java 17 through 26.

## Setup

**1. Create the database — run every file in this order:**

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed_data.sql
mysql -u root -p < database/migration_content.sql
mysql -u root -p < database/migration_quiz.sql
mysql -u root -p < database/migration_progress_certificates.sql
mysql -u root -p < database/migration_career_paths.sql
mysql -u root -p < database/migration_side_quests.sql
```
(On Windows PowerShell, replace `mysql ... < file.sql` with
`Get-Content file.sql | & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p`
— PowerShell doesn't support `<` file redirection.)

**2. Point the backend at your MySQL**

Edit `backend/src/main/resources/application.properties` — update
`spring.datasource.username` / `password` to match your local MySQL, and
change `jwt.secret` to something real (`openssl rand -base64 32`).

**3. Run the backend**

```bash
cd backend
mvn spring-boot:run
```

It starts on `http://localhost:8080`.

**4. Serve the frontend** (don't just double-click `index.html` — serve it,
so `fetch()` behaves consistently):

```bash
cd frontend
python3 -m http.server 5500
```

Then open `http://localhost:5500`. If your backend isn't on
`localhost:8080`, update `API_BASE` at the top of `frontend/js/api.js`.

## Logging in

| Username     | Password       | Role      |
|--------------|----------------|-----------|
| `admin`      | `admin123`     | ADMIN     |
| `registrar`  | `registrar123` | REGISTRAR |

Students self-register from the "New student" tab on the login page — public
sign-up only ever creates a STUDENT account.

## Feature tour

**Enrollment workflow** — the core registration flow:
```
Student requests to enroll  --> PENDING  --> Registrar approves  --> APPROVED (seat held)
                                      \--------> Registrar rejects  --> REJECTED

Student (already APPROVED)
  requests to drop            --> DROP_PENDING --> Registrar approves --> DROPPED (seat released)
                                          \-----------> Registrar rejects --> DROP_REJECTED (stays enrolled)
```
Removing a course (registrar-only) auto-drops any pending/approved
enrollments in it.

**Course content** — admin adds lessons (written text or an external link)
to a course; only students with an APPROVED enrollment can view them, and
can mark each one complete.

**Quizzes** — admin builds multiple-choice quizzes per course. Students take
them and get graded instantly, with a per-question review. The correct
answer is never sent to the browser until after submission.

**Admin analytics** — per course, a table of every approved student's lesson
completion and quiz performance.

**Certificates** — once a student finishes every lesson and passes every
quiz (60%+) in a course, they can claim a certificate — validated
server-side, not just a UI gate. Anyone can verify a certificate is real via
the public, no-login endpoint `/api/certificates/verify/{code}`.

**Badges** — automatic achievements (First Steps, Quiz Ace, Course Complete,
Multi-Talented, Dedicated Learner) computed from existing progress data --
there's no separate "badges" table to keep in sync.

**Side quests** — admin-authored bonus objectives per course (e.g., "post in
the discussion"), worth points, self-marked complete by the student. Kept
deliberately lightweight -- no grading, no verification, pure engagement.

**Career paths** — admin curates a named path (e.g., "Backend Developer")
made of ordered courses. Students browse paths and see each course marked
Completed / In Progress / Not Started.

## API summary

| Method | Path | Who | What |
|--------|------|-----|------|
| POST | `/api/auth/register` | public | create a student account |
| POST | `/api/auth/login` | public | get a JWT |
| GET | `/api/courses?keyword=&semester=` | any signed-in | browse active courses |
| GET | `/api/courses/{id}` | any signed-in | course detail |
| GET | `/api/career-paths` | any signed-in | browse career paths |
| GET | `/api/certificates/verify/{code}` | **public** | verify a certificate is real |
| POST | `/api/student/enrollments` | student | request to enroll `{courseId}` |
| POST | `/api/student/enrollments/{id}/drop` | student | request to drop |
| GET | `/api/student/enrollments` | student | my enrollment history |
| GET | `/api/student/schedule` | student | my approved courses |
| GET | `/api/student/courses/{id}/contents` | student | view a course's lessons |
| POST | `/api/student/contents/{id}/complete` | student | mark a lesson complete |
| GET | `/api/student/courses/{id}/quizzes` | student | quizzes for a course |
| GET | `/api/student/quizzes/{id}/take` | student | quiz questions (no answers) |
| POST | `/api/student/quizzes/{id}/submit` | student | submit + get graded instantly |
| GET | `/api/student/quizzes/{id}/attempts` | student | my past attempts |
| GET | `/api/student/courses/{id}/progress` | student | my completion status |
| POST | `/api/student/courses/{id}/certificate/claim` | student | claim (eligibility checked) |
| GET | `/api/student/courses/{id}/certificate` | student | my certificate, if issued |
| GET | `/api/student/badges` | student | my earned badges |
| GET | `/api/student/courses/{id}/quests` | student | side quests + my points |
| POST | `/api/student/quests/{id}/complete` | student | mark a quest complete |
| GET | `/api/student/career-paths/{id}` | student | path detail with my status per course |
| GET | `/api/admin/courses` | admin | all courses (incl. removed) |
| POST/PUT | `/api/admin/courses[/{id}]` | admin | create/edit a course |
| POST/GET | `/api/admin/courses/{id}/contents` | admin | add/list lessons |
| PUT/DELETE | `/api/admin/contents/{id}` | admin | edit/delete a lesson |
| POST/GET | `/api/admin/courses/{id}/quizzes` | admin | create/list quizzes |
| GET/DELETE | `/api/admin/quizzes/{id}` | admin | quiz detail / delete |
| POST | `/api/admin/quizzes/{id}/questions` | admin | add a question + options |
| DELETE | `/api/admin/questions/{id}` | admin | delete a question |
| GET | `/api/admin/courses/{id}/analytics` | admin | every student's performance |
| POST/GET | `/api/admin/courses/{id}/quests` | admin | create/list side quests |
| DELETE | `/api/admin/quests/{id}` | admin | delete a side quest |
| POST | `/api/admin/career-paths` | admin | create a path |
| GET/DELETE | `/api/admin/career-paths/{id}` | admin | detail / delete a path |
| POST/DELETE | `/api/admin/career-paths/{id}/courses[/{courseId}]` | admin | add/remove a course |
| GET | `/api/registrar/enrollments/pending`, `/drops/pending` | registrar | requests awaiting decision |
| POST | `/api/registrar/enrollments/{id}/approve` \| `reject` | registrar | decide an enroll request |
| POST | `/api/registrar/drops/{id}/approve` \| `reject` | registrar | decide a drop request |
| DELETE | `/api/registrar/courses/{id}` | registrar | remove a course |

Auth is a JWT in `Authorization: Bearer <token>`, issued on login/register.

## Being upfront about the process

This project was built iteratively over a long conversation, in phases, with
real bugs caught and fixed along the way (a `ResultSet.wasNull()` ordering
bug, a fragile inline-JSON pattern in the frontend, a missing CSS rule that
would have shown every dashboard tab stacked at once, a results screen that
referenced an answer it never actually displayed, a hardcoded `orderIndex`
in the career-path response, and a Windows PowerShell redirection gotcha in
setup). Partway through, the sandbox this was built in reset and some
already-written but not-yet-delivered work (the side-quest backend, the icon
upgrade) had to be reconstructed from the delivered zips and rebuilt from
scratch — that reconstruction was then re-verified with the same automated
checks (class/ID cross-referencing, tag-balance checks, Java syntax checks)
before being called done. I was never able to run `mvn spring-boot:run`
against a live database from this sandbox — your first run is the real
end-to-end test.

## Natural next steps (good "future work" talking points)

- Pagination on the course catalog and pending-requests lists
- A waitlist instead of a hard reject when a course is full
- Schedule-conflict detection (nothing currently stops overlapping approvals)
- Unit tests for `EnrollmentService`'s transaction logic
- Refresh tokens (current JWTs expire after 24h and require re-login)
- PDF certificate export (currently an in-browser view, printable via the browser's own print-to-PDF)
