package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.redis.RedisLoginLockService;
import com.example.schedulebook.domain.auth.event.LoginFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginFailureService {
    private final RedisLoginLockService redisLoginLockService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailure(String loginId, String ip, String userAgent) {
        try {
            redisLoginLockService.recordFailure(loginId);

        } catch (Exception e) {
            log.error("레디스 에러 발생 - 로그인 실패 기록 저장 오류 : {}", e.getMessage(), e);
        }

        try {
            applicationEventPublisher.publishEvent(new LoginFailedEvent(loginId, ip, userAgent));

        } catch (Exception e) {
            log.error("로그인 실패 감사 이벤트 발행 에러 : {}", e.getMessage(), e);
        }
    }

    public void validateNotLocked(String loginId) {
        if (redisLoginLockService.isLocked(loginId)) {
            throw new BaseException(ErrorEnum.ACCOUNT_LOCKED);
        }
    }
}
