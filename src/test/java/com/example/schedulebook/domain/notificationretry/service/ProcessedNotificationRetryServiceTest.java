package com.example.schedulebook.domain.notificationretry.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notificationretry.entity.NotificationRetry;
import com.example.schedulebook.domain.notificationretry.entity.ProcessedNotificationRetry;
import com.example.schedulebook.domain.notificationretry.enums.ProcessedNotificationRetryStatus;
import com.example.schedulebook.domain.notificationretry.repository.ProcessedNotificationRetryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessedNotificationRetryService 테스트")
class ProcessedNotificationRetryServiceTest {

    private static final Long OUTBOX_ID = 1L;
    private static final Long RECEIVER_ID = 2L;
    private static final String OWNER = "claim-token-123";
    private static final String OTHER_OWNER = "claim-token-456";

    @Mock
    private ProcessedNotificationRetryRepository processedNotificationRetryRepository;

    @Mock
    private ProcessedNotificationRetryCreateService processedNotificationRetryCreateService;

    @InjectMocks
    private ProcessedNotificationRetryService processedNotificationRetryService;

    private NotificationRetry notificationRetry;

    @BeforeEach
    void setUp() {
        notificationRetry = NotificationRetry.create(
                "event-1",
                OUTBOX_ID,
                RECEIVER_ID,
                NotificationType.values()[0],
                "{}",
                "알림 처리 실패"
        );

        setClaimToken(notificationRetry, OWNER);
    }

    @Test
    @DisplayName("처리 내역이 존재하면 해당 엔티티를 반환한다")
    void findByOutboxIdAndReceiverId_처리_내역이_존재하면_엔티티를_반환한다() {
        // given
        ProcessedNotificationRetry expected =
                ProcessedNotificationRetry.create(OUTBOX_ID, RECEIVER_ID, OWNER);

        when(processedNotificationRetryRepository.findByOutboxIdAndReceiverId(
                OUTBOX_ID, RECEIVER_ID
        )).thenReturn(Optional.of(expected));

        // when
        ProcessedNotificationRetry result =
                processedNotificationRetryService.findByOutboxIdAndReceiverId(
                        OUTBOX_ID, RECEIVER_ID
                );

        // then
        assertThat(result).isSameAs(expected);

        verify(processedNotificationRetryRepository)
                .findByOutboxIdAndReceiverId(OUTBOX_ID, RECEIVER_ID);
    }

    @Test
    @DisplayName("처리 내역이 존재하지 않으면 null을 반환한다")
    void findByOutboxIdAndReceiverId_처리_내역이_없으면_null을_반환한다() {
        // given
        when(processedNotificationRetryRepository.findByOutboxIdAndReceiverId(
                OUTBOX_ID, RECEIVER_ID
        )).thenReturn(Optional.empty());

        // when
        ProcessedNotificationRetry result =
                processedNotificationRetryService.findByOutboxIdAndReceiverId(
                        OUTBOX_ID, RECEIVER_ID
                );

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("실패 상태 변경에 성공하면 정상 종료한다")
    void markFailed_상태_변경에_성공하면_정상_종료한다() {
        // given
        when(processedNotificationRetryRepository.markFailed(
                OUTBOX_ID, RECEIVER_ID, OWNER
        )).thenReturn(1);

        // when & then
        assertThatCode(() ->
                processedNotificationRetryService.markFailed(
                        OUTBOX_ID, RECEIVER_ID, OWNER
                )
        ).doesNotThrowAnyException();

        verify(processedNotificationRetryRepository)
                .markFailed(OUTBOX_ID, RECEIVER_ID, OWNER);
    }

    @Test
    @DisplayName("실패 상태 변경 건수가 1이 아니면 상태 변경 실패 예외가 발생한다")
    void markFailed_상태_변경_건수가_1이_아니면_예외가_발생한다() {
        // given
        when(processedNotificationRetryRepository.markFailed(
                OUTBOX_ID, RECEIVER_ID, OWNER
        )).thenReturn(0);

        // when & then
        assertThatThrownBy(() ->
                processedNotificationRetryService.markFailed(
                        OUTBOX_ID, RECEIVER_ID, OWNER
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.PROCESSED_NOTIFICATION_RETRY_STATUS_CHANGE_FAILED);

        verify(processedNotificationRetryRepository)
                .markFailed(OUTBOX_ID, RECEIVER_ID, OWNER);
    }

    @Test
    @DisplayName("성공 상태 변경에 성공하면 정상 종료한다")
    void markSuccess_상태_변경에_성공하면_정상_종료한다() {
        // given
        when(processedNotificationRetryRepository.markSuccess(
                OUTBOX_ID, RECEIVER_ID, OWNER
        )).thenReturn(1);

        // when & then
        assertThatCode(() ->
                processedNotificationRetryService.markSuccess(
                        OUTBOX_ID, RECEIVER_ID, OWNER
                )
        ).doesNotThrowAnyException();

        verify(processedNotificationRetryRepository)
                .markSuccess(OUTBOX_ID, RECEIVER_ID, OWNER);
    }

    @Test
    @DisplayName("성공 상태 변경 건수가 1이 아니면 상태 변경 실패 예외가 발생한다")
    void markSuccess_상태_변경_건수가_1이_아니면_예외가_발생한다() {
        // given
        when(processedNotificationRetryRepository.markSuccess(
                OUTBOX_ID, RECEIVER_ID, OWNER
        )).thenReturn(0);

        // when & then
        assertThatThrownBy(() ->
                processedNotificationRetryService.markSuccess(
                        OUTBOX_ID, RECEIVER_ID, OWNER
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.PROCESSED_NOTIFICATION_RETRY_STATUS_CHANGE_FAILED);

        verify(processedNotificationRetryRepository)
                .markSuccess(OUTBOX_ID, RECEIVER_ID, OWNER);
    }

    @Test
    @DisplayName("재시도 상태 변경에 성공하면 변경 건수를 반환한다")
    void markRetry_상태_변경에_성공하면_변경_건수를_반환한다() {
        // given
        when(processedNotificationRetryRepository.markRetry(
                OUTBOX_ID, RECEIVER_ID, OWNER
        )).thenReturn(1);

        // when
        int result = processedNotificationRetryService.markRetry(
                OUTBOX_ID, RECEIVER_ID, OWNER
        );

        // then
        assertThat(result).isEqualTo(1);

        verify(processedNotificationRetryRepository)
                .markRetry(OUTBOX_ID, RECEIVER_ID, OWNER);
    }

    @Test
    @DisplayName("재시도 상태 변경에 실패하면 실제 변경 건수를 반환한다")
    void markRetry_상태_변경에_실패하면_변경_건수를_반환한다() {
        // given
        when(processedNotificationRetryRepository.markRetry(
                OUTBOX_ID, RECEIVER_ID, OWNER
        )).thenReturn(0);

        // when
        int result = processedNotificationRetryService.markRetry(
                OUTBOX_ID, RECEIVER_ID, OWNER
        );

        // then
        assertThat(result).isZero();

        verify(processedNotificationRetryRepository)
                .markRetry(OUTBOX_ID, RECEIVER_ID, OWNER);
    }

    @Test
    @DisplayName("처리 내역이 없으면 새로 생성하고 처리를 준비한다")
    void prepareProcessedNotificationRetry_처리_내역이_없으면_새로_생성한다() {
        // given
        ProcessedNotificationRetry created =
                ProcessedNotificationRetry.create(OUTBOX_ID, RECEIVER_ID, OWNER);

        when(processedNotificationRetryRepository.findByOutboxIdAndReceiverId(
                OUTBOX_ID, RECEIVER_ID
        )).thenReturn(Optional.empty());

        when(processedNotificationRetryCreateService.create(
                OUTBOX_ID, RECEIVER_ID, OWNER
        )).thenReturn(created);

        // when
        boolean result =
                processedNotificationRetryService.prepareProcessedNotificationRetry(
                        notificationRetry
                );

        // then
        assertThat(result).isFalse();

        verify(processedNotificationRetryCreateService)
                .create(OUTBOX_ID, RECEIVER_ID, OWNER);

        verify(processedNotificationRetryRepository)
                .findByOutboxIdAndReceiverId(OUTBOX_ID, RECEIVER_ID);
    }

    @Test
    @DisplayName("처리 내역 생성 중 중복 데이터 예외가 발생하면 기존 내역을 재조회한다")
    void prepareProcessedNotificationRetry_중복_생성_예외가_발생하면_기존_내역을_재조회한다() {
        // given
        ProcessedNotificationRetry existing =
                ProcessedNotificationRetry.create(OUTBOX_ID, RECEIVER_ID, OWNER);

        when(processedNotificationRetryRepository.findByOutboxIdAndReceiverId(
                OUTBOX_ID, RECEIVER_ID
        )).thenReturn(Optional.empty(), Optional.of(existing));

        when(processedNotificationRetryCreateService.create(
                OUTBOX_ID, RECEIVER_ID, OWNER
        )).thenThrow(new DataIntegrityViolationException("중복 데이터"));

        // when
        boolean result =
                processedNotificationRetryService.prepareProcessedNotificationRetry(
                        notificationRetry
                );

        // then
        assertThat(result).isFalse();

        verify(processedNotificationRetryRepository, times(2))
                .findByOutboxIdAndReceiverId(OUTBOX_ID, RECEIVER_ID);
    }

    @Test
    @DisplayName("처리 내역을 찾을 수 없으면 처리 내역 미존재 예외가 발생한다")
    void prepareProcessedNotificationRetry_처리_내역을_찾을_수_없으면_예외가_발생한다() {
        // given
        when(processedNotificationRetryRepository.findByOutboxIdAndReceiverId(
                OUTBOX_ID, RECEIVER_ID
        )).thenReturn(Optional.empty());

        when(processedNotificationRetryCreateService.create(
                OUTBOX_ID, RECEIVER_ID, OWNER
        )).thenThrow(new DataIntegrityViolationException("중복 데이터"));

        // when & then
        assertThatThrownBy(() ->
                processedNotificationRetryService.prepareProcessedNotificationRetry(
                        notificationRetry
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.PROCESSED_NOTIFICATION_RETRY_NOT_FOUND);

        verify(processedNotificationRetryRepository, times(2))
                .findByOutboxIdAndReceiverId(OUTBOX_ID, RECEIVER_ID);
    }

    @Test
    @DisplayName("이미 성공한 처리 내역이면 true를 반환한다")
    void prepareProcessedNotificationRetry_이미_성공한_내역이면_true를_반환한다() {
        // given
        ProcessedNotificationRetry success =
                ProcessedNotificationRetry.create(OUTBOX_ID, RECEIVER_ID, OWNER);

        setStatus(success, ProcessedNotificationRetryStatus.SUCCESS);

        when(processedNotificationRetryRepository.findByOutboxIdAndReceiverId(
                OUTBOX_ID, RECEIVER_ID
        )).thenReturn(Optional.of(success));

        // when
        boolean result =
                processedNotificationRetryService.prepareProcessedNotificationRetry(
                        notificationRetry
                );

        // then
        assertThat(result).isTrue();

        verify(processedNotificationRetryRepository, never())
                .markRetry(any(), any(), any());

        verifyNoInteractions(processedNotificationRetryCreateService);
    }

    @Test
    @DisplayName("실패 상태의 처리 내역을 재시도 상태로 변경하면 false를 반환한다")
    void prepareProcessedNotificationRetry_실패_상태에서_재시도_변경에_성공하면_false를_반환한다() {
        // given
        ProcessedNotificationRetry failed =
                ProcessedNotificationRetry.create(OUTBOX_ID, RECEIVER_ID, OWNER);

        setStatus(failed, ProcessedNotificationRetryStatus.FAILED);

        when(processedNotificationRetryRepository.findByOutboxIdAndReceiverId(
                OUTBOX_ID, RECEIVER_ID
        )).thenReturn(Optional.of(failed));

        when(processedNotificationRetryRepository.markRetry(
                OUTBOX_ID, RECEIVER_ID, OWNER
        )).thenReturn(1);

        // when
        boolean result =
                processedNotificationRetryService.prepareProcessedNotificationRetry(
                        notificationRetry
                );

        // then
        assertThat(result).isFalse();

        verify(processedNotificationRetryRepository)
                .markRetry(OUTBOX_ID, RECEIVER_ID, OWNER);
    }

    @Test
    @DisplayName("실패 상태의 처리 내역을 재시도 상태로 변경하지 못하면 true를 반환한다")
    void prepareProcessedNotificationRetry_실패_상태에서_재시도_변경에_실패하면_true를_반환한다() {
        // given
        ProcessedNotificationRetry failed =
                ProcessedNotificationRetry.create(OUTBOX_ID, RECEIVER_ID, OWNER);

        setStatus(failed, ProcessedNotificationRetryStatus.FAILED);

        when(processedNotificationRetryRepository.findByOutboxIdAndReceiverId(
                OUTBOX_ID, RECEIVER_ID
        )).thenReturn(Optional.of(failed));

        when(processedNotificationRetryRepository.markRetry(
                OUTBOX_ID, RECEIVER_ID, OWNER
        )).thenReturn(0);

        // when
        boolean result =
                processedNotificationRetryService.prepareProcessedNotificationRetry(
                        notificationRetry
                );

        // then
        assertThat(result).isTrue();

        verify(processedNotificationRetryRepository)
                .markRetry(OUTBOX_ID, RECEIVER_ID, OWNER);
    }

    @Test
    @DisplayName("처리 중인 내역의 owner가 일치하면 false를 반환한다")
    void prepareProcessedNotificationRetry_처리_중인_내역의_owner가_일치하면_false를_반환한다() {
        // given
        ProcessedNotificationRetry processing =
                ProcessedNotificationRetry.create(OUTBOX_ID, RECEIVER_ID, OWNER);

        when(processedNotificationRetryRepository.findByOutboxIdAndReceiverId(
                OUTBOX_ID, RECEIVER_ID
        )).thenReturn(Optional.of(processing));

        // when
        boolean result =
                processedNotificationRetryService.prepareProcessedNotificationRetry(
                        notificationRetry
                );

        // then
        assertThat(result).isFalse();

        verify(processedNotificationRetryRepository, never())
                .markRetry(any(), any(), any());
    }

    @Test
    @DisplayName("처리 중인 내역의 owner가 일치하지 않으면 true를 반환한다")
    void prepareProcessedNotificationRetry_처리_중인_내역의_owner가_불일치하면_true를_반환한다() {
        // given
        ProcessedNotificationRetry processing =
                ProcessedNotificationRetry.create(
                        OUTBOX_ID, RECEIVER_ID, OTHER_OWNER
                );

        when(processedNotificationRetryRepository.findByOutboxIdAndReceiverId(
                OUTBOX_ID, RECEIVER_ID
        )).thenReturn(Optional.of(processing));

        // when
        boolean result =
                processedNotificationRetryService.prepareProcessedNotificationRetry(
                        notificationRetry
                );

        // then
        assertThat(result).isTrue();

        verify(processedNotificationRetryRepository, never())
                .markRetry(any(), any(), any());
    }

    private void setClaimToken(NotificationRetry target, String claimToken) {
        try {
            var field = NotificationRetry.class.getDeclaredField("claimToken");
            field.setAccessible(true);
            field.set(target, claimToken);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("테스트용 Claim Token 설정에 실패했습니다.", e);
        }
    }

    private void setStatus(
            ProcessedNotificationRetry target,
            ProcessedNotificationRetryStatus status
    ) {
        try {
            var field = ProcessedNotificationRetry.class.getDeclaredField("status");
            field.setAccessible(true);
            field.set(target, status);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("테스트용 상태 설정에 실패했습니다.", e);
        }
    }
}