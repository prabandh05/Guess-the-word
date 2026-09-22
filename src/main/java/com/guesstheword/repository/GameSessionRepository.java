package com.guesstheword.repository;

import com.guesstheword.entity.GameSession;
import com.guesstheword.entity.GameStatus;
import com.guesstheword.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
    long countByUserAndPlayedDate(User user, LocalDate playedDate);
    List<GameSession> findByUserOrderByStartedAtDesc(User user);
    long countByPlayedDateAndStatus(LocalDate playedDate, GameStatus status);
}
