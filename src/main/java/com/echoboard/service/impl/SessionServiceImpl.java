package com.echoboard.service.impl;

import com.echoboard.dto.common.PageResponse;
import com.echoboard.dto.rabbitmq.SessionCreatedEvent;
import com.echoboard.dto.rabbitmq.SessionEndedEvent;
import com.echoboard.dto.session.CreateSessionRequest;
import com.echoboard.dto.session.SessionResponse;
import com.echoboard.dto.session.UpdateSessionRequest;
import com.echoboard.entity.Session;
import com.echoboard.entity.User;
import com.echoboard.exception.AppException;
import com.echoboard.exception.ErrorCode;
import com.echoboard.mapper.SessionMapper;
import com.echoboard.rabbitmq.RabbitMQPublisher;
import com.echoboard.repository.SessionRepository;
import com.echoboard.service.CurrentUserService;
import com.echoboard.service.FileStorageService;
import com.echoboard.service.PollCounterSyncService;
import com.echoboard.service.SessionService;
import com.echoboard.util.AccessCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static com.echoboard.enums.SessionStatus.*;
import static com.echoboard.exception.ErrorCode.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private static final String LOGO_FOLDER_PATH = "sessions/%d/logo";

    private final SessionRepository sessionRepository;
    private final SessionMapper sessionMapper;
    private final CurrentUserService currentUserService;
    private final RabbitMQPublisher rabbitMQPublisher;
    private final PollCounterSyncService pollCounterSyncService;
    private final FileStorageService fileStorageService;

    @Override
    public SessionResponse createSession(CreateSessionRequest request) {
        User user = currentUserService.getCurrentUser();
        Session session = sessionMapper.createSessionRequestToSession(request);
        String accessCode = generateUniqueAccessCode();
        session.setAccessCode(accessCode);
        session.setOwner(user);
        session.setStatus(SCHEDULED);

        session.setModerationEnabled(
                request.getModerationEnabled() != null ? request.getModerationEnabled() : true
        );

        session.setAnonymousAllowed(
                request.getAnonymousAllowed() != null ? request.getAnonymousAllowed() : true
        );

        Session savedSession = sessionRepository.save(session);

        SessionCreatedEvent sessionCreatedEvent = SessionCreatedEvent
                .builder()
                .sessionId(savedSession.getId())
                .title(savedSession.getTitle())
                .createdAt(savedSession.getCreatedAt())
                .ownerId(user.getId())
                .ownerEmail(user.getEmail())
                .build();

        rabbitMQPublisher.publishSessionCreatedEvent(sessionCreatedEvent);

        return sessionMapper.sessionToSessionResponse(savedSession);
    }

    @Override
    public PageResponse<SessionResponse> getMySessions(Pageable pageable) {
        User user = currentUserService.getCurrentUser();

        Page<Session> sessionsPage = sessionRepository.findByOwner(user, pageable);

        List<SessionResponse> content = sessionsPage
                .getContent()
                .stream()
                .map(sessionMapper::sessionToSessionResponse)
                .toList();

        return new PageResponse<>(
                content,
                sessionsPage.getNumber(),
                sessionsPage.getSize(),
                sessionsPage.getTotalElements(),
                sessionsPage.getTotalPages(),
                sessionsPage.isLast()
        );

    }

    @Override
    public SessionResponse getSessionResponseById(Long id) {
        Session session = getOwnedSessionOrThrow(id);
        return sessionMapper.sessionToSessionResponse(session);
    }

    @Override
    public Session getSessionById(Long id) {
        return sessionRepository.findById(id).orElse(null);
    }

    @Override
    public Session getSessionByAccessCode(String accessCode) {
        return sessionRepository
                .findByAccessCode(accessCode).
                orElseThrow(() -> new AppException(
                        RESOURCE_NOT_FOUND,
                        NOT_FOUND,
                        "Invalid access code"
                ));
    }

    @Override
    public SessionResponse updateSession(Long id, UpdateSessionRequest request) {
        Session session = getOwnedSessionOrThrow(id);
        if (session.getStatus() == ENDED || session.getStatus() == ARCHIVED) {
            throw new AppException(ErrorCode.FORBIDDEN, FORBIDDEN, "Can't edit this session");
        }

        if (request.getTitle() != null && request.getTitle().isBlank()) {
            throw new AppException(VALIDATION_ERROR, BAD_REQUEST, "Title must not be empty");
        }

        if (request.getTitle() != null) {
            session.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            session.setDescription(request.getDescription());
        }

        if (request.getModerationEnabled() != null) {
            session.setModerationEnabled(request.getModerationEnabled());
        }

        if (request.getAnonymousAllowed() != null) {
            session.setAnonymousAllowed(request.getAnonymousAllowed());
        }
        Session updatedSession = sessionRepository.save(session);
        return sessionMapper.sessionToSessionResponse(updatedSession);
    }

    @Override
    public SessionResponse startSession(Long id) {
        Session session = getOwnedSessionOrThrow(id);

        if (session.getStatus() != SCHEDULED) {
            throw new AppException(
                    INVALID_SESSION_STATUS,
                    BAD_REQUEST,
                    "Only scheduled sessions can be started"
            );
        }

        session.setStartedAt(LocalDateTime.now());
        session.setStatus(LIVE);
        Session startedSession = sessionRepository.save(session);
        return sessionMapper.sessionToSessionResponse(startedSession);
    }

    @Override
    public SessionResponse endSession(Long id) {
        Session session = getOwnedSessionOrThrow(id);

        if (session.getStatus() != LIVE) {
            throw new AppException(
                    INVALID_SESSION_STATUS,
                    BAD_REQUEST,
                    "Only live sessions can be ended"
            );
        }

        session.setEndedAt(LocalDateTime.now());
        session.setStatus(ENDED);
        Session endedSession = sessionRepository.save(session);

        pollCounterSyncService.syncPollCountersForSession(endedSession.getId());

        SessionEndedEvent sessionEndedEvent = SessionEndedEvent
                .builder()
                .sessionId(endedSession.getId())
                .ownerId(endedSession.getOwner().getId())
                .ownerEmail(endedSession.getOwner().getEmail())
                .title(endedSession.getTitle())
                .startedAt(endedSession.getStartedAt())
                .endedAt(endedSession.getEndedAt())
                .build();

        rabbitMQPublisher.publishSessionEndedEvent(sessionEndedEvent);

        return sessionMapper.sessionToSessionResponse(endedSession);
    }

    @Override
    public SessionResponse archiveSession(Long id) {
        Session session = getOwnedSessionOrThrow(id);
        if (session.getStatus() == LIVE) {
            throw new AppException(
                    INVALID_SESSION_STATUS,
                    BAD_REQUEST,
                    "Live sessions cannot be archived"
            );
        }

        if (session.getStatus() == ARCHIVED) {
            throw new AppException(
                    INVALID_SESSION_STATUS,
                    BAD_REQUEST,
                    "Session is already archived"
            );
        }

        session.setStatus(ARCHIVED);

        Session archivedSession = sessionRepository.save(session);
        return sessionMapper.sessionToSessionResponse(archivedSession);
    }

    @Override
    public void deleteSession(Long id) {
        Session session = getOwnedSessionOrThrow(id);

        if (session.getStatus() != SCHEDULED) {
            throw new AppException(
                    INVALID_SESSION_STATUS,
                    BAD_REQUEST,
                    "Only scheduled sessions can be deleted"
            );
        }

        sessionRepository.delete(session);
    }

    @Override
    public Session getOwnedSessionOrThrow(Long sessionId) {
        User owner = currentUserService.getCurrentUser();

        return sessionRepository.findByIdAndOwner(sessionId, owner)
                .orElseThrow(() -> new AppException(
                        RESOURCE_NOT_FOUND,
                        NOT_FOUND,
                        "Session not found"
                ));
    }

    @Override
    public SessionResponse uploadSessionLogo(Long sessionId, MultipartFile file) {
        Session session = getOwnedSessionOrThrow(sessionId);
        String logoUrl = fileStorageService.storeFile(file, LOGO_FOLDER_PATH.formatted(sessionId));
        session.setLogoUrl(logoUrl);
        Session savedSession = sessionRepository.save(session);
        return sessionMapper.sessionToSessionResponse(savedSession);
    }

    private String generateUniqueAccessCode() {
        String accessCode = AccessCodeGenerator.generateAccessCode();
        while (sessionRepository.existsByAccessCode(accessCode)) {
            accessCode = AccessCodeGenerator.generateAccessCode();
        }
        return accessCode;
    }

}
