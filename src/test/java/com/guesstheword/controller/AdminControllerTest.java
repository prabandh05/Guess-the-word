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

import java.time.LocalDate;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
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
@DisplayName("Admin Controller Integration Tests")
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private GuessRepository guessRepository;
    @Autowired private GameSessionRepository gameSessionRepository;

    private String adminToken;
    private String playerToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clear game data to have predictable state
        guessRepository.deleteAll();
        gameSessionRepository.deleteAll();

        adminToken  = loginAndGetToken("AdminUser",  "Admin123*");
        playerToken = loginAndGetToken("PlayerOne", "Player123$");
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                LoginRequest.builder().username(username).password(password).build());
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    /** Start a game for the player and return the session id */
    private long startGameForPlayer() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/game/start")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    /** Make a guess on a session */
    private void makeGuess(long sessionId, String word) throws Exception {
        mockMvc.perform(post("/api/game/" + sessionId + "/guess")
                .header("Authorization", "Bearer " + playerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new GuessRequest(word))));
    }

    // =========================================================================
    // Access Control
    // =========================================================================

    @Test
    @DisplayName("Admin can access daily report")
    void dailyReport_adminAccess() throws Exception {
        mockMvc.perform(get("/api/admin/reports/daily")
                        .param("date", LocalDate.now().toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.date", notNullValue()));
    }

    @Test
    @DisplayName("Player cannot access daily report — returns 403")
    void dailyReport_playerForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/reports/daily")
                        .param("date", LocalDate.now().toString())
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request is rejected — returns 401")
    void dailyReport_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/reports/daily")
                        .param("date", LocalDate.now().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin can access user report")
    void userReport_adminAccess() throws Exception {
        // Get PlayerOne's userId from the DB (seeded as id=2 typically, but we
        // obtain it dynamically by logging in and checking — we use id from game)
        long sessionId = startGameForPlayer();
        // We just need any user id; the session's userId comes from PlayerOne's account.
        // Retrieve it via the admin user report using id=2 (PlayerOne is always seeded second).
        mockMvc.perform(get("/api/admin/reports/user/2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is("PlayerOne")));
    }

    @Test
    @DisplayName("Player cannot access user report — returns 403")
    void userReport_playerForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/reports/user/1")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User report for non-existent userId returns 400")
    void userReport_notFound() throws Exception {
        mockMvc.perform(get("/api/admin/reports/user/99999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // =========================================================================
    // Data correctness
    // =========================================================================

    @Test
    @DisplayName("Daily report counts increase after game is played")
    void dailyReport_countsCorrect() throws Exception {
        String today = LocalDate.now().toString();

        // Before any games — counts should be 0
        MvcResult before = mockMvc.perform(get("/api/admin/reports/daily")
                        .param("date", today)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        long distinctBefore = objectMapper.readTree(before.getResponse().getContentAsString())
                .path("data").path("distinctUsersPlayed").asLong();

        // PlayerOne plays a game
        startGameForPlayer();

        // After — distinctUsersPlayed should have increased
        mockMvc.perform(get("/api/admin/reports/daily")
                        .param("date", today)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.distinctUsersPlayed",
                        greaterThanOrEqualTo((int) (distinctBefore + 1))));
    }

    @Test
    @DisplayName("User report has one entry per date played")
    void userReport_dailyBreakdown() throws Exception {
        // PlayerOne starts a game today
        startGameForPlayer();

        mockMvc.perform(get("/api/admin/reports/user/2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dailyStats", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.dailyStats[0].wordsTried", greaterThanOrEqualTo(1)));
    }
}
