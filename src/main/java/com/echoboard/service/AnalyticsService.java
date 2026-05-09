package com.echoboard.service;

import com.echoboard.dto.analytics.PollAnalyticsResponse;
import com.echoboard.dto.analytics.SessionAnalyticsResponse;

import java.util.List;

public interface AnalyticsService {

    SessionAnalyticsResponse getSessionAnalytics(Long sessionId);

    String exportCsvSessionQuestions(Long sessionId);

    List<PollAnalyticsResponse> getPollAnalytics(Long sessionId);

    void generateSessionAnalyticsSnapshot(Long sessionId);

}