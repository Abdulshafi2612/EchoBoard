package com.echoboard.rabbitmq;

import com.echoboard.config.RabbitMQConfig;
import com.echoboard.dto.rabbitmq.SessionCreatedEvent;
import com.echoboard.dto.rabbitmq.SessionEndedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitMQPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishSessionCreatedEvent(SessionCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ECHOBOARD_EXCHANGE,
                RabbitMQConfig.EMAIL_SESSION_CREATED_ROUTING_KEY,
                event
        );
    }

    public void publishSessionEndedEvent(SessionEndedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ECHOBOARD_EXCHANGE,
                RabbitMQConfig.ANALYTICS_SESSION_ENDED_ROUTING_KEY,
                event
        );
    }

}
