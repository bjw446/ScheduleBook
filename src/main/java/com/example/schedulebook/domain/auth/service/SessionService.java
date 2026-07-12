package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.redis.RedisBlacklistService;
import com.example.schedulebook.common.redis.RedisRefreshTokenService;
import com.example.schedulebook.common.redis.RedisSessionService;
import com.example.schedulebook.common.security.JwtProperties;
import com.example.schedulebook.common.security.JwtProvider;
import com.example.schedulebook.domain.auth.dto.token.LoginToken;
import com.example.schedulebook.domain.auth.enums.RefreshRotateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RedisRefreshTokenService redisRefreshTokenService;
    private final RedisSessionService redisSessionService;
    private final RedisBlacklistService redisBlacklistService;

    public LoginToken createSession(Long userId) {
        String sessionId = UUID.randomUUID().toString();

        String accessToken = jwtProvider.generateAccessToken(userId, sessionId);

        String refreshToken = jwtProvider.generateRefreshToken(userId, sessionId);

        redisRefreshTokenService.saveRefreshToken(sessionId, refreshToken, jwtProperties.refreshTokenExpiration());

        redisSessionService.addSession(userId, sessionId, jwtProperties.refreshTokenExpiration());

        return new LoginToken(userId, sessionId, accessToken, refreshToken);
    }

    public void logout(String accessToken) {
        LoginToken token = validateAccessToken(accessToken);

        redisRefreshTokenService.deleteRefreshToken(token.sessionId());

        redisSessionService.removeSession(token.userId(), token.sessionId());

        long expiration = jwtProvider.getRemainingTime(accessToken);

        if (!redisBlacklistService.isBlacklisted(accessToken)) {
            redisBlacklistService.saveBlacklistToken(accessToken, expiration);
        }
    }

    public LoginToken refresh(String refreshToken) {
        LoginToken token = validateRefreshToken(refreshToken);

        String newRefreshToken = rotateRefreshToken(token);

        return createLoginToken(token.userId(), token.sessionId(), newRefreshToken);
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
}
