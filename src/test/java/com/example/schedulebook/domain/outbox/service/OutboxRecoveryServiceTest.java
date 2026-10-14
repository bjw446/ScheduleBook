package com.example.schedulebook.domain.outbox.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxRecoveryServiceTest {

    @Mock
    private OutboxTransactionService outboxTransactionService;

    @InjectMocks
    private OutboxRecoveryService outboxRecoveryService;

    @Test
    @DisplayName("recoverStuckOutboxes는 Stuck Outbox 복구를 위임한다")
    void recoverStuckOutboxes_복구를_위임한다() {
        // when
        outboxRecoveryService.recoverStuckOutboxes();

        // then
        verify(outboxTransactionService, times(1)).recoverStuck();
    }

    @Test
    @DisplayName("recoverStuckOutboxes는 복구 중 발생한 예외를 전파한다")
    void recoverStuckOutboxes_복구_중_예외가_발생하면_전파한다() {
        // given
        RuntimeException expectedException = new RuntimeException("Outbox 복구 실패");

        doThrow(expectedException)
                .when(outboxTransactionService)
                .recoverStuck();

        // when & then
        assertThatThrownBy(() -> outboxRecoveryService.recoverStuckOutboxes())
                .isSameAs(expectedException);

        verify(outboxTransactionService, times(1)).recoverStuck();
    }
}