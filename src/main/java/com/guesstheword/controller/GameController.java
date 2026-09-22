package com.guesstheword.controller;

import com.guesstheword.dto.ApiResponse;
import com.guesstheword.dto.GameSessionResponse;
import com.guesstheword.dto.GameStartResponse;
import com.guesstheword.dto.GuessRequest;
import com.guesstheword.dto.GuessResponse;
import com.guesstheword.entity.GameStatus;
import com.guesstheword.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    /**
     * POST /api/game/start
     * Start a new game session for the authenticated player.
     */
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<GameStartResponse>> startGame(
            @AuthenticationPrincipal UserDetails userDetails) {

        GameStartResponse response = gameService.startGame(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.<GameStartResponse>builder()
                .success(true)
                .message("Game started. You have up to 5 guesses. Good luck!")
                .data(response)
                .build());
    }

    /**
     * POST /api/game/{sessionId}/guess
     * Submit a guess for an active game session.
     */
    @PostMapping("/{sessionId}/guess")
    public ResponseEntity<ApiResponse<GuessResponse>> makeGuess(
            @PathVariable Long sessionId,
            @Valid @RequestBody GuessRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        GuessResponse guessResponse = gameService.makeGuess(
                sessionId, userDetails.getUsername(), request.getGuessText());

        String message = buildGuessMessage(guessResponse);
        return ResponseEntity.ok(ApiResponse.<GuessResponse>builder()
                .success(true)
                .message(message)
                .data(guessResponse)
                .build());
    }

    /**
     * GET /api/game/{sessionId}
     * Retrieve the current state and guess history of a session.
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<GameSessionResponse>> getSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        GameSessionResponse response = gameService.getSessionDetails(sessionId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.<GameSessionResponse>builder()
                .success(true)
                .message("Session retrieved successfully.")
                .data(response)
                .build());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String buildGuessMessage(GuessResponse guessResponse) {
        // Derive win/loss from the session state reflected in the next GET call;
        // here we give a friendly hint based on the pattern.
        String pattern = guessResponse.getResultPattern();
        // If all GREEN → WON
        if ("G,G,G,G,G".equals(pattern)) {
            return "Congratulations! You guessed the word correctly!";
        }
        // If this was the 5th guess and not all green → LOST
        if (guessResponse.getSequenceNo() != null && guessResponse.getSequenceNo() == 5) {
            return "Better luck next time! You've used all your guesses.";
        }
        return "Guess submitted. Keep going!";
    }
}
