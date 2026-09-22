package com.guesstheword.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guesstheword.dto.GuessRequest;
import com.guesstheword.dto.LoginRequest;
import com.guesstheword.repository.GameSessionRepository;
import com.guesstheword.repository.GuessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("Game Controller Integration Tests")
class GameControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private GuessRepository guessRepository;
    @Autowired private GameSessionRepository gameSessionRepository;

    private String playerToken;
    private String adminToken;

    // -------------------------------------------------------------------------
    // Reset game data and obtain fresh tokens before each test.
    // Deleting all guesses then all sessions prevents the 3-games/day limit
    // from accumulating across tests that each call startGame for PlayerOne.
    // -------------------------------------------------------------------------
    @BeforeEach
    void setUp() throws Exception {
        guessRepository.deleteAll();
        gameSessionRepository.deleteAll();

        playerToken = loginAndGetToken("PlayerOne", "Player123$");
        adminToken  = loginAndGetToken("AdminUser", "Admin123*");
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                LoginRequest.builder().username(username).password(password).build());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).path("data").path("token").asText();
    }

    // =========================================================================
    // POST /api/game/start
    // =========================================================================

    @Test
    @DisplayName("Player can start a new game")
    void startGame_success() throws Exception {
        mockMvc.perform(post("/api/game/start")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.status", is("IN_PROGRESS")));
    }

    @Test
    @DisplayName("Unauthenticated user cannot start a game")
    void startGame_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/game/start"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // POST /api/game/{sessionId}/guess
    // =========================================================================

    @Test
    @DisplayName("Player can submit a valid 5-letter uppercase guess")
    void makeGuess_validGuess() throws Exception {
        MvcResult startResult = mockMvc.perform(post("/api/game/start")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk())
                .andReturn();

        long sessionId = objectMapper.readTree(
                startResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        GuessRequest request = new GuessRequest("CRANE");
        mockMvc.perform(post("/api/game/" + sessionId + "/guess")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.guessText", is("CRANE")))
                .andExpect(jsonPath("$.data.resultPattern", notNullValue()))
                .andExpect(jsonPath("$.data.sequenceNo", is(1)));
    }

    @Test
    @DisplayName("Guess with lowercase letters is rejected with 400")
    void makeGuess_lowercaseRejected() throws Exception {
        MvcResult startResult = mockMvc.perform(post("/api/game/start")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk())
                .andReturn();

        long sessionId = objectMapper.readTree(
                startResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        GuessRequest request = new GuessRequest("crane");
        mockMvc.perform(post("/api/game/" + sessionId + "/guess")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Guess with wrong length is rejected with 400")
    void makeGuess_wrongLength() throws Exception {
        MvcResult startResult = mockMvc.perform(post("/api/game/start")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk())
                .andReturn();

        long sessionId = objectMapper.readTree(
                startResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        GuessRequest request = new GuessRequest("TOOLONG");
        mockMvc.perform(post("/api/game/" + sessionId + "/guess")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Accessing another user's session is rejected")
    void makeGuess_wrongUserSession() throws Exception {
        // PlayerOne starts a game
        MvcResult startResult = mockMvc.perform(post("/api/game/start")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk())
                .andReturn();

        long sessionId = objectMapper.readTree(
                startResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // AdminUser tries to guess on PlayerOne's session
        GuessRequest request = new GuessRequest("CRANE");
        mockMvc.perform(post("/api/game/" + sessionId + "/guess")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // =========================================================================
    // GET /api/game/{sessionId}
    // =========================================================================

    @Test
    @DisplayName("Player can retrieve session state with guess history")
    void getSession_success() throws Exception {
        // Start game
        MvcResult startResult = mockMvc.perform(post("/api/game/start")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk())
                .andReturn();

        long sessionId = objectMapper.readTree(
                startResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Make a guess
        mockMvc.perform(post("/api/game/" + sessionId + "/guess")
                .header("Authorization", "Bearer " + playerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new GuessRequest("CRANE"))));

        // Get session
        mockMvc.perform(get("/api/game/" + sessionId)
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is((int) sessionId)))
                .andExpect(jsonPath("$.data.guesses").isArray())
                .andExpect(jsonPath("$.data.guesses[0].guessText", is("CRANE")))
                .andExpect(jsonPath("$.data.guesses[0].sequenceNo", is(1)));
    }

    @Test
    @DisplayName("Requesting a non-existent session returns 400")
    void getSession_notFound() throws Exception {
        mockMvc.perform(get("/api/game/99999")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
