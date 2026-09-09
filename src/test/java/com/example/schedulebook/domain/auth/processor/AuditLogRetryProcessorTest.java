package com.example.schedulebook.domain.auth.processor;

import com.example.schedulebook.domain.auth.enums.AuditEventType;
import com.example.schedulebook.domain.auth.event.AuditEvent;
import com.example.schedulebook.domain.auth.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogRetryProcessorTest {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private RefreshReplayDetectedProcessor refreshReplayDetectedProcessor;

    @InjectMocks
    private AuditLogRetryProcessor auditLogRetryProcessor;

    @Test
    void given일반감사이벤트_whenProcess_then감사로그저장() {
        // given
        Long outboxId = 1L;
        AuditEvent event = new AuditEvent(
                "event-123",
                1L,
                null,
                "user01",
                AuditEventType.LOGIN_SUCCESS,
                "127.0.0.1",
                "Mozilla/5.0"
        );

        // when
        auditLogRetryProcessor.process(outboxId, event);

        // then
        verify(auditLogService).save(outboxId, event);
        verifyNoInteractions(refreshReplayDetectedProcessor);
    }

    @Test
    void givenRefreshReplay감사이벤트_whenProcess_then감사로그저장후Replay감지처리() {
        // given
        Long outboxId = 1L;
        AuditEvent event = new AuditEvent(
                "event-123",
                1L,
                null,
                "user01",
                AuditEventType.REFRESH_REPLAY,
                "127.0.0.1",
                "Mozilla/5.0"
        );

        // when
        auditLogRetryProcessor.process(outboxId, event);

        // then
        InOrder inOrder = inOrder(
                auditLogService,
                refreshReplayDetectedProcessor
        );

        inOrder.verify(auditLogService)
                .save(outboxId, event);

        inOrder.verify(refreshReplayDetectedProcessor)
                .process(outboxId, event);
    }

    @Test
    void given감사로그저장실패_whenProcess_then예외재전파() {
        // given
        Long outboxId = 1L;
        AuditEvent event = new AuditEvent(
                "event-123",
                1L,
                null,
                "user01",
                AuditEventType.LOGIN_SUCCESS,
                "127.0.0.1",
                "Mozilla/5.0"
        );

        RuntimeException exception = new RuntimeException("audit log save failed");

        doThrow(exception)
                .when(auditLogService)
                .save(outboxId, event);

        // when & then
        assertThatThrownBy(() ->
                auditLogRetryProcessor.process(outboxId, event)
        )
                .isSameAs(exception);

        verify(auditLogService).save(outboxId, event);
        verifyNoInteractions(refreshReplayDetectedProcessor);
    }

    @Test
    void givenRefreshReplay감지처리실패_whenProcess_then예외재전파() {
        // given
        Long outboxId = 1L;
        AuditEvent event = new AuditEvent(
                "event-123",
                1L,
                null,
                "user01",
                AuditEventType.REFRESH_REPLAY,
                "127.0.0.1",
                "Mozilla/5.0"
        );

        RuntimeException exception =
                new RuntimeException("refresh replay processing failed");

        doThrow(exception)
                .when(refreshReplayDetectedProcessor)
                .process(outboxId, event);

        // when & then
        assertThatThrownBy(() ->
                auditLogRetryProcessor.process(outboxId, event)
        )
                .isSameAs(exception);

        verify(auditLogService).save(outboxId, event);
        verify(refreshReplayDetectedProcessor).process(outboxId, event);
    }
}