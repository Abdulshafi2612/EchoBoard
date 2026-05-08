package com.echoboard.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ECHOBOARD_EXCHANGE = "echoboard.exchange";
    public static final String EMAIL_QUEUE = "email.queue";
    public static final String EMAIL_SESSION_CREATED_ROUTING_KEY = "email.session.created";

    @Bean
    public TopicExchange echoboardExchange() {
        return new TopicExchange(ECHOBOARD_EXCHANGE);
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(EMAIL_QUEUE);
    }

    @Bean
    public Binding emailSessionCreatedBinding(Queue emailQueue, TopicExchange echoboardExchange) {
        return BindingBuilder
                .bind(emailQueue)
                .to(echoboardExchange)
                .with(EMAIL_SESSION_CREATED_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
