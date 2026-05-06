package com.echoboard.dto.websocket;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QuestionDeletedEvent {

    private Long questionId;
    private Long sessionId;
}