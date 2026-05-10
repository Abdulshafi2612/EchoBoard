package com.echoboard.util;

import com.echoboard.dto.session.SessionResponse;
import com.echoboard.entity.Session;

public final class SessionResponseFactory {

    private SessionResponseFactory() {
    }

    public static SessionResponse fromSession(Session session) {
        return new SessionResponse(
                session.getId(),
                session.getTitle(),
                session.getDescription(),
                session.getAccessCode(),
                session.getStatus(),
                session.isModerationEnabled(),
                session.isAnonymousAllowed(),
                session.getOwner().getName(),
                session.getOwner().getId(),
                session.getLogoUrl(),
                session.getCreatedAt(),
                session.getStartedAt(),
                session.getEndedAt()
        );
    }
}