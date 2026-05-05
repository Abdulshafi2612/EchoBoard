package com.echoboard.dto.websocket;

import com.echoboard.enums.QuestionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QuestionEvent {

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