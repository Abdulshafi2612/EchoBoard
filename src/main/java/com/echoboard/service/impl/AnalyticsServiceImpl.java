package com.echoboard.service.impl;

import com.echoboard.dto.analytics.PollAnalyticsResponse;
import com.echoboard.dto.analytics.SessionAnalyticsResponse;
import com.echoboard.dto.question.QuestionResponse;
import com.echoboard.entity.Session;
import com.echoboard.entity.SessionAnalyticsSnapshot;
import com.echoboard.mapper.AnalyticsMapper;
import com.echoboard.repository.SessionAnalyticsSnapshotRepository;
import com.echoboard.service.AnalyticsService;
import com.echoboard.service.ParticipantService;
import com.echoboard.service.PollService;
import com.echoboard.service.QuestionService;
import com.echoboard.service.SessionService;
import com.echoboard.util.CsvExportUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.echoboard.enums.QuestionStatus.APPROVED;
import static com.echoboard.enums.QuestionStatus.ANSWERED;
import static com.echoboard.enums.QuestionStatus.HIDDEN;
import static com.echoboard.enums.QuestionStatus.PENDING;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final SessionService sessionService;
    private final ParticipantService participantService;
    private final QuestionService questionService;
    private final PollService pollService;
    private final SessionAnalyticsSnapshotRepository sessionAnalyticsSnapshotRepository;
    private final AnalyticsMapper analyticsMapper;

    @Override
    public SessionAnalyticsResponse getSessionAnalytics(Long sessionId) {
        Session session = sessionService.getOwnedSessionOrThrow(sessionId);

        return sessionAnalyticsSnapshotRepository.findBySession_Id(sessionId)
                .map(snapshot -> createSessionAnalyticsResponseFromSnapshot(snapshot, session))
                .orElseGet(() -> createSessionAnalyticsResponse(session));
    }

    @Override
    public String exportCsvSessionQuestions(Long sessionId) {
        sessionService.getOwnedSessionOrThrow(sessionId);

        return CsvExportUtil.toQuestionsCsv(
                questionService.getQuestionsForExportBySessionId(sessionId)
        );
    }

    @Override
    public List<PollAnalyticsResponse> getPollAnalytics(Long sessionId) {
        sessionService.getOwnedSessionOrThrow(sessionId);

        return pollService.getPollAnalyticsBySessionId(sessionId);
    }

    @Override
    public void generateSessionAnalyticsSnapshot(Long sessionId) {
        if (sessionAnalyticsSnapshotRepository.existsBySession_Id(sessionId)) {
            log.info("Analytics snapshot already exists for session {}", sessionId);
            return;
        }

        Session session = sessionService.getSessionById(sessionId);

        SessionAnalyticsSnapshot sessionAnalyticsSnapshot = createSessionAnalyticsSnapshot(session);

        sessionAnalyticsSnapshotRepository.save(sessionAnalyticsSnapshot);

        log.info("Generated analytics snapshot for session {}", sessionId);
    }

    private SessionAnalyticsResponse createSessionAnalyticsResponse(Session session) {
        Long sessionId = session.getId();

        long totalParticipants = participantService.getNumberOfTotalParticipantsBySessionId(sessionId);

        long pendingQuestions = questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, PENDING);
        long approvedQuestions = questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, APPROVED);
        long answeredQuestions = questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, ANSWERED);
        long hiddenQuestions = questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, HIDDEN);

        long totalQuestions = pendingQuestions + approvedQuestions + answeredQuestions + hiddenQuestions;
        long totalPolls = pollService.getNumberOfTotalPollsBySessionId(sessionId);
        long totalPollVotes = pollService.getNumberOfTotalVotesBySessionId(sessionId);

        List<QuestionResponse> topQuestions = questionService.getTopUpvotedQuestionBySessionId(sessionId);

        return SessionAnalyticsResponse
                .builder()
                .sessionId(sessionId)
                .sessionTitle(session.getTitle())
                .totalParticipants(totalParticipants)
                .totalQuestions(totalQuestions)
                .pendingQuestions(pendingQuestions)
                .approvedQuestions(approvedQuestions)
                .answeredQuestions(answeredQuestions)
                .hiddenQuestions(hiddenQuestions)
                .topQuestions(topQuestions)
                .totalPolls(totalPolls)
                .totalPollVotes(totalPollVotes)
                .build();
    }

    private SessionAnalyticsSnapshot createSessionAnalyticsSnapshot(Session session) {
        Long sessionId = session.getId();

        long totalParticipants = participantService.getNumberOfTotalParticipantsBySessionId(sessionId);

        long pendingQuestions = questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, PENDING);
        long approvedQuestions = questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, APPROVED);
        long answeredQuestions = questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, ANSWERED);
        long hiddenQuestions = questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, HIDDEN);

        long totalQuestions = pendingQuestions + approvedQuestions + answeredQuestions + hiddenQuestions;
        long totalPolls = pollService.getNumberOfTotalPollsBySessionId(sessionId);
        long totalPollVotes = pollService.getNumberOfTotalVotesBySessionId(sessionId);

        return SessionAnalyticsSnapshot
                .builder()
                .session(session)
                .totalParticipants(totalParticipants)
                .totalQuestions(totalQuestions)
                .pendingQuestions(pendingQuestions)
                .approvedQuestions(approvedQuestions)
                .answeredQuestions(answeredQuestions)
                .hiddenQuestions(hiddenQuestions)
                .totalPolls(totalPolls)
                .totalPollVotes(totalPollVotes)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private SessionAnalyticsResponse createSessionAnalyticsResponseFromSnapshot(
            SessionAnalyticsSnapshot snapshot,
            Session session
    ) {
        List<QuestionResponse> topQuestions =
                questionService.getTopUpvotedQuestionBySessionId(session.getId());

        return analyticsMapper.snapshotToSessionAnalyticsResponse(
                snapshot,
                session,
                topQuestions
        );
    }
}