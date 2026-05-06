package com.echoboard.dto.websocket;

import com.echoboard.enums.QuestionEventType;
import lombok.*;

import static com.echoboard.enums.QuestionEventType.DELETED;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QuestionDeletedEvent {

    @Builder.Default
    private QuestionEventType questionEventType = DELETED;

    private Long questionId;

    private Long sessionId;
}