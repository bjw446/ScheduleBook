package com.example.schedulebook.domain.notificationretry.scheduler;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.domain.notificationretry.repository.ProcessedNotificationRetryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessedNotificationRetryScheduler 테스트")
class ProcessedNotificationRetrySchedulerTest {

    @Mock
    private ProcessedNotificationRetryRepository processedNotificationRetryRepository;

    @InjectMocks
    private ProcessedNotificationRetryScheduler processedNotificationRetryScheduler;

    @Test
    @DisplayName("timeout 기준 시간을 전달하여 오래된 처리 내역을 복구한다")
    void recoverTimeoutProcessing_기준_시간을_전달하여_오래된_처리_내역을_복구한다() {
        // given
        LocalDateTime beforeExecution = LocalDateTime.now();

        when(processedNotificationRetryRepository.recoverTimeoutProcessing(
                any(LocalDateTime.class)
        )).thenReturn(3);

        // when
        processedNotificationRetryScheduler.recoverTimeoutProcessing();

        LocalDateTime afterExecution = LocalDateTime.now();

        // then
        ArgumentCaptor<LocalDateTime> captor =
                ArgumentCaptor.forClass(LocalDateTime.class);

        verify(processedNotificationRetryRepository)
                .recoverTimeoutProcessing(captor.capture());

        LocalDateTime actualCutoffTime = captor.getValue();

        LocalDateTime earliestExpectedTime =
                beforeExecution.minusMinutes(
                        CommonConst.PROCESSING_TIMEOUT_MINUTES
                );

        LocalDateTime latestExpectedTime =
                afterExecution.minusMinutes(
                        CommonConst.PROCESSING_TIMEOUT_MINUTES
                );

        assertThat(actualCutoffTime)
                .isBetween(earliestExpectedTime, latestExpectedTime);
    }

    @Test
    @DisplayName("복구된 처리 내역이 있으면 정상 종료한다")
    void recoverTimeoutProcessing_복구_대상이_있으면_정상_종료한다() {
        // given
        when(processedNotificationRetryRepository.recoverTimeoutProcessing(
                any(LocalDateTime.class)
        )).thenReturn(3);

        // when
        processedNotificationRetryScheduler.recoverTimeoutProcessing();

        // then
        verify(processedNotificationRetryRepository)
                .recoverTimeoutProcessing(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("복구된 처리 내역이 없으면 정상 종료한다")
    void recoverTimeoutProcessing_복구_대상이_없으면_정상_종료한다() {
        // given
        when(processedNotificationRetryRepository.recoverTimeoutProcessing(
                any(LocalDateTime.class)
        )).thenReturn(0);

        // when
        processedNotificationRetryScheduler.recoverTimeoutProcessing();

        // then
        verify(processedNotificationRetryRepository)
                .recoverTimeoutProcessing(any(LocalDateTime.class));

        verifyNoMoreInteractions(processedNotificationRetryRepository);
    }
}