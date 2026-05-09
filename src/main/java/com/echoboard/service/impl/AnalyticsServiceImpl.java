package com.echoboard.service.impl;

import com.echoboard.dto.analytics.SessionAnalyticsResponse;
import com.echoboard.dto.question.QuestionResponse;
import com.echoboard.entity.Session;
import com.echoboard.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.echoboard.enums.QuestionStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final SessionService sessionService;
    private final ParticipantService participantService;
    private final QuestionService questionService;
    private final PollService pollService;

    @Override
    public SessionAnalyticsResponse getSessionAnalytics(Long sessionId) {

        return createSessionAnalytics(sessionId);
    }

    private SessionAnalyticsResponse createSessionAnalytics(Long sessionId) {
        Session session = sessionService.getOwnedSessionOrThrow(sessionId);
        long totalParticipants = participantService.getNumberOfTotalParticipantsBySessionId(sessionId);
        long pendingQuestions = questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, PENDING);
        long approvedQuestions = questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, APPROVED);
        long answeredQuestions = questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, ANSWERED);
        long hiddenQuestions = questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, HIDDEN);
        List<QuestionResponse> topQuestion = questionService.getTopUpvotedQuestionBySessionId(sessionId);
        long totalQuestions = pendingQuestions + approvedQuestions + answeredQuestions + hiddenQuestions;
        long totalPolls = pollService.getNumberOfTotalPollsBySessionId(sessionId);
        long totalPollVotes = pollService.getNumberOfTotalVotesBySessionId(sessionId);

        return SessionAnalyticsResponse
                .builder()
                .sessionId(sessionId)
                .sessionTitle(session.getTitle())
                .totalParticipants(totalParticipants)
                .approvedQuestions(approvedQuestions)
                .answeredQuestions(answeredQuestions)
                .pendingQuestions(pendingQuestions)
                .hiddenQuestions(hiddenQuestions)
                .totalQuestions(totalQuestions)
                .topQuestions(topQuestion)
                .totalPolls(totalPolls)
                .totalPollVotes(totalPollVotes)
                .build();
    }


}
