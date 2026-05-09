package com.echoboard.dto.analytics;

import com.echoboard.dto.question.QuestionResponse;
import lombok.*;

import java.util.List;

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

    private List<QuestionResponse> topQuestions;

    private long hiddenQuestions;

    private long totalPolls;

    private long totalPollVotes;
}
