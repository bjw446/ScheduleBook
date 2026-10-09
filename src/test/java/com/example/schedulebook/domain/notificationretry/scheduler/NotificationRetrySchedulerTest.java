package com.example.schedulebook.domain.notificationretry.scheduler;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.metrics.RetrySchedulerMetrics;
import com.example.schedulebook.domain.notificationretry.entity.NotificationRetry;
import com.example.schedulebook.domain.notificationretry.processor.NotificationRetryProcessor;
import com.example.schedulebook.domain.notificationretry.repository.NotificationRetryRepository;
import com.example.schedulebook.domain.notificationretry.service.NotificationRetryService;
import com.example.schedulebook.domain.notificationretry.service.NotificationRetryStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRetrySchedulerTest {

    private static final String METRIC = "notification";

    private static final Long NOTIFICATION_RETRY_ID = 1L;
    private static final String CLAIM_TOKEN = "claim-token";

    private static final String FAILURE_MESSAGE = "알림 처리 실패";

    @Mock
    private NotificationRetryProcessor notificationRetryProcessor;

    @Mock
    private NotificationRetryService notificationRetryService;

    @Mock
    private NotificationRetryStateService notificationRetryStateService;

    @Mock
    private NotificationRetryRepository notificationRetryRepository;

    @Mock
    private RetrySchedulerMetrics retrySchedulerMetrics;

    @Mock
    private NotificationRetry notificationRetry;

    @InjectMocks
    private NotificationRetryScheduler notificationRetryScheduler;

    @Test
    void process_재시도_대상이_없으면_처리를_종료한다() {
        // given
        when(notificationRetryService.findRetryTargets(CommonConst.BATCH_SIZE))
                .thenReturn(List.of());

        // when
        notificationRetryScheduler.process();

        // then
        verify(retrySchedulerMetrics).schedulerRun(METRIC);

        verify(notificationRetryService)
                .findRetryTargets(CommonConst.BATCH_SIZE);

        verifyNoInteractions(notificationRetryProcessor);
        verifyNoInteractions(notificationRetryStateService);
    }

    @Test
    void process_재시도에_성공하면_성공_상태로_변경하고_성공_메트릭을_기록한다() {
        // given
        givenNotificationRetryId();

        when(notificationRetryService.findRetryTargets(CommonConst.BATCH_SIZE))
                .thenReturn(List.of(notificationRetry), List.of());

        when(notificationRetryService.markProcessing(NOTIFICATION_RETRY_ID))
                .thenReturn(CLAIM_TOKEN);

        // when
        notificationRetryScheduler.process();

        // then
        verify(notificationRetryProcessor)
                .dispatch(NOTIFICATION_RETRY_ID);

        verify(notificationRetryStateService)
                .completeSuccess(notificationRetry, CLAIM_TOKEN);

        verify(retrySchedulerMetrics).schedulerRun(METRIC);
        verify(retrySchedulerMetrics).processed(METRIC);
        verify(retrySchedulerMetrics).success(METRIC);

        verify(retrySchedulerMetrics, never()).error(METRIC);
    }

    @Test
    void process_ClaimToken_발급에_실패하면_해당_대상을_건너뛴다() {
        // given
        givenNotificationRetryId();

        when(notificationRetryService.findRetryTargets(CommonConst.BATCH_SIZE))
                .thenReturn(List.of(notificationRetry), List.of());

        when(notificationRetryService.markProcessing(NOTIFICATION_RETRY_ID))
                .thenReturn(null);

        // when
        notificationRetryScheduler.process();

        // then
        verify(notificationRetryService)
                .markProcessing(NOTIFICATION_RETRY_ID);

        verifyNoInteractions(notificationRetryProcessor);
        verifyNoInteractions(notificationRetryStateService);

        verify(retrySchedulerMetrics).schedulerRun(METRIC);

        verify(retrySchedulerMetrics, never()).processed(METRIC);
        verify(retrySchedulerMetrics, never()).success(METRIC);
        verify(retrySchedulerMetrics, never()).error(METRIC);
    }

    @Test
    void process_처리에_실패하고_최대_재시도_횟수에_도달하지_않으면_재시도_상태로_변경한다() {
        // given
        givenNotificationRetryId();

        givenNotificationRetryCount(0);

        when(notificationRetryService.findRetryTargets(CommonConst.BATCH_SIZE))
                .thenReturn(List.of(notificationRetry), List.of());

        when(notificationRetryService.markProcessing(NOTIFICATION_RETRY_ID))
                .thenReturn(CLAIM_TOKEN);

        doThrow(new RuntimeException(FAILURE_MESSAGE))
                .when(notificationRetryProcessor)
                .dispatch(NOTIFICATION_RETRY_ID);

        // when
        notificationRetryScheduler.process();

        // then
        verify(notificationRetryService)
                .markRetry(
                        NOTIFICATION_RETRY_ID,
                        FAILURE_MESSAGE,
                        0,
                        CLAIM_TOKEN
                );

        verify(retrySchedulerMetrics).processed(METRIC);
        verify(retrySchedulerMetrics).error(METRIC);
        verify(retrySchedulerMetrics).retry(METRIC);

        verify(retrySchedulerMetrics, never()).success(METRIC);
        verify(retrySchedulerMetrics, never()).dlq(METRIC);

        verify(notificationRetryStateService, never())
                .completeSuccess(notificationRetry, CLAIM_TOKEN);
    }

    @Test
    void process_최대_재시도_횟수에_도달하면_실패_상태로_변경하고_DLQ_메트릭을_기록한다() {
        // given
        givenNotificationRetryId();

        givenNotificationRetryCount(CommonConst.MAX_RETRY - 1);

        when(notificationRetryService.findRetryTargets(CommonConst.BATCH_SIZE))
                .thenReturn(List.of(notificationRetry), List.of());

        when(notificationRetryService.markProcessing(NOTIFICATION_RETRY_ID))
                .thenReturn(CLAIM_TOKEN);

        doThrow(new RuntimeException(FAILURE_MESSAGE))
                .when(notificationRetryProcessor)
                .dispatch(NOTIFICATION_RETRY_ID);

        // when
        notificationRetryScheduler.process();

        // then
        verify(notificationRetryStateService)
                .completeFailure(
                        eq(notificationRetry),
                        eq(FAILURE_MESSAGE),
                        eq(CLAIM_TOKEN),
                        any(Exception.class)
                );

        verify(retrySchedulerMetrics).error(METRIC);
        verify(retrySchedulerMetrics).dlq(METRIC);

        // DLQ 경로에서는 markRetry가 호출되지 않아야 한다.
        verify(notificationRetryService, never())
                .markRetry(
                        anyLong(),
                        anyString(),
                        anyInt(),
                        anyString()
                );
    }

    @Test
    void process_DLQ_상태_변경에_실패하면_오류_메트릭을_추가로_기록한다() {
        // given
        givenNotificationRetryId();

        givenNotificationRetryCount(CommonConst.MAX_RETRY - 1);

        when(notificationRetryService.findRetryTargets(CommonConst.BATCH_SIZE))
                .thenReturn(List.of(notificationRetry), List.of());

        when(notificationRetryService.markProcessing(NOTIFICATION_RETRY_ID))
                .thenReturn(CLAIM_TOKEN);

        doThrow(new RuntimeException(FAILURE_MESSAGE))
                .when(notificationRetryProcessor)
                .dispatch(NOTIFICATION_RETRY_ID);

        doThrow(new RuntimeException("DLQ 저장 실패"))
                .when(notificationRetryStateService)
                .completeFailure(
                        eq(notificationRetry),
                        eq(FAILURE_MESSAGE),
                        eq(CLAIM_TOKEN),
                        any(Exception.class)
                );

        // when
        notificationRetryScheduler.process();

        // then
        verify(notificationRetryStateService)
                .completeFailure(
                        eq(notificationRetry),
                        eq(FAILURE_MESSAGE),
                        eq(CLAIM_TOKEN),
                        any(Exception.class)
                );

        // 최초 Processor 실패 + DLQ 상태 변경 실패
        verify(retrySchedulerMetrics, org.mockito.Mockito.times(2))
                .error(METRIC);

        verify(retrySchedulerMetrics, never()).dlq(METRIC);
    }

    @Test
    void process_재시도_상태_변경에_실패하면_오류_메트릭을_기록한다() {
        // given
        givenNotificationRetryId();

        givenNotificationRetryCount(0);

        when(notificationRetryService.findRetryTargets(CommonConst.BATCH_SIZE))
                .thenReturn(List.of(notificationRetry), List.of());

        when(notificationRetryService.markProcessing(NOTIFICATION_RETRY_ID))
                .thenReturn(CLAIM_TOKEN);

        doThrow(new RuntimeException(FAILURE_MESSAGE))
                .when(notificationRetryProcessor)
                .dispatch(NOTIFICATION_RETRY_ID);

        doThrow(new RuntimeException("재시도 상태 변경 실패"))
                .when(notificationRetryService)
                .markRetry(
                        NOTIFICATION_RETRY_ID,
                        FAILURE_MESSAGE,
                        0,
                        CLAIM_TOKEN
                );

        // when
        notificationRetryScheduler.process();

        // then
        verify(notificationRetryService)
                .markRetry(
                        NOTIFICATION_RETRY_ID,
                        FAILURE_MESSAGE,
                        0,
                        CLAIM_TOKEN
                );

        // 최초 Processor 실패 + 재시도 상태 변경 실패
        verify(retrySchedulerMetrics, org.mockito.Mockito.times(2))
                .error(METRIC);

        verify(retrySchedulerMetrics, never()).retry(METRIC);
    }

    @Test
    void registerMetrics_PendingGauge를_등록한다() {
        // given
        when(notificationRetryRepository.countPending()).thenReturn(5L);

        // when
        notificationRetryScheduler.registerMetrics();

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Supplier<Number>> supplierCaptor =
                ArgumentCaptor.forClass(Supplier.class);

        verify(retrySchedulerMetrics)
                .registerPendingGauge(
                        eq(METRIC),
                        supplierCaptor.capture()
                );

        assertThat(supplierCaptor.getValue().get()).isEqualTo(5L);

        verify(notificationRetryRepository).countPending();
    }

    private void givenNotificationRetryId() {
        when(notificationRetry.getId())
                .thenReturn(NOTIFICATION_RETRY_ID);
    }

    private void givenNotificationRetryCount(int retryCount) {
        when(notificationRetry.getRetryCount())
                .thenReturn(retryCount);
    }
}