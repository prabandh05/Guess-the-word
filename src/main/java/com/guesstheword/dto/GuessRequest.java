package com.guesstheword.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuessRequest {
    @NotBlank(message = "Guess text must not be empty")
    @Pattern(regexp = "^[A-Z]{5}$", message = "Guess must be exactly 5 uppercase English letters")
    private String guessText;
}
