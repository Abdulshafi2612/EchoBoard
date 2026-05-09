package com.echoboard.service;

import com.echoboard.dto.analytics.SessionAnalyticsResponse;

public interface AnalyticsService {

    SessionAnalyticsResponse getSessionAnalytics(Long sessionId);

    String exportCsvSessionQuestions(Long sessionId);
}