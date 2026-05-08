package com.echoboard.dto.rabbitmq;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionCreatedEvent {

    private Long sessionId;
    private String title;
    private Long ownerId;
    private String ownerEmail;
    private LocalDateTime createdAt;
}