package com.example.schedulebook.domain.outbox.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.outbox.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxCleanupServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private OutboxCleanupService outboxCleanupService;

    @Test
    @DisplayName("cleanup은 삭제 기간이 0일이면 INVALID_INPUT 예외를 발생시킨다")
    void cleanup_삭제_기간이_0일이면_예외가_발생한다() {
        // given
        int days = 0;

        // when & then
        assertThatThrownBy(() -> outboxCleanupService.cleanup(days))
                .isInstanceOf(BaseException.class)
                .satisfies(exception ->
                        assertThat(((BaseException) exception).getErrorEnum())
                                .isEqualTo(ErrorEnum.INVALID_INPUT)
                );

        verify(outboxRepository, never()).deleteSuccessBefore(
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("cleanup은 삭제 기간이 음수이면 INVALID_INPUT 예외를 발생시킨다")
    void cleanup_삭제_기간이_음수이면_예외가_발생한다() {
        // given
        int days = -1;

        // when & then
        assertThatThrownBy(() -> outboxCleanupService.cleanup(days))
                .isInstanceOf(BaseException.class)
                .satisfies(exception ->
                        assertThat(((BaseException) exception).getErrorEnum())
                                .isEqualTo(ErrorEnum.INVALID_INPUT)
                );

        verify(outboxRepository, never()).deleteSuccessBefore(
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("cleanup은 지정한 기간 이전의 기준 시각으로 삭제를 요청한다")
    void cleanup_지정한_기간을_기준으로_삭제를_요청한다() {
        // given
        int days = 30;
        LocalDateTime before = LocalDateTime.now();

        when(outboxRepository.deleteSuccessBefore(
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        )).thenReturn(3);

        // when
        int result = outboxCleanupService.cleanup(days);

        // then
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<LocalDateTime> targetCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);

        verify(outboxRepository).deleteSuccessBefore(targetCaptor.capture());

        LocalDateTime target = targetCaptor.getValue();

        assertThat(target)
                .isBetween(
                        before.minusDays(days),
                        after.minusDays(days)
                );

        assertThat(result).isEqualTo(3);
    }

    @Test
    @DisplayName("cleanup은 삭제된 Outbox가 없으면 0을 반환한다")
    void cleanup_삭제된_Outbox가_없으면_0을_반환한다() {
        // given
        int days = 30;

        when(outboxRepository.deleteSuccessBefore(
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        )).thenReturn(0);

        // when
        int result = outboxCleanupService.cleanup(days);

        // then
        assertThat(result).isZero();

        verify(outboxRepository).deleteSuccessBefore(
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        );
    }
}