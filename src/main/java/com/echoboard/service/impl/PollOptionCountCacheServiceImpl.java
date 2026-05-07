package com.echoboard.service.impl;

import com.echoboard.service.PollOptionCountCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PollOptionCountCacheServiceImpl implements PollOptionCountCacheService {

    private static final String POLL_OPTION_COUNT_CACHE_KEY_TEMPLATE = "poll:%d:option:%d:count";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void incrementPollOptionCount(Long pollId, Long pollOptionId, int currentDbCount) {
        String pollOptionCountKey = POLL_OPTION_COUNT_CACHE_KEY_TEMPLATE.formatted(pollId, pollOptionId);

        redisTemplate.opsForValue().setIfAbsent(
                pollOptionCountKey,
                String.valueOf(currentDbCount)
        );

        redisTemplate.opsForValue().increment(pollOptionCountKey);
    }

    @Override
    public int getPollOptionCount(Long pollId, Long pollOptionId, int fallbackCount) {
        String pollOptionCountKey = POLL_OPTION_COUNT_CACHE_KEY_TEMPLATE.formatted(pollId, pollOptionId);

        String value = redisTemplate.opsForValue().get(pollOptionCountKey);

        if (value == null) {
            return fallbackCount;
        }

        return Integer.parseInt(value);
    }
}