package com.echoboard.scheduler;

import com.echoboard.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteExpiredRefreshTokens() {
        LocalDateTime now = LocalDateTime.now();

        long deletedCount = refreshTokenRepository.deleteByExpiresAtBefore(now);

        if (deletedCount > 0) {
            log.info("Deleted {} expired refresh tokens", deletedCount);
        }
    }
}
