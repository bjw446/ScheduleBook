package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.redis.RedisLoginLockService;
import com.example.schedulebook.domain.auth.event.LoginSuccessEvent;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginSuccessService {
    private final UserRepository userRepository;
    private final RedisLoginLockService redisLoginLockService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loginSuccess(User user, String ip, String userAgent) {
        user.login();

        userRepository.saveAndFlush(user);

        try {
            applicationEventPublisher.publishEvent(new LoginSuccessEvent(user.getId(), user.getLoginId(), ip, userAgent));

        } catch (Exception e) {
            log.error("로그인 감시 저장 실패 : {}", e.getMessage(), e);
        }

        redisLoginLockService.clear(user.getLoginId());
    }
}
