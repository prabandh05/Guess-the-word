package com.guesstheword.repository;

import com.guesstheword.entity.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WordRepository extends JpaRepository<Word, Long> {
    Optional<Word> findByText(String text);
    boolean existsByText(String text);

    @Query(value = "SELECT * FROM words ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<Word> findRandomWord();
}
