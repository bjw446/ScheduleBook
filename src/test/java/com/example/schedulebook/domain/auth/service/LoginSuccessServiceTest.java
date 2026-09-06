package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.redis.service.RedisLoginLockService;
import com.example.schedulebook.domain.auth.enums.AuditEventType;
import com.example.schedulebook.domain.auth.event.AuditEvent;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginSuccessServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisLoginLockService redisLoginLockService;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private LoginSuccessService loginSuccessService;

    private User user;
    private String ip;
    private String userAgent;

    @BeforeEach
    void setUp() {
        user = User.create(
                "testLoginId",
                "encodedPassword",
                "testNickname",
                "test@example.com",
                "010-1234-5678"
        );

        ip = "127.0.0.1";
        userAgent = "Mozilla/5.0";
    }

    @Test
    void given정상사용자_whenLoginSuccess_then사용자정보저장및로그인잠금해제() {
        // given

        // when
        loginSuccessService.loginSuccess(user, ip, userAgent);

        // then
        verify(userRepository)
                .saveAndFlush(user);

        verify(outboxService)
                .save(
                        anyString(),
                        eq(OutboxAggregateType.USER),
                        anyString(),
                        eq(OutboxEventType.AUDIT_EVENT),
                        any(AuditEvent.class)
                );

        verify(redisLoginLockService)
                .clear(user.getLoginId());
    }

    @Test
    void given정상사용자_whenLoginSuccess_then로그인성공감사이벤트생성() {
        // given
        ArgumentCaptor<String> eventIdCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<AuditEvent> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEvent.class);

        // when
        loginSuccessService.loginSuccess(user, ip, userAgent);

        // then
        verify(outboxService)
                .save(
                        eventIdCaptor.capture(),
                        eq(OutboxAggregateType.USER),
                        eq(String.valueOf(user.getId())),
                        eq(OutboxEventType.AUDIT_EVENT),
                        auditEventCaptor.capture()
                );

        String eventId = eventIdCaptor.getValue();
        AuditEvent auditEvent = auditEventCaptor.getValue();

        assertThat(eventId)
                .isNotBlank();

        assertThat(auditEvent.eventId())
                .isEqualTo(eventId);

        assertThat(auditEvent.userId())
                .isEqualTo(user.getId());

        assertThat(auditEvent.loginId())
                .isEqualTo(user.getLoginId());

        assertThat(auditEvent.eventType())
                .isEqualTo(AuditEventType.LOGIN_SUCCESS);

        assertThat(auditEvent.ip())
                .isEqualTo(ip);

        assertThat(auditEvent.userAgent())
                .isEqualTo(userAgent);
    }

    @Test
    void givenOutbox저장실패_whenLoginSuccess_then예외전파없이로그인잠금해제() {
        // given
        doThrow(new RuntimeException("Outbox unavailable"))
                .when(outboxService)
                .save(
                        anyString(),
                        eq(OutboxAggregateType.USER),
                        anyString(),
                        eq(OutboxEventType.AUDIT_EVENT),
                        any(AuditEvent.class)
                );

        // when & then
        assertThatCode(() ->
                loginSuccessService.loginSuccess(
                        user,
                        ip,
                        userAgent
                )
        ).doesNotThrowAnyException();

        verify(userRepository)
                .saveAndFlush(user);

        verify(outboxService)
                .save(
                        anyString(),
                        eq(OutboxAggregateType.USER),
                        anyString(),
                        eq(OutboxEventType.AUDIT_EVENT),
                        any(AuditEvent.class)
                );

        verify(redisLoginLockService)
                .clear(user.getLoginId());
    }

    @Test
    void givenRedis잠금해제실패_whenLoginSuccess_then예외전파() {
        // given
        doThrow(new RuntimeException("Redis unavailable"))
                .when(redisLoginLockService)
                .clear(user.getLoginId());

        // when & then
        assertThatThrownBy(() ->
                loginSuccessService.loginSuccess(
                        user,
                        ip,
                        userAgent
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Redis unavailable");

        verify(userRepository)
                .saveAndFlush(user);

        verify(outboxService)
                .save(
                        anyString(),
                        eq(OutboxAggregateType.USER),
                        anyString(),
                        eq(OutboxEventType.AUDIT_EVENT),
                        any(AuditEvent.class)
                );

        verify(redisLoginLockService)
                .clear(user.getLoginId());
    }
}