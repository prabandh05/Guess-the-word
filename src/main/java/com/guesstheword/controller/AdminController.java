package com.guesstheword.controller;

import com.guesstheword.dto.ApiResponse;
import com.guesstheword.dto.DailyReportResponse;
import com.guesstheword.dto.UserReportResponse;
import com.guesstheword.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminReportService adminReportService;

    /**
     * GET /api/admin/reports/daily?date=YYYY-MM-DD
     * Returns: number of distinct users who played + number of correct guesses on that date.
     */
    @GetMapping("/reports/daily")
    public ResponseEntity<ApiResponse<DailyReportResponse>> getDailyReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        DailyReportResponse report = adminReportService.getDailyReport(date);
        return ResponseEntity.ok(ApiResponse.<DailyReportResponse>builder()
                .success(true)
                .message("Daily report for " + date)
                .data(report)
                .build());
    }

    /**
     * GET /api/admin/reports/user/{userId}
     * Returns: per-date breakdown of words tried and correct guesses for the given user.
     */
    @GetMapping("/reports/user/{userId}")
    public ResponseEntity<ApiResponse<UserReportResponse>> getUserReport(
            @PathVariable Long userId) {

        UserReportResponse report = adminReportService.getUserReport(userId);
        return ResponseEntity.ok(ApiResponse.<UserReportResponse>builder()
                .success(true)
                .message("Activity report for user id " + userId)
                .data(report)
                .build());
    }
}
