package com.example.schedulebook.domain.notificationretry.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import com.example.schedulebook.domain.deadletter.service.DeadLetterService;
import com.example.schedulebook.domain.notificationretry.entity.NotificationRetry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRetryStateServiceTest {

    private static final Long NOTIFICATION_RETRY_ID = 1L;
    private static final Long OUTBOX_ID = 10L;
    private static final Long RECEIVER_ID = 20L;

    private static final String EVENT_ID = "event-1";
    private static final String PAYLOAD = "{\"message\":\"test\"}";
    private static final String REASON = "알림 전송 실패";
    private static final String CLAIM_TOKEN = "claim-token";

    @Mock
    private NotificationRetryService notificationRetryService;

    @Mock
    private ProcessedNotificationRetryService processedNotificationRetryService;

    @Mock
    private DeadLetterService deadLetterService;

    @Mock
    private NotificationRetry notificationRetry;

    @InjectMocks
    private NotificationRetryStateService notificationRetryStateService;

    @Test
    void completeFailure_재시도_실패_상태로_변경하고_처리_이력과_DLQ를_저장한다() {
        // given
        Exception exception = new RuntimeException("알림 전송 실패");

        givenFailureNotificationRetryForDeadLetter();

        // when
        notificationRetryStateService.completeFailure(
                notificationRetry,
                REASON,
                CLAIM_TOKEN,
                exception
        );

        // then
        verify(notificationRetryService)
                .markFailed(
                        NOTIFICATION_RETRY_ID,
                        REASON,
                        CLAIM_TOKEN
                );

        verify(processedNotificationRetryService)
                .markFailed(
                        OUTBOX_ID,
                        RECEIVER_ID,
                        CLAIM_TOKEN
                );

        verify(deadLetterService)
                .save(
                        eq(DeadLetterType.NOTIFICATION_RETRY),
                        eq(DeadLetterSource.NOTIFICATION_RETRY_SCHEDULER),
                        eq(DeadLetterAggregateType.NOTIFICATION_RETRY),
                        eq(String.valueOf(NOTIFICATION_RETRY_ID)),
                        eq(RECEIVER_ID),
                        eq(PAYLOAD),
                        eq(exception.getMessage()),
                        eq(exception.getClass().getSimpleName()),
                        eq(1),
                        eq(EVENT_ID)
                );
    }

    @Test
    void completeFailure_claimToken이_일치하지_않으면_후속_처리를_수행하지_않는다() {
        // given
        Exception exception = new RuntimeException("알림 전송 실패");

        when(notificationRetry.getId()).thenReturn(NOTIFICATION_RETRY_ID);

        doThrow(new BaseException(ErrorEnum.NOTIFICATION_RETRY_NOT_FOUND))
                .when(notificationRetryService)
                .markFailed(
                        NOTIFICATION_RETRY_ID,
                        REASON,
                        CLAIM_TOKEN
                );

        // when & then
        assertThatThrownBy(() ->
                notificationRetryStateService.completeFailure(
                        notificationRetry,
                        REASON,
                        CLAIM_TOKEN,
                        exception
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.NOTIFICATION_RETRY_NOT_FOUND);

        verify(notificationRetryService)
                .markFailed(
                        NOTIFICATION_RETRY_ID,
                        REASON,
                        CLAIM_TOKEN
                );

        verifyNoInteractions(processedNotificationRetryService);
        verifyNoInteractions(deadLetterService);
    }

    @Test
    void completeFailure_처리_이력_상태_변경에_실패하면_DLQ를_저장하지_않는다() {
        // given
        Exception exception = new RuntimeException("알림 전송 실패");

        givenFailureNotificationRetryForProcessed();

        doThrow(new BaseException(ErrorEnum.NOTIFICATION_RETRY_NOT_FOUND))
                .when(processedNotificationRetryService)
                .markFailed(
                        OUTBOX_ID,
                        RECEIVER_ID,
                        CLAIM_TOKEN
                );

        // when & then
        assertThatThrownBy(() ->
                notificationRetryStateService.completeFailure(
                        notificationRetry,
                        REASON,
                        CLAIM_TOKEN,
                        exception
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.NOTIFICATION_RETRY_NOT_FOUND);

        verify(notificationRetryService)
                .markFailed(
                        NOTIFICATION_RETRY_ID,
                        REASON,
                        CLAIM_TOKEN
                );

        verify(processedNotificationRetryService)
                .markFailed(
                        OUTBOX_ID,
                        RECEIVER_ID,
                        CLAIM_TOKEN
                );

        verifyNoInteractions(deadLetterService);
    }

    @Test
    void completeFailure_DLQ_저장에_실패하면_DEAD_LETTER_SAVE_FAILED_예외를_발생시킨다() {
        // given
        Exception exception = new RuntimeException("알림 전송 실패");
        RuntimeException dlqException = new RuntimeException("DLQ 저장 실패");

        givenFailureNotificationRetryForDeadLetter();

        doThrow(dlqException)
                .when(deadLetterService)
                .save(
                        eq(DeadLetterType.NOTIFICATION_RETRY),
                        eq(DeadLetterSource.NOTIFICATION_RETRY_SCHEDULER),
                        eq(DeadLetterAggregateType.NOTIFICATION_RETRY),
                        eq(String.valueOf(NOTIFICATION_RETRY_ID)),
                        eq(RECEIVER_ID),
                        eq(PAYLOAD),
                        eq(exception.getMessage()),
                        eq(exception.getClass().getSimpleName()),
                        eq(1),
                        eq(EVENT_ID)
                );

        // when & then
        assertThatThrownBy(() ->
                notificationRetryStateService.completeFailure(
                        notificationRetry,
                        REASON,
                        CLAIM_TOKEN,
                        exception
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.DEAD_LETTER_SAVE_FAILED);

        verify(notificationRetryService)
                .markFailed(
                        NOTIFICATION_RETRY_ID,
                        REASON,
                        CLAIM_TOKEN
                );

        verify(processedNotificationRetryService)
                .markFailed(
                        OUTBOX_ID,
                        RECEIVER_ID,
                        CLAIM_TOKEN
                );

        verify(deadLetterService)
                .save(
                        eq(DeadLetterType.NOTIFICATION_RETRY),
                        eq(DeadLetterSource.NOTIFICATION_RETRY_SCHEDULER),
                        eq(DeadLetterAggregateType.NOTIFICATION_RETRY),
                        eq(String.valueOf(NOTIFICATION_RETRY_ID)),
                        eq(RECEIVER_ID),
                        eq(PAYLOAD),
                        eq(exception.getMessage()),
                        eq(exception.getClass().getSimpleName()),
                        eq(1),
                        eq(EVENT_ID)
                );
    }

    @Test
    void completeSuccess_재시도_성공_상태로_변경하고_처리_이력을_성공_상태로_변경한다() {
        // given
        givenSuccessNotificationRetry();

        // when
        notificationRetryStateService.completeSuccess(
                notificationRetry,
                CLAIM_TOKEN
        );

        // then
        verify(notificationRetryService)
                .markSuccess(
                        NOTIFICATION_RETRY_ID,
                        CLAIM_TOKEN
                );

        verify(processedNotificationRetryService)
                .markSuccess(
                        OUTBOX_ID,
                        RECEIVER_ID,
                        CLAIM_TOKEN
                );
    }

    @Test
    void completeSuccess_claimToken이_일치하지_않으면_처리_이력을_변경하지_않는다() {
        // given
        when(notificationRetry.getId())
                .thenReturn(NOTIFICATION_RETRY_ID);

        doThrow(new BaseException(ErrorEnum.NOTIFICATION_RETRY_NOT_FOUND))
                .when(notificationRetryService)
                .markSuccess(
                        NOTIFICATION_RETRY_ID,
                        CLAIM_TOKEN
                );

        // when & then
        assertThatThrownBy(() ->
                notificationRetryStateService.completeSuccess(
                        notificationRetry,
                        CLAIM_TOKEN
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.NOTIFICATION_RETRY_NOT_FOUND);

        verify(notificationRetryService)
                .markSuccess(
                        NOTIFICATION_RETRY_ID,
                        CLAIM_TOKEN
                );

        verifyNoInteractions(processedNotificationRetryService);
    }

    private void givenSuccessNotificationRetry() {
        when(notificationRetry.getId())
                .thenReturn(NOTIFICATION_RETRY_ID);

        when(notificationRetry.getOutboxId())
                .thenReturn(OUTBOX_ID);

        when(notificationRetry.getReceiverId())
                .thenReturn(RECEIVER_ID);
    }

    private void givenFailureNotificationRetryForProcessed() {
        when(notificationRetry.getId())
                .thenReturn(NOTIFICATION_RETRY_ID);

        when(notificationRetry.getOutboxId())
                .thenReturn(OUTBOX_ID);

        when(notificationRetry.getReceiverId())
                .thenReturn(RECEIVER_ID);
    }

    private void givenFailureNotificationRetryForDeadLetter() {
        when(notificationRetry.getId())
                .thenReturn(NOTIFICATION_RETRY_ID);

        when(notificationRetry.getOutboxId())
                .thenReturn(OUTBOX_ID);

        when(notificationRetry.getReceiverId())
                .thenReturn(RECEIVER_ID);

        when(notificationRetry.getPayload())
                .thenReturn(PAYLOAD);

        when(notificationRetry.getRetryCount())
                .thenReturn(0);

        when(notificationRetry.getEventId())
                .thenReturn(EVENT_ID);
    }
}