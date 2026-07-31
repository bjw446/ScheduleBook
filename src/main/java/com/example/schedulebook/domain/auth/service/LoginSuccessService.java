package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.redis.service.RedisLoginLockService;
import com.example.schedulebook.domain.auth.enums.AuditEventType;
import com.example.schedulebook.domain.auth.event.AuditEvent;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginSuccessService {
    private final UserRepository userRepository;
    private final RedisLoginLockService redisLoginLockService;
    private final OutboxService outboxService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loginSuccess(User user, String ip, String userAgent) {
        user.login();

        userRepository.saveAndFlush(user);

        try {
            outboxService.save(
                    OutboxAggregateType.USER,
                    user.getId(),
                    OutboxEventType.AUDIT_EVENT,
                    new AuditEvent(
                            user.getId(),
                            null,
                            user.getLoginId(),
                            AuditEventType.LOGIN_SUCCESS,
                            ip,
                            userAgent
                    )
            );

        } catch (Exception e) {
            log.error("로그인 성공 감사 이벤트 발행 에러 : {}", e.getMessage(), e);
        }

        redisLoginLockService.clear(user.getLoginId());
    }
}
