package com.echoboard.scheduler;

import com.echoboard.entity.Session;
import com.echoboard.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.echoboard.enums.SessionStatus.ARCHIVED;
import static com.echoboard.enums.SessionStatus.ENDED;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionArchiveJob {

    private final SessionRepository sessionRepository;

    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void archiveEndedSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);

        List<Session> sessions = sessionRepository.findByStatusAndEndedAtBefore(ENDED, cutoff);

        sessions.forEach(session -> session.setStatus(ARCHIVED));

        sessionRepository.saveAll(sessions);

        if (!sessions.isEmpty()) {
            log.info("Archived {} ended sessions older than 30 days", sessions.size());
        }
    }
}