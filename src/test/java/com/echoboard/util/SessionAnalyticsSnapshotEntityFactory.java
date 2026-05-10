package com.echoboard.util;

import com.echoboard.entity.Session;
import com.echoboard.entity.SessionAnalyticsSnapshot;

import java.time.LocalDateTime;

public final class SessionAnalyticsSnapshotEntityFactory {

    private SessionAnalyticsSnapshotEntityFactory() {
    }

    public static SessionAnalyticsSnapshot snapshot() {
        return snapshotForSession(SessionEntityFactory.endedSession());
    }

    public static SessionAnalyticsSnapshot snapshotForSession(Session session) {
        return SessionAnalyticsSnapshot
                .builder()
                .id(1L)
                .session(session)
                .totalParticipants(10)
                .totalQuestions(20)
                .pendingQuestions(2)
                .approvedQuestions(8)
                .answeredQuestions(7)
                .hiddenQuestions(3)
                .totalPolls(4)
                .totalPollVotes(30)
                .generatedAt(LocalDateTime.now().minusMinutes(5))
                .build();
    }
}