package com.example.schedulebook.domain.outbox.scheduler;

import com.example.schedulebook.domain.outbox.processor.OutboxProcessor;
import com.example.schedulebook.domain.outbox.service.OutboxCleanupService;
import com.example.schedulebook.domain.outbox.service.OutboxRecoveryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxSchedulerTest {

    @Mock
    private OutboxProcessor outboxProcessor;

    @Mock
    private OutboxRecoveryService outboxRecoveryService;

    @Mock
    private OutboxCleanupService outboxCleanupService;

    @InjectMocks
    private OutboxScheduler outboxScheduler;

    @Test
    @DisplayName("process는 미처리 Outbox 처리를 위임한다")
    void process_미처리_Outbox_처리를_위임한다() {
        // when
        outboxScheduler.process();

        // then
        verify(outboxProcessor, times(1)).processPendingOutbox();
        verifyNoInteractions(outboxRecoveryService, outboxCleanupService);
    }

    @Test
    @DisplayName("process는 Outbox 처리 중 예외가 발생해도 예외를 전파하지 않는다")
    void process_Outbox_처리_중_예외가_발생해도_전파하지_않는다() {
        // given
        doThrow(new RuntimeException("Outbox 처리 실패"))
                .when(outboxProcessor)
                .processPendingOutbox();

        // when & then
        assertThatCode(() -> outboxScheduler.process())
                .doesNotThrowAnyException();

        verify(outboxProcessor, times(1)).processPendingOutbox();
        verifyNoInteractions(outboxRecoveryService, outboxCleanupService);
    }

    @Test
    @DisplayName("recover는 Stuck Outbox 복구를 위임한다")
    void recover_Stuck_Outbox_복구를_위임한다() {
        // when
        outboxScheduler.recover();

        // then
        verify(outboxRecoveryService, times(1)).recoverStuckOutboxes();
        verifyNoInteractions(outboxProcessor, outboxCleanupService);
    }

    @Test
    @DisplayName("recover는 복구 중 예외가 발생해도 예외를 전파하지 않는다")
    void recover_복구_중_예외가_발생해도_전파하지_않는다() {
        // given
        doThrow(new RuntimeException("Stuck Outbox 복구 실패"))
                .when(outboxRecoveryService)
                .recoverStuckOutboxes();

        // when & then
        assertThatCode(() -> outboxScheduler.recover())
                .doesNotThrowAnyException();

        verify(outboxRecoveryService, times(1)).recoverStuckOutboxes();
        verifyNoInteractions(outboxProcessor, outboxCleanupService);
    }

    @Test
    @DisplayName("cleanup은 30일 기준으로 SUCCESS Outbox 정리를 요청한다")
    void cleanup_30일_이전의_SUCCESS_Outbox_정리를_요청한다() {
        // given
        when(outboxCleanupService.cleanup(30)).thenReturn(5);

        // when
        outboxScheduler.cleanup();

        // then
        verify(outboxCleanupService, times(1)).cleanup(30);
        verifyNoInteractions(outboxProcessor, outboxRecoveryService);
    }

    @Test
    @DisplayName("cleanup은 삭제 대상이 없어도 정상 종료한다")
    void cleanup_삭제_대상이_없어도_정상_종료한다() {
        // given
        when(outboxCleanupService.cleanup(30)).thenReturn(0);

        // when & then
        assertThatCode(() -> outboxScheduler.cleanup())
                .doesNotThrowAnyException();

        verify(outboxCleanupService, times(1)).cleanup(30);
        verifyNoInteractions(outboxProcessor, outboxRecoveryService);
    }

    @Test
    @DisplayName("cleanup은 정리 중 예외가 발생해도 예외를 전파하지 않는다")
    void cleanup_정리_중_예외가_발생해도_전파하지_않는다() {
        // given
        when(outboxCleanupService.cleanup(30))
                .thenThrow(new RuntimeException("Outbox 정리 실패"));

        // when & then
        assertThatCode(() -> outboxScheduler.cleanup())
                .doesNotThrowAnyException();

        verify(outboxCleanupService, times(1)).cleanup(30);
        verifyNoInteractions(outboxProcessor, outboxRecoveryService);
    }
}