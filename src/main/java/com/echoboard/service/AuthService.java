package com.echoboard.service;

import com.echoboard.dto.auth.*;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest loginRequest);

    LogoutResponse logout(LogoutRequest request);

}
