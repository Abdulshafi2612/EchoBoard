package com.echoboard.service.impl;

import com.echoboard.dto.auth.*;
import com.echoboard.entity.User;
import com.echoboard.enums.UserRole;
import com.echoboard.exception.AppException;
import com.echoboard.exception.ErrorCode;
import com.echoboard.mapper.UserMapper;
import com.echoboard.repository.UserRepository;
import com.echoboard.security.JwtService;
import com.echoboard.service.AuthService;
import com.echoboard.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.echoboard.exception.ErrorCode.INVALID_CREDENTIALS;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public RegisterResponse register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new AppException(
                    ErrorCode.EMAIL_ALREADY_EXISTS,
                    HttpStatus.BAD_REQUEST,
                    "Email already exists"
            );
        }
        User user = userMapper.RegisterRequestToUser(request);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.PRESENTER);
        user.setEnabled(true);
        User savedUser = userRepository.save(user);

        RegisterResponse response = userMapper.userToRegisterResponse(savedUser);
        response.setMessage("Account created successfully");

        return response;
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        String email = loginRequest.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new AppException(INVALID_CREDENTIALS, UNAUTHORIZED, "Invalid email or password"));
        boolean correctPassword = passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash());
        if (!correctPassword) {
            throw new AppException(
                    INVALID_CREDENTIALS, UNAUTHORIZED, "Invalid email or password"
            );
        }
        if (!user.isEnabled()) {
            throw new AppException(ErrorCode.FORBIDDEN, FORBIDDEN, "Account is disabled");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken);
    }

    @Override
    public LogoutResponse logout(LogoutRequest request) {
        String token = request.getRefreshToken();

        refreshTokenService.revokeToken(token);

        return new LogoutResponse("Logged out successfully");
    }
}
