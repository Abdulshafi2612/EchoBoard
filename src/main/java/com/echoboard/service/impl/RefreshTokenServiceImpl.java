package com.echoboard.service.impl;

import com.echoboard.dto.auth.AuthResponse;
import com.echoboard.dto.auth.RefreshTokenRequest;
import com.echoboard.entity.RefreshToken;
import com.echoboard.entity.User;
import com.echoboard.exception.AppException;
import com.echoboard.repository.RefreshTokenRepository;
import com.echoboard.security.JwtService;
import com.echoboard.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

import static com.echoboard.exception.ErrorCode.INVALID_REFRESH_TOKEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Override
    public String createRefreshToken(User user) {
        String token = jwtService.generateRefreshToken(user);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(
                LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpirationMs))
        );
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return token;
    }

    @Override
    public AuthResponse refreshAccessToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new AppException(
                        INVALID_REFRESH_TOKEN,
                        UNAUTHORIZED,
                        "Invalid refresh token"
                ));

        if (refreshToken.isRevoked()) {
            throw new AppException(
                    INVALID_REFRESH_TOKEN,
                    UNAUTHORIZED,
                    "Refresh token has been revoked"
            );
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(
                    INVALID_REFRESH_TOKEN,
                    UNAUTHORIZED,
                    "Refresh token has expired"
            );
        }

        if (!jwtService.isTokenValid(token)) {
            throw new AppException(
                    INVALID_REFRESH_TOKEN,
                    UNAUTHORIZED,
                    "Invalid refresh token"
            );
        }

        User user = refreshToken.getUser();

        revokeRefreshToken(refreshToken);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = createRefreshToken(user);

        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    @Override
    public void revokeToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new AppException(
                        INVALID_REFRESH_TOKEN,
                        UNAUTHORIZED,
                        "Invalid refresh token"
                ));

        revokeRefreshToken(refreshToken);
    }

    private void revokeRefreshToken(RefreshToken refreshToken) {
        if (!refreshToken.isRevoked()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        }
    }

}
