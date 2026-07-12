package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.redis.RedisLoginLockService;
import com.example.schedulebook.domain.auth.enums.LoginResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginFailureService {
    private final RedisLoginLockService redisLoginLockService;
    private final LoginAuditService loginAuditService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailure(String loginId, String ip, String userAgent) {
        redisLoginLockService.recordFailure(loginId);

        try {
            loginAuditService.save(loginId, LoginResult.FAIL, ip, userAgent);

        } catch (Exception e) {
            log.error("로그인 감시 저장 실패 : {}", e.getMessage(), e);
        }
    }

    public void validateNotLocked(String loginId) {
        if (redisLoginLockService.isLocked(loginId)) {
            throw new BaseException(ErrorEnum.ACCOUNT_LOCKED);
        }
    }
}
