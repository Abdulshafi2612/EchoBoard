package com.echoboard.service.impl;

import com.echoboard.entity.Session;
import com.echoboard.service.SessionService;
import com.echoboard.util.SessionEntityFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionAccessCodeCacheServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private SessionAccessCodeCacheServiceImpl sessionAccessCodeCacheService;

    @Test
    void getSessionByAccessCode_whenCacheHit_shouldReturnSessionFromCachedSessionId() {
        String accessCode = "ABC123";
        String cacheKey = "session:code:ABC123";

        Session session = SessionEntityFactory.liveSession();
        session.setId(1L);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn("1");
        when(sessionService.getSessionById(1L)).thenReturn(session);

        Session result = sessionAccessCodeCacheService.getSessionByAccessCode(accessCode);

        assertEquals(session, result);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(cacheKey);
        verify(sessionService).getSessionById(1L);

        verify(sessionService, never()).getSessionByAccessCode(accessCode);
        verify(valueOperations, never()).set(
                cacheKey,
                String.valueOf(session.getId()),
                Duration.ofMinutes(10)
        );
    }

    @Test
    void getSessionByAccessCode_whenCacheMiss_shouldLoadSessionByAccessCodeAndCacheSessionId() {
        String accessCode = "ABC123";
        String cacheKey = "session:code:ABC123";

        Session session = SessionEntityFactory.liveSession();
        session.setId(1L);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn(null);
        when(sessionService.getSessionByAccessCode(accessCode)).thenReturn(session);

        Session result = sessionAccessCodeCacheService.getSessionByAccessCode(accessCode);

        assertEquals(session, result);

        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOperations).get(cacheKey);
        verify(sessionService).getSessionByAccessCode(accessCode);
        verify(valueOperations).set(
                cacheKey,
                String.valueOf(session.getId()),
                Duration.ofMinutes(10)
        );

        verify(sessionService, never()).getSessionById(session.getId());
    }

    @Test
    void getSessionByAccessCode_whenCacheHit_shouldNotWriteToCacheAgain() {
        String accessCode = "XYZ789";
        String cacheKey = "session:code:XYZ789";

        Session session = SessionEntityFactory.liveSession();
        session.setId(25L);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn("25");
        when(sessionService.getSessionById(25L)).thenReturn(session);

        Session result = sessionAccessCodeCacheService.getSessionByAccessCode(accessCode);

        assertEquals(session, result);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(cacheKey);
        verify(sessionService).getSessionById(25L);

        verify(valueOperations, never()).set(
                cacheKey,
                "25",
                Duration.ofMinutes(10)
        );

        verifyNoMoreInteractions(sessionService);
    }
}