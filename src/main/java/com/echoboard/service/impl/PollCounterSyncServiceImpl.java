package com.echoboard.service.impl;

import com.echoboard.entity.Poll;
import com.echoboard.entity.PollOption;
import com.echoboard.repository.PollOptionRepository;
import com.echoboard.repository.PollRepository;
import com.echoboard.service.PollCounterSyncService;
import com.echoboard.service.PollOptionCountCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PollCounterSyncServiceImpl implements PollCounterSyncService {

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollOptionCountCacheService pollOptionCountCacheService;

    @Override
    public void syncPollCountersForSession(Long sessionId) {
        List<Poll> polls = pollRepository.findBySession_IdOrderByCreatedAtAsc(sessionId);

        for (Poll poll : polls) {
            syncPollCounts(poll.getId());
        }

        log.info("Synced poll counters for session {}", sessionId);
    }

    @Override
    public void syncPollCounts(Long pollId) {
        List<PollOption> options = pollOptionRepository.findByPoll_IdOrderByIdAsc(pollId);
        List<PollOption> updatedOptions = new ArrayList<>();

        for (PollOption option : options) {
            Integer redisCount = pollOptionCountCacheService.getCachedPollOptionCount(
                    pollId,
                    option.getId()
            );

            if (redisCount == null) {
                continue;
            }

            if (option.getVoteCount() != redisCount) {
                option.setVoteCount(redisCount);
                updatedOptions.add(option);
            }
        }

        if (!updatedOptions.isEmpty()) {
            pollOptionRepository.saveAll(updatedOptions);
        }
    }
}