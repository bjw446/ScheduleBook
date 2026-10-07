package com.example.schedulebook.domain.notification.service;

import com.example.schedulebook.domain.auth.event.AuditEvent;
import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
import com.example.schedulebook.domain.notification.entity.Notification;
import com.example.schedulebook.domain.notification.enums.NotificationEventType;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.repository.NotificationRepository;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.enums.UserRole;
import com.example.schedulebook.domain.user.repository.UserRepository;
import com.example.schedulebook.domain.user.validator.UserValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityNotificationServiceTest {

    private static final Long OUTBOX_ID = 10L;
    private static final Long USER_ID = 1L;
    private static final Long ADMIN_ID = 2L;
    private static final Long NOTIFICATION_ID = 100L;

    private static final String LOGIN_ID = "test@example.com";

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserValidator userValidator;

    @Mock
    private OutboxService outboxService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SecurityNotificationService securityNotificationService;

    @Test
    void notifyUser_사용자에게_보안_알림을_생성하고_Outbox_이벤트를_발행한다() {
        // given
        AuditEvent event = mock(AuditEvent.class);
        User user = mock(User.class);

        when(event.userId()).thenReturn(USER_ID);
        when(event.loginId()).thenReturn(LOGIN_ID);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        when(user.getId())
                .thenReturn(USER_ID);

        when(notificationRepository.existsNotification(
                USER_ID,
                OUTBOX_ID,
                NotificationType.REFRESH_REPLAY_USER
        )).thenReturn(false);

        mockNotificationSave();

        when(notificationRepository.countUnreadNotifications(USER_ID))
                .thenReturn(1L);

        // when
        securityNotificationService.notifyUser(
                OUTBOX_ID,
                event
        );

        // then
        verify(userValidator)
                .validateActiveUser(USER_ID);

        verify(notificationRepository)
                .existsNotification(
                        USER_ID,
                        OUTBOX_ID,
                        NotificationType.REFRESH_REPLAY_USER
                );

        verify(notificationRepository)
                .save(any(Notification.class));

        verifyOutboxNotificationCreated(
                USER_ID,
                NotificationType.REFRESH_REPLAY_USER,
                NotificationType.REFRESH_REPLAY_USER.getDefaultMessage()
        );
    }

    @Test
    void notifyAdmin_관리자에게_로그인_ID를_포함한_보안_알림을_생성하고_Outbox_이벤트를_발행한다() {
        // given
        AuditEvent event = mock(AuditEvent.class);
        User admin = mock(User.class);

        when(event.loginId()).thenReturn(LOGIN_ID);
        when(admin.getId()).thenReturn(ADMIN_ID);

        when(notificationRepository.existsNotification(
                ADMIN_ID,
                OUTBOX_ID,
                NotificationType.REFRESH_REPLAY_ADMIN
        )).thenReturn(false);

        mockNotificationSave();

        when(notificationRepository.countUnreadNotifications(ADMIN_ID))
                .thenReturn(1L);

        // when
        securityNotificationService.notifyAdmin(
                admin,
                OUTBOX_ID,
                event
        );

        // then
        verify(notificationRepository)
                .existsNotification(
                        ADMIN_ID,
                        OUTBOX_ID,
                        NotificationType.REFRESH_REPLAY_ADMIN
                );

        verify(notificationRepository)
                .save(any(Notification.class));

        verifyOutboxNotificationCreated(
                ADMIN_ID,
                NotificationType.REFRESH_REPLAY_ADMIN,
                LOGIN_ID + NotificationType.REFRESH_REPLAY_ADMIN.getDefaultMessage()
        );

        verifyNoInteractions(userValidator);
    }

    @Test
    void notifyAdmin_로그인_ID가_null이면_UNKNOWN을_포함한_보안_알림을_생성한다() {
        // given
        AuditEvent event = mock(AuditEvent.class);
        User admin = mock(User.class);

        when(event.loginId()).thenReturn(null);
        when(admin.getId()).thenReturn(ADMIN_ID);

        when(notificationRepository.existsNotification(
                ADMIN_ID,
                OUTBOX_ID,
                NotificationType.REFRESH_REPLAY_ADMIN
        )).thenReturn(false);

        mockNotificationSave();

        when(notificationRepository.countUnreadNotifications(ADMIN_ID))
                .thenReturn(1L);

        // when
        securityNotificationService.notifyAdmin(
                admin,
                OUTBOX_ID,
                event
        );

        // then
        verifyOutboxNotificationCreated(
                ADMIN_ID,
                NotificationType.REFRESH_REPLAY_ADMIN,
                "UNKNOWN" + NotificationType.REFRESH_REPLAY_ADMIN.getDefaultMessage()
        );
    }

    @Test
    void getActiveAdmins_활성_관리자_목록을_반환한다() {
        // given
        User admin1 = mock(User.class);
        User admin2 = mock(User.class);

        List<User> admins = List.of(admin1, admin2);

        when(userRepository.findAllActiveAdmins(UserRole.SUPER_ADMIN))
                .thenReturn(admins);

        // when
        List<User> result = securityNotificationService.getActiveAdmins();

        // then
        assertThat(result)
                .containsExactly(admin1, admin2);

        verify(userRepository)
                .findAllActiveAdmins(UserRole.SUPER_ADMIN);
    }

    @Test
    void notifyAdmin_이미_존재하는_알림이면_중복_생성하지_않는다() {
        // given
        AuditEvent event = mock(AuditEvent.class);
        User admin = mock(User.class);

        when(admin.getId()).thenReturn(ADMIN_ID);

        when(notificationRepository.existsNotification(
                ADMIN_ID,
                OUTBOX_ID,
                NotificationType.REFRESH_REPLAY_ADMIN
        )).thenReturn(true);

        // when
        securityNotificationService.notifyAdmin(
                admin,
                OUTBOX_ID,
                event
        );

        // then
        verify(notificationRepository)
                .existsNotification(
                        ADMIN_ID,
                        OUTBOX_ID,
                        NotificationType.REFRESH_REPLAY_ADMIN
                );

        verify(notificationRepository, never())
                .save(any(Notification.class));

        verify(notificationRepository, never())
                .countUnreadNotifications(anyLong());

        verifyNoInteractions(outboxService);
    }

    private void mockNotificationSave() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification notification = invocation.getArgument(0);

                    ReflectionTestUtils.setField(
                            notification,
                            "id",
                            NOTIFICATION_ID
                    );

                    return notification;
                });
    }

    private void verifyOutboxNotificationCreated(
            Long receiverId,
            NotificationType notificationType,
            String expectedMessage
    ) {
        ArgumentCaptor<NotificationEventResponse> eventCaptor =
                ArgumentCaptor.forClass(NotificationEventResponse.class);

        verify(outboxService).save(
                any(String.class),
                eq(OutboxAggregateType.NOTIFICATION),
                eq(String.valueOf(NOTIFICATION_ID)),
                eq(OutboxEventType.NOTIFICATION_EVENT),
                eventCaptor.capture()
        );

        NotificationEventResponse event = eventCaptor.getValue();

        assertThat(event.eventType())
                .isEqualTo(NotificationEventType.CREATED);

        assertThat(event.eventId())
                .isNotBlank();

        assertThat(event.receiverId())
                .isEqualTo(receiverId);

        assertThat(event.notificationId())
                .isEqualTo(NOTIFICATION_ID);

        assertThat(event.type())
                .isEqualTo(notificationType.name());

        assertThat(event.title())
                .isEqualTo(notificationType.getTitle());

        assertThat(event.message())
                .isEqualTo(expectedMessage);

        assertThat(event.unreadCount())
                .isEqualTo(1L);

        assertThat(event.timestamp())
                .isPositive();
    }
}