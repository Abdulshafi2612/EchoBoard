package com.echoboard.rabbitmq;

import com.echoboard.config.RabbitMQConfig;
import com.echoboard.dto.rabbitmq.SessionCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailNotificationConsumer {

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void handleSessionCreatedEvent(SessionCreatedEvent event) {
        log.info(
                "Mock email notification sent for created session. sessionId={}, title={}, ownerId={}, ownerEmail={}, createdAt={}",
                event.getSessionId(),
                event.getTitle(),
                event.getOwnerId(),
                event.getOwnerEmail(),
                event.getCreatedAt()
        );
    }
}
