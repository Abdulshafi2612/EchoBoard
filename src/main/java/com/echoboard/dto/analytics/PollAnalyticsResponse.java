package com.echoboard.dto.analytics;

import com.echoboard.enums.PollStatus;
import com.echoboard.enums.PollType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollAnalyticsResponse {

    private Long pollId;

    private String title;

    private PollStatus status;

    private PollType type;

    private long totalVotes;

    private LocalDateTime publishedAt;

    private LocalDateTime closedAt;

    private List<PollOptionAnalyticsResponse> options;
}