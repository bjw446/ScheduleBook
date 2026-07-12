package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.redis.RedisTokenService;
import com.example.schedulebook.common.security.JwtProperties;
import com.example.schedulebook.common.security.JwtProvider;
import com.example.schedulebook.domain.auth.dto.request.LoginRequest;
import com.example.schedulebook.domain.auth.dto.request.RefreshRequest;
import com.example.schedulebook.domain.auth.dto.request.SignupRequest;
import com.example.schedulebook.domain.auth.dto.response.LoginResponse;
import com.example.schedulebook.domain.auth.dto.response.SignupResponse;
import com.example.schedulebook.domain.auth.enums.RefreshRotateResult;
import com.example.schedulebook.domain.auth.event.RefreshReplayDetectedEvent;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.repository.UserRepository;
import com.example.schedulebook.domain.user.validator.UserValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UserValidator userValidator;
    private final RedisTokenService redisTokenService;
    private final JwtProperties jwtProperties;
    private final LoginFailureService loginFailureService;
    private final LoginSuccessService loginSuccessService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public SignupResponse signup(SignupRequest request) {
        userValidator.validateDuplicateUser(
                request.loginId(),
                request.email(),
                request.nickname(),
                request.phoneNumber()
        );

        User user = User.create(
                request.loginId(),
                passwordEncoder.encode(request.password()),
                request.nickname(),
                request.email(),
                request.phoneNumber()
        );

        User savedUser = userRepository.save(user);

        return SignupResponse.from(savedUser);
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        loginFailureService.validateNotLocked(request.loginId());

        User user = userRepository.findByLoginId(request.loginId()).orElseThrow(
                () -> new BaseException(ErrorEnum.LOGIN_FAILED)
        );

        userValidator.validateLoginUserStatus(user);

        String ip = servletRequest.getRemoteAddr();

        String userAgent = servletRequest.getHeader("User-Agent");

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            loginFailureService.handleFailure(request.loginId(), ip, userAgent);

            throw new BaseException(ErrorEnum.LOGIN_FAILED);
        }

        processLogin(user, ip, userAgent);

        String accessToken = jwtProvider.generateAccessToken(user.getId());

        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        redisTokenService.saveRefreshToken(user.getId(), refreshToken, jwtProperties.refreshTokenExpiration());

        return LoginResponse.from(user, accessToken, refreshToken);
    }

    public void logout(String accessToken) {
        jwtProvider.validateToken(accessToken);

        Long userId = jwtProvider.extractUserId(accessToken);

        redisTokenService.deleteRefreshToken(userId);

        long expiration = jwtProvider.getRemainingTime(accessToken);

        if (!redisTokenService.isBlacklisted(accessToken)) {
            redisTokenService.saveBlacklistToken(accessToken, expiration);
        }
    }

    public LoginResponse refresh(RefreshRequest request, HttpServletRequest servletRequest) {
        String refreshToken = request.refreshToken();

        jwtProvider.validateToken(refreshToken);

        Long userId = jwtProvider.extractUserId(refreshToken);

        User user = userValidator.validateActiveUser(userId);

        String ip = servletRequest.getRemoteAddr();

        String userAgent = servletRequest.getHeader("User-Agent");

        String newRefreshToken = jwtProvider.generateRefreshToken(userId);

        RefreshRotateResult result = redisTokenService.rotateRefreshToken(
                userId,
                refreshToken,
                newRefreshToken,
                jwtProperties.refreshTokenExpiration()
        );

        switch (result) {
            case SUCCESS -> {}

            case NOT_FOUND -> throw new BaseException(ErrorEnum.REFRESH_TOKEN_INVALID);

            case TOKEN_MISMATCH -> {
                redisTokenService.deleteRefreshToken(userId);

                applicationEventPublisher.publishEvent(new RefreshReplayDetectedEvent(userId, user.getLoginId(), ip, userAgent));

                throw new BaseException(ErrorEnum.REFRESH_TOKEN_REPLAY);
            }
        }

        String newAccessToken = jwtProvider.generateAccessToken(userId);

        return LoginResponse.from(user, newAccessToken, newRefreshToken);
    }

    private void processLogin(User user, String ip, String userAgent) {
        try {
            loginSuccessService.loginSuccess(user, ip, userAgent);

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BaseException(ErrorEnum.LOGIN_CONFLICT);
        }
    }
}
