package com.example.schedulebook.domain.auth.service;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditEventOutboxServiceTest {

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private AuditEventOutboxService auditEventOutboxService;

    @Test
    void given정상정보_whenSaveReplayEvent_thenRefreshReplay감사이벤트생성() {
        // given
        Long userId = 1L;
        String loginId = "testLoginId";
        String ip = "127.0.0.1";
        String userAgent = "Mozilla/5.0";

        ArgumentCaptor<String> eventIdCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<AuditEvent> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEvent.class);

        // when
        auditEventOutboxService.saveReplayEvent(
                userId,
                loginId,
                ip,
                userAgent
        );

        // then
        verify(outboxService).save(
                eventIdCaptor.capture(),
                eq(OutboxAggregateType.USER),
                eq(String.valueOf(userId)),
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
                .isEqualTo(userId);

        assertThat(auditEvent.loginId())
                .isEqualTo(loginId);

        assertThat(auditEvent.eventType())
                .isEqualTo(AuditEventType.REFRESH_REPLAY);

        assertThat(auditEvent.ip())
                .isEqualTo(ip);

        assertThat(auditEvent.userAgent())
                .isEqualTo(userAgent);

        assertThat(auditEvent.adminId())
                .isNull();
    }

    @Test
    void givenOutbox저장실패_whenSaveReplayEvent_then예외전파() {
        // given
        RuntimeException exception =
                new RuntimeException("Outbox unavailable");

        doThrow(exception)
                .when(outboxService)
                .save(
                        anyString(),
                        eq(OutboxAggregateType.USER),
                        eq(String.valueOf(1L)),
                        eq(OutboxEventType.AUDIT_EVENT),
                        any(AuditEvent.class)
                );

        // when & then
        assertThatThrownBy(() ->
                auditEventOutboxService.saveReplayEvent(
                        1L,
                        "testLoginId",
                        "127.0.0.1",
                        "Mozilla/5.0"
                )
        )
                .isSameAs(exception);

        verify(outboxService).save(
                anyString(),
                eq(OutboxAggregateType.USER),
                eq(String.valueOf(1L)),
                eq(OutboxEventType.AUDIT_EVENT),
                any(AuditEvent.class)
        );
    }
}