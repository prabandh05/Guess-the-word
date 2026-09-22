# Guess the Word — Spring Boot Web Application

A full-stack, enterprise-grade Wordle-style word guessing game built with Spring Boot, Spring Security (JWT), Spring Data JPA, and H2/MySQL.

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
   - Default administrator account pre-configured.

---

## Tech Stack

- **Language**: Java 11 (OpenJDK)
- **Framework**: Spring Boot 2.7.18
- **Security**: Spring Security + JSON Web Token (JJWT 0.11.5) + BCrypt
- **Database**: H2 In-Memory/File Database (with full PostgreSQL/MySQL compatibility)
- **ORM**: Spring Data JPA / Hibernate
- **Build Tool**: Maven 3.9+ (with bundled `./mvnw` wrapper)
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
H2 Database Web Console is available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:guesstheword`, User: `sa`, Password: empty).

---

## Pre-seeded Test Accounts

| Role | Username | Password | Access |
|---|---|---|---|
| **ADMIN** | `AdminUser` | `Admin123*` | Admin Reports, Game Ping, Admin Ping |
| **PLAYER** | `PlayerOne` | `Player123$` | Game Play, Game Ping |

---

## API Documentation (Phase 1)

### 1. Register a New User
- **Endpoint:** `POST /api/auth/register`
- **Headers:** `Content-Type: application/json`
- **Body:**
```json
{
  "username": "PlayerTwo",
  "password": "PlayerPass123$",
  "role": "PLAYER"
}
```
- **Response (201 Created):**
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "id": 3,
    "username": "PlayerTwo",
    "role": "PLAYER",
    "message": "User registered successfully"
  }
}
```

### 2. Login
- **Endpoint:** `POST /api/auth/login`
- **Headers:** `Content-Type: application/json`
- **Body:**
```json
{
  "username": "PlayerOne",
  "password": "Player123$"
}
```
- **Response (200 OK):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "id": 2,
    "username": "PlayerOne",
    "role": "PLAYER",
    "message": "Login successful"
  }
}
```

### 3. Role-Protected Health Endpoints
- **Player Ping:** `GET /api/game/ping`
  - Header: `Authorization: Bearer <TOKEN>`
  - Accessible by: `PLAYER` and `ADMIN`
- **Admin Ping:** `GET /api/admin/ping`
  - Header: `Authorization: Bearer <TOKEN>`
  - Accessible by: `ADMIN` only (returns 403 for `PLAYER`)

---

## Project Roadmap

- [x] **Phase 1 (Day 1):** Project Setup, Data Model & Authentication (JWT + Spring Security)
- [ ] **Phase 2 (Day 2):** Core Game Engine & Wordle Comparison Logic
- [ ] **Phase 3 (Day 3):** Persistence Completeness & Admin Aggregate Reports
- [ ] **Phase 4 (Day 4):** Frontend UI (5x5 interactive board + Admin dashboard)
- [ ] **Phase 5 (Day 5):** Hardening, Regression Testing, Docs & Final Delivery
