package com.echoboard.service.impl;

import com.echoboard.exception.AppException;
import com.echoboard.exception.ErrorCode;
import com.echoboard.security.JwtService;
import com.echoboard.service.CurrentParticipantService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.echoboard.enums.TokenType.PARTICIPANT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
public class CurrentParticipantServiceImpl implements CurrentParticipantService {

    private final JwtService jwtService;
    private final HttpServletRequest request;

    @Override
    public Long getCurrentParticipantId() {
        String authorizationHeader = request.getHeader("Authorization");

        if (isInvalidAuthorizationHeader(authorizationHeader)) {
            throw new AppException(
                    ErrorCode.UNAUTHORIZED,
                    UNAUTHORIZED,
                    "Missing or invalid Authorization header"
            );
        }

        String token = extractToken(authorizationHeader);

        if (!jwtService.isTokenValid(token)) {
            throw new AppException(
                    ErrorCode.UNAUTHORIZED,
                    UNAUTHORIZED,
                    "Invalid participant token"
            );
        }

        if (!jwtService.isTokenType(token, PARTICIPANT)) {
            throw new AppException(
                    ErrorCode.FORBIDDEN,
                    FORBIDDEN,
                    "Only participants can perform this action"
            );
        }

        return jwtService.extractParticipantId(token);
    }

    private boolean isInvalidAuthorizationHeader(String authorizationHeader) {
        return authorizationHeader == null || !authorizationHeader.startsWith("Bearer ");
    }

    private String extractToken(String authorizationHeader) {
        return authorizationHeader.substring(7);
    }
}