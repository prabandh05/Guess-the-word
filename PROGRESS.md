# Guess the Word — Project Progress Tracking

**Deadline:** 30 Sep 2026  
**Target Completion:** 27 Sep 2026  
**Repository:** [prabandh05/Guess-the-word](https://github.com/prabandh05/Guess-the-word.git)

---

## Phase 1 (Day 1 - 22 Sep 2026): Project Setup, Data Model & Authentication
- [x] Initialize Spring Boot 2.7.18 project with Maven Wrapper and Java 11
- [x] Configure H2 in-memory/file database and JPA hibernate settings
- [x] Design core entities:
  - [x] `User` (id, username, password, role: ADMIN/PLAYER, createdAt)
  - [x] `Word` (id, text — 5 letters, uppercase)
  - [x] `GameSession` (id, user, word, status: IN_PROGRESS/WON/LOST, startedAt, playedDate)
  - [x] `Guess` (id, gameSession, guessText, resultPattern, guessedAt, sequenceNo)
- [x] Create Spring Data JPA repositories for all entities
- [x] Implement custom validation:
  - [x] Username validation: minimum 5 letters, letters only, both uppercase and lowercase
  - [x] Password validation: minimum 5 characters, alpha, numeric, and special characters (`$`, `%`, `*`, `&`)
- [x] Implement BCrypt password hashing
- [x] Implement JWT token generation, parsing, and request filter
- [x] Implement Spring Security configuration with stateless session and CORS
- [x] Implement registration endpoint `POST /api/auth/register`
- [x] Implement login endpoint `POST /api/auth/login`
- [x] Implement role-protected test endpoints (`/api/game/ping`, `/api/admin/ping`)
- [x] Pre-populate seed data:
  - [x] 20 uppercase 5-letter English words
  - [x] Default ADMIN user (`AdminUser` / `Admin123*`)
  - [x] Default PLAYER user (`PlayerOne` / `Player123$`)
- [x] Comprehensive test suite:
  - [x] Unit tests for custom regex validators (100% pass)
  - [x] MockMvc integration tests for registration and login (100% pass)
  - [x] Role-based security access tests for player and admin roles (100% pass)
  - [x] Seed data verification tests (100% pass)

---

## Phase 2 (Day 2 - 23 Sep 2026): Core Game Engine
- [x] `POST /api/game/start` endpoint:
  - [x] Pick random word from `Word` repository
  - [x] Enforce max 3 games/day limit per user
  - [x] Create and persist `GameSession` (status IN_PROGRESS)
- [x] `POST /api/game/{sessionId}/guess` endpoint:
  - [x] Input validation (5-letter uppercase word)
  - [x] Enforce max 5 guesses per session
  - [x] Wordle-style letter evaluation (GREEN, ORANGE, GREY) with duplicate letter handling
  - [x] Win condition check & congratulatory message
  - [x] Exhausted guesses check & "better luck next time" message
- [x] `GET /api/game/{sessionId}` session state endpoint (returns guesses in chronological order)
- [x] Unit & edge-case tests for game engine

---

## Phase 3 (Day 3 - 24 Sep 2026): Persistence Completeness & Admin Reports
- [x] Ensure all sessions and guesses with timestamps are indexed and stored
- [x] `GET /api/admin/reports/daily?date=YYYY-MM-DD`:
  - [x] Number of distinct users who played on date
  - [x] Number of correct guesses (sessions WON) on date
- [x] `GET /api/admin/reports/user/{userId}`:
  - [x] Per date: number of words tried and number of correct guesses
- [x] Repository aggregate queries and DTOs
- [x] Integration tests for admin reports

---

## Phase 4 (Day 4 - 25 Sep 2026): Frontend / UI
- [ ] Player UI:
  - [ ] Registration and login forms wired to JWT auth APIs
  - [ ] 5x5 interactive Game Board with uppercase letter inputs
  - [ ] Color-coded tiles (Green / Orange / Grey)
  - [ ] Congratulations and "Better luck next time" modals with OK buttons
  - [ ] 3-game daily limit banner and disabling
- [ ] Admin UI:
  - [ ] Daily report view with date picker
  - [ ] User-specific activity and performance report table
- [ ] Responsive styling and user feedback

---

## Phase 5 (Day 5 - 26 Sep 2026): Testing, Hardening & Submission
- [ ] End-to-end regression testing against all project requirements
- [ ] Global exception handling polish
- [ ] Documentation and deployment guide
- [ ] Tag final release `v1.0`
- [ ] Prepare training submission email and GitHub repository link
