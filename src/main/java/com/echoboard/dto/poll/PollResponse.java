package com.echoboard.dto.poll;

import com.echoboard.enums.PollStatus;
import com.echoboard.enums.PollType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PollResponse {

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
