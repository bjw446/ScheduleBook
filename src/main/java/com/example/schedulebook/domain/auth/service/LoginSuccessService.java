package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.redis.RedisLoginLockService;
import com.example.schedulebook.domain.auth.enums.LoginResult;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginSuccessService {
    private final UserRepository userRepository;
    private final RedisLoginLockService redisLoginLockService;
    private final LoginAuditService loginAuditService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loginSuccess(User user, String ip, String userAgent) {
        user.login();

        userRepository.saveAndFlush(user);

        loginAuditService.save(user.getLoginId(), LoginResult.SUCCESS, ip, userAgent);

        redisLoginLockService.clear(user.getLoginId());
    }
}
