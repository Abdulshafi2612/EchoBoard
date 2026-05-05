package com.echoboard.security;

import com.echoboard.exception.AppException;
import com.echoboard.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.echoboard.enums.TokenType.ACCESS;
import static com.echoboard.enums.TokenType.PARTICIPANT;
import static com.echoboard.enums.TokenType.REFRESH;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        }

        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String authorizationHeader = accessor.getFirstNativeHeader("Authorization");

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
                    "Invalid WebSocket token"
            );
        }

        String tokenType = jwtService.extractTokenType(token);
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        if (sessionAttributes == null) {
            throw new AppException(
                    ErrorCode.UNAUTHORIZED,
                    UNAUTHORIZED,
                    "WebSocket session attributes are not available"
            );
        }

        storeAuthenticationAttributes(token, tokenType, sessionAttributes);
    }

    private void storeAuthenticationAttributes(
            String token,
            String tokenType,
            Map<String, Object> sessionAttributes
    ) {
        if (REFRESH.name().equals(tokenType)) {
            throw new AppException(
                    ErrorCode.UNAUTHORIZED,
                    UNAUTHORIZED,
                    "Refresh token is not allowed for WebSocket connection"
            );
        }

        if (PARTICIPANT.name().equals(tokenType)) {
            storeParticipantAttributes(token, sessionAttributes);
            return;
        }

        if (ACCESS.name().equals(tokenType)) {
            storeUserAttributes(token, sessionAttributes);
            return;
        }

        throw new AppException(
                ErrorCode.UNAUTHORIZED,
                UNAUTHORIZED,
                "Unsupported WebSocket token type"
        );
    }

    private void storeParticipantAttributes(String token, Map<String, Object> sessionAttributes) {
        Long participantId = jwtService.extractParticipantId(token);
        Long sessionId = jwtService.extractSessionId(token);

        sessionAttributes.put("tokenType", PARTICIPANT.name());
        sessionAttributes.put("participantId", participantId);
        sessionAttributes.put("sessionId", sessionId);
    }

    private void storeUserAttributes(String token, Map<String, Object> sessionAttributes) {
        Long userId = jwtService.extractUserId(token);
        String email = jwtService.extractEmail(token);
        String role = jwtService.extractRole(token);

        sessionAttributes.put("tokenType", ACCESS.name());
        sessionAttributes.put("userId", userId);
        sessionAttributes.put("email", email);
        sessionAttributes.put("role", role);
    }

    private boolean isInvalidAuthorizationHeader(String authorizationHeader) {
        return authorizationHeader == null || !authorizationHeader.startsWith("Bearer ");
    }

    private String extractToken(String authorizationHeader) {
        return authorizationHeader.substring(7);
    }
}