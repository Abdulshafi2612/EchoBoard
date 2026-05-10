package com.echoboard.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PollOptionCountCacheServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PollOptionCountCacheServiceImpl pollOptionCountCacheService;

    @Test
    void incrementPollOptionCount_shouldSetInitialDbCountIfAbsentThenIncrement() {
        Long pollId = 1L;
        Long pollOptionId = 10L;
        int currentDbCount = 5;

        String cacheKey = "poll:1:option:10:count";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        pollOptionCountCacheService.incrementPollOptionCount(
                pollId,
                pollOptionId,
                currentDbCount
        );

        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOperations).setIfAbsent(cacheKey, "5");
        verify(valueOperations).increment(cacheKey);
    }

    @Test
    void getPollOptionCount_whenCachedValueExists_shouldReturnCachedCount() {
        Long pollId = 1L;
        Long pollOptionId = 10L;
        int fallbackCount = 5;

        String cacheKey = "poll:1:option:10:count";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn("8");

        int result = pollOptionCountCacheService.getPollOptionCount(
                pollId,
                pollOptionId,
                fallbackCount
        );

        assertEquals(8, result);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(cacheKey);
    }

    @Test
    void getPollOptionCount_whenCachedValueDoesNotExist_shouldReturnFallbackCount() {
        Long pollId = 1L;
        Long pollOptionId = 10L;
        int fallbackCount = 5;

        String cacheKey = "poll:1:option:10:count";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn(null);

        int result = pollOptionCountCacheService.getPollOptionCount(
                pollId,
                pollOptionId,
                fallbackCount
        );

        assertEquals(fallbackCount, result);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(cacheKey);
    }

    @Test
    void getCachedPollOptionCount_whenCachedValueExists_shouldReturnCachedCount() {
        Long pollId = 1L;
        Long pollOptionId = 10L;

        String cacheKey = "poll:1:option:10:count";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn("12");

        Integer result = pollOptionCountCacheService.getCachedPollOptionCount(
                pollId,
                pollOptionId
        );

        assertEquals(12, result);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(cacheKey);
    }

    @Test
    void getCachedPollOptionCount_whenCachedValueDoesNotExist_shouldReturnNull() {
        Long pollId = 1L;
        Long pollOptionId = 10L;

        String cacheKey = "poll:1:option:10:count";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn(null);

        Integer result = pollOptionCountCacheService.getCachedPollOptionCount(
                pollId,
                pollOptionId
        );

        assertNull(result);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(cacheKey);
    }
}