package com.echoboard.service.impl;

import com.echoboard.dto.auth.AuthResponse;
import com.echoboard.dto.auth.RefreshTokenRequest;
import com.echoboard.entity.RefreshToken;
import com.echoboard.entity.User;
import com.echoboard.exception.AppException;
import com.echoboard.repository.RefreshTokenRepository;
import com.echoboard.security.JwtService;
import com.echoboard.util.RefreshTokenEntityFactory;
import com.echoboard.util.UserEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static com.echoboard.exception.ErrorCode.INVALID_REFRESH_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                refreshTokenService,
                "refreshTokenExpirationMs",
                604800000L
        );
    }

    @Test
    void createRefreshToken_shouldGenerateTokenSaveItAndReturnToken() {
        User user = UserEntityFactory.presenter();

        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh-token");

        String token = refreshTokenService.createRefreshToken(user);

        assertEquals("new-refresh-token", token);

        ArgumentCaptor<RefreshToken> refreshTokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(jwtService).generateRefreshToken(user);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());

        RefreshToken savedRefreshToken = refreshTokenCaptor.getValue();

        assertEquals(user, savedRefreshToken.getUser());
        assertEquals("new-refresh-token", savedRefreshToken.getToken());
        assertFalse(savedRefreshToken.isRevoked());
        assertNotNull(savedRefreshToken.getExpiresAt());
    }

    @Test
    void refreshAccessToken_whenRefreshTokenIsValid_shouldRevokeOldTokenAndReturnNewTokens() {
        User user = UserEntityFactory.presenter();
        RefreshToken oldRefreshToken = RefreshTokenEntityFactory.validRefreshTokenForUser(user);
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        when(refreshTokenRepository.findByToken("valid-refresh-token"))
                .thenReturn(Optional.of(oldRefreshToken));

        when(jwtService.isTokenValid("valid-refresh-token")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh-token");

        AuthResponse response = refreshTokenService.refreshAccessToken(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());

        assertTrue(oldRefreshToken.isRevoked());

        verify(refreshTokenRepository).findByToken("valid-refresh-token");
        verify(jwtService).isTokenValid("valid-refresh-token");
        verify(jwtService).generateAccessToken(user);
        verify(jwtService).generateRefreshToken(user);

        verify(refreshTokenRepository, atLeastOnce()).save(any(RefreshToken.class));
    }

    @Test
    void refreshAccessToken_whenTokenDoesNotExist_shouldThrowInvalidRefreshTokenException() {
        RefreshTokenRequest request = new RefreshTokenRequest("missing-refresh-token");

        when(refreshTokenRepository.findByToken("missing-refresh-token"))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> refreshTokenService.refreshAccessToken(request)
        );

        assertEquals(INVALID_REFRESH_TOKEN, exception.getErrorCode());
        assertEquals("Invalid refresh token", exception.getMessage());

        verify(refreshTokenRepository).findByToken("missing-refresh-token");

        verifyNoInteractions(jwtService);
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void refreshAccessToken_whenTokenIsRevoked_shouldThrowInvalidRefreshTokenException() {
        RefreshToken revokedToken = RefreshTokenEntityFactory.revokedRefreshToken();
        RefreshTokenRequest request = new RefreshTokenRequest(revokedToken.getToken());

        when(refreshTokenRepository.findByToken(revokedToken.getToken()))
                .thenReturn(Optional.of(revokedToken));

        AppException exception = assertThrows(
                AppException.class,
                () -> refreshTokenService.refreshAccessToken(request)
        );

        assertEquals(INVALID_REFRESH_TOKEN, exception.getErrorCode());
        assertEquals("Refresh token has been revoked", exception.getMessage());

        verify(refreshTokenRepository).findByToken(revokedToken.getToken());

        verifyNoInteractions(jwtService);
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void refreshAccessToken_whenTokenIsExpired_shouldThrowInvalidRefreshTokenException() {
        RefreshToken expiredToken = RefreshTokenEntityFactory.expiredRefreshToken();
        RefreshTokenRequest request = new RefreshTokenRequest(expiredToken.getToken());

        when(refreshTokenRepository.findByToken(expiredToken.getToken()))
                .thenReturn(Optional.of(expiredToken));

        AppException exception = assertThrows(
                AppException.class,
                () -> refreshTokenService.refreshAccessToken(request)
        );

        assertEquals(INVALID_REFRESH_TOKEN, exception.getErrorCode());
        assertEquals("Refresh token has expired", exception.getMessage());

        verify(refreshTokenRepository).findByToken(expiredToken.getToken());

        verifyNoInteractions(jwtService);
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void refreshAccessToken_whenJwtTokenIsInvalid_shouldThrowInvalidRefreshTokenException() {
        RefreshToken refreshToken = RefreshTokenEntityFactory.validRefreshToken();
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken.getToken());

        when(refreshTokenRepository.findByToken(refreshToken.getToken()))
                .thenReturn(Optional.of(refreshToken));

        when(jwtService.isTokenValid(refreshToken.getToken())).thenReturn(false);

        AppException exception = assertThrows(
                AppException.class,
                () -> refreshTokenService.refreshAccessToken(request)
        );

        assertEquals(INVALID_REFRESH_TOKEN, exception.getErrorCode());
        assertEquals("Invalid refresh token", exception.getMessage());

        verify(refreshTokenRepository).findByToken(refreshToken.getToken());
        verify(jwtService).isTokenValid(refreshToken.getToken());

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void revokeToken_whenTokenExistsAndNotRevoked_shouldMarkTokenAsRevokedAndSaveIt() {
        RefreshToken refreshToken = RefreshTokenEntityFactory.validRefreshToken();

        when(refreshTokenRepository.findByToken(refreshToken.getToken()))
                .thenReturn(Optional.of(refreshToken));

        refreshTokenService.revokeToken(refreshToken.getToken());

        assertTrue(refreshToken.isRevoked());

        ArgumentCaptor<RefreshToken> refreshTokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).findByToken(refreshToken.getToken());
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());

        RefreshToken savedRefreshToken = refreshTokenCaptor.getValue();

        assertEquals(refreshToken, savedRefreshToken);

        verifyNoInteractions(jwtService);
    }

    @Test
    void revokeToken_whenTokenAlreadyRevoked_shouldNotSaveAgain() {
        RefreshToken refreshToken = RefreshTokenEntityFactory.revokedRefreshToken();

        when(refreshTokenRepository.findByToken(refreshToken.getToken()))
                .thenReturn(Optional.of(refreshToken));

        refreshTokenService.revokeToken(refreshToken.getToken());

        assertTrue(refreshToken.isRevoked());

        verify(refreshTokenRepository).findByToken(refreshToken.getToken());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));

        verifyNoInteractions(jwtService);
    }

    @Test
    void revokeToken_whenTokenDoesNotExist_shouldThrowInvalidRefreshTokenException() {
        when(refreshTokenRepository.findByToken("missing-refresh-token"))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> refreshTokenService.revokeToken("missing-refresh-token")
        );

        assertEquals(INVALID_REFRESH_TOKEN, exception.getErrorCode());
        assertEquals("Invalid refresh token", exception.getMessage());

        verify(refreshTokenRepository).findByToken("missing-refresh-token");
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));

        verifyNoInteractions(jwtService);
    }
}