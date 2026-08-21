package com.example.schedulebook.common.security;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.user.enums.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtProviderTest {
    private static final String SECRET =
            "test-secret-key-that-is-at-least-32-characters-long";

    private static final long ACCESS_TOKEN_EXPIRATION = 60_000L;
    private static final long REFRESH_TOKEN_EXPIRATION = 300_000L;

    private JwtProvider jwtProvider;

    private final Long userId = 1L;
    private final String sessionId = "session-123";
    private final UserRole userRole = UserRole.USER;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(
                SECRET,
                ACCESS_TOKEN_EXPIRATION,
                REFRESH_TOKEN_EXPIRATION
        );

        jwtProvider = new JwtProvider(jwtProperties);

        jwtProvider.init();
    }

    @Test
    @DisplayName("Access Token 생성")
    void givenValidUser_whenGenerateAccessToken_thenTokenContainsExpectedClaims() {
        // given

        // when
        String token = jwtProvider.generateAccessToken(
                userId,
                sessionId,
                userRole
        );

        // then
        assertNotNull(token);

        var claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(String.valueOf(userId), claims.getSubject());
        assertEquals(sessionId, claims.get("sessionId", String.class));
        assertEquals(userRole.name(), claims.get("userRole", String.class));

        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());

        long remainingTime = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        assertEquals(ACCESS_TOKEN_EXPIRATION, remainingTime);
    }

    @Test
    @DisplayName("Refresh Token 생성")
    void givenValidUser_whenGenerateRefreshToken_thenTokenContainsExpectedClaims() {
        // given

        // when
        String token = jwtProvider.generateRefreshToken(
                userId,
                sessionId,
                userRole
        );

        // then
        assertNotNull(token);

        var claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(String.valueOf(userId), claims.getSubject());
        assertEquals(sessionId, claims.get("sessionId", String.class));
        assertEquals(userRole.name(), claims.get("userRole", String.class));

        long remainingTime = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        assertEquals(REFRESH_TOKEN_EXPIRATION, remainingTime);
    }

    @Test
    @DisplayName("User ID 정상 추출")
    void givenValidToken_whenExtractUserId_thenReturnUserId() {
        // given
        String token = jwtProvider.generateAccessToken(
                userId,
                sessionId,
                userRole
        );

        // when
        Long result = jwtProvider.extractUserId(token);

        // then
        assertEquals(userId, result);
    }

    @Test
    @DisplayName("User ID 잘못된 Subject 거부")
    void givenTokenWithInvalidSubject_whenExtractUserId_thenThrowTokenInvalid() {
        // given
        String token = Jwts.builder()
                .subject("not-a-number")
                .claim("sessionId", sessionId)
                .claim("userRole", userRole.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(getSigningKey())
                .compact();

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> jwtProvider.extractUserId(token)
        );

        assertEquals(ErrorEnum.TOKEN_INVALID, exception.getErrorEnum());
    }

    @Test
    @DisplayName("Subject 누락 시 UserId 추출 실패")
    void givenTokenWithoutSubject_whenExtractUserId_thenThrowTokenInvalid() {
        // given
        String token = Jwts.builder()
                .claim("sessionId", sessionId)
                .claim("userRole", userRole.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(getSigningKey())
                .compact();

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> jwtProvider.extractUserId(token));
        assertEquals(ErrorEnum.TOKEN_INVALID, exception.getErrorEnum());
    }

    @Test
    @DisplayName("User Role 정상 추출")
    void givenValidToken_whenExtractUserRole_thenReturnRole() {
        // given
        String token = jwtProvider.generateAccessToken(
                userId,
                sessionId,
                userRole
        );

        // when
        UserRole result = jwtProvider.extractUserRole(token);

        // then
        assertEquals(userRole, result);
    }

    @Test
    @DisplayName("User Role 누락 거부")
    void givenTokenWithoutUserRole_whenExtractUserRole_thenThrowTokenInvalid() {
        // given
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("sessionId", sessionId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(getSigningKey())
                .compact();

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> jwtProvider.extractUserRole(token)
        );

        assertEquals(ErrorEnum.TOKEN_INVALID, exception.getErrorEnum());
    }

    @Test
    @DisplayName("User Role 잘못된 값 거부")
    void givenTokenWithInvalidUserRole_whenExtractUserRole_thenThrowTokenInvalid() {
        // given
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("sessionId", sessionId)
                .claim("userRole", "INVALID_ROLE")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(getSigningKey())
                .compact();

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> jwtProvider.extractUserRole(token)
        );

        assertEquals(ErrorEnum.TOKEN_INVALID, exception.getErrorEnum());
    }

    @Test
    @DisplayName("Session ID 정상 추출")
    void givenValidToken_whenExtractSessionId_thenReturnSessionId() {
        // given
        String token = jwtProvider.generateAccessToken(
                userId,
                sessionId,
                userRole
        );

        // when
        String result = jwtProvider.extractSessionId(token);

        // then
        assertEquals(sessionId, result);
    }

    @Test
    @DisplayName("Session ID 누락 거부")
    void givenTokenWithoutSessionId_whenExtractSessionId_thenThrowTokenInvalid() {
        // given
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userRole", userRole.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(getSigningKey())
                .compact();

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> jwtProvider.extractSessionId(token)
        );

        assertEquals(ErrorEnum.TOKEN_INVALID, exception.getErrorEnum());
    }

    @Test
    @DisplayName("Session ID blank 거부")
    void givenTokenWithBlankSessionId_whenExtractSessionId_thenThrowTokenInvalid() {
        // given
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("sessionId", "   ")
                .claim("userRole", userRole.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(getSigningKey())
                .compact();

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> jwtProvider.extractSessionId(token)
        );

        assertEquals(ErrorEnum.TOKEN_INVALID, exception.getErrorEnum());
    }

    @Test
    @DisplayName("정상 Token 검증")
    void givenValidToken_whenValidateToken_thenDoNothing() {
        // given
        String token = jwtProvider.generateAccessToken(
                userId,
                sessionId,
                userRole
        );

        // when & then
        assertDoesNotThrow(() -> jwtProvider.validateToken(token));
    }

    @Test
    @DisplayName("만료 Token 거부")
    void givenExpiredToken_whenValidateToken_thenThrowTokenInvalid() {
        // given
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("sessionId", sessionId)
                .claim("userRole", userRole.name())
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(getSigningKey())
                .compact();

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> jwtProvider.validateToken(token)
        );

        assertEquals(ErrorEnum.TOKEN_INVALID, exception.getErrorEnum());
    }

    @Test
    @DisplayName("변조 Token 거부")
    void givenTamperedToken_whenValidateToken_thenThrowTokenInvalid() {
        // given
        String token = jwtProvider.generateAccessToken(
                userId,
                sessionId,
                userRole
        );

        int signatureStart = token.lastIndexOf('.') + 1;
        char original = token.charAt(signatureStart);
        char replacement = original == 'A' ? 'B' : 'A';
        String tamperedToken = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> jwtProvider.validateToken(tamperedToken)
        );

        assertEquals(ErrorEnum.TOKEN_INVALID, exception.getErrorEnum());
    }

    @Test
    @DisplayName("Malformed Token 거부")
    void givenMalformedToken_whenValidateToken_thenThrowTokenInvalid() {
        // given
        String malformedToken = "this-is-not-a-jwt";

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> jwtProvider.validateToken(malformedToken)
        );

        assertEquals(ErrorEnum.TOKEN_INVALID, exception.getErrorEnum());
    }

    @Test
    @DisplayName("다른 Secret으로 서명한 Token 거부")
    void givenTokenSignedWithDifferentSecret_whenValidateToken_thenThrowTokenInvalid() {
        // given
        SecretKey differentSigningKey = Keys.hmacShaKeyFor(
                "another-secret-key-that-is-at-least-32-characters-long"
                        .getBytes(StandardCharsets.UTF_8)
        );
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("sessionId", sessionId)
                .claim("userRole", userRole.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(differentSigningKey)
                .compact();

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> jwtProvider.validateToken(token));
        assertEquals(ErrorEnum.TOKEN_INVALID, exception.getErrorEnum());
    }

    @Test
    @DisplayName("Access Token 남은 시간 정상 반환")
    void givenValidToken_whenGetRemainingTime_thenReturnPositiveRemainingTime() {
        // given
        String token = jwtProvider.generateAccessToken(
                userId,
                sessionId,
                userRole
        );

        // when
        long remainingTime = jwtProvider.getRemainingTime(token);

        // then
        assertTrue(remainingTime > 0);
        assertTrue(remainingTime <= ACCESS_TOKEN_EXPIRATION);
    }

    @Test
    @DisplayName("만료 Token 남은 시간 추출 실패")
    void givenExpiredToken_whenGetRemainingTime_thenThrowTokenInvalid() {
        // given
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("sessionId", sessionId)
                .claim("userRole", userRole.name())
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(getSigningKey())
                .compact();

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> jwtProvider.getRemainingTime(token)
        );

        assertEquals(ErrorEnum.TOKEN_INVALID, exception.getErrorEnum());
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8)
        );
    }
}