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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RedisRefreshTokenService redisRefreshTokenService;
    private final RedisSessionService redisSessionService;
    private final RedisBlacklistService redisBlacklistService;

    public LoginToken createSession(Long userId, String ip, String userAgent) {
        String sessionId = UUID.randomUUID().toString();

        String accessToken = jwtProvider.generateAccessToken(userId, sessionId);

        String refreshToken = jwtProvider.generateRefreshToken(userId, sessionId);

        redisRefreshTokenService.saveRefreshToken(sessionId, refreshToken, jwtProperties.refreshTokenExpiration());

        redisSessionService.addSession(userId, sessionId, jwtProperties.refreshTokenExpiration());

        SessionInfo sessionInfo = SessionInfo.create(
                userId,
                sessionId,
                ip,
                userAgent
        );

        redisSessionService.saveSessionInfo(sessionInfo, jwtProperties.refreshTokenExpiration());

        return new LoginToken(userId, sessionId, accessToken, refreshToken);
    }

    public void logout(String accessToken) {
        LoginToken token = validateAccessToken(accessToken);

        removeSession(token.userId(), token.sessionId());

        long expiration = jwtProvider.getRemainingTime(accessToken);

        if (!redisBlacklistService.isBlacklisted(accessToken)) {
            redisBlacklistService.saveBlacklistToken(accessToken, expiration);
        }
    }

    public LoginToken refresh(String refreshToken) {
        LoginToken token = validateRefreshToken(refreshToken);

        String newRefreshToken = rotateRefreshToken(token);

        LoginToken newToken = createLoginToken(token.userId(), token.sessionId(), newRefreshToken);

        redisSessionService.updateLastAccess(token.sessionId());

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
                .toList();
    }

    public void logoutSession(Long userId, String sessionId) {
        validateSessionOwner(userId, sessionId);

        removeSession(userId, sessionId);
    }

    private LoginToken validateRefreshToken(String refreshToken) {
        jwtProvider.validateToken(refreshToken);

        Long userId = jwtProvider.extractUserId(refreshToken);

        String sessionId = jwtProvider.extractSessionId(refreshToken);

        return new LoginToken(userId, sessionId, null, refreshToken);
    }

    private LoginToken validateAccessToken(String accessToken) {
        jwtProvider.validateToken(accessToken);

        Long userId = jwtProvider.extractUserId(accessToken);

        String sessionId = jwtProvider.extractSessionId(accessToken);

        return new LoginToken(userId, sessionId, accessToken, null);
    }

    private String rotateRefreshToken(LoginToken token) {
        String newRefreshToken = jwtProvider.generateRefreshToken(token.userId(), token.sessionId());

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

    private LoginToken createLoginToken(Long userId, String sessionId, String refreshToken) {
        String accessToken = jwtProvider.generateAccessToken(userId, sessionId);

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
