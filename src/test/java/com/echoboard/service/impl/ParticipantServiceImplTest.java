package com.echoboard.service.impl;

import com.echoboard.dto.participant.JoinSessionRequest;
import com.echoboard.dto.participant.JoinSessionResponse;
import com.echoboard.entity.Participant;
import com.echoboard.entity.Session;
import com.echoboard.enums.SessionStatus;
import com.echoboard.exception.AppException;
import com.echoboard.mapper.ParticipantMapper;
import com.echoboard.repository.ParticipantRepository;
import com.echoboard.security.JwtService;
import com.echoboard.service.SessionAccessCodeCacheService;
import com.echoboard.util.ParticipantEntityFactory;
import com.echoboard.util.SessionEntityFactory;
import com.echoboard.util.TokenHashUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.echoboard.exception.ErrorCode.INVALID_SESSION_STATUS;
import static com.echoboard.exception.ErrorCode.VALIDATION_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipantServiceImplTest {

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private ParticipantMapper participantMapper;

    @Mock
    private JwtService jwtService;

    @Mock
    private SessionAccessCodeCacheService sessionAccessCodeCacheService;

    @InjectMocks
    private ParticipantServiceImpl participantService;

    @Test
    void joinSession_whenSessionIsLiveAndDisplayNameExists_shouldCreateParticipantAndReturnResponse() {
        JoinSessionRequest request = new JoinSessionRequest(" abc123 ", "  Mohamed  ");

        Session session = SessionEntityFactory.liveSession();

        Participant mappedParticipant = new Participant();

        Participant savedParticipant = ParticipantEntityFactory.participantForSession(session);
        savedParticipant.setId(10L);
        savedParticipant.setDisplayName("Mohamed");

        JoinSessionResponse mappedResponse = new JoinSessionResponse();
        mappedResponse.setId(savedParticipant.getId());
        mappedResponse.setSessionId(session.getId());
        mappedResponse.setDisplayName(savedParticipant.getDisplayName());
        mappedResponse.setJoinedAt(savedParticipant.getJoinedAt());

        String participantToken = "participant-token";
        String expectedTokenHash = TokenHashUtil.sha256(participantToken);

        when(participantMapper.joinParticipantRequestToParticipant(request)).thenReturn(mappedParticipant);
        when(sessionAccessCodeCacheService.getSessionByAccessCode("ABC123")).thenReturn(session);
        when(participantRepository.save(mappedParticipant)).thenReturn(savedParticipant);
        when(jwtService.generateParticipantToken(savedParticipant)).thenReturn(participantToken);
        when(participantRepository.save(savedParticipant)).thenReturn(savedParticipant);
        when(participantMapper.participantToJoinSessionResponse(savedParticipant)).thenReturn(mappedResponse);

        JoinSessionResponse response = participantService.joinSession(request);

        assertNotNull(response);
        assertEquals(savedParticipant.getId(), response.getId());
        assertEquals(session.getId(), response.getSessionId());
        assertEquals("Mohamed", response.getDisplayName());
        assertEquals(participantToken, response.getParticipantToken());

        assertEquals(session, mappedParticipant.getSession());
        assertEquals("Mohamed", mappedParticipant.getDisplayName());
        assertEquals(expectedTokenHash, savedParticipant.getParticipantTokenHash());

        verify(participantMapper).joinParticipantRequestToParticipant(request);
        verify(sessionAccessCodeCacheService).getSessionByAccessCode("ABC123");
        verify(participantRepository).save(mappedParticipant);
        verify(jwtService).generateParticipantToken(savedParticipant);
        verify(participantRepository).save(savedParticipant);
        verify(participantMapper).participantToJoinSessionResponse(savedParticipant);
    }

    @Test
    void joinSession_whenDisplayNameIsBlankAndAnonymousAllowed_shouldUseAnonymousDisplayName() {
        JoinSessionRequest request = new JoinSessionRequest("ABC123", "   ");

        Session session = SessionEntityFactory.liveSession();
        session.setAnonymousAllowed(true);

        Participant mappedParticipant = new Participant();

        Participant savedParticipant = ParticipantEntityFactory.anonymousParticipantForSession(session);
        savedParticipant.setId(10L);

        JoinSessionResponse mappedResponse = new JoinSessionResponse();
        mappedResponse.setId(savedParticipant.getId());
        mappedResponse.setSessionId(session.getId());
        mappedResponse.setDisplayName("Anonymous");

        when(participantMapper.joinParticipantRequestToParticipant(request)).thenReturn(mappedParticipant);
        when(sessionAccessCodeCacheService.getSessionByAccessCode("ABC123")).thenReturn(session);
        when(participantRepository.save(mappedParticipant)).thenReturn(savedParticipant);
        when(jwtService.generateParticipantToken(savedParticipant)).thenReturn("participant-token");
        when(participantRepository.save(savedParticipant)).thenReturn(savedParticipant);
        when(participantMapper.participantToJoinSessionResponse(savedParticipant)).thenReturn(mappedResponse);

        JoinSessionResponse response = participantService.joinSession(request);

        assertNotNull(response);
        assertEquals("Anonymous", response.getDisplayName());
        assertEquals("Anonymous", mappedParticipant.getDisplayName());
        assertEquals("participant-token", response.getParticipantToken());

        verify(participantRepository).save(mappedParticipant);
        verify(participantRepository).save(savedParticipant);
    }

    @Test
    void joinSession_whenDisplayNameIsNullAndAnonymousAllowed_shouldUseAnonymousDisplayName() {
        JoinSessionRequest request = new JoinSessionRequest("ABC123", null);

        Session session = SessionEntityFactory.liveSession();
        session.setAnonymousAllowed(true);

        Participant mappedParticipant = new Participant();

        Participant savedParticipant = ParticipantEntityFactory.anonymousParticipantForSession(session);
        savedParticipant.setId(10L);

        JoinSessionResponse mappedResponse = new JoinSessionResponse();
        mappedResponse.setId(savedParticipant.getId());
        mappedResponse.setSessionId(session.getId());
        mappedResponse.setDisplayName("Anonymous");

        when(participantMapper.joinParticipantRequestToParticipant(request)).thenReturn(mappedParticipant);
        when(sessionAccessCodeCacheService.getSessionByAccessCode("ABC123")).thenReturn(session);
        when(participantRepository.save(mappedParticipant)).thenReturn(savedParticipant);
        when(jwtService.generateParticipantToken(savedParticipant)).thenReturn("participant-token");
        when(participantRepository.save(savedParticipant)).thenReturn(savedParticipant);
        when(participantMapper.participantToJoinSessionResponse(savedParticipant)).thenReturn(mappedResponse);

        JoinSessionResponse response = participantService.joinSession(request);

        assertNotNull(response);
        assertEquals("Anonymous", response.getDisplayName());
        assertEquals("Anonymous", mappedParticipant.getDisplayName());
        assertEquals("participant-token", response.getParticipantToken());

        verify(participantRepository).save(mappedParticipant);
        verify(participantRepository).save(savedParticipant);
    }

    @Test
    void joinSession_whenSessionIsNotLive_shouldThrowInvalidSessionStatusException() {
        JoinSessionRequest request = new JoinSessionRequest("ABC123", "Mohamed");

        Session session = SessionEntityFactory.sessionWithStatus(SessionStatus.SCHEDULED);
        Participant mappedParticipant = new Participant();

        when(participantMapper.joinParticipantRequestToParticipant(request)).thenReturn(mappedParticipant);
        when(sessionAccessCodeCacheService.getSessionByAccessCode("ABC123")).thenReturn(session);

        AppException exception = assertThrows(
                AppException.class,
                () -> participantService.joinSession(request)
        );

        assertEquals(INVALID_SESSION_STATUS, exception.getErrorCode());
        assertEquals("Only live sessions can be joined", exception.getMessage());

        verify(participantMapper).joinParticipantRequestToParticipant(request);
        verify(sessionAccessCodeCacheService).getSessionByAccessCode("ABC123");
        verify(participantRepository, never()).save(any(Participant.class));

        verifyNoInteractions(jwtService);
    }

    @Test
    void joinSession_whenAnonymousNotAllowedAndDisplayNameIsBlank_shouldThrowValidationException() {
        JoinSessionRequest request = new JoinSessionRequest("ABC123", "   ");

        Session session = SessionEntityFactory.liveSession();
        session.setAnonymousAllowed(false);

        Participant mappedParticipant = new Participant();

        when(participantMapper.joinParticipantRequestToParticipant(request)).thenReturn(mappedParticipant);
        when(sessionAccessCodeCacheService.getSessionByAccessCode("ABC123")).thenReturn(session);

        AppException exception = assertThrows(
                AppException.class,
                () -> participantService.joinSession(request)
        );

        assertEquals(VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("Display name is required for this session", exception.getMessage());

        verify(participantMapper).joinParticipantRequestToParticipant(request);
        verify(sessionAccessCodeCacheService).getSessionByAccessCode("ABC123");
        verify(participantRepository, never()).save(any(Participant.class));

        verifyNoInteractions(jwtService);
    }

    @Test
    void joinSession_whenAnonymousNotAllowedAndDisplayNameIsNull_shouldThrowValidationException() {
        JoinSessionRequest request = new JoinSessionRequest("ABC123", null);

        Session session = SessionEntityFactory.liveSession();
        session.setAnonymousAllowed(false);

        Participant mappedParticipant = new Participant();

        when(participantMapper.joinParticipantRequestToParticipant(request)).thenReturn(mappedParticipant);
        when(sessionAccessCodeCacheService.getSessionByAccessCode("ABC123")).thenReturn(session);

        AppException exception = assertThrows(
                AppException.class,
                () -> participantService.joinSession(request)
        );

        assertEquals(VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("Display name is required for this session", exception.getMessage());

        verify(participantMapper).joinParticipantRequestToParticipant(request);
        verify(sessionAccessCodeCacheService).getSessionByAccessCode("ABC123");
        verify(participantRepository, never()).save(any(Participant.class));

        verifyNoInteractions(jwtService);
    }

    @Test
    void joinSession_shouldSaveParticipantWithGeneratedTokenHash() {
        JoinSessionRequest request = new JoinSessionRequest("ABC123", "Mohamed");

        Session session = SessionEntityFactory.liveSession();
        Participant mappedParticipant = new Participant();

        Participant savedParticipant = ParticipantEntityFactory.participantForSession(session);
        savedParticipant.setId(10L);

        JoinSessionResponse mappedResponse = new JoinSessionResponse();
        mappedResponse.setId(savedParticipant.getId());
        mappedResponse.setSessionId(session.getId());
        mappedResponse.setDisplayName(savedParticipant.getDisplayName());

        String participantToken = "participant-token";
        String expectedTokenHash = TokenHashUtil.sha256(participantToken);

        when(participantMapper.joinParticipantRequestToParticipant(request)).thenReturn(mappedParticipant);
        when(sessionAccessCodeCacheService.getSessionByAccessCode("ABC123")).thenReturn(session);
        when(participantRepository.save(mappedParticipant)).thenReturn(savedParticipant);
        when(jwtService.generateParticipantToken(savedParticipant)).thenReturn(participantToken);
        when(participantRepository.save(savedParticipant)).thenReturn(savedParticipant);
        when(participantMapper.participantToJoinSessionResponse(savedParticipant)).thenReturn(mappedResponse);

        participantService.joinSession(request);

        ArgumentCaptor<Participant> participantCaptor = ArgumentCaptor.forClass(Participant.class);

        verify(participantRepository, times(2)).save(participantCaptor.capture());

        Participant firstSavedParticipant = participantCaptor.getAllValues().get(0);
        Participant secondSavedParticipant = participantCaptor.getAllValues().get(1);

        assertEquals(mappedParticipant, firstSavedParticipant);
        assertEquals(savedParticipant, secondSavedParticipant);
        assertEquals(expectedTokenHash, secondSavedParticipant.getParticipantTokenHash());
    }

    @Test
    void getParticipantById_whenParticipantExists_shouldReturnParticipant() {
        Long participantId = 1L;
        Participant participant = ParticipantEntityFactory.participant();

        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));

        Participant result = participantService.getParticipantById(participantId);

        assertEquals(participant, result);

        verify(participantRepository).findById(participantId);

        verifyNoInteractions(participantMapper);
        verifyNoInteractions(jwtService);
        verifyNoInteractions(sessionAccessCodeCacheService);
    }

    @Test
    void getParticipantById_whenParticipantDoesNotExist_shouldReturnNull() {
        Long participantId = 404L;

        when(participantRepository.findById(participantId)).thenReturn(Optional.empty());

        Participant result = participantService.getParticipantById(participantId);

        assertEquals(null, result);

        verify(participantRepository).findById(participantId);

        verifyNoInteractions(participantMapper);
        verifyNoInteractions(jwtService);
        verifyNoInteractions(sessionAccessCodeCacheService);
    }

    @Test
    void getNumberOfTotalParticipantsBySessionId_shouldReturnRepositoryCount() {
        Long sessionId = 1L;

        when(participantRepository.countBySession_Id(sessionId)).thenReturn(15L);

        long count = participantService.getNumberOfTotalParticipantsBySessionId(sessionId);

        assertEquals(15L, count);

        verify(participantRepository).countBySession_Id(sessionId);

        verifyNoInteractions(participantMapper);
        verifyNoInteractions(jwtService);
        verifyNoInteractions(sessionAccessCodeCacheService);
    }
}