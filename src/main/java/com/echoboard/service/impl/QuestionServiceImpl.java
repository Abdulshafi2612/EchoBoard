package com.echoboard.service.impl;

import com.echoboard.dto.common.PageResponse;
import com.echoboard.dto.question.QuestionResponse;
import com.echoboard.dto.question.SubmitQuestionRequest;
import com.echoboard.dto.websocket.QuestionDeletedEvent;
import com.echoboard.dto.websocket.QuestionEvent;
import com.echoboard.entity.*;
import com.echoboard.enums.QuestionEventType;
import com.echoboard.enums.QuestionStatus;
import com.echoboard.enums.SessionStatus;
import com.echoboard.exception.AppException;
import com.echoboard.mapper.QuestionMapper;
import com.echoboard.repository.QuestionRepository;
import com.echoboard.repository.QuestionVoteRepository;
import com.echoboard.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.echoboard.enums.QuestionEventType.CREATED;
import static com.echoboard.enums.QuestionEventType.UPDATED;
import static com.echoboard.enums.QuestionStatus.*;
import static com.echoboard.exception.ErrorCode.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private static final String QUESTIONS_TOPIC_TEMPLATE = "/topic/sessions/%d/questions";
    private static final String PENDING_QUESTIONS_TOPIC_TEMPLATE = "/topic/sessions/%d/questions/pending";

    private final QuestionRepository questionRepository;
    private final SessionService sessionService;
    private final ParticipantService participantService;
    private final QuestionMapper questionMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final CurrentParticipantService currentParticipantService;
    private final CurrentUserService currentUserService;
    private final QuestionVoteRepository questionVoteRepository;

    @Override
    public PageResponse<QuestionResponse> getQuestionsBySessionIdAndStatus(
            Pageable pageable,
            Long sessionId,
            QuestionStatus status
    ) {
        User user = currentUserService.getCurrentUser();
        Session session = getLiveSessionOrThrow(sessionId);

        validateUserOwnsSession(session, user);

        Page<Question> questionPage = questionRepository.findBySession_IdAndStatus(
                sessionId,
                status,
                pageable
        );

        List<QuestionResponse> questionResponses = questionPage
                .getContent()
                .stream()
                .map(questionMapper::questionToQuestionResponse)
                .toList();

        return new PageResponse<>(
                questionResponses,
                questionPage.getNumber(),
                questionPage.getSize(),
                questionPage.getTotalElements(),
                questionPage.getTotalPages(),
                questionPage.isLast()
        );
    }

    @Override
    @Transactional
    public void submitQuestion(Long sessionId, SubmitQuestionRequest request) {
        Long participantId = currentParticipantService.getCurrentParticipantId();
        validateParticipantIdentity(participantId);

        Session session = getLiveSessionOrThrow(sessionId);
        Participant participant = getParticipantOrThrow(participantId);

        validateParticipantBelongsToSession(participant, sessionId);
        validateParticipantIsNotMuted(participant);

        Question question = createQuestion(session, participant, request.getContent());
        Question savedQuestion = questionRepository.save(question);

        if (PENDING.equals(savedQuestion.getStatus())) {
            broadcastQuestionEventToPendingTopic(savedQuestion, CREATED);
        } else {
            broadcastQuestionEventToPublicTopic(savedQuestion, CREATED);
        }
    }

    @Override
    @Transactional
    public void deleteQuestion(Long sessionId, Long questionId) {
        Long participantId = currentParticipantService.getCurrentParticipantId();
        validateParticipantIdentity(participantId);

        getLiveSessionOrThrow(sessionId);

        Participant participant = getParticipantOrThrow(participantId);
        validateParticipantBelongsToSession(participant, sessionId);
        validateParticipantIsNotMuted(participant);

        Question question = getParticipantQuestionOrThrow(sessionId, questionId, participantId);
        Long deletedQuestionId = question.getId();

        questionRepository.delete(question);

        broadcastQuestionDeletedEvent(sessionId, deletedQuestionId);
    }

    @Override
    @Transactional
    public void approveQuestion(Long sessionId, Long questionId) {
        approvePendingQuestion(sessionId, questionId);
    }

    @Override
    @Transactional
    public void hideQuestion(Long sessionId, Long questionId) {
        hidePendingQuestion(sessionId, questionId);
    }

    @Override
    @Transactional
    public void pinQuestion(Long sessionId, Long questionId) {
        pinOrUnpinQuestion(sessionId, questionId, true);
    }

    @Override
    @Transactional
    public void unpinquestion(Long sessionId, Long questionId) {
        pinOrUnpinQuestion(sessionId, questionId, false);
    }

    @Override
    @Transactional
    public void markQuestionAsAnswered(Long sessionId, Long questionId) {
        Question question = getQuestionOfOwnedSession(sessionId, questionId);
        validateQuestionIsApproved(question);

        question.setAnswered(true);
        question.setStatus(ANSWERED);
        question.setAnsweredAt(LocalDateTime.now());
        question.setPinned(false);

        Question savedQuestion = questionRepository.save(question);
        broadcastQuestionEventToPublicTopic(savedQuestion, UPDATED);
    }

    @Override
    @Transactional
    public void upvotequestion(Long sessionId, Long questionId) {
        Long participantId = currentParticipantService.getCurrentParticipantId();
        validateParticipantIdentity(participantId);

        getLiveSessionOrThrow(sessionId);

        Participant participant = getParticipantOrThrow(participantId);
        validateParticipantBelongsToSession(participant, sessionId);
        validateParticipantIsNotMuted(participant);

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(
                        RESOURCE_NOT_FOUND,
                        NOT_FOUND,
                        "Question not found"
                ));

        validateQuestionBelongsToSession(question, sessionId);
        validateQuestionIsApproved(question);

        if (questionVoteRepository.existsByParticipant_IdAndQuestion_Id(participantId, questionId)) {
            throw new AppException(
                    FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "Participant cannot upvote the same question again"
            );
        }

        QuestionVote questionVote = new QuestionVote();
        questionVote.setQuestion(question);
        questionVote.setParticipant(participant);

        questionVoteRepository.save(questionVote);

        question.setUpvoteCount(question.getUpvoteCount() + 1);
        Question savedQuestion = questionRepository.save(question);

        broadcastQuestionEventToPublicTopic(savedQuestion, UPDATED);
    }


    private Question getParticipantQuestionOrThrow(Long sessionId, Long questionId, Long participantId) {
        return questionRepository
                .findByIdAndSession_IdAndParticipant_Id(questionId, sessionId, participantId)
                .orElseThrow(() -> new AppException(
                        RESOURCE_NOT_FOUND,
                        NOT_FOUND,
                        "Question not found"
                ));
    }

    private void pinOrUnpinQuestion(Long sessionId, Long questionId, boolean pinned) {
        Question question = getQuestionOfOwnedSession(sessionId, questionId);
        validateQuestionIsApproved(question);

        question.setPinned(pinned);

        Question savedQuestion = questionRepository.save(question);
        broadcastQuestionEventToPublicTopic(savedQuestion, UPDATED);
    }

    private Question getQuestionOfOwnedSession(Long sessionId, Long questionId) {
        User user = currentUserService.getCurrentUser();

        Session session = getLiveSessionOrThrow(sessionId);
        validateUserOwnsSession(session, user);

        Question question = questionRepository
                .findById(questionId)
                .orElseThrow(() -> new AppException(
                        RESOURCE_NOT_FOUND,
                        NOT_FOUND,
                        "Question not found"
                ));

        validateQuestionBelongsToSession(question, sessionId);

        return question;
    }

    private void approvePendingQuestion(Long sessionId, Long questionId) {
        Question question = getQuestionOfOwnedSession(sessionId, questionId);
        validateQuestionIsPending(question);

        question.setStatus(APPROVED);
        question.setApprovedAt(LocalDateTime.now());

        Question savedQuestion = questionRepository.save(question);

        broadcastQuestionEventToPendingTopic(savedQuestion, UPDATED);
        broadcastQuestionEventToPublicTopic(savedQuestion, UPDATED);
    }

    private void hidePendingQuestion(Long sessionId, Long questionId) {
        Question question = getQuestionOfOwnedSession(sessionId, questionId);
        validateQuestionIsPending(question);

        question.setStatus(HIDDEN);

        Question savedQuestion = questionRepository.save(question);

        broadcastQuestionEventToPendingTopic(savedQuestion, UPDATED);
    }

    private void validateParticipantIdentity(Long participantId) {
        if (participantId == null) {
            throw new AppException(
                    FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "Only participants can submit questions"
            );
        }
    }

    private void validateQuestionIsPending(Question question) {
        if (!PENDING.equals(question.getStatus())) {
            throw new AppException(
                    INVALID_QUESTION_STATUS,
                    BAD_REQUEST,
                    "Question should be pending"
            );
        }
    }

    private void validateQuestionIsApproved(Question question) {
        if (!APPROVED.equals(question.getStatus())) {
            throw new AppException(
                    INVALID_QUESTION_STATUS,
                    BAD_REQUEST,
                    "Question should be approved"
            );
        }
    }

    private void validateUserOwnsSession(Session session, User user) {
        if (!session.getOwner().getId().equals(user.getId())) {
            throw new AppException(
                    FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "User does not own this session"
            );
        }
    }

    private void validateQuestionBelongsToSession(Question question, Long sessionId) {
        if (!question.getSession().getId().equals(sessionId)) {
            throw new AppException(
                    FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "Question does not belong to this session"
            );
        }
    }

    private Session getLiveSessionOrThrow(Long sessionId) {
        Session session = sessionService.getSessionById(sessionId);

        if (session == null) {
            throw new AppException(
                    RESOURCE_NOT_FOUND,
                    NOT_FOUND,
                    "Session not found"
            );
        }

        if (SessionStatus.LIVE != session.getStatus()) {
            throw new AppException(
                    INVALID_SESSION_STATUS,
                    BAD_REQUEST,
                    "Only live sessions can receive questions"
            );
        }

        return session;
    }

    private Participant getParticipantOrThrow(Long participantId) {
        Participant participant = participantService.getParticipantById(participantId);

        if (participant == null) {
            throw new AppException(
                    RESOURCE_NOT_FOUND,
                    NOT_FOUND,
                    "Participant not found"
            );
        }

        return participant;
    }

    private void validateParticipantBelongsToSession(Participant participant, Long sessionId) {
        if (!participant.getSession().getId().equals(sessionId)) {
            throw new AppException(
                    FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "Participant is not allowed to access this session"
            );
        }
    }

    private void validateParticipantIsNotMuted(Participant participant) {
        if (participant.getMutedUntil() != null && participant.getMutedUntil().isAfter(LocalDateTime.now())) {
            throw new AppException(
                    FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "Cannot submit question while muted"
            );
        }
    }

    private Question createQuestion(Session session, Participant participant, String content) {
        Question question = new Question();

        question.setContent(content);
        question.setSession(session);
        question.setParticipant(participant);

        QuestionStatus status = determineInitialStatus(session);
        question.setStatus(status);

        if (APPROVED.equals(status)) {
            question.setApprovedAt(LocalDateTime.now());
        }

        return question;
    }

    private QuestionStatus determineInitialStatus(Session session) {
        return session.isModerationEnabled() ? PENDING : APPROVED;
    }

    private void broadcastQuestionEventToPublicTopic(Question question, QuestionEventType eventType) {
        QuestionEvent event = buildQuestionEvent(question, eventType);

        messagingTemplate.convertAndSend(
                QUESTIONS_TOPIC_TEMPLATE.formatted(event.getSessionId()),
                event
        );
    }

    private void broadcastQuestionEventToPendingTopic(Question question, QuestionEventType eventType) {
        QuestionEvent event = buildQuestionEvent(question, eventType);

        messagingTemplate.convertAndSend(
                PENDING_QUESTIONS_TOPIC_TEMPLATE.formatted(event.getSessionId()),
                event
        );
    }

    private QuestionEvent buildQuestionEvent(Question question, QuestionEventType eventType) {
        QuestionEvent event = questionMapper.questionToQuestionEvent(question);
        event.setQuestionEventType(eventType);
        return event;
    }

    private void broadcastQuestionDeletedEvent(Long sessionId, Long questionId) {
        QuestionDeletedEvent event = QuestionDeletedEvent
                .builder()
                .questionId(questionId)
                .sessionId(sessionId)
                .build();

        messagingTemplate.convertAndSend(
                QUESTIONS_TOPIC_TEMPLATE.formatted(sessionId),
                event
        );

        messagingTemplate.convertAndSend(
                PENDING_QUESTIONS_TOPIC_TEMPLATE.formatted(sessionId),
                event
        );
    }
}