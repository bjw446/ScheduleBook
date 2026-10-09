package com.example.schedulebook.domain.notificationretry.service;

import com.example.schedulebook.domain.notificationretry.entity.ProcessedNotificationRetry;
import com.example.schedulebook.domain.notificationretry.enums.ProcessedNotificationRetryStatus;
import com.example.schedulebook.domain.notificationretry.repository.ProcessedNotificationRetryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessedNotificationRetryCreateServiceTest {

    private static final Long OUTBOX_ID = 1L;
    private static final Long RECEIVER_ID = 2L;
    private static final String OWNER = "notification-worker";

    @Mock
    private ProcessedNotificationRetryRepository processedNotificationRetryRepository;

    @InjectMocks
    private ProcessedNotificationRetryCreateService processedNotificationRetryCreateService;

    @Test
    void create_처리_생성에_성공하면_초기_상태로_저장하고_저장된_엔티티를_반환한다() {
        // given
        ProcessedNotificationRetry savedNotificationRetry =
                ProcessedNotificationRetry.create(OUTBOX_ID, RECEIVER_ID, OWNER);

        when(processedNotificationRetryRepository.save(any(ProcessedNotificationRetry.class)))
                .thenReturn(savedNotificationRetry);

        // when
        ProcessedNotificationRetry result =
                processedNotificationRetryCreateService.create(OUTBOX_ID, RECEIVER_ID, OWNER);

        // then
        ArgumentCaptor<ProcessedNotificationRetry> captor =
                ArgumentCaptor.forClass(ProcessedNotificationRetry.class);

        verify(processedNotificationRetryRepository).save(captor.capture());

        ProcessedNotificationRetry capturedNotificationRetry = captor.getValue();

        assertThat(capturedNotificationRetry.getOutboxId()).isEqualTo(OUTBOX_ID);
        assertThat(capturedNotificationRetry.getReceiverId()).isEqualTo(RECEIVER_ID);
        assertThat(capturedNotificationRetry.getProcessingOwner()).isEqualTo(OWNER);
        assertThat(capturedNotificationRetry.getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.PROCESSING);

        assertThat(result).isSameAs(savedNotificationRetry);
    }

    @Test
    void create_저장에_실패하면_예외를_전달한다() {
        // given
        RuntimeException exception = new RuntimeException("처리 저장 실패");

        when(processedNotificationRetryRepository.save(any(ProcessedNotificationRetry.class)))
                .thenThrow(exception);

        // when & then
        assertThatThrownBy(() ->
                processedNotificationRetryCreateService.create(OUTBOX_ID, RECEIVER_ID, OWNER)
        )
                .isSameAs(exception);

        verify(processedNotificationRetryRepository)
                .save(any(ProcessedNotificationRetry.class));
    }
}