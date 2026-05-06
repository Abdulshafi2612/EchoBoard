package com.echoboard.dto.websocket;

import com.echoboard.enums.QuestionEventType;
import com.echoboard.enums.QuestionStatus;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QuestionEvent {

    private QuestionEventType questionEventType;

    private Long questionId;

    private Long sessionId;

    private String participantDisplayName;

    private String content;

    private QuestionStatus status;

    private int upvoteCount;

    private boolean pinned;

    private boolean answered;

    private LocalDateTime createdAt;

    private LocalDateTime approvedAt;

    private LocalDateTime answeredAt;
}