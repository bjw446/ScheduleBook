package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.redis.RedisLoginLockService;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginSuccessService {
    private final UserRepository userRepository;
    private final RedisLoginLockService redisLoginLockService;

    public void loginSuccess(User user) {
        user.login();

        userRepository.saveAndFlush(user);

        redisLoginLockService.clear(user.getLoginId());
    }
}
