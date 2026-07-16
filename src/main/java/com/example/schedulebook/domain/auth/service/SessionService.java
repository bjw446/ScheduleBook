package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.redis.RedisBlacklistService;
import com.example.schedulebook.common.redis.RedisRefreshTokenService;
import com.example.schedulebook.common.redis.RedisSessionService;
import com.example.schedulebook.common.security.JwtProperties;
import com.example.schedulebook.common.security.JwtProvider;
import com.example.schedulebook.domain.auth.dto.response.SessionInfo;
import com.example.schedulebook.domain.auth.dto.response.SessionInfoResponse;
import com.example.schedulebook.domain.auth.dto.token.LoginToken;
import com.example.schedulebook.domain.auth.enums.RefreshRotateResult;
import com.example.schedulebook.domain.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RedisRefreshTokenService redisRefreshTokenService;
    private final RedisSessionService redisSessionService;
    private final RedisBlacklistService redisBlacklistService;

    public LoginToken createSession(Long userId, String ip, String userAgent, UserRole userRole) {
        String sessionId = UUID.randomUUID().toString();

        String accessToken = jwtProvider.generateAccessToken(userId, sessionId, userRole);

        String refreshToken = jwtProvider.generateRefreshToken(userId, sessionId, userRole);

        redisRefreshTokenService.saveRefreshToken(sessionId, refreshToken, jwtProperties.refreshTokenExpiration());

        redisSessionService.addSession(userId, sessionId, jwtProperties.refreshTokenExpiration());

        String safeIp = ip != null ? ip : "unknown";

        String safeUserAgent = userAgent != null ? userAgent : "unknown";

        SessionInfo sessionInfo = SessionInfo.create(
                userId,
                sessionId,
                safeIp,
                safeUserAgent
        );

        redisSessionService.saveSessionInfo(sessionInfo, jwtProperties.refreshTokenExpiration());

        return new LoginToken(userId, sessionId, accessToken, refreshToken);
    }

    public LoginToken logout(String accessToken) {
        LoginToken token = validateAccessToken(accessToken);

        removeSession(token.userId(), token.sessionId());

        long expiration = jwtProvider.getRemainingTime(accessToken);

        if (!redisBlacklistService.isBlacklisted(accessToken)) {
            redisBlacklistService.saveBlacklistToken(accessToken, expiration);
        }

        return new LoginToken(token.userId(), token.sessionId(), accessToken, null);
    }

    public LoginToken refresh(String refreshToken) {
        LoginToken token = validateRefreshToken(refreshToken);

        UserRole userRole = jwtProvider.extractUserRole(token.refreshToken());

        String newRefreshToken = rotateRefreshToken(token, userRole);

        LoginToken newToken = createLoginToken(token.userId(), token.sessionId(), newRefreshToken, userRole);

        try {
            redisSessionService.extendSessionTTL(token.userId(), token.sessionId(), jwtProperties.refreshTokenExpiration());

        } catch (Exception e) {
            log.warn("세션 TTL 연장 실패 : sessionId = {}", token.sessionId(), e);
        }

        try {
            redisSessionService.updateLastAccess(token.sessionId());

        } catch (Exception e) {
            log.warn("세션 최근 접근 시간 갱신 실패 : sessionId = {}", token.sessionId(), e);
        }

        return newToken;
    }

    public List<SessionInfoResponse> findSessions(Long userId) {
        Set<String> sessionIds = redisSessionService.getSessions(userId);

        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }

        return sessionIds.stream()
                .map(redisSessionService::getSessionInfo)
                .flatMap(Optional::stream)
                .map(SessionInfoResponse::from)
                .sorted(Comparator.comparing(SessionInfoResponse::loginAt).reversed())
                .toList();
    }

    public void logoutSession(Long userId, String sessionId) {
        validateSessionOwner(userId, sessionId);

        removeSession(userId, sessionId);
    }

    public void logoutForReplace(Long userId, String sessionId) {
        validateSessionOwner(userId, sessionId);

        removeSession(userId, sessionId);
    }

    private LoginToken validateRefreshToken(String refreshToken) {
        jwtProvider.validateToken(refreshToken);

        Long userId = jwtProvider.extractUserId(refreshToken);

        String sessionId = jwtProvider.extractSessionId(refreshToken);

        if (!redisSessionService.existsSession(sessionId)) {
            throw new BaseException(ErrorEnum.SESSION_NOT_FOUND);
        }

        if (!redisRefreshTokenService.hasRefreshToken(sessionId)) {
            throw new BaseException(ErrorEnum.REFRESH_TOKEN_INVALID);
        }

        return new LoginToken(userId, sessionId, null, refreshToken);
    }

    private LoginToken validateAccessToken(String accessToken) {
        jwtProvider.validateToken(accessToken);

        Long userId = jwtProvider.extractUserId(accessToken);

        String sessionId = jwtProvider.extractSessionId(accessToken);

        return new LoginToken(userId, sessionId, accessToken, null);
    }

    private String rotateRefreshToken(LoginToken token, UserRole userRole) {
        String newRefreshToken = jwtProvider.generateRefreshToken(token.userId(), token.sessionId(), userRole);

        RefreshRotateResult result = redisRefreshTokenService.rotateRefreshToken(
                token.sessionId(),
                token.refreshToken(),
                newRefreshToken,
                jwtProperties.refreshTokenExpiration()
        );

        switch (result) {
            case SUCCESS -> {}

            case NOT_FOUND -> throw new BaseException(ErrorEnum.REFRESH_TOKEN_INVALID);

            case TOKEN_MISMATCH -> {
                redisSessionService.deleteAllSessions(token.userId());

                throw new BaseException(ErrorEnum.REFRESH_TOKEN_REPLAY);
            }
        }

        return newRefreshToken;
    }

    private LoginToken createLoginToken(Long userId, String sessionId, String refreshToken, UserRole userRole) {
        String accessToken = jwtProvider.generateAccessToken(userId, sessionId, userRole);

        return new LoginToken(userId, sessionId, accessToken, refreshToken);
    }

    private void validateSessionOwner(Long userId, String sessionId) {
        SessionInfo sessionInfo = redisSessionService.getSessionInfo(sessionId).orElseThrow(
                () -> new BaseException(ErrorEnum.SESSION_NOT_FOUND)
        );

        if (!sessionInfo.userId().equals(userId)) {
            throw new BaseException(ErrorEnum.FORBIDDEN);
        }
    }

    private void removeSession(Long userId, String sessionId) {
        redisRefreshTokenService.deleteRefreshToken(sessionId);

        redisSessionService.removeSession(userId, sessionId);

        redisSessionService.deleteSessionInfo(sessionId);
    }
}
