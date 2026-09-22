package com.guesstheword.service;

import com.guesstheword.dto.GameSessionResponse;
import com.guesstheword.dto.GameStartResponse;
import com.guesstheword.dto.GuessResponse;
import com.guesstheword.entity.GameSession;
import com.guesstheword.entity.GameStatus;
import com.guesstheword.entity.Guess;
import com.guesstheword.entity.User;
import com.guesstheword.entity.Word;
import com.guesstheword.exception.GameRuleException;
import com.guesstheword.repository.GameSessionRepository;
import com.guesstheword.repository.GuessRepository;
import com.guesstheword.repository.UserRepository;
import com.guesstheword.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameService {

    private static final int MAX_GAMES_PER_DAY = 3;
    private static final int MAX_GUESSES_PER_SESSION = 5;
    private static final int WORD_LENGTH = 5;

    private final UserRepository userRepository;
    private final WordRepository wordRepository;
    private final GameSessionRepository gameSessionRepository;
    private final GuessRepository guessRepository;

    @Transactional
    public GameStartResponse startGame(String username) {
        User user = getUser(username);

        long gamesPlayedToday = gameSessionRepository.countByUserAndPlayedDate(user, LocalDate.now());
        if (gamesPlayedToday >= MAX_GAMES_PER_DAY) {
            throw new GameRuleException("Daily limit reached: you may only play " + MAX_GAMES_PER_DAY + " games per day.");
        }

        Word word = wordRepository.findRandomWord()
                .orElseThrow(() -> new IllegalStateException("No words found in the database."));

        GameSession session = GameSession.builder()
                .user(user)
                .word(word)
                .status(GameStatus.IN_PROGRESS)
                .build();

        session = gameSessionRepository.save(session);

        return GameStartResponse.builder()
                .id(session.getId())
                .status(session.getStatus())
                .startedAt(session.getStartedAt())
                .build();
    }

    @Transactional
    public GuessResponse makeGuess(Long sessionId, String username, String guessText) {
        User user = getUser(username);
        GameSession session = getSessionForUser(sessionId, user);

        if (session.getStatus() != GameStatus.IN_PROGRESS) {
            throw new GameRuleException("This game session is already " + session.getStatus().name().toLowerCase() + ".");
        }

        long guessCount = guessRepository.countByGameSession(session);
        if (guessCount >= MAX_GUESSES_PER_SESSION) {
            throw new GameRuleException("Maximum guesses reached for this session.");
        }

        String target = session.getWord().getText();
        String pattern = evaluateGuess(target, guessText);

        Guess guess = Guess.builder()
                .gameSession(session)
                .guessText(guessText)
                .resultPattern(pattern)
                .sequenceNo((int) guessCount + 1)
                .build();

        guess = guessRepository.save(guess);

        if (guessText.equals(target)) {
            session.setStatus(GameStatus.WON);
            gameSessionRepository.save(session);
        } else if (guessCount + 1 >= MAX_GUESSES_PER_SESSION) {
            session.setStatus(GameStatus.LOST);
            gameSessionRepository.save(session);
        }

        return GuessResponse.builder()
                .guessText(guess.getGuessText())
                .resultPattern(guess.getResultPattern())
                .sequenceNo(guess.getSequenceNo())
                .guessedAt(guess.getGuessedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public GameSessionResponse getSessionDetails(Long sessionId, String username) {
        User user = getUser(username);
        GameSession session = getSessionForUser(sessionId, user);

        List<GuessResponse> guesses = guessRepository
                .findByGameSessionOrderBySequenceNoAsc(session)
                .stream()
                .map(g -> GuessResponse.builder()
                        .guessText(g.getGuessText())
                        .resultPattern(g.getResultPattern())
                        .sequenceNo(g.getSequenceNo())
                        .guessedAt(g.getGuessedAt())
                        .build())
                .collect(Collectors.toList());

        return GameSessionResponse.builder()
                .id(session.getId())
                .status(session.getStatus())
                .startedAt(session.getStartedAt())
                .guesses(guesses)
                .build();
    }

    // ---------------------------------------------------------------------------
    // Wordle-style evaluation
    //
    // Returns a comma-separated pattern of 5 codes:
    //   G = GREEN  (correct letter, correct position)
    //   O = ORANGE (correct letter, wrong position)
    //   X = GREY   (letter not in word)
    //
    // Handles duplicate letters correctly:
    //   1. First pass  – mark exact matches (GREEN) and consume those target slots.
    //   2. Second pass – for non-green guess letters, scan remaining target slots
    //                   left-to-right for a match (ORANGE), consuming each slot once.
    // ---------------------------------------------------------------------------
    String evaluateGuess(String target, String guess) {
        char[] targetChars = target.toCharArray();
        char[] guessChars  = guess.toCharArray();
        String[] result    = new String[WORD_LENGTH];
        boolean[] targetUsed = new boolean[WORD_LENGTH];
        boolean[] guessUsed  = new boolean[WORD_LENGTH];

        // Pass 1: Exact matches → GREEN
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (guessChars[i] == targetChars[i]) {
                result[i]      = "G";
                targetUsed[i]  = true;
                guessUsed[i]   = true;
            }
        }

        // Pass 2: Wrong-position matches → ORANGE; otherwise GREY
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (guessUsed[i]) continue;          // already GREEN
            boolean found = false;
            for (int j = 0; j < WORD_LENGTH; j++) {
                if (!targetUsed[j] && guessChars[i] == targetChars[j]) {
                    result[i]     = "O";
                    targetUsed[j] = true;
                    found         = true;
                    break;
                }
            }
            if (!found) {
                result[i] = "X";
            }
        }

        return String.join(",", result);
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));
    }

    private GameSession getSessionForUser(Long sessionId, User user) {
        GameSession session = gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new GameRuleException("Game session not found."));
        if (!session.getUser().getId().equals(user.getId())) {
            throw new GameRuleException("Access denied: this session does not belong to you.");
        }
        return session;
    }
}
