package com.echoboard.rabbitmq;

import com.echoboard.config.RabbitMQConfig;
import com.echoboard.dto.rabbitmq.SessionEndedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AnalyticsConsumer {

    @RabbitListener(queues = RabbitMQConfig.ANALYTICS_QUEUE)
    public void handleSessionEndedEvent(SessionEndedEvent event) {
        log.info(
                "Mock analytics generation started for ended session. sessionId={}, title={}, ownerId={}, ownerEmail={}, startedAt={}, endedAt={}",
                event.getSessionId(),
                event.getTitle(),
                event.getOwnerId(),
                event.getOwnerEmail(),
                event.getStartedAt(),
                event.getEndedAt()
        );
    }
}