package com.echoboard.service.impl;

import com.echoboard.exception.AppException;
import com.echoboard.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.echoboard.enums.TokenType.ACCESS;
import static com.echoboard.enums.TokenType.PARTICIPANT;
import static com.echoboard.exception.ErrorCode.FORBIDDEN;
import static com.echoboard.exception.ErrorCode.UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentParticipantServiceImplTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private CurrentParticipantServiceImpl currentParticipantService;

    @Test
    void getCurrentParticipantId_whenAuthorizationHeaderIsValidParticipantToken_shouldReturnParticipantId() {
        String token = "participant-token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.isTokenType(token, PARTICIPANT)).thenReturn(true);
        when(jwtService.extractParticipantId(token)).thenReturn(10L);

        Long participantId = currentParticipantService.getCurrentParticipantId();

        assertEquals(10L, participantId);

        verify(request).getHeader("Authorization");
        verify(jwtService).isTokenValid(token);
        verify(jwtService).isTokenType(token, PARTICIPANT);
        verify(jwtService).extractParticipantId(token);
    }

    @Test
    void getCurrentParticipantId_whenAuthorizationHeaderIsMissing_shouldThrowUnauthorizedException() {
        when(request.getHeader("Authorization")).thenReturn(null);

        AppException exception = assertThrows(
                AppException.class,
                () -> currentParticipantService.getCurrentParticipantId()
        );

        assertEquals(UNAUTHORIZED, exception.getErrorCode());
        assertEquals("Missing or invalid Authorization header", exception.getMessage());

        verify(request).getHeader("Authorization");

        verify(jwtService, never()).isTokenValid("participant-token");
        verify(jwtService, never()).isTokenType("participant-token", PARTICIPANT);
        verify(jwtService, never()).extractParticipantId("participant-token");
    }

    @Test
    void getCurrentParticipantId_whenAuthorizationHeaderDoesNotStartWithBearer_shouldThrowUnauthorizedException() {
        when(request.getHeader("Authorization")).thenReturn("participant-token");

        AppException exception = assertThrows(
                AppException.class,
                () -> currentParticipantService.getCurrentParticipantId()
        );

        assertEquals(UNAUTHORIZED, exception.getErrorCode());
        assertEquals("Missing or invalid Authorization header", exception.getMessage());

        verify(request).getHeader("Authorization");

        verify(jwtService, never()).isTokenValid("participant-token");
        verify(jwtService, never()).isTokenType("participant-token", PARTICIPANT);
        verify(jwtService, never()).extractParticipantId("participant-token");
    }

    @Test
    void getCurrentParticipantId_whenTokenIsInvalid_shouldThrowUnauthorizedException() {
        String token = "invalid-token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.isTokenValid(token)).thenReturn(false);

        AppException exception = assertThrows(
                AppException.class,
                () -> currentParticipantService.getCurrentParticipantId()
        );

        assertEquals(UNAUTHORIZED, exception.getErrorCode());
        assertEquals("Invalid participant token", exception.getMessage());

        verify(request).getHeader("Authorization");
        verify(jwtService).isTokenValid(token);

        verify(jwtService, never()).isTokenType(token, PARTICIPANT);
        verify(jwtService, never()).extractParticipantId(token);
    }

    @Test
    void getCurrentParticipantId_whenTokenIsNotParticipantToken_shouldThrowForbiddenException() {
        String token = "access-token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.isTokenType(token, PARTICIPANT)).thenReturn(false);

        AppException exception = assertThrows(
                AppException.class,
                () -> currentParticipantService.getCurrentParticipantId()
        );

        assertEquals(FORBIDDEN, exception.getErrorCode());
        assertEquals("Only participants can perform this action", exception.getMessage());

        verify(request).getHeader("Authorization");
        verify(jwtService).isTokenValid(token);
        verify(jwtService).isTokenType(token, PARTICIPANT);

        verify(jwtService, never()).extractParticipantId(token);
    }

    @Test
    void getCurrentParticipantId_whenAuthorizationHeaderHasBearerWithEmptyToken_shouldCallJwtValidationAndThrowUnauthorized() {
        String token = "";

        when(request.getHeader("Authorization")).thenReturn("Bearer ");
        when(jwtService.isTokenValid(token)).thenReturn(false);

        AppException exception = assertThrows(
                AppException.class,
                () -> currentParticipantService.getCurrentParticipantId()
        );

        assertEquals(UNAUTHORIZED, exception.getErrorCode());
        assertEquals("Invalid participant token", exception.getMessage());

        verify(request).getHeader("Authorization");
        verify(jwtService).isTokenValid(token);

        verify(jwtService, never()).isTokenType(token, PARTICIPANT);
        verify(jwtService, never()).extractParticipantId(token);
    }
}