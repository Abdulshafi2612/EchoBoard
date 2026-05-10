package com.echoboard.service.impl;

import com.echoboard.dto.common.PageResponse;
import com.echoboard.dto.session.CreateSessionRequest;
import com.echoboard.dto.session.SessionResponse;
import com.echoboard.dto.session.UpdateSessionRequest;
import com.echoboard.entity.Session;
import com.echoboard.entity.User;
import com.echoboard.enums.SessionStatus;
import com.echoboard.exception.AppException;
import com.echoboard.mapper.SessionMapper;
import com.echoboard.rabbitmq.RabbitMQPublisher;
import com.echoboard.repository.SessionRepository;
import com.echoboard.service.CurrentUserService;
import com.echoboard.service.FileStorageService;
import com.echoboard.service.PollCounterSyncService;
import com.echoboard.util.SessionEntityFactory;
import com.echoboard.util.SessionResponseFactory;
import com.echoboard.util.UserEntityFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static com.echoboard.enums.SessionStatus.ARCHIVED;
import static com.echoboard.enums.SessionStatus.ENDED;
import static com.echoboard.enums.SessionStatus.LIVE;
import static com.echoboard.enums.SessionStatus.SCHEDULED;
import static com.echoboard.exception.ErrorCode.INVALID_SESSION_STATUS;
import static com.echoboard.exception.ErrorCode.RESOURCE_NOT_FOUND;
import static com.echoboard.exception.ErrorCode.VALIDATION_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private SessionMapper sessionMapper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private RabbitMQPublisher rabbitMQPublisher;

    @Mock
    private PollCounterSyncService pollCounterSyncService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private SessionServiceImpl sessionService;

    @Test
    void createSession_whenRequestIsValid_shouldCreateScheduledSessionPublishEventAndReturnResponse() {
        User owner = UserEntityFactory.presenter();

        CreateSessionRequest request = new CreateSessionRequest(
                "Backend Q&A",
                "Spring Boot session",
                null,
                null
        );

        Session mappedSession = new Session();
        mappedSession.setTitle(request.getTitle());
        mappedSession.setDescription(request.getDescription());

        Session savedSession = SessionEntityFactory.scheduledSession();
        savedSession.setId(1L);
        savedSession.setTitle(request.getTitle());
        savedSession.setDescription(request.getDescription());
        savedSession.setOwner(owner);
        savedSession.setAccessCode("ABC123");

        SessionResponse expectedResponse = SessionResponseFactory.fromSession(savedSession);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionMapper.createSessionRequestToSession(request)).thenReturn(mappedSession);
        when(sessionRepository.existsByAccessCode(any(String.class))).thenReturn(false);
        when(sessionRepository.save(mappedSession)).thenReturn(savedSession);
        when(sessionMapper.sessionToSessionResponse(savedSession)).thenReturn(expectedResponse);

        SessionResponse response = sessionService.createSession(request);

        assertEquals(expectedResponse, response);

        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);

        verify(currentUserService).getCurrentUser();
        verify(sessionMapper).createSessionRequestToSession(request);
        verify(sessionRepository).save(sessionCaptor.capture());
        verify(sessionMapper).sessionToSessionResponse(savedSession);
        verify(rabbitMQPublisher).publishSessionCreatedEvent(any());

        Session sessionBeforeSave = sessionCaptor.getValue();

        assertEquals(owner, sessionBeforeSave.getOwner());
        assertEquals(SCHEDULED, sessionBeforeSave.getStatus());
        assertNotNull(sessionBeforeSave.getAccessCode());
        assertEquals(6, sessionBeforeSave.getAccessCode().length());
        assertEquals(true, sessionBeforeSave.isModerationEnabled());
        assertEquals(true, sessionBeforeSave.isAnonymousAllowed());
    }

    @Test
    void createSession_whenModerationAndAnonymousFlagsAreProvided_shouldUseProvidedValues() {
        User owner = UserEntityFactory.presenter();

        CreateSessionRequest request = new CreateSessionRequest(
                "Backend Q&A",
                "Spring Boot session",
                false,
                false
        );

        Session mappedSession = new Session();
        mappedSession.setTitle(request.getTitle());
        mappedSession.setDescription(request.getDescription());

        Session savedSession = SessionEntityFactory.scheduledSession();
        savedSession.setModerationEnabled(false);
        savedSession.setAnonymousAllowed(false);

        SessionResponse expectedResponse = SessionResponseFactory.fromSession(savedSession);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionMapper.createSessionRequestToSession(request)).thenReturn(mappedSession);
        when(sessionRepository.existsByAccessCode(any(String.class))).thenReturn(false);
        when(sessionRepository.save(mappedSession)).thenReturn(savedSession);
        when(sessionMapper.sessionToSessionResponse(savedSession)).thenReturn(expectedResponse);

        SessionResponse response = sessionService.createSession(request);

        assertEquals(expectedResponse, response);
        assertEquals(false, mappedSession.isModerationEnabled());
        assertEquals(false, mappedSession.isAnonymousAllowed());

        verify(rabbitMQPublisher).publishSessionCreatedEvent(any());
    }

    @Test
    void getMySessions_shouldReturnMappedPageResponse() {
        User owner = UserEntityFactory.presenter();
        PageRequest pageable = PageRequest.of(0, 10);

        Session session = SessionEntityFactory.scheduledSession();
        SessionResponse sessionResponse = SessionResponseFactory.fromSession(session);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByOwner(owner, pageable))
                .thenReturn(new PageImpl<>(List.of(session), pageable, 1));
        when(sessionMapper.sessionToSessionResponse(session)).thenReturn(sessionResponse);

        PageResponse<SessionResponse> response = sessionService.getMySessions(pageable);

        assertEquals(1, response.getContent().size());
        assertEquals(sessionResponse, response.getContent().get(0));
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(1L, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertEquals(true, response.isLast());

        verify(currentUserService).getCurrentUser();
        verify(sessionRepository).findByOwner(owner, pageable);
        verify(sessionMapper).sessionToSessionResponse(session);
    }

    @Test
    void getSessionResponseById_whenOwnedSessionExists_shouldReturnMappedResponse() {
        Long sessionId = 1L;
        User owner = UserEntityFactory.presenter();
        Session session = SessionEntityFactory.scheduledSession();
        SessionResponse expectedResponse = SessionResponseFactory.fromSession(session);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByIdAndOwner(sessionId, owner)).thenReturn(Optional.of(session));
        when(sessionMapper.sessionToSessionResponse(session)).thenReturn(expectedResponse);

        SessionResponse response = sessionService.getSessionResponseById(sessionId);

        assertEquals(expectedResponse, response);

        verify(currentUserService).getCurrentUser();
        verify(sessionRepository).findByIdAndOwner(sessionId, owner);
        verify(sessionMapper).sessionToSessionResponse(session);
    }

    @Test
    void getSessionResponseById_whenOwnedSessionDoesNotExist_shouldThrowResourceNotFoundException() {
        Long sessionId = 404L;
        User owner = UserEntityFactory.presenter();

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByIdAndOwner(sessionId, owner)).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> sessionService.getSessionResponseById(sessionId)
        );

        assertEquals(RESOURCE_NOT_FOUND, exception.getErrorCode());
        assertEquals("Session not found", exception.getMessage());

        verify(currentUserService).getCurrentUser();
        verify(sessionRepository).findByIdAndOwner(sessionId, owner);

        verifyNoInteractions(sessionMapper);
    }

    @Test
    void getSessionById_whenSessionExists_shouldReturnSession() {
        Long sessionId = 1L;
        Session session = SessionEntityFactory.liveSession();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        Session result = sessionService.getSessionById(sessionId);

        assertEquals(session, result);

        verify(sessionRepository).findById(sessionId);
    }

    @Test
    void getSessionById_whenSessionDoesNotExist_shouldReturnNull() {
        Long sessionId = 404L;

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        Session result = sessionService.getSessionById(sessionId);

        assertEquals(null, result);

        verify(sessionRepository).findById(sessionId);
    }

    @Test
    void getSessionByAccessCode_whenSessionExists_shouldReturnSession() {
        String accessCode = "ABC123";
        Session session = SessionEntityFactory.liveSession();

        when(sessionRepository.findByAccessCode(accessCode)).thenReturn(Optional.of(session));

        Session result = sessionService.getSessionByAccessCode(accessCode);

        assertEquals(session, result);

        verify(sessionRepository).findByAccessCode(accessCode);
    }

    @Test
    void getSessionByAccessCode_whenSessionDoesNotExist_shouldThrowResourceNotFoundException() {
        String accessCode = "BAD123";

        when(sessionRepository.findByAccessCode(accessCode)).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> sessionService.getSessionByAccessCode(accessCode)
        );

        assertEquals(RESOURCE_NOT_FOUND, exception.getErrorCode());
        assertEquals("Invalid access code", exception.getMessage());

        verify(sessionRepository).findByAccessCode(accessCode);
    }

    @Test
    void updateSession_whenSessionIsEditable_shouldUpdateProvidedFieldsAndReturnResponse() {
        Long sessionId = 1L;
        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.scheduledSession();

        UpdateSessionRequest request = new UpdateSessionRequest(
                "Updated title",
                "Updated description",
                false,
                false
        );

        Session updatedSession = session;
        SessionResponse expectedResponse = SessionResponseFactory.fromSession(updatedSession);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByIdAndOwner(sessionId, owner)).thenReturn(Optional.of(session));
        when(sessionRepository.save(session)).thenReturn(updatedSession);
        when(sessionMapper.sessionToSessionResponse(updatedSession)).thenReturn(expectedResponse);

        SessionResponse response = sessionService.updateSession(sessionId, request);

        assertEquals(expectedResponse, response);

        assertEquals("Updated title", session.getTitle());
        assertEquals("Updated description", session.getDescription());
        assertEquals(false, session.isModerationEnabled());
        assertEquals(false, session.isAnonymousAllowed());

        verify(sessionRepository).save(session);
        verify(sessionMapper).sessionToSessionResponse(updatedSession);
    }

    @Test
    void updateSession_whenTitleIsBlank_shouldThrowValidationException() {
        Long sessionId = 1L;
        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.scheduledSession();

        UpdateSessionRequest request = new UpdateSessionRequest(
                "   ",
                "Updated description",
                null,
                null
        );

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByIdAndOwner(sessionId, owner)).thenReturn(Optional.of(session));

        AppException exception = assertThrows(
                AppException.class,
                () -> sessionService.updateSession(sessionId, request)
        );

        assertEquals(VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("Title must not be empty", exception.getMessage());

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void updateSession_whenSessionIsEnded_shouldThrowForbiddenException() {
        Long sessionId = 1L;
        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.endedSession();

        UpdateSessionRequest request = new UpdateSessionRequest(
                "Updated title",
                null,
                null,
                null
        );

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByIdAndOwner(sessionId, owner)).thenReturn(Optional.of(session));

        AppException exception = assertThrows(
                AppException.class,
                () -> sessionService.updateSession(sessionId, request)
        );

        assertEquals(com.echoboard.exception.ErrorCode.FORBIDDEN, exception.getErrorCode());
        assertEquals("Can't edit this session", exception.getMessage());

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void startSession_whenSessionIsScheduled_shouldSetLiveStatusStartedAtAndReturnResponse() {
        Long sessionId = 1L;
        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.scheduledSession();
        Session savedSession = session;

        SessionResponse expectedResponse = SessionResponseFactory.fromSession(savedSession);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByIdAndOwner(sessionId, owner)).thenReturn(Optional.of(session));
        when(sessionRepository.save(session)).thenReturn(savedSession);
        when(sessionMapper.sessionToSessionResponse(savedSession)).thenReturn(expectedResponse);

        SessionResponse response = sessionService.startSession(sessionId);

        assertEquals(expectedResponse, response);
        assertEquals(LIVE, session.getStatus());
        assertNotNull(session.getStartedAt());

        verify(sessionRepository).save(session);
        verify(sessionMapper).sessionToSessionResponse(savedSession);
    }

    @Test
    void startSession_whenSessionIsNotScheduled_shouldThrowInvalidSessionStatusException() {
        Long sessionId = 1L;
        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.liveSession();

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByIdAndOwner(sessionId, owner)).thenReturn(Optional.of(session));

        AppException exception = assertThrows(
                AppException.class,
                () -> sessionService.startSession(sessionId)
        );

        assertEquals(INVALID_SESSION_STATUS, exception.getErrorCode());
        assertEquals("Only scheduled sessions can be started", exception.getMessage());

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void endSession_whenSessionIsLive_shouldSetEndedStatusSyncPollCountersPublishEventAndReturnResponse() {
        Long sessionId = 1L;
        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.liveSession();
        session.setOwner(owner);

        Session savedSession = session;
        SessionResponse expectedResponse = SessionResponseFactory.fromSession(savedSession);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByIdAndOwner(sessionId, owner)).thenReturn(Optional.of(session));
        when(sessionRepository.save(session)).thenReturn(savedSession);
        when(sessionMapper.sessionToSessionResponse(savedSession)).thenReturn(expectedResponse);

        SessionResponse response = sessionService.endSession(sessionId);

        assertEquals(expectedResponse, response);
        assertEquals(ENDED, session.getStatus());
        assertNotNull(session.getEndedAt());

        verify(sessionRepository).save(session);
        verify(pollCounterSyncService).syncPollCountersForSession(savedSession.getId());
        verify(rabbitMQPublisher).publishSessionEndedEvent(any());
        verify(sessionMapper).sessionToSessionResponse(savedSession);
    }

    @Test
    void endSession_whenSessionIsNotLive_shouldThrowInvalidSessionStatusException() {
        Long sessionId = 1L;
        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.scheduledSession();

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByIdAndOwner(sessionId, owner)).thenReturn(Optional.of(session));

        AppException exception = assertThrows(
                AppException.class,
                () -> sessionService.endSession(sessionId)
        );

        assertEquals(INVALID_SESSION_STATUS, exception.getErrorCode());
        assertEquals("Only live sessions can be ended", exception.getMessage());

        verify(sessionRepository, never()).save(any(Session.class));
        verifyNoInteractions(pollCounterSyncService);
        verifyNoInteractions(rabbitMQPublisher);
    }

    @Test
    void archiveSession_whenSessionCanBeArchived_shouldSetArchivedStatusAndReturnResponse() {
        Long sessionId = 1L;
        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.endedSession();
        Session savedSession = session;
        SessionResponse expectedResponse = SessionResponseFactory.fromSession(savedSession);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByIdAndOwner(sessionId, owner)).thenReturn(Optional.of(session));
        when(sessionRepository.save(session)).thenReturn(savedSession);
        when(sessionMapper.sessionToSessionResponse(savedSession)).thenReturn(expectedResponse);

        SessionResponse response = sessionService.archiveSession(sessionId);

        assertEquals(expectedResponse, response);
        assertEquals(ARCHIVED, session.getStatus());

        verify(sessionRepository).save(session);
        verify(sessionMapper).sessionToSessionResponse(savedSession);
    }

    @Test
    void archiveSession_whenSessionIsLive_shouldThrowInvalidSessionStatusException() {
        Long sessionId = 1L;
        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.liveSession();

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByIdAndOwner(sessionId, owner)).thenReturn(Optional.of(session));

        AppException exception = assertThrows(
                AppException.class,
                () -> sessionService.archiveSession(sessionId)
        );

        assertEquals(INVALID_SESSION_STATUS, exception.getErrorCode());
        assertEquals("Live sessions cannot be archived", exception.getMessage());

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void archiveSession_whenSessionIsAlreadyArchived_shouldThrowInvalidSessionStatusException() {
        Long sessionId = 1L;
        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.archivedSession();

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByIdAndOwner(sessionId, owner)).thenReturn(Optional.of(session));

        AppException exception = assertThrows(
                AppException.class,
                () -> sessionService.archiveSession(sessionId)
        );

        assertEquals(INVALID_SESSION_STATUS, exception.getErrorCode());
        assertEquals("Session is already archived", exception.getMessage());

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void deleteSession_whenSessionIsScheduled_shouldDeleteSession() {
        Long sessionId = 1L;
        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.scheduledSession();

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByIdAndOwner(sessionId, owner)).thenReturn(Optional.of(session));

        sessionService.deleteSession(sessionId);

        verify(sessionRepository).delete(session);
    }

    @Test
    void deleteSession_whenSessionIsNotScheduled_shouldThrowInvalidSessionStatusException() {
        Long sessionId = 1L;
        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.liveSession();

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByIdAndOwner(sessionId, owner)).thenReturn(Optional.of(session));

        AppException exception = assertThrows(
                AppException.class,
                () -> sessionService.deleteSession(sessionId)
        );

        assertEquals(INVALID_SESSION_STATUS, exception.getErrorCode());
        assertEquals("Only scheduled sessions can be deleted", exception.getMessage());

        verify(sessionRepository, never()).delete(any(Session.class));
    }

    @Test
    void getOwnedSessionOrThrow_whenSessionBelongsToCurrentUser_shouldReturnSession() {
        Long sessionId = 1L;
        User owner = UserEntityFactory.presenter();
        Session session = SessionEntityFactory.scheduledSession();

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByIdAndOwner(sessionId, owner)).thenReturn(Optional.of(session));

        Session result = sessionService.getOwnedSessionOrThrow(sessionId);

        assertEquals(session, result);

        verify(currentUserService).getCurrentUser();
        verify(sessionRepository).findByIdAndOwner(sessionId, owner);
    }

    @Test
    void getOwnedSessionOrThrow_whenSessionDoesNotBelongToCurrentUser_shouldThrowResourceNotFoundException() {
        Long sessionId = 404L;
        User owner = UserEntityFactory.presenter();

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByIdAndOwner(sessionId, owner)).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> sessionService.getOwnedSessionOrThrow(sessionId)
        );

        assertEquals(RESOURCE_NOT_FOUND, exception.getErrorCode());
        assertEquals("Session not found", exception.getMessage());

        verify(currentUserService).getCurrentUser();
        verify(sessionRepository).findByIdAndOwner(sessionId, owner);
    }

    @Test
    void uploadSessionLogo_whenSessionIsLive_shouldStoreImageUpdateLogoAndReturnResponse() {
        Long sessionId = 1L;
        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.liveSession();
        MultipartFile file = mock(MultipartFile.class);

        Session savedSession = session;
        SessionResponse expectedResponse = SessionResponseFactory.fromSession(savedSession);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByIdAndOwner(sessionId, owner)).thenReturn(Optional.of(session));
        when(fileStorageService.storeImageFile(file, "sessions/1/logo")).thenReturn("uploads/sessions/1/logo/logo.png");
        when(sessionRepository.save(session)).thenReturn(savedSession);
        when(sessionMapper.sessionToSessionResponse(savedSession)).thenReturn(expectedResponse);

        SessionResponse response = sessionService.uploadSessionLogo(sessionId, file);

        assertEquals(expectedResponse, response);
        assertEquals("uploads/sessions/1/logo/logo.png", session.getLogoUrl());

        verify(fileStorageService).storeImageFile(file, "sessions/1/logo");
        verify(sessionRepository).save(session);
        verify(sessionMapper).sessionToSessionResponse(savedSession);
    }

    @Test
    void uploadSessionLogo_whenSessionIsScheduled_shouldStoreImageUpdateLogoAndReturnResponse() {
        Long sessionId = 1L;
        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.scheduledSession();
        MultipartFile file = mock(MultipartFile.class);

        Session savedSession = session;
        SessionResponse expectedResponse = SessionResponseFactory.fromSession(savedSession);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByIdAndOwner(sessionId, owner)).thenReturn(Optional.of(session));
        when(fileStorageService.storeImageFile(file, "sessions/1/logo")).thenReturn("uploads/sessions/1/logo/logo.png");
        when(sessionRepository.save(session)).thenReturn(savedSession);
        when(sessionMapper.sessionToSessionResponse(savedSession)).thenReturn(expectedResponse);

        SessionResponse response = sessionService.uploadSessionLogo(sessionId, file);

        assertEquals(expectedResponse, response);
        assertEquals("uploads/sessions/1/logo/logo.png", session.getLogoUrl());

        verify(fileStorageService).storeImageFile(file, "sessions/1/logo");
        verify(sessionRepository).save(session);
        verify(sessionMapper).sessionToSessionResponse(savedSession);
    }

    @Test
    void uploadSessionLogo_whenSessionIsEnded_shouldThrowInvalidSessionStatusException() {
        Long sessionId = 1L;
        User owner = UserEntityFactory.presenter();

        Session session = SessionEntityFactory.endedSession();
        MultipartFile file = mock(MultipartFile.class);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(sessionRepository.findByIdAndOwner(sessionId, owner)).thenReturn(Optional.of(session));

        AppException exception = assertThrows(
                AppException.class,
                () -> sessionService.uploadSessionLogo(sessionId, file)
        );

        assertEquals(INVALID_SESSION_STATUS, exception.getErrorCode());
        assertEquals("you can upload file only scheduled or live sessions", exception.getMessage());

        verifyNoInteractions(fileStorageService);
        verify(sessionRepository, never()).save(any(Session.class));
    }
}