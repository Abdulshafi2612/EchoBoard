package com.echoboard.service;

public interface PollOptionCountCacheService {

    void incrementPollOptionCount(Long pollId, Long pollOptionId, int currentDbCount);

    int getPollOptionCount(Long pollId, Long pollOptionId, int fallbackCount);
}
