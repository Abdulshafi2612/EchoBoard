package com.echoboard.dto.analytics;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SessionAnalyticsResponse {

    private Long sessionId;

    private String sessionTitle;

    private long totalParticipants;

    private long totalQuestions;

    private long pendingQuestions;

    private long approvedQuestions;

    private long answeredQuestions;

    private long hiddenQuestions;

    private long totalPolls;

    private long totalPollVotes;
}
