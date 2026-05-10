package com.echoboard.service.impl;

import com.echoboard.dto.websocket.PresenceEvent;
import com.echoboard.entity.Session;
import com.echoboard.enums.PresenceEventType;
import com.echoboard.enums.SessionStatus;
import com.echoboard.exception.AppException;
import com.echoboard.service.SessionService;
import com.echoboard.util.SessionEntityFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Duration;

import static com.echoboard.enums.TokenType.ACCESS;
import static com.echoboard.enums.TokenType.PARTICIPANT;
import static com.echoboard.exception.ErrorCode.FORBIDDEN;
import static com.echoboard.exception.ErrorCode.INVALID_SESSION_STATUS;
import static com.echoboard.exception.ErrorCode.RESOURCE_NOT_FOUND;
import static com.echoboard.exception.ErrorCode.UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresenceServiceImplTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private SessionService sessionService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PresenceServiceImpl presenceService;

    @Test
    void join_whenWebSocketSessionAlreadyExists_shouldReturnWithoutDoingAnything() {
        Long sessionId = 1L;
        String webSocketSessionId = "ws-1";
        String webSocketPresenceKey = "presence:ws:ws-1";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(webSocketPresenceKey)).thenReturn("1");

        presenceService.join(
                sessionId,
                webSocketSessionId,
                PARTICIPANT.name(),
                sessionId
        );

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(webSocketPresenceKey);

        verify(sessionService, never()).getSessionById(sessionId);
        verify(valueOperations, never()).set(
                webSocketPresenceKey,
                String.valueOf(sessionId),
                Duration.ofSeconds(30)
        );
        verify(valueOperations, never()).increment("presence:session:1:count");
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void join_whenParticipantTokenIsValid_shouldStorePresenceIncrementCountAndBroadcastJoinedEvent() {
        Long sessionId = 1L;
        String webSocketSessionId = "ws-1";
        String webSocketPresenceKey = "presence:ws:ws-1";
        String presenceCountKey = "presence:session:1:count";

        Session session = SessionEntityFactory.liveSession();
        session.setId(sessionId);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(webSocketPresenceKey)).thenReturn(null);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(valueOperations.increment(presenceCountKey)).thenReturn(3L);

        presenceService.join(
                sessionId,
                webSocketSessionId,
                PARTICIPANT.name(),
                sessionId
        );

        verify(valueOperations).get(webSocketPresenceKey);
        verify(sessionService).getSessionById(sessionId);
        verify(valueOperations).set(
                webSocketPresenceKey,
                String.valueOf(sessionId),
                Duration.ofSeconds(30)
        );
        verify(valueOperations).increment(presenceCountKey);

        ArgumentCaptor<PresenceEvent> eventCaptor = ArgumentCaptor.forClass(PresenceEvent.class);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/1/presence"),
                eventCaptor.capture()
        );

        PresenceEvent event = eventCaptor.getValue();

        assertEquals(sessionId, event.getSessionId());
        assertEquals(PresenceEventType.JOINED, event.getType());
        assertEquals(3, event.getOnlineCount());
    }

    @Test
    void join_whenAccessTokenIsValid_shouldStorePresenceIncrementCountAndBroadcastJoinedEvent() {
        Long sessionId = 1L;
        String webSocketSessionId = "ws-1";
        String webSocketPresenceKey = "presence:ws:ws-1";
        String presenceCountKey = "presence:session:1:count";

        Session session = SessionEntityFactory.liveSession();
        session.setId(sessionId);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(webSocketPresenceKey)).thenReturn(null);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(valueOperations.increment(presenceCountKey)).thenReturn(1L);

        presenceService.join(
                sessionId,
                webSocketSessionId,
                ACCESS.name(),
                null
        );

        verify(valueOperations).set(
                webSocketPresenceKey,
                String.valueOf(sessionId),
                Duration.ofSeconds(30)
        );
        verify(valueOperations).increment(presenceCountKey);

        ArgumentCaptor<PresenceEvent> eventCaptor = ArgumentCaptor.forClass(PresenceEvent.class);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/1/presence"),
                eventCaptor.capture()
        );

        PresenceEvent event = eventCaptor.getValue();

        assertEquals(sessionId, event.getSessionId());
        assertEquals(PresenceEventType.JOINED, event.getType());
        assertEquals(1, event.getOnlineCount());
    }

    @Test
    void join_whenSessionDoesNotExist_shouldThrowResourceNotFoundException() {
        Long sessionId = 404L;
        String webSocketSessionId = "ws-1";
        String webSocketPresenceKey = "presence:ws:ws-1";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(webSocketPresenceKey)).thenReturn(null);
        when(sessionService.getSessionById(sessionId)).thenReturn(null);

        AppException exception = assertThrows(
                AppException.class,
                () -> presenceService.join(
                        sessionId,
                        webSocketSessionId,
                        PARTICIPANT.name(),
                        sessionId
                )
        );

        assertEquals(RESOURCE_NOT_FOUND, exception.getErrorCode());
        assertEquals("Session not found", exception.getMessage());

        verify(valueOperations).get(webSocketPresenceKey);
        verify(sessionService).getSessionById(sessionId);
        verify(valueOperations, never()).set(
                webSocketPresenceKey,
                String.valueOf(sessionId),
                Duration.ofSeconds(30)
        );
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void join_whenSessionIsNotLive_shouldThrowInvalidSessionStatusException() {
        Long sessionId = 1L;
        String webSocketSessionId = "ws-1";
        String webSocketPresenceKey = "presence:ws:ws-1";

        Session session = SessionEntityFactory.sessionWithStatus(SessionStatus.SCHEDULED);
        session.setId(sessionId);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(webSocketPresenceKey)).thenReturn(null);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);

        AppException exception = assertThrows(
                AppException.class,
                () -> presenceService.join(
                        sessionId,
                        webSocketSessionId,
                        PARTICIPANT.name(),
                        sessionId
                )
        );

        assertEquals(INVALID_SESSION_STATUS, exception.getErrorCode());
        assertEquals("Cannot join presence for a non-live session", exception.getMessage());

        verify(valueOperations).get(webSocketPresenceKey);
        verify(sessionService).getSessionById(sessionId);
        verify(valueOperations, never()).set(
                webSocketPresenceKey,
                String.valueOf(sessionId),
                Duration.ofSeconds(30)
        );
    }

    @Test
    void join_whenParticipantTokenSessionDoesNotMatchRequestedSession_shouldThrowForbiddenException() {
        Long requestedSessionId = 1L;
        Long tokenSessionId = 2L;
        String webSocketSessionId = "ws-1";
        String webSocketPresenceKey = "presence:ws:ws-1";

        Session session = SessionEntityFactory.liveSession();
        session.setId(requestedSessionId);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(webSocketPresenceKey)).thenReturn(null);
        when(sessionService.getSessionById(requestedSessionId)).thenReturn(session);

        AppException exception = assertThrows(
                AppException.class,
                () -> presenceService.join(
                        requestedSessionId,
                        webSocketSessionId,
                        PARTICIPANT.name(),
                        tokenSessionId
                )
        );

        assertEquals(FORBIDDEN, exception.getErrorCode());
        assertEquals("Participant is not allowed to join this session presence", exception.getMessage());

        verify(valueOperations).get(webSocketPresenceKey);
        verify(sessionService).getSessionById(requestedSessionId);
        verify(valueOperations, never()).set(
                webSocketPresenceKey,
                String.valueOf(requestedSessionId),
                Duration.ofSeconds(30)
        );
    }

    @Test
    void join_whenTokenTypeIsUnsupported_shouldThrowUnauthorizedException() {
        Long sessionId = 1L;
        String webSocketSessionId = "ws-1";
        String webSocketPresenceKey = "presence:ws:ws-1";

        Session session = SessionEntityFactory.liveSession();
        session.setId(sessionId);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(webSocketPresenceKey)).thenReturn(null);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);

        AppException exception = assertThrows(
                AppException.class,
                () -> presenceService.join(
                        sessionId,
                        webSocketSessionId,
                        "REFRESH",
                        sessionId
                )
        );

        assertEquals(UNAUTHORIZED, exception.getErrorCode());
        assertEquals("Unsupported WebSocket token type", exception.getMessage());

        verify(valueOperations).get(webSocketPresenceKey);
        verify(sessionService).getSessionById(sessionId);
        verify(valueOperations, never()).set(
                webSocketPresenceKey,
                String.valueOf(sessionId),
                Duration.ofSeconds(30)
        );
    }

    @Test
    void join_whenIncrementReturnsNull_shouldStorePresenceButNotBroadcast() {
        Long sessionId = 1L;
        String webSocketSessionId = "ws-1";
        String webSocketPresenceKey = "presence:ws:ws-1";
        String presenceCountKey = "presence:session:1:count";

        Session session = SessionEntityFactory.liveSession();
        session.setId(sessionId);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(webSocketPresenceKey)).thenReturn(null);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(valueOperations.increment(presenceCountKey)).thenReturn(null);

        presenceService.join(
                sessionId,
                webSocketSessionId,
                ACCESS.name(),
                null
        );

        verify(valueOperations).set(
                webSocketPresenceKey,
                String.valueOf(sessionId),
                Duration.ofSeconds(30)
        );
        verify(valueOperations).increment(presenceCountKey);
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void leave_whenWebSocketSessionIsNotCached_shouldReturnWithoutDoingAnything() {
        String webSocketSessionId = "ws-1";
        String webSocketPresenceKey = "presence:ws:ws-1";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(webSocketPresenceKey)).thenReturn(null);

        presenceService.leave(webSocketSessionId);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(webSocketPresenceKey);
        verify(redisTemplate, never()).delete(webSocketPresenceKey);
        verify(valueOperations, never()).decrement("presence:session:1:count");
    }

    @Test
    void leave_whenCachedSessionExistsAndCountStaysPositive_shouldDeleteWebSocketKeyDecrementAndBroadcastLeftEvent() {
        String webSocketSessionId = "ws-1";
        String webSocketPresenceKey = "presence:ws:ws-1";
        String presenceCountKey = "presence:session:1:count";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(webSocketPresenceKey)).thenReturn("1");
        when(valueOperations.decrement(presenceCountKey)).thenReturn(2L);

        presenceService.leave(webSocketSessionId);

        verify(valueOperations).get(webSocketPresenceKey);
        verify(redisTemplate).delete(webSocketPresenceKey);
        verify(valueOperations).decrement(presenceCountKey);

        ArgumentCaptor<PresenceEvent> eventCaptor = ArgumentCaptor.forClass(PresenceEvent.class);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/1/presence"),
                eventCaptor.capture()
        );

        PresenceEvent event = eventCaptor.getValue();

        assertEquals(1L, event.getSessionId());
        assertEquals(PresenceEventType.LEFT, event.getType());
        assertEquals(2, event.getOnlineCount());
    }

    @Test
    void leave_whenCountBecomesZero_shouldDeletePresenceCountKeyAndBroadcastZeroOnlineCount() {
        String webSocketSessionId = "ws-1";
        String webSocketPresenceKey = "presence:ws:ws-1";
        String presenceCountKey = "presence:session:1:count";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(webSocketPresenceKey)).thenReturn("1");
        when(valueOperations.decrement(presenceCountKey)).thenReturn(0L);

        presenceService.leave(webSocketSessionId);

        verify(valueOperations).get(webSocketPresenceKey);
        verify(redisTemplate).delete(webSocketPresenceKey);
        verify(valueOperations).decrement(presenceCountKey);
        verify(redisTemplate).delete(presenceCountKey);

        ArgumentCaptor<PresenceEvent> eventCaptor = ArgumentCaptor.forClass(PresenceEvent.class);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/1/presence"),
                eventCaptor.capture()
        );

        PresenceEvent event = eventCaptor.getValue();

        assertEquals(1L, event.getSessionId());
        assertEquals(PresenceEventType.LEFT, event.getType());
        assertEquals(0, event.getOnlineCount());
    }

    @Test
    void leave_whenDecrementReturnsNull_shouldDeletePresenceCountKeyAndBroadcastZeroOnlineCount() {
        String webSocketSessionId = "ws-1";
        String webSocketPresenceKey = "presence:ws:ws-1";
        String presenceCountKey = "presence:session:1:count";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(webSocketPresenceKey)).thenReturn("1");
        when(valueOperations.decrement(presenceCountKey)).thenReturn(null);

        presenceService.leave(webSocketSessionId);

        verify(valueOperations).get(webSocketPresenceKey);
        verify(redisTemplate).delete(webSocketPresenceKey);
        verify(valueOperations).decrement(presenceCountKey);
        verify(redisTemplate).delete(presenceCountKey);

        ArgumentCaptor<PresenceEvent> eventCaptor = ArgumentCaptor.forClass(PresenceEvent.class);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/1/presence"),
                eventCaptor.capture()
        );

        PresenceEvent event = eventCaptor.getValue();

        assertEquals(1L, event.getSessionId());
        assertEquals(PresenceEventType.LEFT, event.getType());
        assertEquals(0, event.getOnlineCount());
    }

    @Test
    void disconnect_shouldDelegateToLeave() {
        String webSocketSessionId = "ws-1";
        String webSocketPresenceKey = "presence:ws:ws-1";
        String presenceCountKey = "presence:session:1:count";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(webSocketPresenceKey)).thenReturn("1");
        when(valueOperations.decrement(presenceCountKey)).thenReturn(0L);

        presenceService.disconnect(webSocketSessionId);

        verify(valueOperations).get(webSocketPresenceKey);
        verify(redisTemplate).delete(webSocketPresenceKey);
        verify(valueOperations).decrement(presenceCountKey);
        verify(redisTemplate).delete(presenceCountKey);
    }
}