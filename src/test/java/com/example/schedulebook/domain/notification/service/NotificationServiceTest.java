package com.example.schedulebook.domain.notification.service;

import com.example.schedulebook.domain.notification.dto.response.NotificationDetailResponse;
import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
import com.example.schedulebook.domain.notification.dto.response.NotificationSummaryResponse;
import com.example.schedulebook.domain.notification.dto.response.UnreadNotificationCountResponse;
import com.example.schedulebook.domain.notification.entity.Notification;
import com.example.schedulebook.domain.notification.enums.NotificationEventType;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.repository.NotificationRepository;
import com.example.schedulebook.domain.notification.validator.NotificationValidator;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.validator.UserValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long RECEIVER_ID = 2L;
    private static final Long FRIEND_ID = 10L;
    private static final Long SHARE_ID = 20L;
    private static final Long SCHEDULE_ID = 30L;
    private static final Long NOTIFICATION_ID = 40L;

    private static final String REQUESTER_NICKNAME = "요청자";
    private static final String ACCEPTED_NICKNAME = "수락자";
    private static final String OWNER_NICKNAME = "일정작성자";
    private static final String WRITER_NICKNAME = "댓글작성자";
    private static final String SCHEDULE_TITLE = "스터디 일정";

    private static final LocalDateTime CREATED_AT =
            LocalDateTime.of(2026, 10, 6, 12, 0);

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserValidator userValidator;

    @Mock
    private NotificationValidator notificationValidator;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void createFriendRequestNotification_친구_요청_알림을_생성하고_Outbox_이벤트를_저장한다() {
        // given
        mockNotificationCreation(RECEIVER_ID);

        // when
        notificationService.createFriendRequestNotification(
                RECEIVER_ID,
                REQUESTER_NICKNAME,
                FRIEND_ID
        );

        // then
        verify(userValidator).validateActiveUser(RECEIVER_ID);
        verify(notificationRepository).save(any(Notification.class));

        verifyOutboxNotificationCreated(
                RECEIVER_ID,
                NotificationType.FRIEND_REQUEST,
                REQUESTER_NICKNAME + NotificationType.FRIEND_REQUEST.getDefaultMessage()
        );
    }

    @Test
    void createFriendAcceptedNotification_친구_수락_알림을_생성하고_Outbox_이벤트를_저장한다() {
        // given
        mockNotificationCreation(RECEIVER_ID);

        // when
        notificationService.createFriendAcceptedNotification(
                RECEIVER_ID,
                ACCEPTED_NICKNAME,
                FRIEND_ID
        );

        // then
        verify(userValidator).validateActiveUser(RECEIVER_ID);
        verify(notificationRepository).save(any(Notification.class));

        verifyOutboxNotificationCreated(
                RECEIVER_ID,
                NotificationType.FRIEND_ACCEPTED,
                ACCEPTED_NICKNAME + NotificationType.FRIEND_ACCEPTED.getDefaultMessage()
        );
    }

    @Test
    void createScheduleSharedNotification_일정_공유_알림을_생성하고_Outbox_이벤트를_저장한다() {
        // given
        mockNotificationCreation(RECEIVER_ID);

        // when
        notificationService.createScheduleSharedNotification(
                RECEIVER_ID,
                OWNER_NICKNAME,
                SHARE_ID
        );

        // then
        verify(userValidator).validateActiveUser(RECEIVER_ID);
        verify(notificationRepository).save(any(Notification.class));

        verifyOutboxNotificationCreated(
                RECEIVER_ID,
                NotificationType.SCHEDULE_SHARED,
                OWNER_NICKNAME + NotificationType.SCHEDULE_SHARED.getDefaultMessage()
        );
    }

    @Test
    void createScheduleReminderNotification_일정_리마인드_알림을_생성하고_Outbox_이벤트를_저장한다() {
        // given
        mockNotificationCreation(RECEIVER_ID);

        // when
        notificationService.createScheduleReminderNotification(
                RECEIVER_ID,
                SCHEDULE_ID,
                SCHEDULE_TITLE
        );

        // then
        verify(userValidator).validateActiveUser(RECEIVER_ID);
        verify(notificationRepository).save(any(Notification.class));

        verifyOutboxNotificationCreated(
                RECEIVER_ID,
                NotificationType.SCHEDULE_REMINDER,
                SCHEDULE_TITLE + NotificationType.SCHEDULE_REMINDER.getDefaultMessage()
        );
    }

    @Test
    void createScheduleCommentNotification_중복되지_않은_일정_댓글_알림을_생성한다() {
        // given
        mockNotificationCreation(RECEIVER_ID);

        when(notificationRepository.existsNotification(
                RECEIVER_ID,
                SCHEDULE_ID,
                NotificationType.SCHEDULE_COMMENT
        )).thenReturn(false);

        // when
        notificationService.createScheduleCommentNotification(
                RECEIVER_ID,
                WRITER_NICKNAME,
                SCHEDULE_ID
        );

        // then
        verify(notificationRepository)
                .existsNotification(
                        RECEIVER_ID,
                        SCHEDULE_ID,
                        NotificationType.SCHEDULE_COMMENT
                );

        verify(userValidator).validateActiveUser(RECEIVER_ID);
        verify(notificationRepository).save(any(Notification.class));

        verifyOutboxNotificationCreated(
                RECEIVER_ID,
                NotificationType.SCHEDULE_COMMENT,
                WRITER_NICKNAME + NotificationType.SCHEDULE_COMMENT.getDefaultMessage()
        );
    }

    @Test
    void createScheduleCommentNotification_이미_존재하는_일정_댓글_알림이면_새로_생성하지_않는다() {
        // given
        when(notificationRepository.existsNotification(
                RECEIVER_ID,
                SCHEDULE_ID,
                NotificationType.SCHEDULE_COMMENT
        )).thenReturn(true);

        // when
        notificationService.createScheduleCommentNotification(
                RECEIVER_ID,
                WRITER_NICKNAME,
                SCHEDULE_ID
        );

        // then
        verify(notificationRepository)
                .existsNotification(
                        RECEIVER_ID,
                        SCHEDULE_ID,
                        NotificationType.SCHEDULE_COMMENT
                );

        verifyNoInteractions(userValidator, outboxService);

        verify(notificationRepository, never())
                .save(any(Notification.class));
    }

    @Test
    void createCommentReplyNotification_댓글_답글_알림을_생성하고_Outbox_이벤트를_저장한다() {
        // given
        mockNotificationCreation(RECEIVER_ID);

        // when
        notificationService.createCommentReplyNotification(
                RECEIVER_ID,
                WRITER_NICKNAME,
                SCHEDULE_ID
        );

        // then
        verify(userValidator).validateActiveUser(RECEIVER_ID);
        verify(notificationRepository).save(any(Notification.class));

        verifyOutboxNotificationCreated(
                RECEIVER_ID,
                NotificationType.COMMENT_REPLY,
                WRITER_NICKNAME + NotificationType.COMMENT_REPLY.getDefaultMessage()
        );
    }

    @Test
    void findAllMyNotification_활성_사용자의_알림_목록을_조회하고_응답으로_변환한다() {
        // given
        Notification notification1 = mockSummaryNotification(
                NOTIFICATION_ID,
                "친구 요청",
                NotificationType.FRIEND_REQUEST,
                false
        );

        Notification notification2 = mockSummaryNotification(
                41L,
                "일정 공유",
                NotificationType.SCHEDULE_SHARED,
                true
        );

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(mock(User.class));

        when(notificationRepository.findAllByReceiverId(USER_ID))
                .thenReturn(List.of(notification1, notification2));

        // when
        List<NotificationSummaryResponse> result =
                notificationService.findAllMyNotification(USER_ID);

        // then
        assertThat(result).hasSize(2);

        assertThat(result.get(0).notificationId())
                .isEqualTo(NOTIFICATION_ID);
        assertThat(result.get(0).title())
                .isEqualTo("친구 요청");
        assertThat(result.get(0).notificationType())
                .isEqualTo(NotificationType.FRIEND_REQUEST);
        assertThat(result.get(0).isRead())
                .isFalse();
        assertThat(result.get(0).createdAt())
                .isEqualTo(CREATED_AT);

        assertThat(result.get(1).notificationId())
                .isEqualTo(41L);
        assertThat(result.get(1).title())
                .isEqualTo("일정 공유");
        assertThat(result.get(1).notificationType())
                .isEqualTo(NotificationType.SCHEDULE_SHARED);
        assertThat(result.get(1).isRead())
                .isTrue();
        assertThat(result.get(1).createdAt())
                .isEqualTo(CREATED_AT);

        verify(userValidator).validateActiveUser(USER_ID);
        verify(notificationRepository).findAllByReceiverId(USER_ID);
    }

    @Test
    void findAllMyNotification_알림이_없으면_빈_목록을_반환한다() {
        // given
        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(mock(User.class));

        when(notificationRepository.findAllByReceiverId(USER_ID))
                .thenReturn(List.of());

        // when
        List<NotificationSummaryResponse> result =
                notificationService.findAllMyNotification(USER_ID);

        // then
        assertThat(result).isEmpty();

        verify(userValidator).validateActiveUser(USER_ID);
        verify(notificationRepository).findAllByReceiverId(USER_ID);
    }

    @Test
    void findOneMyNotification_본인_알림을_조회하면_상세_응답을_반환한다() {
        // given
        Notification notification = mock(Notification.class);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(mock(User.class));

        when(notificationValidator.validateOwnedNotification(
                NOTIFICATION_ID,
                USER_ID
        )).thenReturn(notification);

        when(notification.getId())
                .thenReturn(NOTIFICATION_ID);
        when(notification.getTitle())
                .thenReturn("댓글 알림");
        when(notification.getContent())
                .thenReturn("새로운 댓글이 작성되었습니다.");
        when(notification.getNotificationType())
                .thenReturn(NotificationType.SCHEDULE_COMMENT);
        when(notification.isRead())
                .thenReturn(false);
        when(notification.getTargetId())
                .thenReturn(SCHEDULE_ID);
        when(notification.getCreatedAt())
                .thenReturn(CREATED_AT);

        // when
        NotificationDetailResponse result =
                notificationService.findOneMyNotification(
                        NOTIFICATION_ID,
                        USER_ID
                );

        // then
        assertThat(result).isNotNull();
        assertThat(result.notificationId())
                .isEqualTo(NOTIFICATION_ID);
        assertThat(result.title())
                .isEqualTo("댓글 알림");
        assertThat(result.content())
                .isEqualTo("새로운 댓글이 작성되었습니다.");
        assertThat(result.notificationType())
                .isEqualTo(NotificationType.SCHEDULE_COMMENT);
        assertThat(result.isRead())
                .isFalse();
        assertThat(result.targetId())
                .isEqualTo(SCHEDULE_ID);
        assertThat(result.createdAt())
                .isEqualTo(CREATED_AT);

        verify(userValidator).validateActiveUser(USER_ID);
        verify(notificationValidator)
                .validateOwnedNotification(NOTIFICATION_ID, USER_ID);
    }

    @Test
    void getUnreadCount_읽지_않은_알림_개수를_반환한다() {
        // given
        long unreadCount = 5L;

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(mock(User.class));

        when(notificationRepository.countUnreadNotifications(USER_ID))
                .thenReturn(unreadCount);

        // when
        UnreadNotificationCountResponse result =
                notificationService.getUnreadCount(USER_ID);

        // then
        assertThat(result.count())
                .isEqualTo(unreadCount);

        verify(userValidator).validateActiveUser(USER_ID);
        verify(notificationRepository)
                .countUnreadNotifications(USER_ID);
    }

    @Test
    void readNotification_본인_알림을_읽음_처리하고_Outbox에_READ_이벤트를_저장한다() {
        // given
        Notification notification = mock(Notification.class);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(mock(User.class));

        when(notificationValidator.validateOwnedNotification(
                NOTIFICATION_ID,
                USER_ID
        )).thenReturn(notification);

        when(notificationRepository.countUnreadNotifications(USER_ID))
                .thenReturn(3L);

        // when
        notificationService.readNotification(
                NOTIFICATION_ID,
                USER_ID
        );

        // then
        verify(userValidator).validateActiveUser(USER_ID);
        verify(notificationValidator)
                .validateOwnedNotification(NOTIFICATION_ID, USER_ID);

        verify(notification).read();

        verify(notificationRepository)
                .countUnreadNotifications(USER_ID);

        ArgumentCaptor<NotificationEventResponse> eventCaptor =
                ArgumentCaptor.forClass(NotificationEventResponse.class);

        verify(outboxService).save(
                any(String.class),
                eq(OutboxAggregateType.NOTIFICATION),
                eq(String.valueOf(USER_ID)),
                eq(OutboxEventType.NOTIFICATION_EVENT),
                eventCaptor.capture()
        );

        NotificationEventResponse event = eventCaptor.getValue();

        assertThat(event.eventType())
                .isEqualTo(NotificationEventType.READ);
        assertThat(event.eventId())
                .isNotBlank();
        assertThat(event.receiverId())
                .isEqualTo(USER_ID);
        assertThat(event.notificationId())
                .isEqualTo(NOTIFICATION_ID);
        assertThat(event.type())
                .isNull();
        assertThat(event.title())
                .isNull();
        assertThat(event.message())
                .isNull();
        assertThat(event.unreadCount())
                .isEqualTo(3L);
        assertThat(event.timestamp())
                .isPositive();
    }

    @Test
    void readAllNotifications_전체_알림을_읽음_처리하고_Outbox에_ALL_READ_이벤트를_저장한다() {
        // given
        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(mock(User.class));

        when(notificationRepository.countUnreadNotifications(USER_ID))
                .thenReturn(0L);

        // when
        notificationService.readAllNotifications(USER_ID);

        // then
        verify(userValidator).validateActiveUser(USER_ID);

        verify(notificationRepository)
                .readAllNotifications(USER_ID);

        verify(notificationRepository)
                .countUnreadNotifications(USER_ID);

        ArgumentCaptor<NotificationEventResponse> eventCaptor =
                ArgumentCaptor.forClass(NotificationEventResponse.class);

        verify(outboxService).save(
                any(String.class),
                eq(OutboxAggregateType.NOTIFICATION),
                eq(String.valueOf(USER_ID)),
                eq(OutboxEventType.NOTIFICATION_EVENT),
                eventCaptor.capture()
        );

        NotificationEventResponse event = eventCaptor.getValue();

        assertThat(event.eventType())
                .isEqualTo(NotificationEventType.ALL_READ);
        assertThat(event.eventId())
                .isNotBlank();
        assertThat(event.receiverId())
                .isEqualTo(USER_ID);
        assertThat(event.notificationId())
                .isNull();
        assertThat(event.type())
                .isNull();
        assertThat(event.title())
                .isNull();
        assertThat(event.message())
                .isNull();
        assertThat(event.unreadCount())
                .isEqualTo(0L);
        assertThat(event.timestamp())
                .isPositive();
    }

    @Test
    void deleteAllNotifications_사용자의_모든_알림을_삭제한다() {
        // when
        notificationService.deleteAllNotifications(USER_ID);

        // then
        verify(notificationRepository)
                .softDeleteAllByReceiverId(USER_ID);

        verifyNoInteractions(
                userValidator,
                notificationValidator,
                outboxService
        );
    }

    private void mockNotificationCreation(Long receiverId) {
        User receiver = mock(User.class);

        when(userValidator.validateActiveUser(receiverId))
                .thenReturn(receiver);

        when(receiver.getId())
                .thenReturn(receiverId);

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

        when(notificationRepository.countUnreadNotifications(receiverId))
                .thenReturn(1L);
    }

    private Notification mockSummaryNotification(
            Long notificationId,
            String title,
            NotificationType notificationType,
            boolean isRead
    ) {
        Notification notification = mock(Notification.class);

        when(notification.getId())
                .thenReturn(notificationId);
        when(notification.getTitle())
                .thenReturn(title);
        when(notification.getNotificationType())
                .thenReturn(notificationType);
        when(notification.isRead())
                .thenReturn(isRead);
        when(notification.getCreatedAt())
                .thenReturn(CREATED_AT);

        return notification;
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