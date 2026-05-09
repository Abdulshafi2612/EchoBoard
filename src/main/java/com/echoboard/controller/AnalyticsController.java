package com.echoboard.controller;

import com.echoboard.dto.analytics.SessionAnalyticsResponse;
import com.echoboard.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<SessionAnalyticsResponse> getSessionAnalytics(@PathVariable Long sessionId) {
        SessionAnalyticsResponse response = analyticsService.getSessionAnalytics(sessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportSessionQuestionsAsCsv(@PathVariable Long sessionId) {
        String csv = analyticsService.exportCsvSessionQuestions(sessionId);

        return ResponseEntity.ok()
                .header(CONTENT_DISPOSITION, "attachment; filename=\"session-" + sessionId + "-questions.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}