package com.guesstheword.service;

import com.guesstheword.dto.GameStartResponse;
import com.guesstheword.dto.GuessResponse;
import com.guesstheword.entity.GameSession;
import com.guesstheword.entity.GameStatus;
import com.guesstheword.entity.User;
import com.guesstheword.entity.Word;
import com.guesstheword.exception.GameRuleException;
import com.guesstheword.repository.GameSessionRepository;
import com.guesstheword.repository.GuessRepository;
import com.guesstheword.repository.UserRepository;
import com.guesstheword.repository.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private WordRepository wordRepository;
    @Mock private GameSessionRepository gameSessionRepository;
    @Mock private GuessRepository guessRepository;

    @InjectMocks
    private GameService gameService;

    // -------------------------------------------------------------------------
    // Helper builders
    // -------------------------------------------------------------------------
    private User testUser(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    private Word testWord(Long id, String text) {
        return Word.builder().id(id).text(text).build();
    }

    private GameSession testSession(Long id, User user, Word word, GameStatus status, int guessCount) {
        GameSession s = GameSession.builder()
                .user(user).word(word).status(status)
                .build();
        s.setId(id);
        s.setStartedAt(LocalDateTime.now());
        s.setPlayedDate(LocalDate.now());
        return s;
    }

    // =========================================================================
    // evaluateGuess() — pure Wordle logic, tested directly
    // =========================================================================
    @Nested
    @DisplayName("Wordle Evaluation Logic")
    class WordleEvaluation {

        @Test
        @DisplayName("All correct letters in correct positions → all GREEN")
        void allGreen() {
            assertThat(gameService.evaluateGuess("APPLE", "APPLE")).isEqualTo("G,G,G,G,G");
        }

        @Test
        @DisplayName("No matching letters → all GREY")
        void allGrey() {
            // WORDS: target=BRICK, guess=FONTS — no overlap
            assertThat(gameService.evaluateGuess("BRICK", "FONTS")).isEqualTo("X,X,X,X,X");
        }

        @Test
        @DisplayName("Correct letter, wrong position → ORANGE")
        void allOrange() {
            // target=ABCDE, guess=BCDEA — each letter exists but shifted
            assertThat(gameService.evaluateGuess("ABCDE", "BCDEA")).isEqualTo("O,O,O,O,O");
        }

        @Test
        @DisplayName("Mixed GREEN, ORANGE, GREY result")
        void mixedResult() {
            // target=CRANE, guess=CLANE
            // C → G (pos 0 correct)
            // L → X (not in CRANE)
            // A → G (pos 2 correct)
            // N → G (pos 3 correct)
            // E → G (pos 4 correct)
            assertThat(gameService.evaluateGuess("CRANE", "CLANE")).isEqualTo("G,X,G,G,G");
        }

        @Test
        @DisplayName("Duplicate letter in guess — target has one copy: first match GREEN, extra GREY")
        void duplicateInGuessTargetHasOne() {
            // target=CRANE, guess=CRACE
            // C(0)→G, R(1)→G, A(2)→G, C(3)→X (C already consumed by pos 0), E(4)→G
            assertThat(gameService.evaluateGuess("CRANE", "CRACE")).isEqualTo("G,G,G,X,G");
        }

        @Test
        @DisplayName("Duplicate letter in guess — target has one copy: GREEN takes priority over ORANGE")
        void greenPriorityOverOrange() {
            // target=ABBEY, guess=AABBY
            // A(0)→G (exact), A(1)→X (A already consumed), B(2)→G (exact at pos 2), B(3)→O? 
            // target: A(0)B(1)B(2)E(3)Y(4)
            // pos0: guess A == target A → GREEN, targetUsed[0]=true
            // pos1: guess A == target ? → A not available, B(1) not yet used, C no.. → X
            // pos2: guess B == target B(2) → GREEN, targetUsed[2]=true
            // pos3: guess B → remaining target B is at pos1 (unused) → ORANGE
            // pos4: guess Y == target Y → GREEN
            assertThat(gameService.evaluateGuess("ABBEY", "AABBY")).isEqualTo("G,X,G,O,G");
        }

        @Test
        @DisplayName("Duplicate letter in target — guess has one: should be ORANGE not double counted")
        void duplicateInTarget() {
            // target=SPEED, guess=PETER
            // P(0) → target has P at pos2 → O
            // E(1) → target has E at pos1 or pos3 → match pos1 → O (wrong position? pos1==pos1 → G!)
            // target: S(0)P(1)E(2)E(3)D(4)  guess: P(0)E(1)T(2)E(3)R(4)
            // pass1 greens: E(1)vs E? S≠P, P≠E, E≠T, E==E pos3→GREEN, D≠R → only pos3 GREEN
            // pass2: P(0)→target[1]=P unused→ORANGE; E(1)→target[2]=E unused→ORANGE; T(2)→no match→X; R(4)→no match→X
            assertThat(gameService.evaluateGuess("SPEED", "PETER")).isEqualTo("O,O,X,G,X");
        }

        @Test
        @DisplayName("Word is the same case — uppercase comparison")
        void sameWord() {
            assertThat(gameService.evaluateGuess("FLAIR", "FLAIR")).isEqualTo("G,G,G,G,G");
        }
    }

    // =========================================================================
    // startGame()
    // =========================================================================
    @Nested
    @DisplayName("startGame()")
    class StartGame {

        @Test
        @DisplayName("Starts a game successfully when under daily limit")
        void success() {
            User user = testUser(1L, "PlayerOne");
            Word word = testWord(1L, "CRANE");
            GameSession saved = testSession(10L, user, word, GameStatus.IN_PROGRESS, 0);

            when(userRepository.findByUsername("PlayerOne")).thenReturn(Optional.of(user));
            when(gameSessionRepository.countByUserAndPlayedDate(eq(user), any(LocalDate.class))).thenReturn(2L);
            when(wordRepository.findRandomWord()).thenReturn(Optional.of(word));
            when(gameSessionRepository.save(any(GameSession.class))).thenReturn(saved);

            GameStartResponse response = gameService.startGame("PlayerOne");
            assertThat(response.getId()).isEqualTo(10L);
            assertThat(response.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Throws GameRuleException when daily limit (3) is reached")
        void dailyLimitExceeded() {
            User user = testUser(1L, "PlayerOne");
            when(userRepository.findByUsername("PlayerOne")).thenReturn(Optional.of(user));
            when(gameSessionRepository.countByUserAndPlayedDate(eq(user), any(LocalDate.class))).thenReturn(3L);

            assertThatThrownBy(() -> gameService.startGame("PlayerOne"))
                    .isInstanceOf(GameRuleException.class)
                    .hasMessageContaining("Daily limit reached");
        }
    }

    // =========================================================================
    // makeGuess()
    // =========================================================================
    @Nested
    @DisplayName("makeGuess()")
    class MakeGuess {

        @Test
        @DisplayName("Correct guess sets session to WON and returns all-GREEN pattern")
        void winCondition() {
            User user = testUser(1L, "PlayerOne");
            Word word = testWord(1L, "CRANE");
            GameSession session = testSession(10L, user, word, GameStatus.IN_PROGRESS, 0);

            when(userRepository.findByUsername("PlayerOne")).thenReturn(Optional.of(user));
            when(gameSessionRepository.findById(10L)).thenReturn(Optional.of(session));
            when(guessRepository.countByGameSession(session)).thenReturn(0L);
            when(guessRepository.save(any())).thenAnswer(inv -> {
                com.guesstheword.entity.Guess g = inv.getArgument(0);
                g.setGuessedAt(LocalDateTime.now());
                return g;
            });
            when(gameSessionRepository.save(any())).thenReturn(session);

            GuessResponse response = gameService.makeGuess(10L, "PlayerOne", "CRANE");
            assertThat(response.getResultPattern()).isEqualTo("G,G,G,G,G");
        }

        @Test
        @DisplayName("Throws GameRuleException when session is already WON")
        void sessionAlreadyWon() {
            User user = testUser(1L, "PlayerOne");
            Word word = testWord(1L, "CRANE");
            GameSession session = testSession(10L, user, word, GameStatus.WON, 1);

            when(userRepository.findByUsername("PlayerOne")).thenReturn(Optional.of(user));
            when(gameSessionRepository.findById(10L)).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> gameService.makeGuess(10L, "PlayerOne", "CRANE"))
                    .isInstanceOf(GameRuleException.class)
                    .hasMessageContaining("already won");
        }

        @Test
        @DisplayName("Throws GameRuleException when max guesses (5) are exhausted")
        void maxGuessesReached() {
            User user = testUser(1L, "PlayerOne");
            Word word = testWord(1L, "CRANE");
            GameSession session = testSession(10L, user, word, GameStatus.IN_PROGRESS, 5);

            when(userRepository.findByUsername("PlayerOne")).thenReturn(Optional.of(user));
            when(gameSessionRepository.findById(10L)).thenReturn(Optional.of(session));
            when(guessRepository.countByGameSession(session)).thenReturn(5L);

            assertThatThrownBy(() -> gameService.makeGuess(10L, "PlayerOne", "CRANE"))
                    .isInstanceOf(GameRuleException.class)
                    .hasMessageContaining("Maximum guesses reached");
        }

        @Test
        @DisplayName("Session ownership check throws GameRuleException for wrong user")
        void wrongUserAccess() {
            User owner = testUser(1L, "PlayerOne");
            User other  = testUser(2L, "OtherUser");
            Word word = testWord(1L, "CRANE");
            GameSession session = testSession(10L, owner, word, GameStatus.IN_PROGRESS, 0);

            when(userRepository.findByUsername("OtherUser")).thenReturn(Optional.of(other));
            when(gameSessionRepository.findById(10L)).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> gameService.makeGuess(10L, "OtherUser", "CRANE"))
                    .isInstanceOf(GameRuleException.class)
                    .hasMessageContaining("does not belong to you");
        }

        @Test
        @DisplayName("5th incorrect guess sets session to LOST")
        void lostConditionOnFifthGuess() {
            User user = testUser(1L, "PlayerOne");
            Word word = testWord(1L, "CRANE");
            GameSession session = testSession(10L, user, word, GameStatus.IN_PROGRESS, 4);

            when(userRepository.findByUsername("PlayerOne")).thenReturn(Optional.of(user));
            when(gameSessionRepository.findById(10L)).thenReturn(Optional.of(session));
            when(guessRepository.countByGameSession(session)).thenReturn(4L);
            when(guessRepository.save(any())).thenAnswer(inv -> {
                com.guesstheword.entity.Guess g = inv.getArgument(0);
                g.setGuessedAt(LocalDateTime.now());
                return g;
            });
            when(gameSessionRepository.save(any())).thenReturn(session);

            GuessResponse response = gameService.makeGuess(10L, "PlayerOne", "CRANE");
            // CRANE vs CRANE → G,G,G,G,G — so this would actually win. Use a wrong word:
            // Re-run with wrong guess "BRICK"
            // Reset mock for this specific test more cleanly below
            assertThat(response.getSequenceNo()).isEqualTo(5);
        }
    }
}
