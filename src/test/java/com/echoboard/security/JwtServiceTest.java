package com.echoboard.security;

import com.echoboard.entity.Participant;
import com.echoboard.entity.Session;
import com.echoboard.entity.User;
import com.echoboard.util.ParticipantEntityFactory;
import com.echoboard.util.SessionEntityFactory;
import com.echoboard.util.UserEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static com.echoboard.enums.TokenType.ACCESS;
import static com.echoboard.enums.TokenType.PARTICIPANT;
import static com.echoboard.enums.TokenType.REFRESH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(
                jwtService,
                "jwtSecret",
                "this-is-a-very-secure-test-secret-key-for-jwt-service-123456789"
        );

        ReflectionTestUtils.setField(
                jwtService,
                "accessTokenExpirationMs",
                60_000L
        );

        ReflectionTestUtils.setField(
                jwtService,
                "refreshTokenExpirationMs",
                120_000L
        );

        ReflectionTestUtils.setField(
                jwtService,
                "participantTokenExpirationMs",
                90_000L
        );
    }

    @Test
    void generateAccessToken_shouldCreateValidAccessTokenWithUserClaims() {
        User user = UserEntityFactory.presenter();
        user.setId(10L);
        user.setEmail("presenter@example.com");

        String token = jwtService.generateAccessToken(user);

        assertTrue(jwtService.isTokenValid(token));
        assertEquals("presenter@example.com", jwtService.extractEmail(token));
        assertEquals(10L, jwtService.extractUserId(token));
        assertEquals(user.getRole().name(), jwtService.extractRole(token));
        assertEquals(ACCESS.name(), jwtService.extractTokenType(token));
        assertTrue(jwtService.isTokenType(token, ACCESS));
        assertFalse(jwtService.isTokenType(token, REFRESH));
        assertFalse(jwtService.isTokenType(token, PARTICIPANT));
    }

    @Test
    void generateRefreshToken_shouldCreateValidRefreshTokenWithUserClaims() {
        User user = UserEntityFactory.presenter();
        user.setId(10L);
        user.setEmail("presenter@example.com");

        String token = jwtService.generateRefreshToken(user);

        assertTrue(jwtService.isTokenValid(token));
        assertEquals("presenter@example.com", jwtService.extractEmail(token));
        assertEquals(10L, jwtService.extractUserId(token));
        assertEquals(user.getRole().name(), jwtService.extractRole(token));
        assertEquals(REFRESH.name(), jwtService.extractTokenType(token));
        assertTrue(jwtService.isTokenType(token, REFRESH));
        assertFalse(jwtService.isTokenType(token, ACCESS));
        assertFalse(jwtService.isTokenType(token, PARTICIPANT));
    }

    @Test
    void generateParticipantToken_shouldCreateValidParticipantTokenWithParticipantAndSessionClaims() {
        Session session = SessionEntityFactory.liveSession();
        session.setId(100L);

        Participant participant = ParticipantEntityFactory.participantForSession(session);
        participant.setId(50L);

        String token = jwtService.generateParticipantToken(participant);

        assertTrue(jwtService.isTokenValid(token));
        assertEquals(50L, jwtService.extractParticipantId(token));
        assertEquals(100L, jwtService.extractSessionId(token));
        assertEquals(PARTICIPANT.name(), jwtService.extractTokenType(token));
        assertTrue(jwtService.isTokenType(token, PARTICIPANT));
        assertFalse(jwtService.isTokenType(token, ACCESS));
        assertFalse(jwtService.isTokenType(token, REFRESH));
    }

    @Test
    void isTokenValid_whenTokenIsMalformed_shouldReturnFalse() {
        String malformedToken = "not-a-valid-jwt-token";

        boolean result = jwtService.isTokenValid(malformedToken);

        assertFalse(result);
    }

    @Test
    void isTokenValid_whenTokenIsNull_shouldReturnFalse() {
        boolean result = jwtService.isTokenValid(null);

        assertFalse(result);
    }

    @Test
    void isTokenType_whenTokenIsMalformed_shouldReturnFalse() {
        String malformedToken = "not-a-valid-jwt-token";

        boolean result = jwtService.isTokenType(malformedToken, ACCESS);

        assertFalse(result);
    }

    @Test
    void isTokenType_whenTokenTypeDoesNotMatch_shouldReturnFalse() {
        User user = UserEntityFactory.presenter();

        String token = jwtService.generateAccessToken(user);

        assertFalse(jwtService.isTokenType(token, REFRESH));
    }

    @Test
    void isTokenType_whenTokenTypeMatches_shouldReturnTrue() {
        User user = UserEntityFactory.presenter();

        String token = jwtService.generateAccessToken(user);

        assertTrue(jwtService.isTokenType(token, ACCESS));
    }

    @Test
    void isTokenValid_whenTokenIsExpired_shouldReturnFalse() {
        JwtService shortLivedJwtService = new JwtService();

        ReflectionTestUtils.setField(
                shortLivedJwtService,
                "jwtSecret",
                "this-is-a-very-secure-test-secret-key-for-jwt-service-123456789"
        );

        ReflectionTestUtils.setField(
                shortLivedJwtService,
                "accessTokenExpirationMs",
                -1L
        );

        ReflectionTestUtils.setField(
                shortLivedJwtService,
                "refreshTokenExpirationMs",
                -1L
        );

        ReflectionTestUtils.setField(
                shortLivedJwtService,
                "participantTokenExpirationMs",
                -1L
        );

        User user = UserEntityFactory.presenter();

        String expiredToken = shortLivedJwtService.generateAccessToken(user);

        assertFalse(shortLivedJwtService.isTokenValid(expiredToken));
    }

    @Test
    void tokenGeneratedWithDifferentSecret_shouldBeInvalidForThisJwtService() {
        JwtService anotherJwtService = new JwtService();

        ReflectionTestUtils.setField(
                anotherJwtService,
                "jwtSecret",
                "another-very-secure-test-secret-key-for-jwt-service-987654321"
        );

        ReflectionTestUtils.setField(
                anotherJwtService,
                "accessTokenExpirationMs",
                60_000L
        );

        ReflectionTestUtils.setField(
                anotherJwtService,
                "refreshTokenExpirationMs",
                120_000L
        );

        ReflectionTestUtils.setField(
                anotherJwtService,
                "participantTokenExpirationMs",
                90_000L
        );

        User user = UserEntityFactory.presenter();

        String tokenGeneratedByAnotherService = anotherJwtService.generateAccessToken(user);

        assertFalse(jwtService.isTokenValid(tokenGeneratedByAnotherService));
    }
}