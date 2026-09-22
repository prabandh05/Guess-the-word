package com.guesstheword.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "guesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Guess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession gameSession;

    @Column(name = "guess_text", nullable = false, length = 5)
    private String guessText;

    @Column(name = "result_pattern", nullable = false, length = 100)
    private String resultPattern;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Column(name = "guessed_at", nullable = false)
    private LocalDateTime guessedAt;

    @PrePersist
    protected void onCreate() {
        if (this.guessedAt == null) {
            this.guessedAt = LocalDateTime.now();
        }
    }
}
