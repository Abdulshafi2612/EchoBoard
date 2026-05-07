package com.echoboard.service.impl;

import com.echoboard.entity.Session;
import com.echoboard.service.SessionAccessCodeCacheService;
import com.echoboard.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SessionAccessCodeCacheServiceImpl implements SessionAccessCodeCacheService {

    private static final String SESSION_ACCESS_CODE_CACHE_KEY_TEMPLATE = "session:code:%s";
    private static final Duration SESSION_ACCESS_CODE_CACHE_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final SessionService sessionService;

    @Override
    public Session getSessionByAccessCode(String accessCode) {
        String key = SESSION_ACCESS_CODE_CACHE_KEY_TEMPLATE.formatted(accessCode);

        String cachedSessionId = redisTemplate.opsForValue().get(key);

        if (cachedSessionId != null) {
            return sessionService.getSessionById(Long.valueOf(cachedSessionId));
        }

        Session session = sessionService.getSessionByAccessCode(accessCode);

        redisTemplate.opsForValue().set(
                key,
                String.valueOf(session.getId()),
                SESSION_ACCESS_CODE_CACHE_TTL
        );

        return session;
    }
}