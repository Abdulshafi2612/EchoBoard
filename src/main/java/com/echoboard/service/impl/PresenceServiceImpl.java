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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static com.echoboard.enums.PresenceEventType.JOINED;
import static com.echoboard.enums.PresenceEventType.LEFT;
import static com.echoboard.enums.TokenType.ACCESS;
import static com.echoboard.enums.TokenType.PARTICIPANT;
import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class PresenceServiceImpl implements PresenceService {

    private static final String PRESENCE_COUNT_KEY_TEMPLATE = "presence:session:%d:count";
    private static final String PRESENCE_WEBSOCKET_SESSION_KEY_TEMPLATE = "presence:ws:%s";
    private static final Duration PRESENCE_SESSION_TTL = Duration.ofSeconds(30);

    private final SimpMessagingTemplate messagingTemplate;
    private final SessionService sessionService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void join(Long sessionId, String webSocketSessionId, String tokenType, Long tokenSessionId) {

        if (redisTemplate
                .opsForValue()
                .get(PRESENCE_WEBSOCKET_SESSION_KEY_TEMPLATE.formatted(webSocketSessionId))
                != null
        ) {
            return;
        }

        validateLiveSession(sessionId);
        validatePresenceAccess(sessionId, tokenType, tokenSessionId);


        String webSocketPresenceKey = PRESENCE_WEBSOCKET_SESSION_KEY_TEMPLATE.formatted(webSocketSessionId);


        redisTemplate.opsForValue().set(
                webSocketPresenceKey,
                String.valueOf(sessionId),
                PRESENCE_SESSION_TTL
        );

        String presenceCountKey = PRESENCE_COUNT_KEY_TEMPLATE.formatted(sessionId);

        Long count = redisTemplate.opsForValue().increment(presenceCountKey);

        if (count == null) {
            return;
        }

        broadcastPresenceEvent(sessionId, count.intValue(), JOINED);
    }

    @Override
    public void leave(String webSocketSessionId) {
        String webSocketPresenceKey = PRESENCE_WEBSOCKET_SESSION_KEY_TEMPLATE.formatted(webSocketSessionId);

        String cachedSessionId = redisTemplate.opsForValue().get(webSocketPresenceKey);

        if (cachedSessionId == null) {
            return;
        }

        redisTemplate.delete(webSocketPresenceKey);

        Long sessionId = Long.valueOf(cachedSessionId);
        String presenceCountKey = PRESENCE_COUNT_KEY_TEMPLATE.formatted(sessionId);

        Long count = redisTemplate.opsForValue().decrement(presenceCountKey);

        if (count == null || count <= 0) {
            redisTemplate.delete(presenceCountKey);
            count = 0L;
        }

        broadcastPresenceEvent(sessionId, count.intValue(), LEFT);
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