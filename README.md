# Guess the Word — Spring Boot Web Application

A full-stack, enterprise-grade Wordle-style word guessing game built with Spring Boot, Spring Security (JWT), Spring Data JPA, and vanilla HTML/JS on the frontend.

Developed as part of the technical training project with daily phased milestones and clean version control.

---

## Technical Specifications & Rules

1. **User Management & Roles**:
   - **Player User**: Plays the game with a daily limit of 3 words and a maximum of 5 guesses per word.
   - **Admin User**: Accesses daily and per-user performance reports.
2. **Registration & Password Policy**:
   - **Username**: Must have at least 5 letters, letters only, containing both uppercase and lowercase characters (e.g. `UserOne`, `AdminUser`).
   - **Password**: Must be at least 5 characters long and contain alphabetic characters, numeric digits, and at least one special character from (`$`, `%`, `*`, `&`).
3. **Database Pre-seeding**:
   - 20 uppercase 5-letter English words loaded automatically upon boot.
   - Default administrator account and a default player account pre-configured.

---

## Tech Stack

- **Language**: Java 11 (OpenJDK)
- **Framework**: Spring Boot 2.7.18
- **Security**: Spring Security + JSON Web Token (JJWT 0.11.5) + BCrypt
- **Database**: H2 In-Memory/File Database (with full PostgreSQL/MySQL compatibility)
- **ORM**: Spring Data JPA / Hibernate
- **Build Tool**: Maven 3.9+ (with bundled `./mvnw` wrapper)
- **Frontend**: Vanilla HTML5, CSS3, JavaScript (no external frameworks, served directly via Spring Boot)
- **Testing**: JUnit 5, AssertJ, Spring Boot Test, MockMvc

---

## Getting Started

### Prerequisites
- Java 11 JDK (`openjdk version "11.0.x"`)
- Git

### Running the Application
Using the bundled Maven wrapper:

```bash
# Clone the repository
git clone git@github.com:prabandh05/Guess-the-word.git
cd "Guess the word"

# Run tests
./mvnw clean test

# Start the Spring Boot server
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`.  
- **Frontend UI**: `http://localhost:8080/index.html`
- **H2 Database Web Console**: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:guesstheword`, User: `sa`, Password: empty).

---

## Pre-seeded Test Accounts

| Role | Username | Password | Access |
|---|---|---|---|
| **ADMIN** | `AdminUser` | `Admin123*` | Admin Reports Dashboard |
| **PLAYER** | `PlayerOne` | `Player123$` | Game Play |

---

## Complete API Documentation

### 1. Authentication Endpoints
- `POST /api/auth/register` — Register a new user (Requires `username`, `password`, `role`).
- `POST /api/auth/login` — Login and receive JWT token.

### 2. Game Endpoints (Requires `PLAYER` role)
- `POST /api/game/start` — Start a new game session. (Enforces 3 games/day limit).
- `POST /api/game/{sessionId}/guess` — Submit a 5-letter uppercase guess. Returns evaluation (G, O, X).
- `GET /api/game/{sessionId}` — Retrieve current game state and guess history.

### 3. Admin Endpoints (Requires `ADMIN` role)
- `GET /api/admin/reports/daily?date=YYYY-MM-DD` — Get total players and correct guesses for a date.
- `GET /api/admin/users` — Get a list of all registered users.
- `GET /api/admin/reports/user/{userId}` — Get per-date activity breakdown for a specific user.

---

## Project Roadmap (Completed)

- [x] **Phase 1:** Project Setup, Data Model & Authentication (JWT + Spring Security)
- [x] **Phase 2:** Core Game Engine & Wordle Comparison Logic
- [x] **Phase 3:** Persistence Completeness & Admin Aggregate Reports
- [x] **Phase 4:** Frontend UI (5x5 interactive board + Admin dashboard)
- [x] **Phase 5:** Hardening, Regression Testing, Docs & Final Delivery
