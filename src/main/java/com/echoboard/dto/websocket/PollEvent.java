package com.echoboard.dto.websocket;

import com.echoboard.dto.poll.PollOptionResponse;
import com.echoboard.enums.PollEventType;
import com.echoboard.enums.PollStatus;
import com.echoboard.enums.PollType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PollEvent {

    private PollEventType eventType;

    private Long id;

    private Long sessionId;

    private String title;

    private PollStatus status;

    private PollType type;

    private List<PollOptionResponse> options;

    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    private LocalDateTime closedAt;
}
