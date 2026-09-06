package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.redis.service.RedisLoginLockService;
import com.example.schedulebook.domain.auth.enums.AuditEventType;
import com.example.schedulebook.domain.auth.event.AuditEvent;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginFailureServiceTest {

    @Mock
    private RedisLoginLockService redisLoginLockService;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private LoginFailureService loginFailureService;

    private final String eventId = "event-123";
    private final String loginId = "test-user";
    private final String ip = "127.0.0.1";
    private final String userAgent = "Mozilla/5.0";

    @Test
    void givenValidLoginFailure_whenHandleFailure_thenRecordFailureAndSaveAuditEvent() {
        // when
        loginFailureService.handleFailure(
                eventId,
                loginId,
                ip,
                userAgent
        );

        // then
        verify(redisLoginLockService)
                .recordFailure(loginId);

        ArgumentCaptor<AuditEvent> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEvent.class);

        verify(outboxService)
                .save(
                        eq(eventId),
                        eq(OutboxAggregateType.USER),
                        isNull(),
                        eq(OutboxEventType.AUDIT_EVENT),
                        auditEventCaptor.capture()
                );

        AuditEvent auditEvent = auditEventCaptor.getValue();

        assertThat(auditEvent)
                .isEqualTo(
                        new AuditEvent(
                                eventId,
                                null,
                                null,
                                loginId,
                                AuditEventType.LOGIN_FAILED,
                                ip,
                                userAgent
                        )
                );
    }

    @Test
    void givenRedisFailure_whenHandleFailure_thenContinueToSaveAuditEvent() {
        // given
        doThrow(new RuntimeException("Redis unavailable"))
                .when(redisLoginLockService)
                .recordFailure(loginId);

        // when & then
        assertThatCode(() ->
                loginFailureService.handleFailure(
                        eventId,
                        loginId,
                        ip,
                        userAgent
                )
        ).doesNotThrowAnyException();

        verify(redisLoginLockService)
                .recordFailure(loginId);

        verify(outboxService)
                .save(
                        eq(eventId),
                        eq(OutboxAggregateType.USER),
                        isNull(),
                        eq(OutboxEventType.AUDIT_EVENT),
                        any(AuditEvent.class)
                );
    }

    @Test
    void givenOutboxFailure_whenHandleFailure_thenDoNotPropagateException() {
        // given
        doThrow(new RuntimeException("Outbox unavailable"))
                .when(outboxService)
                .save(
                        eq(eventId),
                        eq(OutboxAggregateType.USER),
                        isNull(),
                        eq(OutboxEventType.AUDIT_EVENT),
                        any(AuditEvent.class)
                );

        // when & then
        assertThatCode(() ->
                loginFailureService.handleFailure(
                        eventId,
                        loginId,
                        ip,
                        userAgent
                )
        ).doesNotThrowAnyException();

        // Redis 처리는 정상적으로 수행되어야 한다.
        verify(redisLoginLockService)
                .recordFailure(loginId);

        // Outbox 저장까지 시도해야 한다.
        verify(outboxService)
                .save(
                        eq(eventId),
                        eq(OutboxAggregateType.USER),
                        isNull(),
                        eq(OutboxEventType.AUDIT_EVENT),
                        any(AuditEvent.class)
                );
    }

    @Test
    void givenRedisFailureAndOutboxFailure_whenHandleFailure_thenDoNotPropagateException() {
        // given
        doThrow(new RuntimeException("Redis unavailable"))
                .when(redisLoginLockService)
                .recordFailure(loginId);

        doThrow(new RuntimeException("Outbox unavailable"))
                .when(outboxService)
                .save(
                        eq(eventId),
                        eq(OutboxAggregateType.USER),
                        isNull(),
                        eq(OutboxEventType.AUDIT_EVENT),
                        any(AuditEvent.class)
                );

        // when & then
        assertThatCode(() ->
                loginFailureService.handleFailure(
                        eventId,
                        loginId,
                        ip,
                        userAgent
                )
        ).doesNotThrowAnyException();

        // 첫 번째 외부 의존성 실패가 두 번째 처리까지 막지 않아야 한다.
        verify(redisLoginLockService)
                .recordFailure(loginId);

        verify(outboxService)
                .save(
                        eq(eventId),
                        eq(OutboxAggregateType.USER),
                        isNull(),
                        eq(OutboxEventType.AUDIT_EVENT),
                        any(AuditEvent.class)
                );
    }

    @Test
    void givenLoginIdIsNotLocked_whenValidateNotLocked_thenPass() {
        // given
        when(redisLoginLockService.isLocked(loginId))
                .thenReturn(false);

        // when & then
        assertThatCode(() ->
                loginFailureService.validateNotLocked(loginId)
        ).doesNotThrowAnyException();

        verify(redisLoginLockService)
                .isLocked(loginId);
    }

    @Test
    void givenLoginIdIsLocked_whenValidateNotLocked_thenThrowAccountLocked() {
        // given
        when(redisLoginLockService.isLocked(loginId))
                .thenReturn(true);

        // when
        BaseException exception = catchThrowableOfType(
                () -> loginFailureService.validateNotLocked(loginId),
                BaseException.class
        );

        // then
        assertThat(exception)
                .isNotNull();

        assertThat(exception.getErrorEnum())
                .isEqualTo(ErrorEnum.ACCOUNT_LOCKED);

        verify(redisLoginLockService)
                .isLocked(loginId);
    }
}