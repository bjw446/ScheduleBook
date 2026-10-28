package com.example.schedulebook.domain.deadletter.scheduler;

import com.example.schedulebook.domain.deadletter.service.DeadLetterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadLetterSchedulerTest {

    @Mock
    private DeadLetterService deadLetterService;

    @InjectMocks
    private DeadLetterScheduler deadLetterScheduler;

    @Test
    @DisplayName("만료된 PROCESSING DeadLetter가 있으면 회수를 실행한다")
    void reclaimExpiredProcessing_만료된_PROCESSING을_회수한다() {
        // given
        when(deadLetterService.reclaimExpiredProcessing()).thenReturn(3);

        // when
        deadLetterScheduler.reclaimExpiredProcessing();

        // then
        verify(deadLetterService, times(1)).reclaimExpiredProcessing();
        verifyNoMoreInteractions(deadLetterService);
    }

    @Test
    @DisplayName("회수할 만료된 PROCESSING DeadLetter가 없어도 정상 실행한다")
    void reclaimExpiredProcessing_회수할_대상이_없다() {
        // given
        when(deadLetterService.reclaimExpiredProcessing()).thenReturn(0);

        // when
        deadLetterScheduler.reclaimExpiredProcessing();

        // then
        verify(deadLetterService, times(1)).reclaimExpiredProcessing();
        verifyNoMoreInteractions(deadLetterService);
    }

    @Test
    @DisplayName("만료된 PROCESSING 회수 중 예외가 발생하면 예외를 전파한다")
    void reclaimExpiredProcessing_회수_중_예외가_발생한다() {
        // given
        RuntimeException exception = new RuntimeException("DeadLetter 회수 실패");

        when(deadLetterService.reclaimExpiredProcessing())
                .thenThrow(exception);

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> deadLetterScheduler.reclaimExpiredProcessing()
                )
                .isSameAs(exception);

        verify(deadLetterService, times(1)).reclaimExpiredProcessing();
        verifyNoMoreInteractions(deadLetterService);
    }
}