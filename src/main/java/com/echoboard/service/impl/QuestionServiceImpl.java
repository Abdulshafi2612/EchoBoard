package com.echoboard.service.impl;

import com.echoboard.dto.question.SubmitQuestionRequest;
import com.echoboard.dto.websocket.QuestionEvent;
import com.echoboard.entity.Participant;
import com.echoboard.entity.Question;
import com.echoboard.entity.Session;
import com.echoboard.enums.QuestionStatus;
import com.echoboard.enums.SessionStatus;
import com.echoboard.exception.AppException;
import com.echoboard.mapper.QuestionMapper;
import com.echoboard.repository.QuestionRepository;
import com.echoboard.service.ParticipantService;
import com.echoboard.service.QuestionService;
import com.echoboard.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.echoboard.enums.QuestionStatus.APPROVED;
import static com.echoboard.enums.QuestionStatus.PENDING;
import static com.echoboard.exception.ErrorCode.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final SessionService sessionService;
    private final ParticipantService participantService;
    private final QuestionMapper questionMapper;
    private final SimpMessagingTemplate messagingTemplate;


    @Override
    @Transactional
    public void submitQuestion(Long sessionId, Long participantId, SubmitQuestionRequest request) {
        validateParticipantIdentity(participantId);

        Session session = getLiveSessionOrThrow(sessionId);
        Participant participant = getParticipantOrThrow(participantId);

        validateParticipantBelongsToSession(participant, sessionId);
        validateParticipantIsNotMuted(participant);

        Question question = createQuestion(session, participant, request.getContent());
        Question savedQuestion = questionRepository.save(question);

        broadcastQuestionEvent(savedQuestion);
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
                    "Participant is not allowed to submit questions to this session"
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

    private void broadcastQuestionEvent(Question question) {
        QuestionEvent event = questionMapper.questionToQuestionEvent(question);
        String destinationSuffix = event.getStatus().equals(QuestionStatus.APPROVED) ? "" : "/pending";
        messagingTemplate.convertAndSend(
                "/topic/sessions/" + event.getSessionId() + "/questions" + destinationSuffix,
                event
        );
    }
}