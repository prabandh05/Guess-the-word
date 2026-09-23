package com.guesstheword.service;

import com.guesstheword.dto.DailyReportResponse;
import com.guesstheword.dto.UserDailyStatDto;
import com.guesstheword.dto.UserReportResponse;
import com.guesstheword.dto.UserSummaryDto;
import com.guesstheword.entity.GameSession;
import com.guesstheword.entity.GameStatus;
import com.guesstheword.entity.User;
import com.guesstheword.exception.GameRuleException;
import com.guesstheword.repository.GameSessionRepository;
import com.guesstheword.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final GameSessionRepository gameSessionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserSummaryDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> UserSummaryDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .role(user.getRole().name())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Daily report: number of distinct players who played on the given date,
     * and number of sessions won (correct guesses) on that date.
     */
    @Transactional(readOnly = true)
    public DailyReportResponse getDailyReport(LocalDate date) {
        long distinctUsers = gameSessionRepository.countDistinctUsersByPlayedDate(date);
        long correctGuesses = gameSessionRepository.countByPlayedDateAndStatus(date, GameStatus.WON);

        return DailyReportResponse.builder()
                .date(date)
                .distinctUsersPlayed(distinctUsers)
                .correctGuesses(correctGuesses)
                .build();
    }

    /**
     * User activity report: per date — how many words the user tried and
     * how many they guessed correctly.
     */
    @Transactional(readOnly = true)
    public UserReportResponse getUserReport(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GameRuleException("User not found with id: " + userId));

        List<GameSession> sessions = gameSessionRepository.findByUserOrderByPlayedDateAsc(user);

        // Group sessions by played date using a LinkedHashMap to preserve date order
        Map<LocalDate, List<GameSession>> byDate = new LinkedHashMap<>();
        for (GameSession session : sessions) {
            byDate.computeIfAbsent(session.getPlayedDate(), k -> new ArrayList<>()).add(session);
        }

        List<UserDailyStatDto> dailyStats = new ArrayList<>();
        for (Map.Entry<LocalDate, List<GameSession>> entry : byDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<GameSession> dateSessions = entry.getValue();

            long wordsTried = dateSessions.size();
            long correctGuesses = dateSessions.stream()
                    .filter(s -> s.getStatus() == GameStatus.WON)
                    .count();

            dailyStats.add(UserDailyStatDto.builder()
                    .date(date)
                    .wordsTried(wordsTried)
                    .correctGuesses(correctGuesses)
                    .build());
        }

        return UserReportResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .dailyStats(dailyStats)
                .build();
    }
}
