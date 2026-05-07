package com.echoboard.service.impl;

import com.echoboard.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimiterServiceImpl implements RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean isAllowed(String key, int maxRequests, Duration window) {

        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) {
            return false;
        }
        if (count == 1) {
            redisTemplate.expire(key, window);
        }
        return count <= maxRequests;
    }
}
