package com.echoboard.scheduler;

import com.echoboard.entity.PollOption;
import com.echoboard.repository.PollOptionRepository;
import com.echoboard.service.PollOptionCountCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.echoboard.enums.PollStatus.PUBLISHED;

@Component
@Slf4j
@RequiredArgsConstructor
public class PollCounterSyncJob {

    private final PollOptionRepository pollOptionRepository;
    private final PollOptionCountCacheService pollOptionCountCacheService;

    @Transactional
    @Scheduled(cron = "0 */5 * * * *")
    public void syncPublishedPollOptionCounts() {
        List<PollOption> options = pollOptionRepository.findByPoll_Status(PUBLISHED);
        List<PollOption> updatedOptions = new ArrayList<>();

        for (PollOption option : options) {
            Integer redisCount = pollOptionCountCacheService.getCachedPollOptionCount(
                    option.getPoll().getId(),
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
            log.info("Synced {} published poll option counters from Redis to PostgreSQL", updatedOptions.size());
        }
    }
}