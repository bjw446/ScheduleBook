package com.example.schedulebook.domain.auth.scheduler;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.metrics.RetrySchedulerMetrics;
import com.example.schedulebook.domain.auth.dispatcher.ForceLogoutDispatcher;
import com.example.schedulebook.domain.auth.entity.ForceLogoutRetry;
import com.example.schedulebook.domain.auth.repository.ForceLogoutRetryRepository;
import com.example.schedulebook.domain.auth.service.ForceLogoutRetryService;
import com.example.schedulebook.domain.auth.service.ForceLogoutRetryStateService;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ForceLogoutRetrySchedulerTest {

    @Mock
    private ForceLogoutRetryService forceLogoutRetryService;

    @Mock
    private ForceLogoutRetryStateService forceLogoutRetryStateService;

    @Mock
    private ForceLogoutDispatcher forceLogoutDispatcher;

    @Mock
    private RetrySchedulerMetrics retrySchedulerMetrics;

    @Mock
    private ForceLogoutRetryRepository forceLogoutRetryRepository;

    @InjectMocks
    private ForceLogoutRetryScheduler forceLogoutRetryScheduler;

    @Test
    void given재시도대상존재및Dispatch성공_whenProcess_then성공상태갱신및SuccessMetric기록() {
        // given
        ForceLogoutRetry retry = createRetry(1L, 0);

        String claimToken = "claim-token";

        given(forceLogoutRetryService.findRetryTargets(CommonConst.BATCH_SIZE))
                .willReturn(List.of(retry))
                .willReturn(List.of());

        given(forceLogoutRetryService.markProcessing(retry.getId()))
                .willReturn(claimToken);

        ForceLogoutSessionEvent event = createEvent();

        given(forceLogoutRetryService.deserialize(retry))
                .willReturn(event);

        // when
        forceLogoutRetryScheduler.process();

        // then
        verify(forceLogoutDispatcher).dispatch(event);

        verify(forceLogoutRetryStateService)
                .completeSuccess(retry, claimToken);

        verify(retrySchedulerMetrics).schedulerRun("force_logout");
        verify(retrySchedulerMetrics).processed("force_logout");
        verify(retrySchedulerMetrics).success("force_logout");

        verify(retrySchedulerMetrics, never()).error("force_logout");
        verify(retrySchedulerMetrics, never()).retry("force_logout");
        verify(retrySchedulerMetrics, never()).dlq("force_logout");
    }

    @Test
    void givenClaimToken획득실패_whenProcess_thenDispatch하지않고다음처리() {
        // given
        ForceLogoutRetry retry = createRetry(1L, 0);

        given(forceLogoutRetryService.findRetryTargets(CommonConst.BATCH_SIZE))
                .willReturn(List.of(retry))
                .willReturn(List.of());

        given(forceLogoutRetryService.markProcessing(retry.getId()))
                .willReturn(null);

        // when
        forceLogoutRetryScheduler.process();

        // then
        verify(forceLogoutRetryService)
                .markProcessing(retry.getId());

        verifyNoInteractions(forceLogoutDispatcher);

        verify(forceLogoutRetryStateService, never())
                .completeSuccess(any(), anyString());

        verify(retrySchedulerMetrics).schedulerRun("force_logout");
        verify(retrySchedulerMetrics, never()).processed("force_logout");
    }

    @Test
    void given역직렬화실패_whenProcess_thenDLQ처리() {
        // given
        ForceLogoutRetry retry = createRetry(1L, 0);
        String claimToken = "claim-token";

        BaseException exception =
                new BaseException(ErrorEnum.JSON_DESERIALIZATION_FAILED);

        given(forceLogoutRetryService.findRetryTargets(CommonConst.BATCH_SIZE))
                .willReturn(List.of(retry))
                .willReturn(List.of());

        given(forceLogoutRetryService.markProcessing(retry.getId()))
                .willReturn(claimToken);

        given(forceLogoutRetryService.deserialize(retry))
                .willThrow(exception);

        // when
        forceLogoutRetryScheduler.process();

        // then
        verify(forceLogoutRetryStateService)
                .completeFailure(
                        eq(retry),
                        eq(exception.getMessage()),
                        eq(claimToken),
                        eq(exception)
                );

        verify(retrySchedulerMetrics).error("force_logout");
        verify(retrySchedulerMetrics).dlq("force_logout");

        verify(forceLogoutRetryService, never())
                .markRetry(anyLong(), anyString(), anyInt(), anyString());

        verify(forceLogoutDispatcher, never()).dispatch(any());
    }

    @Test
    void givenDispatch에서BaseException발생및재시도횟수미달_whenProcess_thenRetry상태갱신() {
        // given
        ForceLogoutRetry retry = createRetry(1L, 0);
        String claimToken = "claim-token";

        BaseException exception =
                new BaseException(ErrorEnum.REDIS_UNAVAILABLE);

        ForceLogoutSessionEvent event = createEvent();

        given(forceLogoutRetryService.findRetryTargets(CommonConst.BATCH_SIZE))
                .willReturn(List.of(retry))
                .willReturn(List.of());

        given(forceLogoutRetryService.markProcessing(retry.getId()))
                .willReturn(claimToken);

        given(forceLogoutRetryService.deserialize(retry))
                .willReturn(event);

        doThrow(exception)
                .when(forceLogoutDispatcher)
                .dispatch(event);

        // when
        forceLogoutRetryScheduler.process();

        // then
        verify(forceLogoutDispatcher).dispatch(event);

        verify(forceLogoutRetryService)
                .markRetry(
                        retry.getId(),
                        exception.getMessage(),
                        retry.getRetryCount(),
                        claimToken
                );

        verify(retrySchedulerMetrics).error("force_logout");
        verify(retrySchedulerMetrics).retry("force_logout");

        verify(forceLogoutRetryStateService, never())
                .completeSuccess(any(), anyString());

        verify(forceLogoutRetryStateService, never())
                .completeFailure(any(), anyString(), anyString(), any());
    }

    @Test
    void givenDispatch에서일반예외발생및재시도횟수미달_whenProcess_thenRetry상태갱신() {
        // given
        ForceLogoutRetry retry = createRetry(1L, 0);
        String claimToken = "claim-token";

        RuntimeException exception =
                new RuntimeException("WebSocket dispatch failed");

        ForceLogoutSessionEvent event = createEvent();

        given(forceLogoutRetryService.findRetryTargets(CommonConst.BATCH_SIZE))
                .willReturn(List.of(retry))
                .willReturn(List.of());

        given(forceLogoutRetryService.markProcessing(retry.getId()))
                .willReturn(claimToken);

        given(forceLogoutRetryService.deserialize(retry))
                .willReturn(event);

        doThrow(exception)
                .when(forceLogoutDispatcher)
                .dispatch(event);

        // when
        forceLogoutRetryScheduler.process();

        // then
        verify(forceLogoutRetryService)
                .markRetry(
                        retry.getId(),
                        exception.getMessage(),
                        retry.getRetryCount(),
                        claimToken
                );

        verify(retrySchedulerMetrics).error("force_logout");
        verify(retrySchedulerMetrics).retry("force_logout");
    }

    @Test
    void given최대재시도횟수도달_whenProcess_thenDLQ처리() {
        // given
        int retryCount = CommonConst.MAX_RETRY - 1;

        ForceLogoutRetry retry = createRetry(1L, retryCount);
        String claimToken = "claim-token";

        RuntimeException exception =
                new RuntimeException("dispatch failed");

        ForceLogoutSessionEvent event = createEvent();

        given(forceLogoutRetryService.findRetryTargets(CommonConst.BATCH_SIZE))
                .willReturn(List.of(retry))
                .willReturn(List.of());

        given(forceLogoutRetryService.markProcessing(retry.getId()))
                .willReturn(claimToken);

        given(forceLogoutRetryService.deserialize(retry))
                .willReturn(event);

        doThrow(exception)
                .when(forceLogoutDispatcher)
                .dispatch(event);

        // when
        forceLogoutRetryScheduler.process();

        // then
        verify(forceLogoutRetryStateService)
                .completeFailure(
                        retry,
                        exception.getMessage(),
                        claimToken,
                        exception
                );

        verify(retrySchedulerMetrics).error("force_logout");
        verify(retrySchedulerMetrics).dlq("force_logout");

        verify(forceLogoutRetryService, never())
                .markRetry(anyLong(), anyString(), anyInt(), anyString());
    }

    @Test
    void given첫번째Retry처리실패_whenProcess_then다음Retry대상계속처리() {
        // given
        ForceLogoutRetry retry1 = createRetry(1L, 0);
        ForceLogoutRetry retry2 = createRetry(2L, 0);

        String claimToken1 = "claim-token-1";
        String claimToken2 = "claim-token-2";

        ForceLogoutSessionEvent event1 = createEvent();
        ForceLogoutSessionEvent event2 = new ForceLogoutSessionEvent(
                "event-456",
                2L,
                "session-456",
                60_000L
        );

        given(forceLogoutRetryService.findRetryTargets(CommonConst.BATCH_SIZE))
                .willReturn(List.of(retry1, retry2))
                .willReturn(List.of());

        given(forceLogoutRetryService.markProcessing(retry1.getId()))
                .willReturn(claimToken1);

        given(forceLogoutRetryService.markProcessing(retry2.getId()))
                .willReturn(claimToken2);

        given(forceLogoutRetryService.deserialize(retry1))
                .willReturn(event1);

        given(forceLogoutRetryService.deserialize(retry2))
                .willReturn(event2);

        doThrow(new RuntimeException("first failed"))
                .when(forceLogoutDispatcher)
                .dispatch(event1);

        // when
        forceLogoutRetryScheduler.process();

        // then
        verify(forceLogoutDispatcher).dispatch(event1);
        verify(forceLogoutDispatcher).dispatch(event2);

        verify(forceLogoutRetryService)
                .markRetry(
                        retry1.getId(),
                        "first failed",
                        retry1.getRetryCount(),
                        claimToken1
                );

        verify(forceLogoutRetryStateService)
                .completeSuccess(retry2, claimToken2);

        verify(retrySchedulerMetrics, times(2))
                .processed("force_logout");
    }

    @Test
    void givenRetry대상없음_whenProcess_then조회후종료() {
        // given
        given(forceLogoutRetryService.findRetryTargets(CommonConst.BATCH_SIZE))
                .willReturn(List.of());

        // when
        forceLogoutRetryScheduler.process();

        // then
        verify(forceLogoutRetryService)
                .findRetryTargets(CommonConst.BATCH_SIZE);

        verifyNoInteractions(forceLogoutDispatcher);
        verify(retrySchedulerMetrics).schedulerRun("force_logout");
    }

    @Test
    void givenCompleteSuccess실패_whenProcess_thenErrorMetric만기록() {
        // given
        ForceLogoutRetry retry = createRetry(1L, 0);
        String claimToken = "claim-token";

        ForceLogoutSessionEvent event = createEvent();

        RuntimeException exception =
                new RuntimeException("complete success failed");

        given(forceLogoutRetryService.findRetryTargets(CommonConst.BATCH_SIZE))
                .willReturn(List.of(retry))
                .willReturn(List.of());

        given(forceLogoutRetryService.markProcessing(retry.getId()))
                .willReturn(claimToken);

        given(forceLogoutRetryService.deserialize(retry))
                .willReturn(event);

        doThrow(exception)
                .when(forceLogoutRetryStateService)
                .completeSuccess(retry, claimToken);

        // when
        forceLogoutRetryScheduler.process();

        // then
        verify(forceLogoutRetryStateService)
                .completeSuccess(retry, claimToken);

        verify(retrySchedulerMetrics).error("force_logout");
        verify(retrySchedulerMetrics, never()).success("force_logout");
    }

    @Test
    void givenCompleteFailure실패_whenProcess_thenErrorMetric만기록() {
        // given
        ForceLogoutRetry retry = createRetry(1L, 0);
        String claimToken = "claim-token";

        BaseException exception =
                new BaseException(ErrorEnum.JSON_DESERIALIZATION_FAILED);

        given(forceLogoutRetryService.findRetryTargets(CommonConst.BATCH_SIZE))
                .willReturn(List.of(retry))
                .willReturn(List.of());

        given(forceLogoutRetryService.markProcessing(retry.getId()))
                .willReturn(claimToken);

        given(forceLogoutRetryService.deserialize(retry))
                .willThrow(exception);

        doThrow(new RuntimeException("DLQ save failed"))
                .when(forceLogoutRetryStateService)
                .completeFailure(
                        retry,
                        exception.getMessage(),
                        claimToken,
                        exception
                );

        // when
        forceLogoutRetryScheduler.process();

        // then
        verify(forceLogoutRetryStateService)
                .completeFailure(
                        retry,
                        exception.getMessage(),
                        claimToken,
                        exception
                );

        verify(retrySchedulerMetrics, times(2)).error("force_logout");
        verify(retrySchedulerMetrics, never()).dlq("force_logout");
    }

    @Test
    void givenMetrics등록_whenRegisterMetrics_thenPendingGauge등록() {
        // given
        // when
        forceLogoutRetryScheduler.registerMetrics();

        // then
        verify(retrySchedulerMetrics)
                .registerPendingGauge(
                        eq("force_logout"),
                        any()
                );
    }

    private ForceLogoutRetry createRetry(Long id, int retryCount) {
        ForceLogoutRetry retry = ForceLogoutRetry.create(
                "event-" + id,
                "session-" + id,
                1L,
                "{}",
                "retry reason"
        );

        ReflectionTestUtils.setField(retry, "id", id);
        ReflectionTestUtils.setField(retry, "retryCount", retryCount);

        return retry;
    }

    private ForceLogoutSessionEvent createEvent() {
        return new ForceLogoutSessionEvent(
                "event-123",
                1L,
                "session-123",
                60_000L
        );
    }
}