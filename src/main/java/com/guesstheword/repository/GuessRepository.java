package com.guesstheword.repository;

import com.guesstheword.entity.GameSession;
import com.guesstheword.entity.Guess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuessRepository extends JpaRepository<Guess, Long> {
    List<Guess> findByGameSessionOrderBySequenceNoAsc(GameSession gameSession);
    long countByGameSession(GameSession gameSession);
}
