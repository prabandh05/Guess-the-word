package com.guesstheword.dto;

import com.guesstheword.entity.GameStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStartResponse {
    private Long id;
    private GameStatus status;
    private LocalDateTime startedAt;
}
