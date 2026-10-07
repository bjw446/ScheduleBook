package com.example.schedulebook.domain.notification.processor;

import com.example.schedulebook.common.executor.LoggingExecutor;
import com.example.schedulebook.domain.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupProcessorTest {

    private static final Long OUTBOX_ID = 1L;
    private static final Long USER_ID = 2L;

    @Mock
    private NotificationService notificationService;

    @Mock
    private LoggingExecutor loggingExecutor;

    @InjectMocks
    private NotificationCleanupProcessor notificationCleanupProcessor;

    @Test
    void process_알림_전체_삭제가_성공하면_true를_반환한다() {
        // given
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(2);

            task.run();

            return true;
        }).when(loggingExecutor).execute(
                eq(OUTBOX_ID),
                eq("알림 삭제"),
                any(Runnable.class)
        );

        // when
        boolean result = notificationCleanupProcessor.process(OUTBOX_ID, USER_ID);

        // then
        assertThat(result).isTrue();

        verify(notificationService)
                .deleteAllNotifications(USER_ID);
    }

    @Test
    void process_알림_전체_삭제_중_예외가_발생하면_false를_반환한다() {
        // given
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(2);

            try {
                task.run();
                return true;
            } catch (Exception e) {
                return false;
            }
        }).when(loggingExecutor).execute(
                eq(OUTBOX_ID),
                eq("알림 삭제"),
                any(Runnable.class)
        );

        doThrow(new RuntimeException("알림 삭제 실패"))
                .when(notificationService)
                .deleteAllNotifications(USER_ID);

        // when
        boolean result = notificationCleanupProcessor.process(OUTBOX_ID, USER_ID);

        // then
        assertThat(result).isFalse();

        verify(notificationService)
                .deleteAllNotifications(USER_ID);
    }
}