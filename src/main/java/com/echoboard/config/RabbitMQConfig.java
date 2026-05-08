package com.echoboard.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ECHOBOARD_EXCHANGE = "echoboard.exchange";
    public static final String EMAIL_QUEUE = "email.queue";
    public static final String EMAIL_SESSION_CREATED_ROUTING_KEY = "email.session.created";
    public static final String ANALYTICS_QUEUE = "analytics.queue";
    public static final String ANALYTICS_SESSION_ENDED_ROUTING_KEY = "analytics.session.ended";
    public static final String ECHOBOARD_DLX = "echoboard.dlx";
    public static final String ANALYTICS_DLQ = "analytics.dlq";
    public static final String ANALYTICS_DLQ_ROUTING_KEY = "analytics.dlq";

    @Bean
    public TopicExchange echoboardExchange() {
        return new TopicExchange(ECHOBOARD_EXCHANGE);
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(EMAIL_QUEUE);
    }

    @Bean
    public Queue analyticsQueue() {
        return QueueBuilder
                .durable(ANALYTICS_QUEUE)
                .deadLetterExchange(ECHOBOARD_DLX)
                .deadLetterRoutingKey(ANALYTICS_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange echoboardDlx() {
        return new DirectExchange(ECHOBOARD_DLX);
    }

    @Bean
    public Queue analyticsDlq() {
        return new Queue(ANALYTICS_DLQ);
    }

    @Bean
    public Binding analyticsDlqBinding(Queue analyticsDlq, DirectExchange echoboardDlx) {
        return BindingBuilder
                .bind(analyticsDlq)
                .to(echoboardDlx)
                .with(ANALYTICS_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding emailSessionCreatedBinding(Queue emailQueue, TopicExchange echoboardExchange) {
        return BindingBuilder
                .bind(emailQueue)
                .to(echoboardExchange)
                .with(EMAIL_SESSION_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding analyticsSessionEndedBinding(Queue analyticsQueue, TopicExchange echoboardExchange) {
        return BindingBuilder
                .bind(analyticsQueue)
                .to(echoboardExchange)
                .with(ANALYTICS_SESSION_ENDED_ROUTING_KEY);
    }


    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
