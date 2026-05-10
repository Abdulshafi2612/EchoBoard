package com.echoboard.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimiterServiceImpl rateLimiterService;

    @Test
    void isAllowed_whenRedisIncrementReturnsNull_shouldReturnFalse() {
        String key = "rate:participant:1:questions";
        int maxRequests = 5;
        Duration window = Duration.ofSeconds(60);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(null);

        boolean result = rateLimiterService.isAllowed(key, maxRequests, window);

        assertFalse(result);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).increment(key);
        verify(redisTemplate, never()).expire(key, window);
    }

    @Test
    void isAllowed_whenFirstRequest_shouldSetExpirationAndReturnTrue() {
        String key = "rate:participant:1:questions";
        int maxRequests = 5;
        Duration window = Duration.ofSeconds(60);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(1L);

        boolean result = rateLimiterService.isAllowed(key, maxRequests, window);

        assertTrue(result);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).increment(key);
        verify(redisTemplate).expire(key, window);
    }

    @Test
    void isAllowed_whenCountIsBelowMaxRequests_shouldReturnTrueWithoutResettingExpiration() {
        String key = "rate:participant:1:questions";
        int maxRequests = 5;
        Duration window = Duration.ofSeconds(60);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(3L);

        boolean result = rateLimiterService.isAllowed(key, maxRequests, window);

        assertTrue(result);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).increment(key);
        verify(redisTemplate, never()).expire(key, window);
    }

    @Test
    void isAllowed_whenCountEqualsMaxRequests_shouldReturnTrue() {
        String key = "rate:participant:1:questions";
        int maxRequests = 5;
        Duration window = Duration.ofSeconds(60);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(5L);

        boolean result = rateLimiterService.isAllowed(key, maxRequests, window);

        assertTrue(result);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).increment(key);
        verify(redisTemplate, never()).expire(key, window);
    }

    @Test
    void isAllowed_whenCountIsGreaterThanMaxRequests_shouldReturnFalse() {
        String key = "rate:participant:1:questions";
        int maxRequests = 5;
        Duration window = Duration.ofSeconds(60);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(6L);

        boolean result = rateLimiterService.isAllowed(key, maxRequests, window);

        assertFalse(result);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).increment(key);
        verify(redisTemplate, never()).expire(key, window);
    }
}