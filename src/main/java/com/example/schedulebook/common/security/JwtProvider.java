package com.example.schedulebook.common.security;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.user.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {
    private final JwtProperties jwtProperties;
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String sessionId, UserRole userRole) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.accessTokenExpiration());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("sessionId", sessionId)
                .claim("userRole", userRole.name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    public Long extractUserId(String token) {
        try {
            Claims claims = parseClaims(token);

            return Long.parseLong(claims.getSubject());

        } catch (NumberFormatException e) {
            throw new BaseException(ErrorEnum.TOKEN_INVALID);

        } catch (JwtException | IllegalArgumentException e) {
            throw new BaseException(ErrorEnum.TOKEN_INVALID);
        }
    }

    public UserRole extractUserRole(String token) {
        try {
            Claims claims = parseClaims(token);

            String userRole = claims.get("userRole", String.class);

            return UserRole.valueOf(userRole);

        } catch (JwtException | IllegalArgumentException e) {
            throw new BaseException(ErrorEnum.TOKEN_INVALID);
        }
    }

    public void validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);

        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 유효하지 않은 토큰 : {}", e.getMessage());
            throw new BaseException(ErrorEnum.TOKEN_INVALID);
        }
    }

    public long getRemainingTime(String token) {
        try {
            Claims claims = parseClaims(token);

            return claims.getExpiration().getTime() - System.currentTimeMillis();

        } catch (JwtException e) {
            throw new BaseException(ErrorEnum.TOKEN_INVALID);
        }
    }

    public String generateRefreshToken(Long userId, String sessionId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.refreshTokenExpiration());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("sessionId", sessionId)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    public String extractSessionId(String token) {
        try {
            Claims claims = parseClaims(token);

            String sessionId = claims.get("sessionId", String.class);

            if (sessionId == null || sessionId.isBlank()) {
                throw new BaseException(ErrorEnum.TOKEN_INVALID);
            }

            return sessionId;

        } catch (JwtException | IllegalArgumentException e) {
            throw new BaseException(ErrorEnum.TOKEN_INVALID);
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
