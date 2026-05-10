package com.echoboard.util;

import com.echoboard.entity.Session;
import com.echoboard.entity.User;
import com.echoboard.enums.SessionStatus;

import java.time.LocalDateTime;

public final class SessionEntityFactory {

    private SessionEntityFactory() {
    }

    public static Session scheduledSession() {
        return sessionWithStatus(SessionStatus.SCHEDULED);
    }

    public static Session liveSession() {
        return sessionWithStatus(SessionStatus.LIVE);
    }

    public static Session endedSession() {
        Session session = sessionWithStatus(SessionStatus.ENDED);

        session.setStartedAt(LocalDateTime.now().minusHours(2));
        session.setEndedAt(LocalDateTime.now().minusMinutes(10));

        return session;
    }

    public static Session archivedSession() {
        return sessionWithStatus(SessionStatus.ARCHIVED);
    }

    public static Session sessionWithStatus(SessionStatus status) {
        User owner = UserEntityFactory.presenter();

        Session session = new Session();

        session.setId(1L);
        session.setTitle("Test Session");
        session.setDescription("Test session description");
        session.setAccessCode("ABC123");
        session.setStatus(status);
        session.setOwner(owner);
        session.setLogoUrl(null);
        session.setModerationEnabled(true);
        session.setAnonymousAllowed(true);
        session.setCreatedAt(LocalDateTime.now().minusDays(1));

        if (status == SessionStatus.LIVE) {
            session.setStartedAt(LocalDateTime.now().minusMinutes(30));
        }

        if (status == SessionStatus.ENDED || status == SessionStatus.ARCHIVED) {
            session.setStartedAt(LocalDateTime.now().minusHours(2));
            session.setEndedAt(LocalDateTime.now().minusMinutes(30));
        }

        return session;
    }

    public static Session liveSessionOwnedBy(User owner) {
        Session session = liveSession();

        session.setOwner(owner);

        return session;
    }
}