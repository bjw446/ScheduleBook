package com.example.schedulebook.domain.deadletter.service;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.deadletter.entity.DeadLetterQueue;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import com.example.schedulebook.domain.deadletter.repository.DeadLetterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadLetterServiceTest {

    @Mock
    private DeadLetterRepository deadLetterRepository;

    private DeadLetterService deadLetterService;

    @BeforeEach
    void setUp() {
        deadLetterService = new DeadLetterService(deadLetterRepository);
    }

    @Test
    @DisplayName("Dead Letter 정보를 입력값과 일치하게 저장한다")
    void save_DeadLetter정보를_저장한다() {
        // given
        DeadLetterType deadLetterType = DeadLetterType.values()[0];
        DeadLetterSource deadLetterSource = DeadLetterSource.values()[0];
        DeadLetterAggregateType aggregateType = DeadLetterAggregateType.values()[0];

        String aggregateId = "aggregate-123";
        Long userId = 1L;
        String payload = "{\"eventId\":\"event-123\"}";
        String reason = "처리 실패";
        String exceptionType = "RuntimeException";
        int retryCount = 3;
        String eventId = "event-123";

        // when
        deadLetterService.save(
                deadLetterType,
                deadLetterSource,
                aggregateType,
                aggregateId,
                userId,
                payload,
                reason,
                exceptionType,
                retryCount,
                eventId
        );

        // then
        ArgumentCaptor<DeadLetterQueue> captor =
                ArgumentCaptor.forClass(DeadLetterQueue.class);

        verify(deadLetterRepository).save(captor.capture());

        DeadLetterQueue saved = captor.getValue();

        assertThat(saved.getDeadLetterType()).isEqualTo(deadLetterType);
        assertThat(saved.getDeadLetterSource()).isEqualTo(deadLetterSource);
        assertThat(saved.getDeadLetterAggregateType()).isEqualTo(aggregateType);
        assertThat(saved.getAggregateId()).isEqualTo(aggregateId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getPayload()).isEqualTo(payload);
        assertThat(saved.getReason()).isEqualTo(reason);
        assertThat(saved.getExceptionType()).isEqualTo(exceptionType);
        assertThat(saved.getRetryCount()).isEqualTo(retryCount);
        assertThat(saved.getEventId()).isEqualTo(eventId);
    }

    @Test
    @DisplayName("Recovered 상태로 변경되면 정상 종료한다")
    void markRecovered_상태변경에_성공하면_정상종료한다() {
        // given
        Long deadLetterId = 1L;
        String claimToken = "claim-token-123";

        when(deadLetterRepository.markRecovered(deadLetterId, claimToken))
                .thenReturn(1);

        // when & then
        assertThatCode(() -> deadLetterService.markRecovered(deadLetterId, claimToken))
                .doesNotThrowAnyException();

        verify(deadLetterRepository).markRecovered(deadLetterId, claimToken);
    }

    @Test
    @DisplayName("Recovered 상태 변경에 실패하면 예외가 발생한다")
    void markRecovered_상태변경에_실패하면_예외가_발생한다() {
        // given
        Long deadLetterId = 1L;
        String claimToken = "claim-token-123";

        when(deadLetterRepository.markRecovered(deadLetterId, claimToken))
                .thenReturn(0);

        // when & then
        assertThatThrownBy(() -> deadLetterService.markRecovered(deadLetterId, claimToken))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.DEAD_LETTER_RECOVER_FAILED.getMessage());

        verify(deadLetterRepository).markRecovered(deadLetterId, claimToken);
    }

    @Test
    @DisplayName("존재하는 Dead Letter를 Processing 상태로 변경하고 claim token과 lease를 설정한다")
    void markProcessing_상태변경에_성공하면_claimToken을_반환한다() {
        // given
        Long deadLetterId = 1L;

        when(deadLetterRepository.existsById(deadLetterId)).thenReturn(true);
        when(deadLetterRepository.markProcessing(
                eq(deadLetterId),
                any(String.class),
                any(LocalDateTime.class)
        )).thenReturn(1);

        LocalDateTime beforeCall = LocalDateTime.now();

        // when
        String claimToken = deadLetterService.markProcessing(deadLetterId);

        LocalDateTime afterCall = LocalDateTime.now();

        // then
        assertThat(claimToken).isNotBlank();

        ArgumentCaptor<String> tokenCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> leaseCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);

        verify(deadLetterRepository).markProcessing(
                eq(deadLetterId),
                tokenCaptor.capture(),
                leaseCaptor.capture()
        );

        assertThat(tokenCaptor.getValue()).isEqualTo(claimToken);

        LocalDateTime leaseUntil = leaseCaptor.getValue();

        assertThat(leaseUntil)
                .isAfterOrEqualTo(
                        beforeCall.plusMinutes(
                                CommonConst.DEAD_LETTER_PROCESSING_LEASE_MINUTES
                        )
                )
                .isBeforeOrEqualTo(
                        afterCall.plusMinutes(
                                CommonConst.DEAD_LETTER_PROCESSING_LEASE_MINUTES
                        ).plusSeconds(1)
                );

        verify(deadLetterRepository, never()).findById(deadLetterId);
    }

    @Test
    @DisplayName("존재하지 않는 Dead Letter를 Processing 상태로 변경하면 예외가 발생한다")
    void markProcessing_존재하지_않으면_예외가_발생한다() {
        // given
        Long deadLetterId = 1L;

        when(deadLetterRepository.existsById(deadLetterId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> deadLetterService.markProcessing(deadLetterId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.DEAD_LETTER_NOT_FOUND.getMessage());

        verify(deadLetterRepository, never()).markProcessing(
                eq(deadLetterId),
                any(String.class),
                any(LocalDateTime.class)
        );
        verify(deadLetterRepository, never()).findById(deadLetterId);
    }

    @Test
    @DisplayName("이미 Recovered 상태인 Dead Letter를 처리하면 예외가 발생한다")
    void markProcessing_이미Recovered상태이면_예외가_발생한다() {
        // given
        Long deadLetterId = 1L;

        DeadLetterQueue deadLetterQueue = mock(DeadLetterQueue.class);

        when(deadLetterRepository.existsById(deadLetterId)).thenReturn(true);
        when(deadLetterRepository.markProcessing(
                eq(deadLetterId),
                any(String.class),
                any(LocalDateTime.class)
        )).thenReturn(0);
        when(deadLetterRepository.findById(deadLetterId))
                .thenReturn(Optional.of(deadLetterQueue));
        when(deadLetterQueue.getDeadLetterStatus())
                .thenReturn(DeadLetterStatus.RECOVERED);

        // when & then
        assertThatThrownBy(() -> deadLetterService.markProcessing(deadLetterId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(
                        ErrorEnum.DEAD_LETTER_ALREADY_RECOVERED.getMessage()
                );

        verify(deadLetterRepository).findById(deadLetterId);
    }

    @Test
    @DisplayName("이미 Processing 상태인 Dead Letter를 처리하면 예외가 발생한다")
    void markProcessing_이미Processing상태이면_예외가_발생한다() {
        // given
        Long deadLetterId = 1L;

        DeadLetterQueue deadLetterQueue = mock(DeadLetterQueue.class);

        when(deadLetterRepository.existsById(deadLetterId)).thenReturn(true);
        when(deadLetterRepository.markProcessing(
                eq(deadLetterId),
                any(String.class),
                any(LocalDateTime.class)
        )).thenReturn(0);
        when(deadLetterRepository.findById(deadLetterId))
                .thenReturn(Optional.of(deadLetterQueue));
        when(deadLetterQueue.getDeadLetterStatus())
                .thenReturn(DeadLetterStatus.PROCESSING);

        // when & then
        assertThatThrownBy(() -> deadLetterService.markProcessing(deadLetterId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(
                        ErrorEnum.DEAD_LETTER_ALREADY_PROCESSING.getMessage()
                );

        verify(deadLetterRepository).findById(deadLetterId);
    }

    @Test
    @DisplayName("Processing 상태 변경에 실패하고 조회된 상태가 다른 경우 claim token을 반환한다")
    void markProcessing_상태변경에_실패하고_다른상태이면_claimToken을_반환한다() {
        // given
        Long deadLetterId = 1L;

        DeadLetterQueue deadLetterQueue = mock(DeadLetterQueue.class);

        when(deadLetterRepository.existsById(deadLetterId)).thenReturn(true);
        when(deadLetterRepository.markProcessing(
                eq(deadLetterId),
                any(String.class),
                any(LocalDateTime.class)
        )).thenReturn(0);
        when(deadLetterRepository.findById(deadLetterId))
                .thenReturn(Optional.of(deadLetterQueue));
        when(deadLetterQueue.getDeadLetterStatus())
                .thenReturn(DeadLetterStatus.PENDING);

        // when
        String claimToken = deadLetterService.markProcessing(deadLetterId);

        // then
        assertThat(claimToken).isNotBlank();
    }

    @Test
    @DisplayName("Processing 상태 변경에 실패하고 조회 상태가 PENDING이면 claim token을 반환한다")
    void markProcessing_상태변경에_실패하고_Pending상태이면_claimToken을_반환한다() {
        // given
        Long deadLetterId = 1L;

        DeadLetterQueue deadLetterQueue = mock(DeadLetterQueue.class);

        when(deadLetterRepository.existsById(deadLetterId))
                .thenReturn(true);

        when(deadLetterRepository.markProcessing(
                eq(deadLetterId),
                any(String.class),
                any(LocalDateTime.class)
        )).thenReturn(0);

        when(deadLetterRepository.findById(deadLetterId))
                .thenReturn(Optional.of(deadLetterQueue));

        when(deadLetterQueue.getDeadLetterStatus())
                .thenReturn(DeadLetterStatus.PENDING);

        // when
        String claimToken = deadLetterService.markProcessing(deadLetterId);

        // then
        assertThat(claimToken).isNotBlank();

        verify(deadLetterRepository).markProcessing(
                eq(deadLetterId),
                any(String.class),
                any(LocalDateTime.class)
        );

        verify(deadLetterRepository).findById(deadLetterId);
    }

    @Test
    @DisplayName("Pending 상태로 변경되면 정상 종료한다")
    void markPending_상태변경에_성공하면_정상종료한다() {
        // given
        Long deadLetterId = 1L;
        String claimToken = "claim-token-123";

        when(deadLetterRepository.markPending(deadLetterId, claimToken))
                .thenReturn(1);

        // when & then
        assertThatCode(() -> deadLetterService.markPending(deadLetterId, claimToken))
                .doesNotThrowAnyException();

        verify(deadLetterRepository).markPending(deadLetterId, claimToken);
    }

    @Test
    @DisplayName("Pending 상태 변경에 실패하면 예외가 발생한다")
    void markPending_상태변경에_실패하면_예외가_발생한다() {
        // given
        Long deadLetterId = 1L;
        String claimToken = "claim-token-123";

        when(deadLetterRepository.markPending(deadLetterId, claimToken))
                .thenReturn(0);

        // when & then
        assertThatThrownBy(() -> deadLetterService.markPending(deadLetterId, claimToken))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.DEAD_LETTER_RECOVER_FAILED.getMessage());

        verify(deadLetterRepository).markPending(deadLetterId, claimToken);
    }

    @Test
    @DisplayName("만료된 Processing lease 회수를 Repository에 위임하고 처리 건수를 반환한다")
    void reclaimExpiredProcessing_회수건수를_반환한다() {
        // given
        int reclaimedCount = 3;

        when(deadLetterRepository.reclaimExpiredProcessing())
                .thenReturn(reclaimedCount);

        // when
        int result = deadLetterService.reclaimExpiredProcessing();

        // then
        assertThat(result).isEqualTo(reclaimedCount);
        verify(deadLetterRepository).reclaimExpiredProcessing();
    }

    @Test
    @DisplayName("Lease 갱신에 성공하면 설정된 기간으로 갱신한다")
    void renewLease_갱신에_성공하면_정상종료한다() {
        // given
        Long deadLetterId = 1L;
        String claimToken = "claim-token-123";

        when(deadLetterRepository.renewLease(
                eq(deadLetterId),
                eq(claimToken),
                any(LocalDateTime.class)
        )).thenReturn(1);

        LocalDateTime beforeCall = LocalDateTime.now();

        // when
        deadLetterService.renewLease(deadLetterId, claimToken);

        LocalDateTime afterCall = LocalDateTime.now();

        // then
        ArgumentCaptor<LocalDateTime> leaseCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);

        verify(deadLetterRepository).renewLease(
                eq(deadLetterId),
                eq(claimToken),
                leaseCaptor.capture()
        );

        LocalDateTime leaseUntil = leaseCaptor.getValue();

        assertThat(leaseUntil)
                .isAfterOrEqualTo(
                        beforeCall.plusMinutes(
                                CommonConst.DEAD_LETTER_PROCESSING_LEASE_MINUTES
                        )
                )
                .isBeforeOrEqualTo(
                        afterCall.plusMinutes(
                                CommonConst.DEAD_LETTER_PROCESSING_LEASE_MINUTES
                        ).plusSeconds(1)
                );
    }

    @Test
    @DisplayName("Lease 갱신에 실패하면 예외가 발생한다")
    void renewLease_갱신에_실패하면_예외가_발생한다() {
        // given
        Long deadLetterId = 1L;
        String claimToken = "claim-token-123";

        when(deadLetterRepository.renewLease(
                eq(deadLetterId),
                eq(claimToken),
                any(LocalDateTime.class)
        )).thenReturn(0);

        // when & then
        assertThatThrownBy(() -> deadLetterService.renewLease(deadLetterId, claimToken))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.DEAD_LETTER_RECOVER_FAILED.getMessage());
    }
}