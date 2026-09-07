package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.domain.auth.entity.AuditLog;
import com.example.schedulebook.domain.auth.enums.AuditEventType;
import com.example.schedulebook.domain.auth.event.AuditEvent;
import com.example.schedulebook.domain.auth.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void given미등록Outbox_whenSave_thenAuditLog저장() {
        // given
        Long outboxId = 1L;
        Long userId = 10L;
        String loginId = "testUser";
        String ip = "127.0.0.1";
        String userAgent = "Mozilla/5.0";

        AuditEvent event = new AuditEvent(
                "event-id",
                userId,
                null,
                loginId,
                AuditEventType.LOGIN_SUCCESS,
                ip,
                userAgent
        );

        given(auditLogRepository.existsByOutboxId(outboxId))
                .willReturn(false);

        // when
        auditLogService.save(outboxId, event);

        // then
        ArgumentCaptor<AuditLog> captor =
                ArgumentCaptor.forClass(AuditLog.class);

        verify(auditLogRepository).existsByOutboxId(outboxId);
        verify(auditLogRepository).save(captor.capture());

        AuditLog auditLog = captor.getValue();

        assertThat(auditLog.getUserId()).isEqualTo(userId);
        assertThat(auditLog.getAdminId()).isNull();
        assertThat(auditLog.getLoginId()).isEqualTo(loginId);
        assertThat(auditLog.getAuditEventType())
                .isEqualTo(AuditEventType.LOGIN_SUCCESS);
        assertThat(auditLog.getDescription())
                .isEqualTo(AuditEventType.LOGIN_SUCCESS.getDescription());
        assertThat(auditLog.getIp()).isEqualTo(ip);
        assertThat(auditLog.getUserAgent()).isEqualTo(userAgent);
        assertThat(auditLog.getOutboxId()).isEqualTo(outboxId);
    }

    @Test
    void given이미등록된Outbox_whenSave_thenAuditLog저장하지않음() {
        // given
        Long outboxId = 1L;

        AuditEvent event = new AuditEvent(
                "event-id",
                10L,
                null,
                "testUser",
                AuditEventType.LOGIN_SUCCESS,
                "127.0.0.1",
                "Mozilla/5.0"
        );

        given(auditLogRepository.existsByOutboxId(outboxId))
                .willReturn(true);

        // when
        auditLogService.save(outboxId, event);

        // then
        verify(auditLogRepository).existsByOutboxId(outboxId);
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void givenAuditLog저장실패_whenSave_then예외전파() {
        // given
        Long outboxId = 1L;

        AuditEvent event = new AuditEvent(
                "event-id",
                10L,
                null,
                "testUser",
                AuditEventType.LOGIN_SUCCESS,
                "127.0.0.1",
                "Mozilla/5.0"
        );

        RuntimeException exception = new RuntimeException("DB error");

        given(auditLogRepository.existsByOutboxId(outboxId))
                .willReturn(false);

        given(auditLogRepository.save(any(AuditLog.class)))
                .willThrow(exception);

        // when & then
        assertThatThrownBy(() ->
                auditLogService.save(outboxId, event)
        )
                .isSameAs(exception);

        verify(auditLogRepository).existsByOutboxId(outboxId);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void given관리자감사이벤트_whenSave_thenAdminId정상매핑() {
        // given
        Long outboxId = 1L;
        Long adminId = 99L;

        AuditEvent event = new AuditEvent(
                "event-id",
                null,
                adminId,
                "adminUser",
                AuditEventType.LOGIN_SUCCESS,
                "127.0.0.1",
                "Mozilla/5.0"
        );

        given(auditLogRepository.existsByOutboxId(outboxId))
                .willReturn(false);

        // when
        auditLogService.save(outboxId, event);

        // then
        ArgumentCaptor<AuditLog> captor =
                ArgumentCaptor.forClass(AuditLog.class);

        verify(auditLogRepository).save(captor.capture());

        AuditLog auditLog = captor.getValue();

        assertThat(auditLog.getAdminId())
                .isEqualTo(adminId);
    }
}