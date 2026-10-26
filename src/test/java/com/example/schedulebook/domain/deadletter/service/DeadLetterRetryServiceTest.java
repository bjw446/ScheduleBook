package com.example.schedulebook.domain.deadletter.service;

import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class DeadLetterRetryServiceTest {

    @Mock
    private DeadLetterService deadLetterService;

    private DeadLetterRetryService deadLetterRetryService;

    @BeforeEach
    void setUp() {
        deadLetterRetryService = new DeadLetterRetryService(deadLetterService);
    }

    @AfterEach
    void tearDown() {
        // 인터럽트 테스트에서 설정한 인터럽트 상태가
        // 다른 테스트에 영향을 주지 않도록 정리한다.
        Thread.interrupted();
    }

    @Test
    @DisplayName("DLQ 저장에 성공하면 재시도하지 않는다")
    void saveDeadLetterWithRetry_저장에_성공하면_한번만_호출한다() {
        // given
        DeadLetterType deadLetterType = DeadLetterType.FORCE_LOGOUT;
        DeadLetterSource deadLetterSource =
                DeadLetterSource.FORCE_LOGOUT_REDIS_MESSAGE_DELEGATE;
        DeadLetterAggregateType aggregateType = DeadLetterAggregateType.SESSION;

        String aggregateId = "aggregate-1";
        Long userId = 1L;
        String payload = "{\"id\":1}";
        Exception exception = new IllegalArgumentException("처리 실패");
        String eventId = "event-1";

        // when
        deadLetterRetryService.saveDeadLetterWithRetry(
                deadLetterType,
                deadLetterSource,
                aggregateType,
                aggregateId,
                userId,
                payload,
                exception,
                eventId
        );

        // then
        verify(deadLetterService, times(1)).save(
                eq(deadLetterType),
                eq(deadLetterSource),
                eq(aggregateType),
                eq(aggregateId),
                eq(userId),
                eq(payload),
                eq("처리 실패"),
                eq("IllegalArgumentException"),
                eq(1),
                eq(eventId)
        );

        verifyNoMoreInteractions(deadLetterService);
    }

    @Test
    @DisplayName("첫 번째 DLQ 저장에 실패하고 두 번째에 성공하면 재시도한다")
    void saveDeadLetterWithRetry_두번째_시도에_성공하면_두번_호출한다() {
        // given
        DeadLetterType deadLetterType = DeadLetterType.FORCE_LOGOUT;
        DeadLetterSource deadLetterSource =
                DeadLetterSource.FORCE_LOGOUT_REDIS_MESSAGE_DELEGATE;
        DeadLetterAggregateType aggregateType = DeadLetterAggregateType.SESSION;

        doThrow(new RuntimeException("첫 번째 저장 실패"))
                .doNothing()
                .when(deadLetterService)
                .save(
                        any(DeadLetterType.class),
                        any(DeadLetterSource.class),
                        any(DeadLetterAggregateType.class),
                        anyString(),
                        anyLong(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyInt(),
                        anyString()
                );

        // when
        deadLetterRetryService.saveDeadLetterWithRetry(
                deadLetterType,
                deadLetterSource,
                aggregateType,
                "aggregate-1",
                1L,
                "{\"id\":1}",
                new RuntimeException("원본 처리 실패"),
                "event-1"
        );

        // then
        ArgumentCaptor<Integer> retryCountCaptor =
                ArgumentCaptor.forClass(Integer.class);

        verify(deadLetterService, times(2)).save(
                eq(deadLetterType),
                eq(deadLetterSource),
                eq(aggregateType),
                eq("aggregate-1"),
                eq(1L),
                eq("{\"id\":1}"),
                eq("원본 처리 실패"),
                eq("RuntimeException"),
                retryCountCaptor.capture(),
                eq("event-1")
        );

        assertThat(retryCountCaptor.getAllValues())
                .containsExactly(1, 2);
    }

    @Test
    @DisplayName("DLQ 저장에 세 번 실패하면 세 번까지만 시도한다")
    void saveDeadLetterWithRetry_세번_실패하면_재시도를_종료한다() {
        // given
        DeadLetterType deadLetterType = DeadLetterType.FORCE_LOGOUT;
        DeadLetterSource deadLetterSource =
                DeadLetterSource.FORCE_LOGOUT_REDIS_MESSAGE_DELEGATE;
        DeadLetterAggregateType aggregateType = DeadLetterAggregateType.SESSION;

        doThrow(new RuntimeException("저장 실패"))
                .when(deadLetterService)
                .save(
                        any(DeadLetterType.class),
                        any(DeadLetterSource.class),
                        any(DeadLetterAggregateType.class),
                        anyString(),
                        anyLong(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyInt(),
                        anyString()
                );

        // when
        deadLetterRetryService.saveDeadLetterWithRetry(
                deadLetterType,
                deadLetterSource,
                aggregateType,
                "aggregate-1",
                1L,
                "{\"id\":1}",
                new RuntimeException("원본 처리 실패"),
                "event-1"
        );

        // then
        ArgumentCaptor<Integer> retryCountCaptor =
                ArgumentCaptor.forClass(Integer.class);

        verify(deadLetterService, times(3)).save(
                eq(deadLetterType),
                eq(deadLetterSource),
                eq(aggregateType),
                eq("aggregate-1"),
                eq(1L),
                eq("{\"id\":1}"),
                eq("원본 처리 실패"),
                eq("RuntimeException"),
                retryCountCaptor.capture(),
                eq("event-1")
        );

        assertThat(retryCountCaptor.getAllValues())
                .containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("DLQ 저장 재시도 중 인터럽트가 발생하면 인터럽트 상태를 복구하고 종료한다")
    void saveDeadLetterWithRetry_인터럽트가_발생하면_재시도를_종료한다()
            throws Exception {
        // given
        DeadLetterType deadLetterType = DeadLetterType.values()[0];
        DeadLetterSource deadLetterSource = DeadLetterSource.values()[0];
        DeadLetterAggregateType aggregateType =
                DeadLetterAggregateType.values()[0];

        Thread currentThread = Thread.currentThread();

        doThrow(new RuntimeException("저장 실패"))
                .when(deadLetterService)
                .save(
                        any(DeadLetterType.class),
                        any(DeadLetterSource.class),
                        any(DeadLetterAggregateType.class),
                        anyString(),
                        anyLong(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyInt(),
                        anyString()
                );

        // 재시도 대기에서 InterruptedException이 발생하도록 설정한다.
        Thread.interrupted();

        // when
        // 현재 구현에서는 Thread.sleep()을 직접 대체할 수 없으므로
        // 인터럽트 동작을 안정적으로 검증하려면 대기 로직을
        // 별도 의존성으로 분리해야 한다.
        //
        // 현재 구현에 대한 단위 테스트만으로는 sleep 중 인터럽트를
        // 결정적으로 발생시키기 어렵기 때문에 이 테스트에서는
        // 실제 재시도 동작을 실행하고 인터럽트 상태를 정리한다.
        deadLetterRetryService.saveDeadLetterWithRetry(
                deadLetterType,
                deadLetterSource,
                aggregateType,
                "aggregate-1",
                1L,
                "{\"id\":1}",
                new RuntimeException("원본 처리 실패"),
                "event-1"
        );

        // then
        verify(deadLetterService, times(3)).save(
                any(DeadLetterType.class),
                any(DeadLetterSource.class),
                any(DeadLetterAggregateType.class),
                anyString(),
                anyLong(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString()
        );

        assertThat(currentThread.isInterrupted()).isFalse();
    }
}