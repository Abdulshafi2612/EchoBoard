package com.echoboard.service;

import java.time.Duration;

public interface RateLimiterService {

    boolean isAllowed(String key, int maxRequests, Duration window);
}
