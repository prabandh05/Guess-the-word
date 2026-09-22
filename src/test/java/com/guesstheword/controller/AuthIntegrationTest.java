package com.guesstheword.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guesstheword.dto.LoginRequest;
import com.guesstheword.dto.RegisterRequest;
import com.guesstheword.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("NewPlayer")
                .password("Secret123$")
                .role(Role.PLAYER)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is("NewPlayer")))
                .andExpect(jsonPath("$.data.role", is("PLAYER")))
                .andExpect(jsonPath("$.data.token", notNullValue()));
    }

    @Test
    void testRegisterDuplicateUsername() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("DuplicateUser")
                .password("Secret123%")
                .build();

        // Register first time
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Register second time with same username
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void testRegisterInvalidUsernameFailsValidation() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("alllowercase")
                .password("Secret123*")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Validation failed")));
    }

    @Test
    void testRegisterInvalidPasswordFailsValidation() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("ValidUser")
                .password("no_special_123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Validation failed")));
    }

    @Test
    void testLoginSuccess() throws Exception {
        // Default seeded user: PlayerOne / Player123$
        LoginRequest request = LoginRequest.builder()
                .username("PlayerOne")
                .password("Player123$")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.username", is("PlayerOne")))
                .andExpect(jsonPath("$.data.role", is("PLAYER")));
    }

    @Test
    void testLoginInvalidCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("PlayerOne")
                .password("WrongPassword123$")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
