package com.echoboard.util;

import com.echoboard.entity.RefreshToken;
import com.echoboard.entity.User;

import java.time.LocalDateTime;

public final class RefreshTokenEntityFactory {

    private RefreshTokenEntityFactory() {
    }

    public static RefreshToken validRefreshToken() {
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setId(1L);
        refreshToken.setUser(UserEntityFactory.presenter());
        refreshToken.setToken("valid-refresh-token");
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        refreshToken.setRevoked(false);

        return refreshToken;
    }

    public static RefreshToken validRefreshTokenForUser(User user) {
        RefreshToken refreshToken = validRefreshToken();

        refreshToken.setUser(user);

        return refreshToken;
    }

    public static RefreshToken revokedRefreshToken() {
        RefreshToken refreshToken = validRefreshToken();

        refreshToken.setRevoked(true);

        return refreshToken;
    }

    public static RefreshToken expiredRefreshToken() {
        RefreshToken refreshToken = validRefreshToken();

        refreshToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        return refreshToken;
    }
}