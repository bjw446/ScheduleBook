package com.example.schedulebook.domain.notificationretry.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notificationretry.entity.NotificationRetry;
import com.example.schedulebook.domain.notificationretry.enums.NotificationRetryStatus;
import com.example.schedulebook.domain.notificationretry.repository.NotificationRetryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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
class NotificationRetryServiceTest {

    private static final Long NOTIFICATION_RETRY_ID = 1L;
    private static final Long OUTBOX_ID = 10L;
    private static final Long RECEIVER_ID = 20L;

    private static final String EVENT_ID = "event-1";
    private static final String CLAIM_TOKEN = "claim-token";
    private static final String REASON = "알림 전송 실패";
    private static final String PAYLOAD = "{\"message\":\"test\"}";

    @Mock
    private NotificationRetryRepository notificationRetryRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationRetryService notificationRetryService;

    @Test
    void save_이벤트를_JSON으로_직렬화하고_NotificationRetry를_저장한다() throws Exception {
        // given
        Object event = new Object();

        when(objectMapper.writeValueAsString(event))
                .thenReturn(PAYLOAD);

        ArgumentCaptor<NotificationRetry> captor =
                ArgumentCaptor.forClass(NotificationRetry.class);

        // when
        notificationRetryService.save(
                EVENT_ID,
                OUTBOX_ID,
                RECEIVER_ID,
                NotificationType.SCHEDULE_COMMENT,
                event,
                REASON
        );

        // then
        verify(objectMapper)
                .writeValueAsString(event);

        verify(notificationRetryRepository)
                .save(captor.capture());

        NotificationRetry result = captor.getValue();

        assertThat(result.getEventId())
                .isEqualTo(EVENT_ID);
        assertThat(result.getOutboxId())
                .isEqualTo(OUTBOX_ID);
        assertThat(result.getReceiverId())
                .isEqualTo(RECEIVER_ID);
        assertThat(result.getNotificationType())
                .isEqualTo(NotificationType.SCHEDULE_COMMENT);
        assertThat(result.getPayload())
                .isEqualTo(PAYLOAD);
        assertThat(result.getRetryCount())
                .isZero();
        assertThat(result.getNotificationRetryStatus())
                .isEqualTo(NotificationRetryStatus.PENDING);
        assertThat(result.getReason())
                .isEqualTo(REASON);
        assertThat(result.getNextRetryAt())
                .isAfter(LocalDateTime.now());
    }

    @Test
    void save_이벤트_JSON_직렬화에_실패하면_JSON_SERIALIZATION_FAILED_예외를_발생시킨다() throws Exception {
        // given
        Object event = new Object();

        when(objectMapper.writeValueAsString(event))
                .thenThrow(new JsonProcessingException("직렬화 실패") {
                });

        // when & then
        assertThatThrownBy(() ->
                notificationRetryService.save(
                        EVENT_ID,
                        OUTBOX_ID,
                        RECEIVER_ID,
                        NotificationType.SCHEDULE_COMMENT,
                        event,
                        REASON
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.JSON_SERIALIZATION_FAILED);

        verify(objectMapper)
                .writeValueAsString(event);

        verifyNoInteractions(notificationRetryRepository);
    }

    @Test
    void markProcessing_재시도_처리_대상으로_선점되면_claimToken을_반환한다() {
        // given
        when(notificationRetryRepository.markProcessing(
                eq(NOTIFICATION_RETRY_ID),
                any(String.class),
                any(LocalDateTime.class)
        )).thenReturn(1);

        // when
        String claimToken =
                notificationRetryService.markProcessing(NOTIFICATION_RETRY_ID);

        // then
        assertThat(claimToken)
                .isNotNull()
                .isNotBlank();

        verify(notificationRetryRepository)
                .markProcessing(
                        eq(NOTIFICATION_RETRY_ID),
                        eq(claimToken),
                        any(LocalDateTime.class)
                );
    }

    @Test
    void markProcessing_이미_다른_작업이_선점했으면_null을_반환한다() {
        // given
        when(notificationRetryRepository.markProcessing(
                eq(NOTIFICATION_RETRY_ID),
                any(String.class),
                any(LocalDateTime.class)
        )).thenReturn(0);

        // when
        String claimToken =
                notificationRetryService.markProcessing(NOTIFICATION_RETRY_ID);

        // then
        assertThat(claimToken)
                .isNull();

        verify(notificationRetryRepository)
                .markProcessing(
                        eq(NOTIFICATION_RETRY_ID),
                        any(String.class),
                        any(LocalDateTime.class)
                );
    }

    @Test
    void markSuccess_정상적으로_처리된_재시도_건을_성공_상태로_변경한다() {
        // given
        when(notificationRetryRepository.markSuccess(
                NOTIFICATION_RETRY_ID,
                CLAIM_TOKEN
        )).thenReturn(1);

        // when
        notificationRetryService.markSuccess(
                NOTIFICATION_RETRY_ID,
                CLAIM_TOKEN
        );

        // then
        verify(notificationRetryRepository)
                .markSuccess(
                        NOTIFICATION_RETRY_ID,
                        CLAIM_TOKEN
                );
    }

    @Test
    void markSuccess_claimToken이_일치하지_않으면_예외를_발생시킨다() {
        // given
        when(notificationRetryRepository.markSuccess(
                NOTIFICATION_RETRY_ID,
                CLAIM_TOKEN
        )).thenReturn(0);

        // when & then
        assertThatThrownBy(() ->
                notificationRetryService.markSuccess(
                        NOTIFICATION_RETRY_ID,
                        CLAIM_TOKEN
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.NOTIFICATION_RETRY_NOT_FOUND);

        verify(notificationRetryRepository)
                .markSuccess(
                        NOTIFICATION_RETRY_ID,
                        CLAIM_TOKEN
                );
    }

    @Test
    void markFailed_재시도_처리가_실패하면_FAILED_상태로_변경한다() {
        // given
        when(notificationRetryRepository.markFailed(
                NOTIFICATION_RETRY_ID,
                REASON,
                CLAIM_TOKEN
        )).thenReturn(1);

        // when
        notificationRetryService.markFailed(
                NOTIFICATION_RETRY_ID,
                REASON,
                CLAIM_TOKEN
        );

        // then
        verify(notificationRetryRepository)
                .markFailed(
                        NOTIFICATION_RETRY_ID,
                        REASON,
                        CLAIM_TOKEN
                );
    }

    @Test
    void markFailed_claimToken이_일치하지_않으면_예외를_발생시킨다() {
        // given
        when(notificationRetryRepository.markFailed(
                NOTIFICATION_RETRY_ID,
                REASON,
                CLAIM_TOKEN
        )).thenReturn(0);

        // when & then
        assertThatThrownBy(() ->
                notificationRetryService.markFailed(
                        NOTIFICATION_RETRY_ID,
                        REASON,
                        CLAIM_TOKEN
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.NOTIFICATION_RETRY_NOT_FOUND);

        verify(notificationRetryRepository)
                .markFailed(
                        NOTIFICATION_RETRY_ID,
                        REASON,
                        CLAIM_TOKEN
                );
    }

    @Test
    void markRetry_재시도_상태로_변경하고_다음_재시도_시간을_저장한다() {
        // given
        int retryCount = 1;

        when(notificationRetryRepository.markRetry(
                eq(NOTIFICATION_RETRY_ID),
                eq(REASON),
                any(LocalDateTime.class),
                eq(CLAIM_TOKEN)
        )).thenReturn(1);

        // when
        notificationRetryService.markRetry(
                NOTIFICATION_RETRY_ID,
                REASON,
                retryCount,
                CLAIM_TOKEN
        );

        // then
        ArgumentCaptor<LocalDateTime> delayCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);

        verify(notificationRetryRepository)
                .markRetry(
                        eq(NOTIFICATION_RETRY_ID),
                        eq(REASON),
                        delayCaptor.capture(),
                        eq(CLAIM_TOKEN)
                );

        assertThat(delayCaptor.getValue())
                .isAfter(LocalDateTime.now());
    }

    @Test
    void markRetry_claimToken이_일치하지_않으면_예외를_발생시킨다() {
        // given
        when(notificationRetryRepository.markRetry(
                eq(NOTIFICATION_RETRY_ID),
                eq(REASON),
                any(LocalDateTime.class),
                eq(CLAIM_TOKEN)
        )).thenReturn(0);

        // when & then
        assertThatThrownBy(() ->
                notificationRetryService.markRetry(
                        NOTIFICATION_RETRY_ID,
                        REASON,
                        1,
                        CLAIM_TOKEN
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.NOTIFICATION_RETRY_NOT_FOUND);

        verify(notificationRetryRepository)
                .markRetry(
                        eq(NOTIFICATION_RETRY_ID),
                        eq(REASON),
                        any(LocalDateTime.class),
                        eq(CLAIM_TOKEN)
                );
    }

    @Test
    void findRetryTargets_재시도_대상_목록을_조회한다() {
        // given
        int size = 10;

        Page<NotificationRetry> page =
                new PageImpl<>(List.of());

        when(notificationRetryRepository.findRetryTargets(
                any(LocalDateTime.class),
                eq(PageRequest.of(0, size))
        )).thenReturn(page);

        // when
        List<NotificationRetry> result =
                notificationRetryService.findRetryTargets(size);

        // then
        assertThat(result)
                .isEmpty();

        verify(notificationRetryRepository)
                .findRetryTargets(
                        any(LocalDateTime.class),
                        eq(PageRequest.of(0, size))
                );
    }

    @Test
    void findById_존재하는_재시도_건을_반환한다() {
        // given
        NotificationRetry notificationRetry =
                NotificationRetry.create(
                        EVENT_ID,
                        OUTBOX_ID,
                        RECEIVER_ID,
                        NotificationType.SCHEDULE_COMMENT,
                        PAYLOAD,
                        REASON
                );

        when(notificationRetryRepository.findById(NOTIFICATION_RETRY_ID))
                .thenReturn(Optional.of(notificationRetry));

        // when
        NotificationRetry result =
                notificationRetryService.findById(NOTIFICATION_RETRY_ID);

        // then
        assertThat(result)
                .isSameAs(notificationRetry);

        verify(notificationRetryRepository)
                .findById(NOTIFICATION_RETRY_ID);
    }

    @Test
    void findById_존재하지_않는_재시도_건이면_예외를_발생시킨다() {
        // given
        when(notificationRetryRepository.findById(NOTIFICATION_RETRY_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                notificationRetryService.findById(NOTIFICATION_RETRY_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.NOTIFICATION_RETRY_NOT_FOUND);

        verify(notificationRetryRepository)
                .findById(NOTIFICATION_RETRY_ID);
    }

    @Test
    void recover_PENDING_상태이면_이미_복구된_것으로_처리하고_종료한다() {
        // given
        NotificationRetry notificationRetry =
                NotificationRetry.create(
                        EVENT_ID,
                        OUTBOX_ID,
                        RECEIVER_ID,
                        NotificationType.SCHEDULE_COMMENT,
                        PAYLOAD,
                        REASON
                );

        when(notificationRetryRepository.findById(NOTIFICATION_RETRY_ID))
                .thenReturn(Optional.of(notificationRetry));

        // when
        notificationRetryService.recover(NOTIFICATION_RETRY_ID);

        // then
        verify(notificationRetryRepository)
                .findById(NOTIFICATION_RETRY_ID);

        verify(notificationRetryRepository, never())
                .updateRecover(NOTIFICATION_RETRY_ID);
    }

    @Test
    void recover_FAILED_상태이면_PENDING_상태로_복구한다() {
        // given
        NotificationRetry notificationRetry =
                org.mockito.Mockito.mock(NotificationRetry.class);

        when(notificationRetry.getNotificationRetryStatus())
                .thenReturn(NotificationRetryStatus.FAILED);

        when(notificationRetryRepository.findById(NOTIFICATION_RETRY_ID))
                .thenReturn(Optional.of(notificationRetry));

        when(notificationRetryRepository.updateRecover(NOTIFICATION_RETRY_ID))
                .thenReturn(1);

        // when
        notificationRetryService.recover(NOTIFICATION_RETRY_ID);

        // then
        verify(notificationRetryRepository)
                .findById(NOTIFICATION_RETRY_ID);

        verify(notificationRetryRepository)
                .updateRecover(NOTIFICATION_RETRY_ID);
    }

    @Test
    void recover_복구할_수_없는_상태이면_예외를_발생시킨다() {
        // given
        NotificationRetry notificationRetry =
                org.mockito.Mockito.mock(NotificationRetry.class);

        when(notificationRetry.getNotificationRetryStatus())
                .thenReturn(NotificationRetryStatus.PROCESSING);

        when(notificationRetryRepository.findById(NOTIFICATION_RETRY_ID))
                .thenReturn(Optional.of(notificationRetry));

        // when & then
        assertThatThrownBy(() ->
                notificationRetryService.recover(NOTIFICATION_RETRY_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.DEAD_LETTER_RECOVER_FAILED);

        verify(notificationRetryRepository)
                .findById(NOTIFICATION_RETRY_ID);

        verify(notificationRetryRepository, never())
                .updateRecover(NOTIFICATION_RETRY_ID);
    }

    @Test
    void recover_복구_업데이트에_실패하면_예외를_발생시킨다() {
        // given
        NotificationRetry notificationRetry =
                org.mockito.Mockito.mock(NotificationRetry.class);

        when(notificationRetry.getNotificationRetryStatus())
                .thenReturn(NotificationRetryStatus.FAILED);

        when(notificationRetryRepository.findById(NOTIFICATION_RETRY_ID))
                .thenReturn(Optional.of(notificationRetry));

        when(notificationRetryRepository.updateRecover(NOTIFICATION_RETRY_ID))
                .thenReturn(0);

        // when & then
        assertThatThrownBy(() ->
                notificationRetryService.recover(NOTIFICATION_RETRY_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.DEAD_LETTER_RECOVER_FAILED);

        verify(notificationRetryRepository)
                .updateRecover(NOTIFICATION_RETRY_ID);
    }

    @Test
    void recover_존재하지_않는_재시도_건이면_예외를_발생시킨다() {
        // given
        when(notificationRetryRepository.findById(NOTIFICATION_RETRY_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                notificationRetryService.recover(NOTIFICATION_RETRY_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.NOTIFICATION_RETRY_NOT_FOUND);

        verify(notificationRetryRepository)
                .findById(NOTIFICATION_RETRY_ID);

        verify(notificationRetryRepository, never())
                .updateRecover(NOTIFICATION_RETRY_ID);
    }
}