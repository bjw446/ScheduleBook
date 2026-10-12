package com.example.schedulebook.domain.outbox.service;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import com.example.schedulebook.domain.deadletter.service.DeadLetterService;
import com.example.schedulebook.domain.outbox.dto.RetryDecision;
import com.example.schedulebook.domain.outbox.entity.Outbox;
import com.example.schedulebook.domain.outbox.enums.OutboxStatus;
import com.example.schedulebook.domain.outbox.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxTransactionServiceTest {

    private static final Long OUTBOX_ID = 10L;
    private static final String EVENT_ID = "event-123";
    private static final String PAYLOAD = "{\"message\":\"test\"}";
    private static final String ERROR_MESSAGE = "메시지 발행 실패";

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OutboxRetryPolicy outboxRetryPolicy;

    @Mock
    private DeadLetterService deadLetterService;

    @InjectMocks
    private OutboxTransactionService outboxTransactionService;

    @Mock
    private Outbox outbox;

    // -------------------------------------------------------------------------
    // claimOutboxes
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("claimOutboxes는 재시도 대상 Outbox를 PROCESSING 상태로 변경하고 ID 목록을 반환한다")
    void claimOutboxes_재시도_대상을_PROCESSING으로_변경하고_ID_목록을_반환한다() {
        // given
        Outbox firstOutbox = org.mockito.Mockito.mock(Outbox.class);
        Outbox secondOutbox = org.mockito.Mockito.mock(Outbox.class);

        when(firstOutbox.getId()).thenReturn(1L);
        when(secondOutbox.getId()).thenReturn(2L);

        when(outboxRepository.findRetryTargets(
                any(LocalDateTime.class),
                eq(CommonConst.BATCH_SIZE)
        )).thenReturn(List.of(firstOutbox, secondOutbox));

        // when
        List<Long> result = outboxTransactionService.claimOutboxes();

        // then
        assertThat(result).containsExactly(1L, 2L);

        verify(firstOutbox).processing();
        verify(secondOutbox).processing();
        verify(outboxRepository).flush();
    }

    @Test
    @DisplayName("claimOutboxes는 처리할 대상이 없으면 빈 목록을 반환한다")
    void claimOutboxes_처리할_대상이_없으면_빈_목록을_반환한다() {
        // given
        when(outboxRepository.findRetryTargets(
                any(LocalDateTime.class),
                eq(CommonConst.BATCH_SIZE)
        )).thenReturn(List.of());

        // when
        List<Long> result = outboxTransactionService.claimOutboxes();

        // then
        assertThat(result).isEmpty();

        verify(outboxRepository).flush();
        verifyNoInteractions(outboxRetryPolicy, deadLetterService);
    }

    // -------------------------------------------------------------------------
    // markSuccess
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("markSuccess는 PROCESSING 상태의 Outbox를 SUCCESS로 변경한다")
    void markSuccess_상태_변경에_성공한다() {
        // given
        when(outboxRepository.updateStatusIfProcessing(
                eq(OUTBOX_ID),
                eq(OutboxStatus.SUCCESS),
                any(LocalDateTime.class)
        )).thenReturn(1);

        // when
        outboxTransactionService.markSuccess(OUTBOX_ID);

        // then
        verify(outboxRepository).updateStatusIfProcessing(
                eq(OUTBOX_ID),
                eq(OutboxStatus.SUCCESS),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("markSuccess는 변경된 행이 없으면 추가 처리를 하지 않는다")
    void markSuccess_변경된_행이_없으면_추가_처리하지_않는다() {
        // given
        when(outboxRepository.updateStatusIfProcessing(
                eq(OUTBOX_ID),
                eq(OutboxStatus.SUCCESS),
                any(LocalDateTime.class)
        )).thenReturn(0);

        // when
        outboxTransactionService.markSuccess(OUTBOX_ID);

        // then
        verify(outboxRepository).updateStatusIfProcessing(
                eq(OUTBOX_ID),
                eq(OutboxStatus.SUCCESS),
                any(LocalDateTime.class)
        );

        verifyNoInteractions(outboxRetryPolicy, deadLetterService);
    }

    // -------------------------------------------------------------------------
    // handleFailure
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("handleFailure는 재시도 가능한 실패를 FAILED 상태로 변경한다")
    void handleFailure_재시도_가능한_실패를_FAILED로_변경한다() {
        // given
        LocalDateTime nextRetryAt = LocalDateTime.now().plusSeconds(60);
        RetryDecision retryDecision =
                new RetryDecision(OutboxStatus.FAILED, nextRetryAt);

        when(outboxRepository.findById(OUTBOX_ID))
                .thenReturn(Optional.of(outbox));
        when(outbox.getId()).thenReturn(OUTBOX_ID);
        when(outbox.getRetryCount()).thenReturn(0);

        when(outboxRetryPolicy.decide(1)).thenReturn(retryDecision);

        when(outboxRepository.updateFailureIfProcessing(
                OUTBOX_ID,
                OutboxStatus.FAILED,
                ERROR_MESSAGE,
                nextRetryAt
        )).thenReturn(1);

        // when
        outboxTransactionService.handleFailure(
                OUTBOX_ID,
                new RuntimeException(ERROR_MESSAGE)
        );

        // then
        verify(outboxRetryPolicy).decide(1);

        verify(outboxRepository).updateFailureIfProcessing(
                OUTBOX_ID,
                OutboxStatus.FAILED,
                ERROR_MESSAGE,
                nextRetryAt
        );

        verifyNoInteractions(deadLetterService);
    }

    @Test
    @DisplayName("handleFailure는 최대 재시도 횟수에 도달하면 DEAD 처리하고 DLQ에 저장한다")
    void handleFailure_최대_재시도_횟수에_도달하면_DEAD_처리하고_DLQ에_저장한다() {
        // given
        int nextRetry = CommonConst.MAX_RETRY;
        RuntimeException exception = new RuntimeException(ERROR_MESSAGE);

        RetryDecision retryDecision =
                new RetryDecision(OutboxStatus.DEAD, null);

        when(outboxRepository.findById(OUTBOX_ID))
                .thenReturn(Optional.of(outbox));
        when(outbox.getId()).thenReturn(OUTBOX_ID);
        when(outbox.getRetryCount()).thenReturn(nextRetry - 1);
        when(outbox.getPayload()).thenReturn(PAYLOAD);
        when(outbox.getEventId()).thenReturn(EVENT_ID);

        when(outboxRetryPolicy.decide(nextRetry)).thenReturn(retryDecision);

        when(outboxRepository.updateFailureIfProcessing(
                OUTBOX_ID,
                OutboxStatus.DEAD,
                ERROR_MESSAGE,
                null
        )).thenReturn(1);

        // when
        outboxTransactionService.handleFailure(OUTBOX_ID, exception);

        // then
        verify(deadLetterService).save(
                DeadLetterType.OUTBOX,
                DeadLetterSource.OUTBOX_TRANSACTION_SERVICE,
                DeadLetterAggregateType.OUTBOX,
                OUTBOX_ID.toString(),
                null,
                PAYLOAD,
                ERROR_MESSAGE,
                RuntimeException.class.getSimpleName(),
                nextRetry,
                EVENT_ID
        );

        verify(outboxRepository).updateFailureIfProcessing(
                OUTBOX_ID,
                OutboxStatus.DEAD,
                ERROR_MESSAGE,
                null
        );
    }

    @Test
    @DisplayName("handleFailure는 DEAD 결정 후 상태 변경된 행이 없어도 DLQ에 저장한다")
    void handleFailure_DEAD_결정_후_변경된_행이_없어도_DLQ에_저장한다() {
        // given
        int nextRetry = CommonConst.MAX_RETRY;
        RuntimeException exception = new RuntimeException(ERROR_MESSAGE);

        RetryDecision retryDecision =
                new RetryDecision(OutboxStatus.DEAD, null);

        when(outboxRepository.findById(OUTBOX_ID))
                .thenReturn(Optional.of(outbox));
        when(outbox.getId()).thenReturn(OUTBOX_ID);
        when(outbox.getRetryCount()).thenReturn(nextRetry - 1);
        when(outbox.getPayload()).thenReturn(PAYLOAD);
        when(outbox.getEventId()).thenReturn(EVENT_ID);

        when(outboxRetryPolicy.decide(nextRetry)).thenReturn(retryDecision);

        when(outboxRepository.updateFailureIfProcessing(
                OUTBOX_ID,
                OutboxStatus.DEAD,
                ERROR_MESSAGE,
                null
        )).thenReturn(0);

        // when
        outboxTransactionService.handleFailure(OUTBOX_ID, exception);

        // then
        verify(deadLetterService).save(
                DeadLetterType.OUTBOX,
                DeadLetterSource.OUTBOX_TRANSACTION_SERVICE,
                DeadLetterAggregateType.OUTBOX,
                OUTBOX_ID.toString(),
                null,
                PAYLOAD,
                ERROR_MESSAGE,
                RuntimeException.class.getSimpleName(),
                nextRetry,
                EVENT_ID
        );

        verify(outboxRepository).updateFailureIfProcessing(
                OUTBOX_ID,
                OutboxStatus.DEAD,
                ERROR_MESSAGE,
                null
        );
    }

    @Test
    @DisplayName("handleFailure는 Outbox를 찾을 수 없으면 OUTBOX_NOT_FOUND 예외를 던진다")
    void handleFailure_Outbox가_없으면_OUTBOX_NOT_FOUND_예외를_던진다() {
        // given
        when(outboxRepository.findById(OUTBOX_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                outboxTransactionService.handleFailure(
                        OUTBOX_ID,
                        new RuntimeException(ERROR_MESSAGE)
                )
        )
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorEnum())
                                .isEqualTo(ErrorEnum.OUTBOX_NOT_FOUND)
                );

        verifyNoInteractions(outboxRetryPolicy, deadLetterService);
    }

    // -------------------------------------------------------------------------
    // recoverStuck
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("recoverStuck은 처리 시간이 초과된 Outbox를 FAILED 상태로 복구한다")
    void recoverStuck_처리_시간이_초과된_Outbox를_FAILED로_복구한다() {
        // given
        LocalDateTime nextRetryAt = LocalDateTime.now().plusSeconds(60);

        RetryDecision retryDecision =
                new RetryDecision(OutboxStatus.FAILED, nextRetryAt);

        when(outboxRepository.findStuckOutboxes(
                any(LocalDateTime.class),
                eq(CommonConst.BATCH_SIZE)
        )).thenReturn(List.of(outbox));

        when(outbox.getId()).thenReturn(OUTBOX_ID);
        when(outbox.getRetryCount()).thenReturn(0);

        when(outboxRetryPolicy.decide(1)).thenReturn(retryDecision);

        when(outboxRepository.updateRecoverIfProcessing(
                OUTBOX_ID,
                OutboxStatus.FAILED,
                "Outbox 처리 중 시간 초과",
                nextRetryAt
        )).thenReturn(1);

        // when
        outboxTransactionService.recoverStuck();

        // then
        verify(outboxRetryPolicy).decide(1);

        verify(outboxRepository).updateRecoverIfProcessing(
                OUTBOX_ID,
                OutboxStatus.FAILED,
                "Outbox 처리 중 시간 초과",
                nextRetryAt
        );

        verifyNoInteractions(deadLetterService);
    }

    @Test
    @DisplayName("recoverStuck은 최대 재시도 횟수에 도달하면 DEAD 처리하고 DLQ에 저장한다")
    void recoverStuck_최대_재시도_횟수에_도달하면_DEAD_처리하고_DLQ에_저장한다() {
        // given
        int nextRetry = CommonConst.MAX_RETRY;

        RetryDecision retryDecision =
                new RetryDecision(OutboxStatus.DEAD, null);

        when(outboxRepository.findStuckOutboxes(
                any(LocalDateTime.class),
                eq(CommonConst.BATCH_SIZE)
        )).thenReturn(List.of(outbox));

        when(outbox.getId()).thenReturn(OUTBOX_ID);
        when(outbox.getRetryCount()).thenReturn(nextRetry - 1);
        when(outbox.getPayload()).thenReturn(PAYLOAD);
        when(outbox.getEventId()).thenReturn(EVENT_ID);

        when(outboxRetryPolicy.decide(nextRetry)).thenReturn(retryDecision);

        when(outboxRepository.updateRecoverIfProcessing(
                OUTBOX_ID,
                OutboxStatus.DEAD,
                "Outbox lease timeout으로 DEAD 처리",
                null
        )).thenReturn(1);

        // when
        outboxTransactionService.recoverStuck();

        // then
        verify(deadLetterService).save(
                DeadLetterType.OUTBOX,
                DeadLetterSource.OUTBOX_TRANSACTION_SERVICE,
                DeadLetterAggregateType.OUTBOX,
                OUTBOX_ID.toString(),
                null,
                PAYLOAD,
                "Outbox lease timeout으로 DEAD 처리",
                "LEASE_TIMEOUT",
                nextRetry,
                EVENT_ID
        );

        verify(outboxRepository).updateRecoverIfProcessing(
                OUTBOX_ID,
                OutboxStatus.DEAD,
                "Outbox lease timeout으로 DEAD 처리",
                null
        );
    }

    @Test
    @DisplayName("recoverStuck은 변경된 행이 없어도 예외를 던지지 않는다")
    void recoverStuck_변경된_행이_없으면_예외를_던지지_않는다() {
        // given
        LocalDateTime nextRetryAt = LocalDateTime.now().plusSeconds(60);

        RetryDecision retryDecision =
                new RetryDecision(OutboxStatus.FAILED, nextRetryAt);

        when(outboxRepository.findStuckOutboxes(
                any(LocalDateTime.class),
                eq(CommonConst.BATCH_SIZE)
        )).thenReturn(List.of(outbox));

        when(outbox.getId()).thenReturn(OUTBOX_ID);
        when(outbox.getRetryCount()).thenReturn(0);

        when(outboxRetryPolicy.decide(1)).thenReturn(retryDecision);

        when(outboxRepository.updateRecoverIfProcessing(
                OUTBOX_ID,
                OutboxStatus.FAILED,
                "Outbox 처리 중 시간 초과",
                nextRetryAt
        )).thenReturn(0);

        // when
        outboxTransactionService.recoverStuck();

        // then
        verify(outboxRepository).updateRecoverIfProcessing(
                OUTBOX_ID,
                OutboxStatus.FAILED,
                "Outbox 처리 중 시간 초과",
                nextRetryAt
        );
    }

    // -------------------------------------------------------------------------
    // recover
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("recover는 이미 PENDING 상태인 Outbox를 그대로 유지한다")
    void recover_이미_PENDING이면_상태를_변경하지_않는다() {
        // given
        when(outboxRepository.findById(OUTBOX_ID))
                .thenReturn(Optional.of(outbox));
        when(outbox.getStatus()).thenReturn(OutboxStatus.PENDING);

        // when
        outboxTransactionService.recover(OUTBOX_ID);

        // then
        verify(outboxRepository, never()).updateRecover(OUTBOX_ID);
    }

    @Test
    @DisplayName("recover는 DEAD가 아닌 상태이면 INVALID_OUTBOX_STATUS 예외를 던진다")
    void recover_DEAD가_아닌_상태이면_INVALID_OUTBOX_STATUS_예외를_던진다() {
        // given
        when(outboxRepository.findById(OUTBOX_ID))
                .thenReturn(Optional.of(outbox));
        when(outbox.getStatus()).thenReturn(OutboxStatus.PROCESSING);

        // when & then
        assertThatThrownBy(() -> outboxTransactionService.recover(OUTBOX_ID))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorEnum())
                                .isEqualTo(ErrorEnum.INVALID_OUTBOX_STATUS)
                );

        verify(outboxRepository, never()).updateRecover(OUTBOX_ID);
    }

    @Test
    @DisplayName("recover는 DEAD 상태의 Outbox를 PENDING으로 복구한다")
    void recover_DEAD_상태의_Outbox를_PENDING으로_복구한다() {
        // given
        when(outboxRepository.findById(OUTBOX_ID))
                .thenReturn(Optional.of(outbox));
        when(outbox.getStatus()).thenReturn(OutboxStatus.DEAD);
        when(outboxRepository.updateRecover(OUTBOX_ID)).thenReturn(1);

        // when
        outboxTransactionService.recover(OUTBOX_ID);

        // then
        verify(outboxRepository).updateRecover(OUTBOX_ID);
    }

    @Test
    @DisplayName("recover는 복구된 행이 없으면 DEAD_LETTER_RECOVER_FAILED 예외를 던진다")
    void recover_복구된_행이_없으면_DEAD_LETTER_RECOVER_FAILED_예외를_던진다() {
        // given
        when(outboxRepository.findById(OUTBOX_ID))
                .thenReturn(Optional.of(outbox));
        when(outbox.getStatus()).thenReturn(OutboxStatus.DEAD);
        when(outboxRepository.updateRecover(OUTBOX_ID)).thenReturn(0);

        // when & then
        assertThatThrownBy(() -> outboxTransactionService.recover(OUTBOX_ID))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorEnum())
                                .isEqualTo(ErrorEnum.DEAD_LETTER_RECOVER_FAILED)
                );

        verify(outboxRepository).updateRecover(OUTBOX_ID);
    }

    @Test
    @DisplayName("recover는 Outbox가 없으면 OUTBOX_NOT_FOUND 예외를 던진다")
    void recover_Outbox가_없으면_OUTBOX_NOT_FOUND_예외를_던진다() {
        // given
        when(outboxRepository.findById(OUTBOX_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> outboxTransactionService.recover(OUTBOX_ID))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorEnum())
                                .isEqualTo(ErrorEnum.OUTBOX_NOT_FOUND)
                );

        verify(outboxRepository, never()).updateRecover(OUTBOX_ID);
    }
}