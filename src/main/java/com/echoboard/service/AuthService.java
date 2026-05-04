package com.echoboard.service;

import com.echoboard.dto.auth.*;
import com.echoboard.dto.user.UserProfileResponse;
import com.echoboard.security.CustomUserDetails;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest loginRequest);

    LogoutResponse logout(LogoutRequest request);

    UserProfileResponse getUserProfileResponse();
}
