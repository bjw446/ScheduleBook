package com.example.schedulebook.domain.notification.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.notification.entity.Notification;
import com.example.schedulebook.domain.notification.repository.NotificationRepository;
import com.example.schedulebook.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationValidatorTest {

    private static final Long NOTIFICATION_ID = 1L;
    private static final Long CURRENT_USER_ID = 10L;
    private static final Long OTHER_USER_ID = 20L;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationValidator notificationValidator;

    @Test
    void validateNotification_존재하는_알림이면_알림을_반환한다() {
        // given
        Notification notification = mock(Notification.class);

        when(notificationRepository.findByIdWithReceiver(NOTIFICATION_ID))
                .thenReturn(Optional.of(notification));

        // when
        Notification result =
                notificationValidator.validateNotification(NOTIFICATION_ID);

        // then
        assertThat(result)
                .isSameAs(notification);

        verify(notificationRepository)
                .findByIdWithReceiver(NOTIFICATION_ID);
    }

    @Test
    void validateNotification_존재하지_않는_알림이면_예외를_발생시킨다() {
        // given
        when(notificationRepository.findByIdWithReceiver(NOTIFICATION_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                notificationValidator.validateNotification(NOTIFICATION_ID)
        )
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorEnum())
                                .isEqualTo(ErrorEnum.NOTIFICATION_NOT_FOUND)
                );

        verify(notificationRepository)
                .findByIdWithReceiver(NOTIFICATION_ID);
    }

    @Test
    void validateNotificationOwner_본인_알림이면_예외가_발생하지_않는다() {
        // given
        Notification notification = mock(Notification.class);
        User receiver = mock(User.class);

        when(notification.getReceiver())
                .thenReturn(receiver);

        when(receiver.getId())
                .thenReturn(CURRENT_USER_ID);

        // when & then
        assertThatCode(() ->
                notificationValidator.validateNotificationOwner(
                        notification,
                        CURRENT_USER_ID
                )
        ).doesNotThrowAnyException();

        verify(notification).getReceiver();
        verify(receiver).getId();
    }

    @Test
    void validateNotificationOwner_타인_알림이면_예외를_발생시킨다() {
        // given
        Notification notification = mock(Notification.class);
        User receiver = mock(User.class);

        when(notification.getReceiver())
                .thenReturn(receiver);

        when(receiver.getId())
                .thenReturn(OTHER_USER_ID);

        // when & then
        assertThatThrownBy(() ->
                notificationValidator.validateNotificationOwner(
                        notification,
                        CURRENT_USER_ID
                )
        )
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorEnum())
                                .isEqualTo(ErrorEnum.NOTIFICATION_FORBIDDEN)
                );

        verify(notification).getReceiver();
        verify(receiver).getId();
    }

    @Test
    void validateOwnedNotification_본인_알림이면_알림을_반환한다() {
        // given
        Notification notification = mock(Notification.class);
        User receiver = mock(User.class);

        when(notificationRepository.findByIdWithReceiver(NOTIFICATION_ID))
                .thenReturn(Optional.of(notification));

        when(notification.getReceiver())
                .thenReturn(receiver);

        when(receiver.getId())
                .thenReturn(CURRENT_USER_ID);

        // when
        Notification result =
                notificationValidator.validateOwnedNotification(
                        NOTIFICATION_ID,
                        CURRENT_USER_ID
                );

        // then
        assertThat(result)
                .isSameAs(notification);

        verify(notificationRepository)
                .findByIdWithReceiver(NOTIFICATION_ID);

        verify(notification)
                .getReceiver();

        verify(receiver)
                .getId();
    }

    @Test
    void validateOwnedNotification_존재하지_않는_알림이면_알림_소유자_검증을_수행하지_않고_예외를_발생시킨다() {
        // given
        when(notificationRepository.findByIdWithReceiver(NOTIFICATION_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                notificationValidator.validateOwnedNotification(
                        NOTIFICATION_ID,
                        CURRENT_USER_ID
                )
        )
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorEnum())
                                .isEqualTo(ErrorEnum.NOTIFICATION_NOT_FOUND)
                );

        verify(notificationRepository)
                .findByIdWithReceiver(NOTIFICATION_ID);
    }

    @Test
    void validateOwnedNotification_타인_알림이면_예외를_발생시킨다() {
        // given
        Notification notification = mock(Notification.class);
        User receiver = mock(User.class);

        when(notificationRepository.findByIdWithReceiver(NOTIFICATION_ID))
                .thenReturn(Optional.of(notification));

        when(notification.getReceiver())
                .thenReturn(receiver);

        when(receiver.getId())
                .thenReturn(OTHER_USER_ID);

        // when & then
        assertThatThrownBy(() ->
                notificationValidator.validateOwnedNotification(
                        NOTIFICATION_ID,
                        CURRENT_USER_ID
                )
        )
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorEnum())
                                .isEqualTo(ErrorEnum.NOTIFICATION_FORBIDDEN)
                );

        verify(notificationRepository)
                .findByIdWithReceiver(NOTIFICATION_ID);

        verify(notification)
                .getReceiver();

        verify(receiver)
                .getId();
    }
}