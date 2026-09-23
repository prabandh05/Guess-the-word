package com.guesstheword.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDailyStatDto {
    private LocalDate date;
    private long wordsTried;
    private long correctGuesses;
}
