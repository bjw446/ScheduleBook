package com.example.schedulebook.domain.outbox.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.outbox.entity.ProcessedOutbox;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.enums.ProcessedOutboxStatus;
import com.example.schedulebook.domain.outbox.repository.ProcessedOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessedOutboxTransactionServiceTest {

    private static final Long OUTBOX_ID = 10L;

    @Mock
    private ProcessedOutboxRepository processedOutboxRepository;

    @InjectMocks
    private ProcessedOutboxTransactionService processedOutboxTransactionService;

    // -------------------------------------------------------------------------
    // findByOutboxId
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findByOutboxId는 Outbox ID에 해당하는 ProcessedOutbox를 반환한다")
    void findByOutboxId_조회에_성공한다() {
        // given
        ProcessedOutbox processedOutbox = ProcessedOutbox.of(
                OUTBOX_ID,
                OutboxAggregateType.values()[0],
                OutboxEventType.values()[0]
        );

        when(processedOutboxRepository.findByOutboxId(OUTBOX_ID))
                .thenReturn(Optional.of(processedOutbox));

        // when
        ProcessedOutbox result =
                processedOutboxTransactionService.findByOutboxId(OUTBOX_ID);

        // then
        assertThat(result).isSameAs(processedOutbox);
        assertThat(result.getOutboxId()).isEqualTo(OUTBOX_ID);
    }

    @Test
    @DisplayName("findByOutboxId는 조회 결과가 없으면 null을 반환한다")
    void findByOutboxId_조회_결과가_없으면_null을_반환한다() {
        // given
        when(processedOutboxRepository.findByOutboxId(OUTBOX_ID))
                .thenReturn(Optional.empty());

        // when
        ProcessedOutbox result =
                processedOutboxTransactionService.findByOutboxId(OUTBOX_ID);

        // then
        assertThat(result).isNull();
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create는 PROCESSING 상태의 ProcessedOutbox를 생성하고 저장 결과를 반환한다")
    void create_ProcessedOutbox를_생성하고_저장한다() {
        // given
        OutboxAggregateType aggregateType = OutboxAggregateType.values()[0];
        OutboxEventType eventType = OutboxEventType.values()[0];

        ProcessedOutbox savedOutbox = ProcessedOutbox.of(
                OUTBOX_ID,
                aggregateType,
                eventType
        );

        when(processedOutboxRepository.save(
                org.mockito.ArgumentMatchers.any(ProcessedOutbox.class)
        )).thenReturn(savedOutbox);

        // when
        ProcessedOutbox result = processedOutboxTransactionService.create(
                OUTBOX_ID,
                aggregateType,
                eventType
        );

        // then
        assertThat(result).isSameAs(savedOutbox);
        assertThat(result.getOutboxId()).isEqualTo(OUTBOX_ID);
        assertThat(result.getAggregateType()).isEqualTo(aggregateType);
        assertThat(result.getEventType()).isEqualTo(eventType);
        assertThat(result.getStatus()).isEqualTo(ProcessedOutboxStatus.PROCESSING);

        verify(processedOutboxRepository).save(
                org.mockito.ArgumentMatchers.any(ProcessedOutbox.class)
        );
    }

    @Test
    @DisplayName("create는 중복 Outbox 저장 시 Repository 예외를 그대로 전파한다")
    void create_중복_Outbox_저장_시_예외를_전파한다() {
        // given
        OutboxAggregateType aggregateType = OutboxAggregateType.values()[0];
        OutboxEventType eventType = OutboxEventType.values()[0];

        RuntimeException exception = new RuntimeException("중복 Outbox");

        when(processedOutboxRepository.save(
                org.mockito.ArgumentMatchers.any(ProcessedOutbox.class)
        )).thenThrow(exception);

        // when & then
        assertThatThrownBy(() -> processedOutboxTransactionService.create(
                OUTBOX_ID,
                aggregateType,
                eventType
        ))
                .isSameAs(exception);

        verify(processedOutboxRepository).save(
                org.mockito.ArgumentMatchers.any(ProcessedOutbox.class)
        );
    }

    // -------------------------------------------------------------------------
    // markProcessedOutboxFailed
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("markProcessedOutboxFailed는 PROCESSING 상태를 FAILED로 변경한다")
    void markProcessedOutboxFailed_상태_변경에_성공한다() {
        // given
        when(processedOutboxRepository.markFailed(OUTBOX_ID))
                .thenReturn(1);

        // when
        processedOutboxTransactionService.markProcessedOutboxFailed(OUTBOX_ID);

        // then
        verify(processedOutboxRepository).markFailed(OUTBOX_ID);
    }

    @Test
    @DisplayName("markProcessedOutboxFailed는 변경된 행이 없으면 예외를 던진다")
    void markProcessedOutboxFailed_변경된_행이_없으면_예외를_던진다() {
        // given
        when(processedOutboxRepository.markFailed(OUTBOX_ID))
                .thenReturn(0);

        // when & then
        assertThatThrownBy(() ->
                processedOutboxTransactionService.markProcessedOutboxFailed(OUTBOX_ID)
        )
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorEnum())
                                .isEqualTo(ErrorEnum.PROCESSED_OUTBOX_STATUS_CHANGE_FAILED)
                );

        verify(processedOutboxRepository).markFailed(OUTBOX_ID);
    }

    // -------------------------------------------------------------------------
    // markProcessedOutboxSuccess
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("markProcessedOutboxSuccess는 PROCESSING 상태를 SUCCESS로 변경한다")
    void markProcessedOutboxSuccess_상태_변경에_성공한다() {
        // given
        when(processedOutboxRepository.markSuccess(OUTBOX_ID))
                .thenReturn(1);

        // when
        processedOutboxTransactionService.markProcessedOutboxSuccess(OUTBOX_ID);

        // then
        verify(processedOutboxRepository).markSuccess(OUTBOX_ID);
    }

    @Test
    @DisplayName("markProcessedOutboxSuccess는 변경된 행이 없으면 예외를 던진다")
    void markProcessedOutboxSuccess_변경된_행이_없으면_예외를_던진다() {
        // given
        when(processedOutboxRepository.markSuccess(OUTBOX_ID))
                .thenReturn(0);

        // when & then
        assertThatThrownBy(() ->
                processedOutboxTransactionService.markProcessedOutboxSuccess(OUTBOX_ID)
        )
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorEnum())
                                .isEqualTo(ErrorEnum.PROCESSED_OUTBOX_STATUS_CHANGE_FAILED)
                );

        verify(processedOutboxRepository).markSuccess(OUTBOX_ID);
    }

    // -------------------------------------------------------------------------
    // markProcessedOutboxRetry
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("markProcessedOutboxRetry는 FAILED 상태의 ProcessedOutbox 재시도에 성공한다")
    void markProcessedOutboxRetry_재시도에_성공한다() {
        // given
        when(processedOutboxRepository.retry(OUTBOX_ID))
                .thenReturn(1);

        // when
        processedOutboxTransactionService.markProcessedOutboxRetry(OUTBOX_ID);

        // then
        verify(processedOutboxRepository).retry(OUTBOX_ID);
    }

    @Test
    @DisplayName("markProcessedOutboxRetry는 변경된 행이 없으면 예외를 던진다")
    void markProcessedOutboxRetry_변경된_행이_없으면_예외를_던진다() {
        // given
        when(processedOutboxRepository.retry(OUTBOX_ID))
                .thenReturn(0);

        // when & then
        assertThatThrownBy(() ->
                processedOutboxTransactionService.markProcessedOutboxRetry(OUTBOX_ID)
        )
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorEnum())
                                .isEqualTo(ErrorEnum.PROCESSED_OUTBOX_STATUS_CHANGE_FAILED)
                );

        verify(processedOutboxRepository).retry(OUTBOX_ID);
    }
}