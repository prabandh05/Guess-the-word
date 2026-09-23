package com.guesstheword.repository;

import com.guesstheword.entity.GameSession;
import com.guesstheword.entity.GameStatus;
import com.guesstheword.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    long countByUserAndPlayedDate(User user, LocalDate playedDate);

    List<GameSession> findByUserOrderByStartedAtDesc(User user);

    long countByPlayedDateAndStatus(LocalDate playedDate, GameStatus status);

    // --- Admin report queries ---

    /** Count of distinct players who played on a given date */
    @Query("SELECT COUNT(DISTINCT g.user) FROM GameSession g WHERE g.playedDate = :date")
    long countDistinctUsersByPlayedDate(@Param("date") LocalDate date);

    /** All sessions for a user, oldest date first — used for per-date grouping */
    List<GameSession> findByUserOrderByPlayedDateAsc(User user);
}
