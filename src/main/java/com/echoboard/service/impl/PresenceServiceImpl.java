package com.echoboard.service.impl;

import com.echoboard.dto.websocket.PresenceEvent;
import com.echoboard.entity.Session;
import com.echoboard.enums.PresenceEventType;
import com.echoboard.enums.SessionStatus;
import com.echoboard.exception.AppException;
import com.echoboard.exception.ErrorCode;
import com.echoboard.service.PresenceService;
import com.echoboard.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.echoboard.enums.PresenceEventType.JOINED;
import static com.echoboard.enums.PresenceEventType.LEFT;
import static com.echoboard.enums.TokenType.ACCESS;
import static com.echoboard.enums.TokenType.PARTICIPANT;
import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class PresenceServiceImpl implements PresenceService {

    private final Map<Long, Integer> onlineCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> webSocketSessionToSessionId = new ConcurrentHashMap<>();

    private final SimpMessagingTemplate messagingTemplate;
    private final SessionService sessionService;

    @Override
    public void join(Long sessionId, String webSocketSessionId, String tokenType, Long tokenSessionId) {
        if (webSocketSessionToSessionId.containsKey(webSocketSessionId)) {
            return;
        }

        validateLiveSession(sessionId);
        validatePresenceAccess(sessionId, tokenType, tokenSessionId);

        webSocketSessionToSessionId.put(webSocketSessionId, sessionId);

        int count = onlineCounts.merge(sessionId, 1, Integer::sum);
        broadcastPresenceEvent(sessionId, count, JOINED);
    }

    @Override
    public void leave(String webSocketSessionId) {
        Long sessionId = webSocketSessionToSessionId.remove(webSocketSessionId);
        if (sessionId == null) {
            return;
        }

        Integer count = onlineCounts.computeIfPresent(sessionId, (key, currentCount) -> {
            int newCount = currentCount - 1;
            return newCount <= 0 ? null : newCount;
        });

        count = count == null ? 0 : count;
        broadcastPresenceEvent(sessionId, count, LEFT);
    }

    @Override
    public void disconnect(String webSocketSessionId) {
        leave(webSocketSessionId);
    }

    private void validateLiveSession(Long sessionId) {
        Session session = sessionService.getSessionById(sessionId);

        if (session == null) {
            throw new AppException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    NOT_FOUND,
                    "Session not found"
            );
        }

        if (!SessionStatus.LIVE.equals(session.getStatus())) {
            throw new AppException(
                    ErrorCode.INVALID_SESSION_STATUS,
                    BAD_REQUEST,
                    "Cannot join presence for a non-live session"
            );
        }
    }

    private void validatePresenceAccess(Long sessionId, String tokenType, Long tokenSessionId) {
        if (PARTICIPANT.name().equals(tokenType)) {
            validateParticipantPresenceAccess(sessionId, tokenSessionId);
            return;
        }

        if (ACCESS.name().equals(tokenType)) {
            return;
        }

        throw new AppException(
                ErrorCode.UNAUTHORIZED,
                UNAUTHORIZED,
                "Unsupported WebSocket token type"
        );
    }

    private void validateParticipantPresenceAccess(Long sessionId, Long tokenSessionId) {
        if (!sessionId.equals(tokenSessionId)) {
            throw new AppException(
                    ErrorCode.FORBIDDEN,
                    FORBIDDEN,
                    "Participant is not allowed to join this session presence"
            );
        }
    }

    private void broadcastPresenceEvent(Long sessionId, int count, PresenceEventType type) {
        PresenceEvent event = PresenceEvent
                .builder()
                .sessionId(sessionId)
                .type(type)
                .onlineCount(count)
                .build();

        messagingTemplate.convertAndSend(
                "/topic/sessions/" + sessionId + "/presence",
                event
        );
    }
}