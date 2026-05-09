package com.echoboard.rabbitmq;

import com.echoboard.config.RabbitMQConfig;
import com.echoboard.dto.rabbitmq.SessionEndedEvent;
import com.echoboard.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AnalyticsConsumer {

    private final AnalyticsService analyticsService;

    @RabbitListener(queues = RabbitMQConfig.ANALYTICS_QUEUE)
    public void handleSessionEndedEvent(SessionEndedEvent event) {
        try {
            log.info(
                    "Analytics snapshot generation started. sessionId={}, title={}, ownerId={}, endedAt={}",
                    event.getSessionId(),
                    event.getTitle(),
                    event.getOwnerId(),
                    event.getEndedAt()
            );

            analyticsService.generateSessionAnalyticsSnapshot(event.getSessionId());

            log.info(
                    "Analytics snapshot generation completed. sessionId={}",
                    event.getSessionId()
            );

        } catch (Exception exception) {
            log.error(
                    "Analytics snapshot generation failed. sessionId={}",
                    event.getSessionId(),
                    exception
            );

            throw new AmqpRejectAndDontRequeueException(
                    "Failed to generate analytics snapshot for session " + event.getSessionId(),
                    exception
            );
        }
    }
}