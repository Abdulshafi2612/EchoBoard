package com.echoboard.scheduler;

import com.echoboard.dto.rabbitmq.SessionEndedEvent;
import com.echoboard.entity.Session;
import com.echoboard.rabbitmq.RabbitMQPublisher;
import com.echoboard.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.echoboard.enums.SessionStatus.ENDED;
import static com.echoboard.enums.SessionStatus.LIVE;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionAutoEndJob {

    private final SessionRepository sessionRepository;
    private final RabbitMQPublisher rabbitMQPublisher;

    @Transactional
    @Scheduled(cron = "0 */10 * * * *")
    public void autoEndLongRunningSessions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusHours(24);

        List<Session> sessions = sessionRepository.findByStatusAndStartedAtBefore(LIVE, cutoff);

        for (Session session : sessions) {
            session.setStatus(ENDED);
            session.setEndedAt(now);

            publishSessionEndedEvent(session);
        }

        if (!sessions.isEmpty()) {
            sessionRepository.saveAll(sessions);
            log.info("Auto-ended {} live sessions older than 24 hours", sessions.size());
        }
    }

    private void publishSessionEndedEvent(Session session) {
        SessionEndedEvent event = SessionEndedEvent
                .builder()
                .sessionId(session.getId())
                .title(session.getTitle())
                .ownerId(session.getOwner().getId())
                .ownerEmail(session.getOwner().getEmail())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .build();

        rabbitMQPublisher.publishSessionEndedEvent(event);
    }
}