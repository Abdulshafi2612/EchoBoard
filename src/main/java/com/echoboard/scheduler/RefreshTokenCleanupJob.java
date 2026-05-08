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
    public void cleanupRefreshTokens() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime revokedTokenCutoff = now.minusDays(7);

        long expiredDeletedCount = refreshTokenRepository.deleteByExpiresAtBefore(now);
        long revokedDeletedCount = refreshTokenRepository.deleteByRevokedTrueAndCreatedAtBefore(revokedTokenCutoff);

        if (expiredDeletedCount > 0 || revokedDeletedCount > 0) {
            log.info(
                    "Refresh token cleanup completed. expiredDeleted={}, revokedDeleted={}",
                    expiredDeletedCount,
                    revokedDeletedCount
            );
        }
    }
}