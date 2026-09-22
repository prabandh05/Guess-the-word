package com.guesstheword.controller;

import com.guesstheword.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/game/ping")
    public ResponseEntity<ApiResponse<Map<String, String>>> gamePing(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, String> data = new HashMap<>();
        data.put("status", "UP");
        data.put("user", userDetails != null ? userDetails.getUsername() : "anonymous");
        data.put("access", "PLAYER");
        return ResponseEntity.ok(ApiResponse.success("Player access confirmed", data));
    }

    @GetMapping("/admin/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> adminPing(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, String> data = new HashMap<>();
        data.put("status", "UP");
        data.put("user", userDetails != null ? userDetails.getUsername() : "anonymous");
        data.put("access", "ADMIN");
        return ResponseEntity.ok(ApiResponse.success("Admin access confirmed", data));
    }
}
