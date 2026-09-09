package com.example.schedulebook.domain.auth.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.auth.event.AuditEvent;
import com.example.schedulebook.domain.auth.enums.AuditEventType;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.service.SecurityNotificationService;
import com.example.schedulebook.domain.notificationretry.service.NotificationRetryService;
import com.example.schedulebook.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshReplayDetectedProcessorTest {

    @Mock
    private SecurityNotificationService securityNotificationService;

    @Mock
    private NotificationRetryService notificationRetryService;

    @InjectMocks
    private RefreshReplayDetectedProcessor refreshReplayDetectedProcessor;

    @Test
    void given사용자와운영자가존재_whenProcess_then사용자및모든운영자에게알림전송() {
        // given
        Long outboxId = 1L;

        AuditEvent event = createAuditEvent();

        User admin1 = User.create(
                "admin1",
                "password",
                "관리자1",
                "admin1@test.com",
                "01011111111"
        );
        User admin2 = User.create(
                "admin2",
                "password",
                "관리자2",
                "admin2@test.com",
                "01022222222"
        );

        ReflectionTestUtils.setField(admin1, "id", 10L);
        ReflectionTestUtils.setField(admin2, "id", 20L);

        given(securityNotificationService.getActiveAdmins())
                .willReturn(List.of(admin1, admin2));

        // when
        refreshReplayDetectedProcessor.process(outboxId, event);

        // then
        verify(securityNotificationService)
                .notifyUser(outboxId, event);

        verify(securityNotificationService)
                .notifyAdmin(admin1, outboxId, event);

        verify(securityNotificationService)
                .notifyAdmin(admin2, outboxId, event);

        verifyNoInteractions(notificationRetryService);
    }

    @Test
    void given사용자알림전송실패_whenProcess_then사용자Retry저장후운영자알림계속처리() {
        // given
        Long outboxId = 1L;
        AuditEvent event = createAuditEvent();

        User admin = User.create(
                "admin",
                "password",
                "관리자",
                "admin@test.com",
                "01011111111"
        );

        ReflectionTestUtils.setField(admin, "id", 10L);

        RuntimeException exception = new RuntimeException("사용자 알림 전송 실패");

        doThrow(exception)
                .when(securityNotificationService)
                .notifyUser(outboxId, event);

        given(securityNotificationService.getActiveAdmins())
                .willReturn(List.of(admin));

        // when
        refreshReplayDetectedProcessor.process(outboxId, event);

        // then
        verify(securityNotificationService)
                .notifyUser(outboxId, event);

        verify(notificationRetryService)
                .save(
                        event.eventId(),
                        outboxId,
                        event.userId(),
                        NotificationType.REFRESH_REPLAY_USER,
                        event,
                        exception.getMessage()
                );

        verify(securityNotificationService)
                .notifyAdmin(admin, outboxId, event);
    }

    @Test
    void given사용자알림전송및Retry저장실패_whenProcess_thenRetry저장실패예외발생() {
        // given
        Long outboxId = 1L;
        AuditEvent event = createAuditEvent();

        RuntimeException retryException =
                new RuntimeException("Retry 저장 실패");

        doThrow(new RuntimeException("사용자 알림 전송 실패"))
                .when(securityNotificationService)
                .notifyUser(outboxId, event);

        doThrow(retryException)
                .when(notificationRetryService)
                .save(
                        event.eventId(),
                        outboxId,
                        event.userId(),
                        NotificationType.REFRESH_REPLAY_USER,
                        event,
                        "사용자 알림 전송 실패"
                );

        // when & then
        assertThatThrownBy(() ->
                refreshReplayDetectedProcessor.process(outboxId, event)
        )
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;

                    assertThat(baseException.getErrorEnum())
                            .isEqualTo(ErrorEnum.NOTIFICATION_RETRY_SAVE_FAILED);
                });

        verify(securityNotificationService)
                .notifyUser(outboxId, event);

        verify(notificationRetryService)
                .save(
                        event.eventId(),
                        outboxId,
                        event.userId(),
                        NotificationType.REFRESH_REPLAY_USER,
                        event,
                        "사용자 알림 전송 실패"
                );

        verify(securityNotificationService, never())
                .getActiveAdmins();
    }

    @Test
    void given운영자알림전송실패_whenProcess_then운영자Retry저장() {
        // given
        Long outboxId = 1L;
        AuditEvent event = createAuditEvent();

        User admin = User.create(
                "admin",
                "password",
                "관리자",
                "admin@test.com",
                "01011111111"
        );

        ReflectionTestUtils.setField(admin, "id", 10L);

        RuntimeException exception =
                new RuntimeException("운영자 알림 전송 실패");

        given(securityNotificationService.getActiveAdmins())
                .willReturn(List.of(admin));

        doThrow(exception)
                .when(securityNotificationService)
                .notifyAdmin(admin, outboxId, event);

        // when
        refreshReplayDetectedProcessor.process(outboxId, event);

        // then
        verify(securityNotificationService)
                .notifyUser(outboxId, event);

        verify(securityNotificationService)
                .notifyAdmin(admin, outboxId, event);

        verify(notificationRetryService)
                .save(
                        event.eventId(),
                        outboxId,
                        admin.getId(),
                        NotificationType.REFRESH_REPLAY_ADMIN,
                        event,
                        exception.getMessage()
                );
    }

    @Test
    void given첫번째운영자알림실패_whenProcess_then다음운영자알림계속처리() {
        // given
        Long outboxId = 1L;
        AuditEvent event = createAuditEvent();

        User admin1 = User.create(
                "admin1",
                "password",
                "관리자1",
                "admin1@test.com",
                "01011111111"
        );

        User admin2 = User.create(
                "admin2",
                "password",
                "관리자2",
                "admin2@test.com",
                "01022222222"
        );

        ReflectionTestUtils.setField(admin1, "id", 10L);
        ReflectionTestUtils.setField(admin2, "id", 20L);

        RuntimeException exception =
                new RuntimeException("첫 번째 운영자 알림 실패");

        given(securityNotificationService.getActiveAdmins())
                .willReturn(List.of(admin1, admin2));

        doThrow(exception)
                .when(securityNotificationService)
                .notifyAdmin(admin1, outboxId, event);

        // when
        refreshReplayDetectedProcessor.process(outboxId, event);

        // then
        verify(securityNotificationService)
                .notifyAdmin(admin1, outboxId, event);

        verify(securityNotificationService)
                .notifyAdmin(admin2, outboxId, event);

        verify(notificationRetryService)
                .save(
                        event.eventId(),
                        outboxId,
                        admin1.getId(),
                        NotificationType.REFRESH_REPLAY_ADMIN,
                        event,
                        exception.getMessage()
                );
    }

    @Test
    void given운영자알림및Retry저장실패_whenProcess_thenRetry저장실패예외발생() {
        // given
        Long outboxId = 1L;
        AuditEvent event = createAuditEvent();

        User admin = User.create(
                "admin",
                "password",
                "관리자",
                "admin@test.com",
                "01011111111"
        );

        ReflectionTestUtils.setField(admin, "id", 10L);

        RuntimeException notifyException =
                new RuntimeException("운영자 알림 전송 실패");

        RuntimeException retryException =
                new RuntimeException("Retry 저장 실패");

        given(securityNotificationService.getActiveAdmins())
                .willReturn(List.of(admin));

        doThrow(notifyException)
                .when(securityNotificationService)
                .notifyAdmin(admin, outboxId, event);

        doThrow(retryException)
                .when(notificationRetryService)
                .save(
                        event.eventId(),
                        outboxId,
                        admin.getId(),
                        NotificationType.REFRESH_REPLAY_ADMIN,
                        event,
                        notifyException.getMessage()
                );

        // when & then
        assertThatThrownBy(() ->
                refreshReplayDetectedProcessor.process(outboxId, event)
        )
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;

                    assertThat(baseException.getErrorEnum())
                            .isEqualTo(ErrorEnum.NOTIFICATION_RETRY_SAVE_FAILED);
                });

        verify(securityNotificationService)
                .notifyAdmin(admin, outboxId, event);

        verify(notificationRetryService)
                .save(
                        event.eventId(),
                        outboxId,
                        admin.getId(),
                        NotificationType.REFRESH_REPLAY_ADMIN,
                        event,
                        notifyException.getMessage()
                );
    }

    private AuditEvent createAuditEvent() {
        return new AuditEvent(
                "event-123",
                1L,
                null,
                "user01",
                AuditEventType.REFRESH_REPLAY,
                "127.0.0.1",
                "Mozilla/5.0"
        );
    }
}