package com.guesstheword.dto;

import com.guesstheword.entity.GameStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSessionResponse {
    private Long id;
    private GameStatus status;
    private LocalDateTime startedAt;
    private List<GuessResponse> guesses;
}
