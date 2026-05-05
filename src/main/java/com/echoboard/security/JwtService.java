package com.echoboard.security;

import com.echoboard.entity.Participant;
import com.echoboard.entity.User;
import com.echoboard.enums.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static com.echoboard.enums.TokenType.*;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${app.jwt.participant-token-expiration-ms}")
    private long participantTokenExpirationMs;


    public String generateAccessToken(User user) {
        return generateToken(user, accessTokenExpirationMs, ACCESS);
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, refreshTokenExpirationMs, REFRESH);
    }

    public String extractEmail(String token) {
        return extractClaims(token)
                .getSubject();
    }

    public Long extractUserId(String token) {
        return extractClaims(token)
                .get("userId", Long.class);
    }

    public String extractRole(String token) {
        return extractClaims(token)
                .get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }



    public String generateParticipantToken(Participant participant) {
        Date now = new Date();
        Date expirationTime = new Date(now.getTime() + participantTokenExpirationMs);
        return Jwts
                .builder()
                .claim("participantId", participant.getId())
                .claim("sessionId", participant.getSession().getId())
                .claim("type", PARTICIPANT.name())
                .issuedAt(now)
                .expiration(expirationTime)
                .signWith(getSigningKey())
                .compact();

    }

    public Long extractParticipantId(String token) {
        return extractClaims(token)
                .get("participantId", Long.class);
    }

    public Long extractSessionId(String token) {
        return extractClaims(token)
                .get("sessionId", Long.class);
    }

    public String extractTokenType(String token) {
        return extractClaims(token)
                .get("type", String.class);
    }

    public boolean isTokenType(String token, TokenType expectedType) {
        try {
            String actualType = extractTokenType(token);
            return expectedType.name().equals(actualType);
        } catch (Exception e) {
            return false;
        }
    }

    private String generateToken(User user, long expirationMs, TokenType type) {
        Date now = new Date();
        Date expirationTime = new Date(now.getTime() + expirationMs);

        return Jwts
                .builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .claim("type", type.name())
                .issuedAt(now)
                .expiration(expirationTime)
                .signWith(getSigningKey())
                .compact();
    }


    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
