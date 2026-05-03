package com.echoboard.service;

import com.echoboard.dto.auth.AuthResponse;
import com.echoboard.dto.auth.LogoutRequest;
import com.echoboard.dto.auth.LogoutResponse;
import com.echoboard.dto.auth.RefreshTokenRequest;
import com.echoboard.entity.RefreshToken;
import com.echoboard.entity.User;

public interface RefreshTokenService {

    String createRefreshToken(User user);


    AuthResponse refreshAccessToken(RefreshTokenRequest request);

    void revokeToken(String token);
}
