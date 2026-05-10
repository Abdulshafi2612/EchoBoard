package com.echoboard.service.impl;

import com.echoboard.dto.common.PageResponse;
import com.echoboard.dto.question.QuestionAttachmentResponse;
import com.echoboard.dto.question.QuestionResponse;
import com.echoboard.dto.question.SubmitQuestionRequest;
import com.echoboard.dto.websocket.QuestionDeletedEvent;
import com.echoboard.dto.websocket.QuestionEvent;
import com.echoboard.entity.Participant;
import com.echoboard.entity.Question;
import com.echoboard.entity.QuestionAttachment;
import com.echoboard.entity.QuestionVote;
import com.echoboard.entity.Session;
import com.echoboard.entity.User;
import com.echoboard.enums.FileType;
import com.echoboard.enums.QuestionEventType;
import com.echoboard.enums.QuestionStatus;
import com.echoboard.enums.SessionStatus;
import com.echoboard.exception.AppException;
import com.echoboard.mapper.QuestionAttachmentMapper;
import com.echoboard.mapper.QuestionMapper;
import com.echoboard.repository.QuestionAttachmentRepository;
import com.echoboard.repository.QuestionRepository;
import com.echoboard.repository.QuestionVoteRepository;
import com.echoboard.service.CurrentParticipantService;
import com.echoboard.service.CurrentUserService;
import com.echoboard.service.FileStorageService;
import com.echoboard.service.ParticipantService;
import com.echoboard.service.RateLimiterService;
import com.echoboard.service.SessionService;
import com.echoboard.util.ParticipantEntityFactory;
import com.echoboard.util.QuestionAttachmentEntityFactory;
import com.echoboard.util.QuestionEntityFactory;
import com.echoboard.util.QuestionResponseFactory;
import com.echoboard.util.SessionEntityFactory;
import com.echoboard.util.UserEntityFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static com.echoboard.exception.ErrorCode.FORBIDDEN;
import static com.echoboard.exception.ErrorCode.INVALID_QUESTION_STATUS;
import static com.echoboard.exception.ErrorCode.INVALID_SESSION_STATUS;
import static com.echoboard.exception.ErrorCode.RATE_LIMIT_EXCEEDED;
import static com.echoboard.exception.ErrorCode.RESOURCE_NOT_FOUND;
import static com.echoboard.exception.ErrorCode.VALIDATION_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceImplTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private SessionService sessionService;

    @Mock
    private ParticipantService participantService;

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private CurrentParticipantService currentParticipantService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private QuestionVoteRepository questionVoteRepository;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private QuestionAttachmentRepository questionAttachmentRepository;

    @Mock
    private QuestionAttachmentMapper questionAttachmentMapper;

    @InjectMocks
    private QuestionServiceImpl questionService;

    @Test
    void getQuestionsBySessionIdAndStatus_whenUserOwnsLiveSession_shouldReturnMappedPageResponse() {
        Long sessionId = 1L;
        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.liveSessionOwnedBy(owner);
        session.setId(sessionId);

        Question question = QuestionEntityFactory.approvedQuestion();
        question.setSession(session);

        QuestionResponse questionResponse = QuestionResponseFactory.approvedQuestionResponse();

        PageRequest pageable = PageRequest.of(0, 10);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(questionRepository.findBySession_IdAndStatus(sessionId, QuestionStatus.APPROVED, pageable))
                .thenReturn(new PageImpl<>(List.of(question), pageable, 1));
        when(questionMapper.questionToQuestionResponse(question)).thenReturn(questionResponse);

        PageResponse<QuestionResponse> response =
                questionService.getQuestionsBySessionIdAndStatus(pageable, sessionId, QuestionStatus.APPROVED);

        assertEquals(1, response.getContent().size());
        assertEquals(questionResponse, response.getContent().get(0));
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(1L, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertEquals(true, response.isLast());
    }

    @Test
    void getQuestionsBySessionIdAndStatus_whenUserDoesNotOwnSession_shouldThrowForbiddenException() {
        Long sessionId = 1L;

        User currentUser = UserEntityFactory.presenter();
        currentUser.setId(1L);

        User anotherOwner = UserEntityFactory.presenter();
        anotherOwner.setId(2L);

        Session session = SessionEntityFactory.liveSessionOwnedBy(anotherOwner);

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);

        AppException exception = assertThrows(
                AppException.class,
                () -> questionService.getQuestionsBySessionIdAndStatus(
                        PageRequest.of(0, 10),
                        sessionId,
                        QuestionStatus.PENDING
                )
        );

        assertEquals(FORBIDDEN, exception.getErrorCode());
        assertEquals("User does not own this session", exception.getMessage());

        verifyNoInteractions(questionRepository);
    }

    @Test
    void submitQuestion_whenModerationEnabled_shouldCreatePendingQuestionBroadcastToPendingTopicAndReturnResponse() {
        Long sessionId = 1L;
        Long participantId = 10L;

        Session session = SessionEntityFactory.liveSession();
        session.setId(sessionId);
        session.setModerationEnabled(true);

        Participant participant = ParticipantEntityFactory.participantForSession(session);
        participant.setId(participantId);

        SubmitQuestionRequest request = new SubmitQuestionRequest("What is Redis?");

        Question savedQuestion = new Question();
        savedQuestion.setId(100L);
        savedQuestion.setSession(session);
        savedQuestion.setParticipant(participant);
        savedQuestion.setContent(request.getContent());
        savedQuestion.setStatus(QuestionStatus.PENDING);

        QuestionResponse expectedResponse = QuestionResponseFactory.questionResponseWithStatus(QuestionStatus.PENDING);

        QuestionEvent event = QuestionEvent
                .builder()
                .questionId(100L)
                .sessionId(sessionId)
                .status(QuestionStatus.PENDING)
                .build();

        when(currentParticipantService.getCurrentParticipantId()).thenReturn(participantId);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(participantService.getParticipantById(participantId)).thenReturn(participant);
        when(rateLimiterService.isAllowed(
                "rate:participant:10:questions",
                5,
                Duration.ofSeconds(60)
        )).thenReturn(true);
        when(questionRepository.save(any(Question.class))).thenReturn(savedQuestion);
        when(questionMapper.questionToQuestionEvent(savedQuestion)).thenReturn(event);
        when(questionMapper.questionToQuestionResponse(savedQuestion)).thenReturn(expectedResponse);

        QuestionResponse response = questionService.submitQuestion(sessionId, request);

        assertEquals(expectedResponse, response);

        ArgumentCaptor<Question> questionCaptor = ArgumentCaptor.forClass(Question.class);
        ArgumentCaptor<QuestionEvent> eventCaptor = ArgumentCaptor.forClass(QuestionEvent.class);

        verify(questionRepository).save(questionCaptor.capture());
        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/1/questions/pending"),
                eventCaptor.capture()
        );

        Question questionBeforeSave = questionCaptor.getValue();

        assertEquals(session, questionBeforeSave.getSession());
        assertEquals(participant, questionBeforeSave.getParticipant());
        assertEquals("What is Redis?", questionBeforeSave.getContent());
        assertEquals(QuestionStatus.PENDING, questionBeforeSave.getStatus());
        assertEquals(QuestionEventType.CREATED, eventCaptor.getValue().getQuestionEventType());
    }

    @Test
    void submitQuestion_whenModerationDisabled_shouldCreateApprovedQuestionBroadcastToPublicTopicAndReturnResponse() {
        Long sessionId = 1L;
        Long participantId = 10L;

        Session session = SessionEntityFactory.liveSession();
        session.setId(sessionId);
        session.setModerationEnabled(false);

        Participant participant = ParticipantEntityFactory.participantForSession(session);
        participant.setId(participantId);

        SubmitQuestionRequest request = new SubmitQuestionRequest("What is RabbitMQ?");

        Question savedQuestion = new Question();
        savedQuestion.setId(100L);
        savedQuestion.setSession(session);
        savedQuestion.setParticipant(participant);
        savedQuestion.setContent(request.getContent());
        savedQuestion.setStatus(QuestionStatus.APPROVED);

        QuestionResponse expectedResponse = QuestionResponseFactory.approvedQuestionResponse();

        QuestionEvent event = QuestionEvent
                .builder()
                .questionId(100L)
                .sessionId(sessionId)
                .status(QuestionStatus.APPROVED)
                .build();

        when(currentParticipantService.getCurrentParticipantId()).thenReturn(participantId);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(participantService.getParticipantById(participantId)).thenReturn(participant);
        when(rateLimiterService.isAllowed(
                "rate:participant:10:questions",
                5,
                Duration.ofSeconds(60)
        )).thenReturn(true);
        when(questionRepository.save(any(Question.class))).thenReturn(savedQuestion);
        when(questionMapper.questionToQuestionEvent(savedQuestion)).thenReturn(event);
        when(questionMapper.questionToQuestionResponse(savedQuestion)).thenReturn(expectedResponse);

        QuestionResponse response = questionService.submitQuestion(sessionId, request);

        assertEquals(expectedResponse, response);

        ArgumentCaptor<Question> questionCaptor = ArgumentCaptor.forClass(Question.class);
        ArgumentCaptor<QuestionEvent> eventCaptor = ArgumentCaptor.forClass(QuestionEvent.class);

        verify(questionRepository).save(questionCaptor.capture());
        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/1/questions"),
                eventCaptor.capture()
        );

        Question questionBeforeSave = questionCaptor.getValue();

        assertEquals(QuestionStatus.APPROVED, questionBeforeSave.getStatus());
        assertNotNull(questionBeforeSave.getApprovedAt());
        assertEquals(QuestionEventType.CREATED, eventCaptor.getValue().getQuestionEventType());
    }

    @Test
    void submitQuestion_whenCurrentParticipantIdIsNull_shouldThrowForbiddenException() {
        when(currentParticipantService.getCurrentParticipantId()).thenReturn(null);

        AppException exception = assertThrows(
                AppException.class,
                () -> questionService.submitQuestion(1L, new SubmitQuestionRequest("Question"))
        );

        assertEquals(FORBIDDEN, exception.getErrorCode());
        assertEquals("Only participants can submit questions", exception.getMessage());

        verifyNoInteractions(sessionService);
        verifyNoInteractions(participantService);
        verifyNoInteractions(questionRepository);
    }

    @Test
    void submitQuestion_whenRateLimitExceeded_shouldThrowRateLimitExceededException() {
        Long sessionId = 1L;
        Long participantId = 10L;

        Session session = SessionEntityFactory.liveSession();
        session.setId(sessionId);

        Participant participant = ParticipantEntityFactory.participantForSession(session);
        participant.setId(participantId);

        when(currentParticipantService.getCurrentParticipantId()).thenReturn(participantId);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(participantService.getParticipantById(participantId)).thenReturn(participant);
        when(rateLimiterService.isAllowed(
                "rate:participant:10:questions",
                5,
                Duration.ofSeconds(60)
        )).thenReturn(false);

        AppException exception = assertThrows(
                AppException.class,
                () -> questionService.submitQuestion(sessionId, new SubmitQuestionRequest("Question"))
        );

        assertEquals(RATE_LIMIT_EXCEEDED, exception.getErrorCode());
        assertEquals("Too many questions. Please wait before submitting again.", exception.getMessage());

        verify(questionRepository, never()).save(any(Question.class));
    }

    @Test
    void approveQuestion_whenQuestionIsPendingInOwnedLiveSession_shouldApproveAndBroadcastToPendingAndPublicTopics() {
        Long sessionId = 1L;
        Long questionId = 100L;

        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.liveSessionOwnedBy(owner);
        session.setId(sessionId);

        Question question = QuestionEntityFactory.pendingQuestion();
        question.setId(questionId);
        question.setSession(session);

        Question savedQuestion = question;

        QuestionEvent pendingEvent = QuestionEvent
                .builder()
                .questionId(questionId)
                .sessionId(sessionId)
                .status(QuestionStatus.APPROVED)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(questionRepository.save(question)).thenReturn(savedQuestion);
        when(questionMapper.questionToQuestionEvent(savedQuestion)).thenReturn(pendingEvent);

        questionService.approveQuestion(sessionId, questionId);

        assertEquals(QuestionStatus.APPROVED, question.getStatus());
        assertNotNull(question.getApprovedAt());

        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/1/questions/pending"),
                any(QuestionEvent.class)
        );
        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/1/questions"),
                any(QuestionEvent.class)
        );
    }

    @Test
    void approveQuestion_whenQuestionIsNotPending_shouldThrowInvalidQuestionStatusException() {
        Long sessionId = 1L;
        Long questionId = 100L;

        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.liveSessionOwnedBy(owner);
        session.setId(sessionId);

        Question question = QuestionEntityFactory.approvedQuestion();
        question.setId(questionId);
        question.setSession(session);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        AppException exception = assertThrows(
                AppException.class,
                () -> questionService.approveQuestion(sessionId, questionId)
        );

        assertEquals(INVALID_QUESTION_STATUS, exception.getErrorCode());
        assertEquals("Question should be pending", exception.getMessage());

        verify(questionRepository, never()).save(any(Question.class));
    }

    @Test
    void hideQuestion_whenQuestionIsPending_shouldHideAndBroadcastToPendingTopic() {
        Long sessionId = 1L;
        Long questionId = 100L;

        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.liveSessionOwnedBy(owner);
        session.setId(sessionId);

        Question question = QuestionEntityFactory.pendingQuestion();
        question.setId(questionId);
        question.setSession(session);

        QuestionEvent event = QuestionEvent
                .builder()
                .questionId(questionId)
                .sessionId(sessionId)
                .status(QuestionStatus.HIDDEN)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(questionRepository.save(question)).thenReturn(question);
        when(questionMapper.questionToQuestionEvent(question)).thenReturn(event);

        questionService.hideQuestion(sessionId, questionId);

        assertEquals(QuestionStatus.HIDDEN, question.getStatus());

        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/1/questions/pending"),
                any(QuestionEvent.class)
        );
        verify(messagingTemplate, never()).convertAndSend(
                eq("/topic/sessions/1/questions"),
                any(QuestionEvent.class)
        );
    }

    @Test
    void pinQuestion_whenQuestionIsApproved_shouldSetPinnedTrueAndBroadcastPublicUpdate() {
        Long sessionId = 1L;
        Long questionId = 100L;

        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.liveSessionOwnedBy(owner);
        session.setId(sessionId);

        Question question = QuestionEntityFactory.approvedQuestion();
        question.setId(questionId);
        question.setSession(session);

        QuestionEvent event = QuestionEvent
                .builder()
                .questionId(questionId)
                .sessionId(sessionId)
                .status(QuestionStatus.APPROVED)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(questionRepository.save(question)).thenReturn(question);
        when(questionMapper.questionToQuestionEvent(question)).thenReturn(event);

        questionService.pinQuestion(sessionId, questionId);

        assertEquals(true, question.isPinned());

        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/1/questions"),
                any(QuestionEvent.class)
        );
    }

    @Test
    void unpinquestion_whenQuestionIsApproved_shouldSetPinnedFalseAndBroadcastPublicUpdate() {
        Long sessionId = 1L;
        Long questionId = 100L;

        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.liveSessionOwnedBy(owner);
        session.setId(sessionId);

        Question question = QuestionEntityFactory.approvedQuestion();
        question.setId(questionId);
        question.setSession(session);
        question.setPinned(true);

        QuestionEvent event = QuestionEvent
                .builder()
                .questionId(questionId)
                .sessionId(sessionId)
                .status(QuestionStatus.APPROVED)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(questionRepository.save(question)).thenReturn(question);
        when(questionMapper.questionToQuestionEvent(question)).thenReturn(event);

        questionService.unpinquestion(sessionId, questionId);

        assertEquals(false, question.isPinned());

        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/1/questions"),
                any(QuestionEvent.class)
        );
    }

    @Test
    void markQuestionAsAnswered_whenQuestionIsApproved_shouldSetAnsweredStatusAnsweredAtUnpinAndBroadcast() {
        Long sessionId = 1L;
        Long questionId = 100L;

        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.liveSessionOwnedBy(owner);
        session.setId(sessionId);

        Question question = QuestionEntityFactory.approvedQuestion();
        question.setId(questionId);
        question.setSession(session);
        question.setPinned(true);

        QuestionEvent event = QuestionEvent
                .builder()
                .questionId(questionId)
                .sessionId(sessionId)
                .status(QuestionStatus.ANSWERED)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(questionRepository.save(question)).thenReturn(question);
        when(questionMapper.questionToQuestionEvent(question)).thenReturn(event);

        questionService.markQuestionAsAnswered(sessionId, questionId);

        assertEquals(true, question.isAnswered());
        assertEquals(false, question.isPinned());
        assertEquals(QuestionStatus.ANSWERED, question.getStatus());
        assertNotNull(question.getAnsweredAt());

        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/1/questions"),
                any(QuestionEvent.class)
        );
    }

    @Test
    void deleteQuestion_whenParticipantOwnsQuestion_shouldDeleteAndBroadcastDeletedEventToBothTopics() {
        Long sessionId = 1L;
        Long questionId = 100L;
        Long participantId = 10L;

        Session session = SessionEntityFactory.liveSession();
        session.setId(sessionId);

        Participant participant = ParticipantEntityFactory.participantForSession(session);
        participant.setId(participantId);

        Question question = QuestionEntityFactory.pendingQuestion();
        question.setId(questionId);
        question.setSession(session);
        question.setParticipant(participant);

        when(currentParticipantService.getCurrentParticipantId()).thenReturn(participantId);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(participantService.getParticipantById(participantId)).thenReturn(participant);
        when(questionRepository.findByIdAndSession_IdAndParticipant_Id(questionId, sessionId, participantId))
                .thenReturn(Optional.of(question));

        questionService.deleteQuestion(sessionId, questionId);

        ArgumentCaptor<QuestionDeletedEvent> eventCaptor = ArgumentCaptor.forClass(QuestionDeletedEvent.class);

        verify(questionRepository).delete(question);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/1/questions"),
                eventCaptor.capture()
        );
        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/1/questions/pending"),
                any(QuestionDeletedEvent.class)
        );

        assertEquals(questionId, eventCaptor.getValue().getQuestionId());
        assertEquals(sessionId, eventCaptor.getValue().getSessionId());
        assertEquals(QuestionEventType.DELETED, eventCaptor.getValue().getQuestionEventType());
    }

    @Test
    void upvotequestion_whenParticipantCanUpvote_shouldSaveVoteIncrementQuestionAndBroadcast() {
        Long sessionId = 1L;
        Long questionId = 100L;
        Long participantId = 10L;

        Session session = SessionEntityFactory.liveSession();
        session.setId(sessionId);

        Participant participant = ParticipantEntityFactory.participantForSession(session);
        participant.setId(participantId);

        Question question = QuestionEntityFactory.approvedQuestion();
        question.setId(questionId);
        question.setSession(session);
        question.setUpvoteCount(2);

        QuestionEvent event = QuestionEvent
                .builder()
                .questionId(questionId)
                .sessionId(sessionId)
                .status(QuestionStatus.APPROVED)
                .build();

        when(currentParticipantService.getCurrentParticipantId()).thenReturn(participantId);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(participantService.getParticipantById(participantId)).thenReturn(participant);
        when(rateLimiterService.isAllowed(
                "rate:participant:10:votes",
                5,
                Duration.ofSeconds(60)
        )).thenReturn(true);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(questionVoteRepository.existsByParticipant_IdAndQuestion_Id(participantId, questionId)).thenReturn(false);
        when(questionRepository.save(question)).thenReturn(question);
        when(questionMapper.questionToQuestionEvent(question)).thenReturn(event);

        questionService.upvotequestion(sessionId, questionId);

        ArgumentCaptor<QuestionVote> voteCaptor = ArgumentCaptor.forClass(QuestionVote.class);

        verify(questionVoteRepository).save(voteCaptor.capture());
        verify(questionRepository).save(question);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/1/questions"),
                any(QuestionEvent.class)
        );

        assertEquals(3, question.getUpvoteCount());
        assertEquals(question, voteCaptor.getValue().getQuestion());
        assertEquals(participant, voteCaptor.getValue().getParticipant());
    }

    @Test
    void upvotequestion_whenParticipantAlreadyUpvoted_shouldThrowForbiddenException() {
        Long sessionId = 1L;
        Long questionId = 100L;
        Long participantId = 10L;

        Session session = SessionEntityFactory.liveSession();
        session.setId(sessionId);

        Participant participant = ParticipantEntityFactory.participantForSession(session);
        participant.setId(participantId);

        Question question = QuestionEntityFactory.approvedQuestion();
        question.setId(questionId);
        question.setSession(session);

        when(currentParticipantService.getCurrentParticipantId()).thenReturn(participantId);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(participantService.getParticipantById(participantId)).thenReturn(participant);
        when(rateLimiterService.isAllowed(
                "rate:participant:10:votes",
                5,
                Duration.ofSeconds(60)
        )).thenReturn(true);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(questionVoteRepository.existsByParticipant_IdAndQuestion_Id(participantId, questionId)).thenReturn(true);

        AppException exception = assertThrows(
                AppException.class,
                () -> questionService.upvotequestion(sessionId, questionId)
        );

        assertEquals(FORBIDDEN, exception.getErrorCode());
        assertEquals("Participant cannot upvote the same question again", exception.getMessage());

        verify(questionVoteRepository, never()).save(any(QuestionVote.class));
        verify(questionRepository, never()).save(any(Question.class));
    }

    @Test
    void getNumberOfQuestionsBySessionIdAndStatus_shouldReturnRepositoryCount() {
        when(questionRepository.countBySession_IdAndStatus(1L, QuestionStatus.APPROVED)).thenReturn(5L);

        long count = questionService.getNumberOfQuestionsBySessionIdAndStatus(1L, QuestionStatus.APPROVED);

        assertEquals(5L, count);

        verify(questionRepository).countBySession_IdAndStatus(1L, QuestionStatus.APPROVED);
    }

    @Test
    void getTopUpvotedQuestionBySessionId_shouldReturnMappedTopQuestions() {
        Long sessionId = 1L;

        Question question = QuestionEntityFactory.approvedQuestion();
        QuestionResponse response = QuestionResponseFactory.approvedQuestionResponse();

        when(questionRepository.findTop5BySession_IdAndStatusInOrderByUpvoteCountDescCreatedAtAsc(
                eq(sessionId),
                eq(List.of(QuestionStatus.APPROVED, QuestionStatus.ANSWERED))
        )).thenReturn(List.of(question));
        when(questionMapper.questionToQuestionResponse(question)).thenReturn(response);

        List<QuestionResponse> result = questionService.getTopUpvotedQuestionBySessionId(sessionId);

        assertEquals(1, result.size());
        assertEquals(response, result.get(0));
    }

    @Test
    void getQuestionsForExportBySessionId_shouldReturnMappedQuestionsOrderedByCreatedAt() {
        Long sessionId = 1L;

        Question question = QuestionEntityFactory.approvedQuestion();
        QuestionResponse response = QuestionResponseFactory.approvedQuestionResponse();

        when(questionRepository.findBySession_IdOrderByCreatedAtAsc(sessionId)).thenReturn(List.of(question));
        when(questionMapper.questionToQuestionResponse(question)).thenReturn(response);

        List<QuestionResponse> result = questionService.getQuestionsForExportBySessionId(sessionId);

        assertEquals(1, result.size());
        assertEquals(response, result.get(0));
    }

    @Test
    void uploadQuestionAttachment_whenParticipantOwnsQuestionAndFileIsValid_shouldStoreAttachmentAndReturnResponse() {
        Long sessionId = 1L;
        Long questionId = 100L;
        Long participantId = 10L;

        Session session = SessionEntityFactory.liveSession();
        session.setId(sessionId);

        Participant participant = ParticipantEntityFactory.participantForSession(session);
        participant.setId(participantId);

        Question question = QuestionEntityFactory.approvedQuestion();
        question.setId(questionId);
        question.setSession(session);
        question.setParticipant(participant);

        MultipartFile file = mock(MultipartFile.class);

        QuestionAttachment savedAttachment = QuestionAttachmentEntityFactory.attachmentForQuestion(question);

        QuestionAttachmentResponse expectedResponse = new QuestionAttachmentResponse(
                savedAttachment.getId(),
                questionId,
                savedAttachment.getFileName(),
                savedAttachment.getFileUrl(),
                savedAttachment.getFileType(),
                savedAttachment.getFileSize(),
                savedAttachment.getUploadedAt()
        );

        when(currentParticipantService.getCurrentParticipantId()).thenReturn(participantId);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(participantService.getParticipantById(participantId)).thenReturn(participant);
        when(rateLimiterService.isAllowed(
                "rate:participant:10:attachments",
                5,
                Duration.ofSeconds(60)
        )).thenReturn(true);
        when(fileStorageService.storeAttachmentFile(file, "sessions/1/questions/100"))
                .thenReturn("uploads/sessions/1/questions/100/file.pdf");
        when(fileStorageService.getFileType(file)).thenReturn(FileType.PDF);
        when(file.getOriginalFilename()).thenReturn("file.pdf");
        when(file.getSize()).thenReturn(1024L);
        when(questionAttachmentRepository.save(any(QuestionAttachment.class))).thenReturn(savedAttachment);
        when(questionAttachmentMapper.questionAttachmentToQuestionAttachmentResponse(savedAttachment))
                .thenReturn(expectedResponse);

        QuestionAttachmentResponse response =
                questionService.uploadQuestionAttachment(sessionId, questionId, file);

        assertEquals(expectedResponse, response);

        ArgumentCaptor<QuestionAttachment> attachmentCaptor = ArgumentCaptor.forClass(QuestionAttachment.class);

        verify(questionAttachmentRepository).save(attachmentCaptor.capture());

        QuestionAttachment attachmentBeforeSave = attachmentCaptor.getValue();

        assertEquals(question, attachmentBeforeSave.getQuestion());
        assertEquals("file.pdf", attachmentBeforeSave.getFileName());
        assertEquals("uploads/sessions/1/questions/100/file.pdf", attachmentBeforeSave.getFileUrl());
        assertEquals(FileType.PDF, attachmentBeforeSave.getFileType());
        assertEquals(1024L, attachmentBeforeSave.getFileSize());
    }

    @Test
    void uploadQuestionAttachment_whenOriginalFileNameIsBlank_shouldThrowValidationException() {
        Long sessionId = 1L;
        Long questionId = 100L;
        Long participantId = 10L;

        Session session = SessionEntityFactory.liveSession();
        session.setId(sessionId);

        Participant participant = ParticipantEntityFactory.participantForSession(session);
        participant.setId(participantId);

        Question question = QuestionEntityFactory.approvedQuestion();
        question.setId(questionId);
        question.setSession(session);
        question.setParticipant(participant);

        MultipartFile file = mock(MultipartFile.class);

        when(currentParticipantService.getCurrentParticipantId()).thenReturn(participantId);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(participantService.getParticipantById(participantId)).thenReturn(participant);
        when(rateLimiterService.isAllowed(
                "rate:participant:10:attachments",
                5,
                Duration.ofSeconds(60)
        )).thenReturn(true);
        when(fileStorageService.storeAttachmentFile(file, "sessions/1/questions/100"))
                .thenReturn("uploads/sessions/1/questions/100/file.pdf");
        when(fileStorageService.getFileType(file)).thenReturn(FileType.PDF);
        when(file.getOriginalFilename()).thenReturn("   ");

        AppException exception = assertThrows(
                AppException.class,
                () -> questionService.uploadQuestionAttachment(sessionId, questionId, file)
        );

        assertEquals(VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("Invalid file name", exception.getMessage());

        verify(questionAttachmentRepository, never()).save(any(QuestionAttachment.class));
    }

    @Test
    void getLiveSessionValidation_whenSessionIsNotLive_shouldThrowInvalidSessionStatusException() {
        Long sessionId = 1L;
        Long participantId = 10L;

        Session session = SessionEntityFactory.sessionWithStatus(SessionStatus.SCHEDULED);

        when(currentParticipantService.getCurrentParticipantId()).thenReturn(participantId);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);

        AppException exception = assertThrows(
                AppException.class,
                () -> questionService.submitQuestion(sessionId, new SubmitQuestionRequest("Question"))
        );

        assertEquals(INVALID_SESSION_STATUS, exception.getErrorCode());
        assertEquals("Only live sessions can receive questions", exception.getMessage());
    }

    @Test
    void getParticipantOrThrow_whenParticipantDoesNotExist_shouldThrowResourceNotFoundException() {
        Long sessionId = 1L;
        Long participantId = 10L;

        Session session = SessionEntityFactory.liveSession();

        when(currentParticipantService.getCurrentParticipantId()).thenReturn(participantId);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(participantService.getParticipantById(participantId)).thenReturn(null);

        AppException exception = assertThrows(
                AppException.class,
                () -> questionService.submitQuestion(sessionId, new SubmitQuestionRequest("Question"))
        );

        assertEquals(RESOURCE_NOT_FOUND, exception.getErrorCode());
        assertEquals("Participant not found", exception.getMessage());
    }
}