package com.echoboard.dto.rabbitmq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionEndedEvent {

    private Long sessionId;
    private String title;
    private Long ownerId;
    private String ownerEmail;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}