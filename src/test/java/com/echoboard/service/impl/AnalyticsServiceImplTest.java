package com.echoboard.service.impl;

import com.echoboard.dto.analytics.PollAnalyticsResponse;
import com.echoboard.dto.analytics.SessionAnalyticsResponse;
import com.echoboard.dto.question.QuestionResponse;
import com.echoboard.entity.Session;
import com.echoboard.entity.SessionAnalyticsSnapshot;
import com.echoboard.enums.QuestionStatus;
import com.echoboard.mapper.AnalyticsMapper;
import com.echoboard.repository.SessionAnalyticsSnapshotRepository;
import com.echoboard.service.ParticipantService;
import com.echoboard.service.PollService;
import com.echoboard.service.QuestionService;
import com.echoboard.service.SessionService;
import com.echoboard.util.PollAnalyticsResponseFactory;
import com.echoboard.util.QuestionResponseFactory;
import com.echoboard.util.SessionAnalyticsSnapshotEntityFactory;
import com.echoboard.util.SessionEntityFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.echoboard.enums.QuestionStatus.APPROVED;
import static com.echoboard.enums.QuestionStatus.ANSWERED;
import static com.echoboard.enums.QuestionStatus.HIDDEN;
import static com.echoboard.enums.QuestionStatus.PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private SessionService sessionService;

    @Mock
    private ParticipantService participantService;

    @Mock
    private QuestionService questionService;

    @Mock
    private PollService pollService;

    @Mock
    private SessionAnalyticsSnapshotRepository sessionAnalyticsSnapshotRepository;

    @Mock
    private AnalyticsMapper analyticsMapper;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Test
    void getSessionAnalytics_whenSnapshotExists_shouldReturnMappedSnapshotAnalytics() {
        Long sessionId = 1L;
        Session session = SessionEntityFactory.endedSession();
        SessionAnalyticsSnapshot snapshot = SessionAnalyticsSnapshotEntityFactory.snapshotForSession(session);
        List<QuestionResponse> topQuestions = List.of(QuestionResponseFactory.approvedQuestionResponse());

        SessionAnalyticsResponse expectedResponse = SessionAnalyticsResponse
                .builder()
                .sessionId(sessionId)
                .sessionTitle(session.getTitle())
                .totalParticipants(snapshot.getTotalParticipants())
                .totalQuestions(snapshot.getTotalQuestions())
                .pendingQuestions(snapshot.getPendingQuestions())
                .approvedQuestions(snapshot.getApprovedQuestions())
                .answeredQuestions(snapshot.getAnsweredQuestions())
                .hiddenQuestions(snapshot.getHiddenQuestions())
                .topQuestions(topQuestions)
                .totalPolls(snapshot.getTotalPolls())
                .totalPollVotes(snapshot.getTotalPollVotes())
                .build();

        when(sessionService.getOwnedSessionOrThrow(sessionId)).thenReturn(session);
        when(sessionAnalyticsSnapshotRepository.findBySession_Id(sessionId)).thenReturn(Optional.of(snapshot));
        when(questionService.getTopUpvotedQuestionBySessionId(sessionId)).thenReturn(topQuestions);
        when(analyticsMapper.snapshotToSessionAnalyticsResponse(snapshot, session, topQuestions))
                .thenReturn(expectedResponse);

        SessionAnalyticsResponse actualResponse = analyticsService.getSessionAnalytics(sessionId);

        assertEquals(expectedResponse, actualResponse);

        verify(sessionService).getOwnedSessionOrThrow(sessionId);
        verify(sessionAnalyticsSnapshotRepository).findBySession_Id(sessionId);
        verify(questionService).getTopUpvotedQuestionBySessionId(sessionId);
        verify(analyticsMapper).snapshotToSessionAnalyticsResponse(snapshot, session, topQuestions);

        verifyNoInteractions(participantService);
        verifyNoInteractions(pollService);
    }

    @Test
    void getSessionAnalytics_whenSnapshotDoesNotExist_shouldBuildLiveAnalytics() {
        Long sessionId = 1L;
        Session session = SessionEntityFactory.liveSession();
        List<QuestionResponse> topQuestions = List.of(
                QuestionResponseFactory.approvedQuestionResponse(),
                QuestionResponseFactory.answeredQuestionResponse()
        );

        when(sessionService.getOwnedSessionOrThrow(sessionId)).thenReturn(session);
        when(sessionAnalyticsSnapshotRepository.findBySession_Id(sessionId)).thenReturn(Optional.empty());

        when(participantService.getNumberOfTotalParticipantsBySessionId(sessionId)).thenReturn(12L);

        when(questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, PENDING)).thenReturn(2L);
        when(questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, APPROVED)).thenReturn(5L);
        when(questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, ANSWERED)).thenReturn(3L);
        when(questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, HIDDEN)).thenReturn(1L);

        when(pollService.getNumberOfTotalPollsBySessionId(sessionId)).thenReturn(4L);
        when(pollService.getNumberOfTotalVotesBySessionId(sessionId)).thenReturn(30L);

        when(questionService.getTopUpvotedQuestionBySessionId(sessionId)).thenReturn(topQuestions);

        SessionAnalyticsResponse response = analyticsService.getSessionAnalytics(sessionId);

        assertEquals(sessionId, response.getSessionId());
        assertEquals(session.getTitle(), response.getSessionTitle());
        assertEquals(12L, response.getTotalParticipants());
        assertEquals(11L, response.getTotalQuestions());
        assertEquals(2L, response.getPendingQuestions());
        assertEquals(5L, response.getApprovedQuestions());
        assertEquals(3L, response.getAnsweredQuestions());
        assertEquals(1L, response.getHiddenQuestions());
        assertEquals(4L, response.getTotalPolls());
        assertEquals(30L, response.getTotalPollVotes());
        assertEquals(topQuestions, response.getTopQuestions());

        verify(sessionService).getOwnedSessionOrThrow(sessionId);
        verify(sessionAnalyticsSnapshotRepository).findBySession_Id(sessionId);
        verify(participantService).getNumberOfTotalParticipantsBySessionId(sessionId);
        verifyQuestionCountCalled(sessionId, PENDING);
        verifyQuestionCountCalled(sessionId, APPROVED);
        verifyQuestionCountCalled(sessionId, ANSWERED);
        verifyQuestionCountCalled(sessionId, HIDDEN);
        verify(pollService).getNumberOfTotalPollsBySessionId(sessionId);
        verify(pollService).getNumberOfTotalVotesBySessionId(sessionId);
        verify(questionService).getTopUpvotedQuestionBySessionId(sessionId);

        verifyNoInteractions(analyticsMapper);
    }

    @Test
    void exportCsvSessionQuestions_shouldValidateOwnershipAndReturnQuestionsCsv() {
        Long sessionId = 1L;
        Session session = SessionEntityFactory.liveSession();

        QuestionResponse question = QuestionResponseFactory.approvedQuestionResponse();
        question.setId(10L);
        question.setSessionId(sessionId);
        question.setParticipantDisplayName("Ali");
        question.setContent("How does Redis help here?");
        question.setUpvoteCount(4);
        question.setPinned(true);

        when(sessionService.getOwnedSessionOrThrow(sessionId)).thenReturn(session);
        when(questionService.getQuestionsForExportBySessionId(sessionId)).thenReturn(List.of(question));

        String csv = analyticsService.exportCsvSessionQuestions(sessionId);

        assertNotNull(csv);
        assertFalse(csv.isBlank());
        assertContains(csv, "id");
        assertContains(csv, "sessionId");
        assertContains(csv, "participantDisplayName");
        assertContains(csv, "How does Redis help here?");
        assertContains(csv, "APPROVED");

        verify(sessionService).getOwnedSessionOrThrow(sessionId);
        verify(questionService).getQuestionsForExportBySessionId(sessionId);
    }

    @Test
    void getPollAnalytics_shouldValidateOwnershipAndReturnPollAnalytics() {
        Long sessionId = 1L;
        Session session = SessionEntityFactory.liveSession();
        List<PollAnalyticsResponse> expectedResponse = List.of(
                PollAnalyticsResponseFactory.pollAnalyticsResponse()
        );

        when(sessionService.getOwnedSessionOrThrow(sessionId)).thenReturn(session);
        when(pollService.getPollAnalyticsBySessionId(sessionId)).thenReturn(expectedResponse);

        List<PollAnalyticsResponse> actualResponse = analyticsService.getPollAnalytics(sessionId);

        assertEquals(expectedResponse, actualResponse);

        verify(sessionService).getOwnedSessionOrThrow(sessionId);
        verify(pollService).getPollAnalyticsBySessionId(sessionId);
    }

    @Test
    void generateSessionAnalyticsSnapshot_whenSnapshotAlreadyExists_shouldNotGenerateNewSnapshot() {
        Long sessionId = 1L;

        when(sessionAnalyticsSnapshotRepository.existsBySession_Id(sessionId)).thenReturn(true);

        analyticsService.generateSessionAnalyticsSnapshot(sessionId);

        verify(sessionAnalyticsSnapshotRepository).existsBySession_Id(sessionId);
        verify(sessionAnalyticsSnapshotRepository, never()).save(any(SessionAnalyticsSnapshot.class));

        verifyNoInteractions(sessionService);
        verifyNoInteractions(participantService);
        verifyNoInteractions(questionService);
        verifyNoInteractions(pollService);
        verifyNoInteractions(analyticsMapper);
    }

    @Test
    void generateSessionAnalyticsSnapshot_whenSnapshotDoesNotExist_shouldCreateAndSaveSnapshot() {
        Long sessionId = 1L;
        Session session = SessionEntityFactory.endedSession();

        when(sessionAnalyticsSnapshotRepository.existsBySession_Id(sessionId)).thenReturn(false);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);

        when(participantService.getNumberOfTotalParticipantsBySessionId(sessionId)).thenReturn(20L);

        when(questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, PENDING)).thenReturn(1L);
        when(questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, APPROVED)).thenReturn(6L);
        when(questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, ANSWERED)).thenReturn(4L);
        when(questionService.getNumberOfQuestionsBySessionIdAndStatus(sessionId, HIDDEN)).thenReturn(2L);

        when(pollService.getNumberOfTotalPollsBySessionId(sessionId)).thenReturn(3L);
        when(pollService.getNumberOfTotalVotesBySessionId(sessionId)).thenReturn(25L);

        analyticsService.generateSessionAnalyticsSnapshot(sessionId);

        ArgumentCaptor<SessionAnalyticsSnapshot> snapshotCaptor =
                ArgumentCaptor.forClass(SessionAnalyticsSnapshot.class);

        verify(sessionAnalyticsSnapshotRepository).existsBySession_Id(sessionId);
        verify(sessionService).getSessionById(sessionId);
        verify(sessionAnalyticsSnapshotRepository).save(snapshotCaptor.capture());

        SessionAnalyticsSnapshot savedSnapshot = snapshotCaptor.getValue();

        assertEquals(session, savedSnapshot.getSession());
        assertEquals(20L, savedSnapshot.getTotalParticipants());
        assertEquals(13L, savedSnapshot.getTotalQuestions());
        assertEquals(1L, savedSnapshot.getPendingQuestions());
        assertEquals(6L, savedSnapshot.getApprovedQuestions());
        assertEquals(4L, savedSnapshot.getAnsweredQuestions());
        assertEquals(2L, savedSnapshot.getHiddenQuestions());
        assertEquals(3L, savedSnapshot.getTotalPolls());
        assertEquals(25L, savedSnapshot.getTotalPollVotes());
        assertNotNull(savedSnapshot.getGeneratedAt());

        verify(participantService).getNumberOfTotalParticipantsBySessionId(sessionId);
        verifyQuestionCountCalled(sessionId, PENDING);
        verifyQuestionCountCalled(sessionId, APPROVED);
        verifyQuestionCountCalled(sessionId, ANSWERED);
        verifyQuestionCountCalled(sessionId, HIDDEN);
        verify(pollService).getNumberOfTotalPollsBySessionId(sessionId);
        verify(pollService).getNumberOfTotalVotesBySessionId(sessionId);

        verifyNoInteractions(analyticsMapper);
    }

    private void verifyQuestionCountCalled(Long sessionId, QuestionStatus status) {
        verify(questionService).getNumberOfQuestionsBySessionIdAndStatus(eq(sessionId), eq(status));
    }

    private void assertContains(String actual, String expected) {
        boolean contains = actual.contains(expected);

        if (!contains) {
            throw new AssertionError("Expected text to contain: " + expected + ", but was: " + actual);
        }
    }
}