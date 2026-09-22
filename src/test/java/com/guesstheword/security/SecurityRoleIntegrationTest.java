package com.guesstheword.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityRoleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void testUnauthenticatedAccessToPingFails() throws Exception {
        mockMvc.perform(get("/api/game/ping"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testPlayerCanAccessGamePing() throws Exception {
        String token = jwtTokenProvider.generateTokenForUser("PlayerOne", "PLAYER");

        mockMvc.perform(get("/api/game/ping")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.access").value("PLAYER"));
    }

    @Test
    void testPlayerCannotAccessAdminPing() throws Exception {
        String token = jwtTokenProvider.generateTokenForUser("PlayerOne", "PLAYER");

        mockMvc.perform(get("/api/admin/ping")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminCanAccessAdminPing() throws Exception {
        String token = jwtTokenProvider.generateTokenForUser("AdminUser", "ADMIN");

        mockMvc.perform(get("/api/admin/ping")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.access").value("ADMIN"));
    }

    @Test
    void testAdminCanAccessGamePing() throws Exception {
        String token = jwtTokenProvider.generateTokenForUser("AdminUser", "ADMIN");

        mockMvc.perform(get("/api/game/ping")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
