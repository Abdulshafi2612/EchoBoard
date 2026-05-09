package com.echoboard.service;

public interface PollCounterSyncService {

    void syncPollCountersForSession(Long sessionId);

    void syncPollCounts(Long pollId);
}