# Guess the Word — Project Plan (Spring Boot)

**Deadline:** 30 Sep 2026
**Stack:** Spring Boot, Spring Security, Spring Data JPA, MySQL/PostgreSQL (or H2 for dev), Thymeleaf or a lightweight frontend (React optional)
**Approach:** 5 phases, one phase per day, one feature-branch + PR + merge per phase

---

## Repo & Commit Strategy (apply every day)

- `main` stays deployable. Work on a branch per phase: `phase-1-auth`, `phase-2-game-engine`, etc.
- Commit small and often within a phase (setup → entity → repo → service → controller → test), not one giant commit.
- Suggested commit message pattern: `feat(phase-1): add user registration with validation`
- End each day: push branch → open PR → merge to `main` → tag if useful (`v0.1-phase1`) → update README with what's done.
- Add a `PROGRESS.md` checklist you tick off — makes the final submission easy to describe.

---

## Phase 1 (Day 1): Project Setup, Data Model, Auth

**Goal:** A runnable Spring Boot app with registration/login working end-to-end.

- Initialize project (Spring Initializr): Web, Security, Data JPA, Validation, H2/MySQL driver, Lombok.
- Design core entities:
  - `User` (id, username, password_hash, role: ADMIN/PLAYER, createdAt)
  - `Word` (id, text — 5 letters, uppercase)
  - `GameSession` (id, user, word, status: IN_PROGRESS/WON/LOST, startedAt, playedDate)
  - `Guess` (id, gameSession, guessText, resultPattern, guessedAt, sequenceNo)
- Registration endpoint `POST /api/auth/register`:
  - Username: min 5 letters, letters only (upper/lower).
  - Password: min 5 chars, must contain a letter, a digit, and one of `$ % *` — implement with a regex validator.
  - Hash password with BCrypt.
- Login endpoint `POST /api/auth/login` — issue JWT (or use session auth if you prefer simplicity).
- Role-based security config: `ADMIN` vs `PLAYER` endpoints separated (`/api/admin/**` vs `/api/game/**`).
- Seed data: a `CommandLineRunner`/`data.sql` that inserts the 20 uppercase 5-letter words and one ADMIN user.
- Write unit tests for the validation regex and registration flow.

**Commit checkpoints:** initial scaffold → entities+repos → registration+validation → login/JWT → security config → seed data → tests.

**End of day:** you can register, log in, and hit a protected `/api/game/ping` as PLAYER, and `/api/admin/ping` as ADMIN.

---

## Phase 2 (Day 2): Core Game Engine

**Goal:** A player can start a game and submit guesses with correct color-coded feedback.

- `POST /api/game/start` — picks a random word from `Word` table, creates a `GameSession` (status IN_PROGRESS), enforces the **max 3 games/day per user** rule (query `GameSession` by user + `playedDate = today`; reject with a clear message if limit hit).
- `POST /api/game/{sessionId}/guess` — accepts a 5-letter uppercase word:
  - Validate length = 5, letters only, uppercase (reject/normalize otherwise).
  - Enforce max 5 guesses per session (reject 6th attempt).
  - Implement the Wordle-style comparison algorithm carefully (handle duplicate letters correctly):
    1. First pass: mark exact position matches GREEN.
    2. Second pass: for remaining letters, mark ORANGE if the letter exists elsewhere in the target with remaining count, else GREY.
  - Persist each `Guess` with its result pattern and sequence number.
  - If guess == word → mark session WON, return congratulations payload.
  - If 5th guess used and not correct → mark session LOST, return "better luck next time" payload.
  - Otherwise return the guess result and let the player continue.
- `GET /api/game/{sessionId}` — returns session state + all prior guesses **in order**, so the UI can redraw the board (needed for refresh / resume).
- Unit tests: exact match, no match, duplicate-letter edge cases (e.g., guess `LEVEL` against target `EAGLE`), guess-limit enforcement, daily-limit enforcement.

**Commit checkpoints:** start-game endpoint + daily limit → guess entity/repo → comparison algorithm + tests → guess endpoint + guess-limit → win/lose handling → session-state endpoint.

**End of day:** full game playable via API (Postman) start-to-finish, including win and loss paths.

---

## Phase 3 (Day 3): Persistence Completeness + Admin Reports

**Goal:** All game/guess history is queryable; admin reporting endpoints are done.

- Confirm every started word and every guess (with date) is saved — add indices on `playedDate`/`user_id` for report performance.
- `GET /api/admin/reports/daily?date=YYYY-MM-DD`:
  - Number of distinct users who played that day.
  - Number of correct guesses (sessions WON) that day.
- `GET /api/admin/reports/user/{userId}`:
  - Per date: number of words tried (sessions started) and number of correct guesses (sessions WON), grouped by date.
- Use JPQL/native aggregate queries (`GROUP BY`) rather than pulling everything into Java and looping, for performance.
- DTOs for report responses; input validation on date params; proper 403 for non-admins.
- Integration tests for both report endpoints against seeded sample sessions/guesses.

**Commit checkpoints:** repository aggregate queries → daily report endpoint + DTO → user report endpoint + DTO → admin authorization tests → integration tests with seeded data.

**End of day:** admin can pull both reports and get correct numbers against test data.

---

## Phase 4 (Day 4): Frontend / UI

**Goal:** A usable UI for both Player and Admin (Thymeleaf pages are enough — React only if you have time/want the extra polish).

- **Player UI:**
  - Registration/login pages/forms wired to the auth APIs.
  - Game board: 5x5 grid, uppercase letters, cells colored green/orange/grey per guess, prior guesses shown in the order submitted.
  - Modal/alert for "Congratulations" (on win) and "Better luck next time" (on loss/exhausted attempts) with an OK button that ends the game view.
  - Disable "start new game" once 3 games are used for the day, with a clear message.
- **Admin UI:**
  - Simple page: pick a date → show daily report; pick a user → show that user's report (table by date).
- Client-side input validation mirroring backend rules (5 letters, A-Z only) for a smoother UX, but backend remains the source of truth.
- Basic styling (Bootstrap or plain CSS) — polish is optional, correctness isn't.

**Commit checkpoints:** auth pages → game board rendering → guess submission + color rendering → win/lose modals + daily-limit UX → admin report pages.

**End of day:** the whole flow is playable and reportable from the browser, not just Postman.

---

## Phase 5 (Day 5): Testing, Hardening, Docs, Submission

**Goal:** Everything specified is verified, documented, and shipped.

- Full regression pass against the spec, explicitly checking:
  - Username/password validation edge cases.
  - 20 words seeded correctly, all uppercase, 5 letters.
  - Daily 3-game limit enforced across midnight boundaries (use server date consistently).
  - 5-guess limit, correct color logic (including duplicate-letter cases), win/lose messaging.
  - Guess history displayed in original order on resume.
  - Both admin reports return correct aggregates.
- Add exception handling (`@ControllerAdvice`) for clean error responses (invalid input, limits exceeded, unauthorized).
- Write the `README.md`: setup instructions, tech stack, how to run, sample credentials, API summary/Postman collection link.
- Finalize `PROGRESS.md` / close out any remaining checklist items.
- Merge final PR to `main`, tag `v1.0`.
- Reply-all to the training status email thread and share the GitHub repo link (per the assignment instructions).

**Commit checkpoints:** bug fixes from regression pass → global exception handling → README → final polish commit → tag v1.0.

---

## Quick Entity/API Reference

| Entity | Key fields |
|---|---|
| User | id, username, passwordHash, role |
| Word | id, text (5-letter, UPPER) |
| GameSession | id, userId, wordId, status, playedDate, startedAt |
| Guess | id, sessionId, guessText, resultPattern, sequenceNo, guessedAt |

| Endpoint | Method | Who |
|---|---|---|
| /api/auth/register | POST | Public |
| /api/auth/login | POST | Public |
| /api/game/start | POST | Player |
| /api/game/{id}/guess | POST | Player |
| /api/game/{id} | GET | Player |
| /api/admin/reports/daily | GET | Admin |
| /api/admin/reports/user/{id} | GET | Admin |

If you fall behind on any single day, Phase 3 (reports) is the safest one to compress — the core game (Phases 1-2) and UI (Phase 4) are what a reviewer will look at first.
