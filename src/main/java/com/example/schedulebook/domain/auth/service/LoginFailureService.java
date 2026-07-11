package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.redis.RedisLoginLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginFailureService {
    private final RedisLoginLockService redisLoginLockService;

    public void handleFailure(String loginId) {
        redisLoginLockService.recordFailure(loginId);

        throw new BaseException(ErrorEnum.LOGIN_FAILED);
    }

    public void validateNotLocked(String loginId) {
        if (redisLoginLockService.isLocked(loginId)) {
            throw new BaseException(ErrorEnum.ACCOUNT_LOCKED);
        }
    }
}
