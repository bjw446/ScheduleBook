package com.example.schedulebook.domain.admin.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.response.PageResponse;
import com.example.schedulebook.domain.admin.dto.response.DeadLetterDetailResponse;
import com.example.schedulebook.domain.admin.dto.response.DeadLetterSummaryResponse;
import com.example.schedulebook.domain.auth.service.ForceLogoutRetryService;
import com.example.schedulebook.domain.deadletter.entity.DeadLetterQueue;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import com.example.schedulebook.domain.deadletter.repository.DeadLetterRepository;
import com.example.schedulebook.domain.deadletter.service.DeadLetterDeserializationRecoveryService;
import com.example.schedulebook.domain.deadletter.service.DeadLetterService;
import com.example.schedulebook.domain.notificationretry.service.NotificationRetryService;
import com.example.schedulebook.domain.outbox.service.OutboxTransactionService;
import com.example.schedulebook.domain.user.validator.UserValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDeadLetterServiceTest {

    @Mock
    private DeadLetterRepository deadLetterRepository;

    @Mock
    private UserValidator userValidator;

    @Mock
    private ForceLogoutRetryService forceLogoutRetryService;

    @Mock
    private OutboxTransactionService outboxTransactionService;

    @Mock
    private DeadLetterService deadLetterService;

    @Mock
    private DeadLetterDeserializationRecoveryService deadLetterDeserializationRecoveryService;

    @Mock
    private NotificationRetryService notificationRetryService;

    @InjectMocks
    private AdminDeadLetterService adminDeadLetterService;

    private static final Long ADMIN_ID = 1L;
    private static final Long DEAD_LETTER_ID = 100L;
    private static final String CLAIM_TOKEN = "claim-token";

    @Test
    void 전체_DeadLetter를_조회하면_페이지_응답을_반환한다() {

        // given
        DeadLetterQueue deadLetterQueue = createDeadLetter(
                DeadLetterAggregateType.SESSION,
                "session-123"
        );

        Page<DeadLetterQueue> page =
                new PageImpl<>(List.of(deadLetterQueue));

        Pageable pageable = PageRequest.of(0, 10);

        when(deadLetterRepository.findAll(pageable))
                .thenReturn(page);

        // when
        PageResponse<DeadLetterSummaryResponse> result =
                adminDeadLetterService.findAllDeadLetters(
                        ADMIN_ID,
                        pageable
                );

        // then
        assertThat(result).isNotNull();

        verify(userValidator)
                .validateActiveAdmin(ADMIN_ID);

        verify(deadLetterRepository)
                .findAll(pageable);
    }

    @Test
    void 관리자_권한_검증에_실패하면_DeadLetter를_조회하지_않는다() {

        // given
        BaseException exception =
                new BaseException(ErrorEnum.ADMIN_NOT_FOUND);

        doThrow(exception)
                .when(userValidator)
                .validateActiveAdmin(ADMIN_ID);

        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() ->
                adminDeadLetterService.findAllDeadLetters(
                        ADMIN_ID,
                        pageable
                )
        )
                .isSameAs(exception);

        verify(userValidator)
                .validateActiveAdmin(ADMIN_ID);

        verify(deadLetterRepository, never())
                .findAll(any(Pageable.class));
    }

    @Test
    void 단건_DeadLetter를_조회하면_상세_응답을_반환한다() {

        // given
        DeadLetterQueue deadLetterQueue = createDeadLetter(
                DeadLetterAggregateType.SESSION,
                "session-123"
        );

        ReflectionTestUtils.setField(
                deadLetterQueue,
                "id",
                DEAD_LETTER_ID
        );

        when(deadLetterRepository.findById(DEAD_LETTER_ID))
                .thenReturn(Optional.of(deadLetterQueue));

        // when
        DeadLetterDetailResponse result =
                adminDeadLetterService.findOneDeadLetter(
                        ADMIN_ID,
                        DEAD_LETTER_ID
                );

        // then
        assertThat(result).isNotNull();

        verify(userValidator)
                .validateActiveAdmin(ADMIN_ID);

        verify(deadLetterRepository)
                .findById(DEAD_LETTER_ID);
    }

    @Test
    void 존재하지_않는_DeadLetter를_조회하면_예외가_발생한다() {

        // given
        when(deadLetterRepository.findById(DEAD_LETTER_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                adminDeadLetterService.findOneDeadLetter(
                        ADMIN_ID,
                        DEAD_LETTER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.DEAD_LETTER_NOT_FOUND);

        verify(userValidator)
                .validateActiveAdmin(ADMIN_ID);

        verify(deadLetterRepository)
                .findById(DEAD_LETTER_ID);
    }

    @Test
    void SESSION_타입이면_강제_로그아웃_복구를_위임한다() {

        // given
        DeadLetterQueue deadLetterQueue = createDeadLetter(
                DeadLetterAggregateType.SESSION,
                "session-123"
        );

        givenProcessingDeadLetter(deadLetterQueue);

        // when
        adminDeadLetterService.recoverDeadLetter(
                ADMIN_ID,
                DEAD_LETTER_ID
        );

        // then
        InOrder inOrder = org.mockito.Mockito.inOrder(
                deadLetterService,
                forceLogoutRetryService
        );

        inOrder.verify(deadLetterService)
                .markProcessing(DEAD_LETTER_ID);

        inOrder.verify(forceLogoutRetryService)
                .recover("session-123");

        inOrder.verify(deadLetterService)
                .markRecovered(DEAD_LETTER_ID, CLAIM_TOKEN);
    }

    @Test
    void OUTBOX_타입이면_Outbox_복구를_위임한다() {

        // given
        DeadLetterQueue deadLetterQueue = createDeadLetter(
                DeadLetterAggregateType.OUTBOX,
                "456"
        );

        givenProcessingDeadLetter(deadLetterQueue);

        // when
        adminDeadLetterService.recoverDeadLetter(
                ADMIN_ID,
                DEAD_LETTER_ID
        );

        // then
        verify(outboxTransactionService)
                .recover(456L);

        verify(deadLetterService)
                .markRecovered(DEAD_LETTER_ID, CLAIM_TOKEN);
    }

    @Test
    void NOTIFICATION_RETRY_타입이면_알림_재시도를_복구한다() {

        // given
        DeadLetterQueue deadLetterQueue = createDeadLetter(
                DeadLetterAggregateType.NOTIFICATION_RETRY,
                "789"
        );

        givenProcessingDeadLetter(deadLetterQueue);

        // when
        adminDeadLetterService.recoverDeadLetter(
                ADMIN_ID,
                DEAD_LETTER_ID
        );

        // then
        verify(notificationRetryService)
                .recover(789L);

        verify(deadLetterService)
                .markRecovered(DEAD_LETTER_ID, CLAIM_TOKEN);
    }

    @Test
    void DESERIALIZATION_ERROR_타입이면_역직렬화_복구를_위임한다() {

        // given
        DeadLetterQueue deadLetterQueue = createDeadLetter(
                DeadLetterAggregateType.DESERIALIZATION_ERROR,
                null
        );

        givenProcessingDeadLetter(deadLetterQueue);

        // when
        adminDeadLetterService.recoverDeadLetter(
                ADMIN_ID,
                DEAD_LETTER_ID
        );

        // then
        verify(deadLetterDeserializationRecoveryService)
                .recover(deadLetterQueue);

        verify(deadLetterService)
                .markRecovered(DEAD_LETTER_ID, CLAIM_TOKEN);
    }

    @Test
    void ClaimToken_검증에_실패하면_복구하지_않는다() {

        // given
        when(deadLetterService.markProcessing(DEAD_LETTER_ID))
                .thenReturn(CLAIM_TOKEN);

        when(deadLetterRepository
                .findByIdAndClaimTokenAndDeadLetterStatus(
                        DEAD_LETTER_ID,
                        CLAIM_TOKEN,
                        DeadLetterStatus.PROCESSING
                ))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                adminDeadLetterService.recoverDeadLetter(
                        ADMIN_ID,
                        DEAD_LETTER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.DEAD_LETTER_CLAIM_FAILED);

        verifyNoInteractions(
                forceLogoutRetryService,
                outboxTransactionService,
                notificationRetryService,
                deadLetterDeserializationRecoveryService
        );

        verify(deadLetterService, never())
                .markRecovered(anyLong(), anyString());

        verify(deadLetterService, never())
                .markPending(anyLong(), anyString());
    }

    @Test
    void 복구에_실패하면_Pending으로_되돌리고_원래_예외를_재전파한다() {

        // given
        DeadLetterQueue deadLetterQueue = createDeadLetter(
                DeadLetterAggregateType.SESSION,
                "session-123"
        );

        givenProcessingDeadLetter(deadLetterQueue);

        RuntimeException exception =
                new RuntimeException("복구 실패");

        doThrow(exception)
                .when(forceLogoutRetryService)
                .recover("session-123");

        // when & then
        assertThatThrownBy(() ->
                adminDeadLetterService.recoverDeadLetter(
                        ADMIN_ID,
                        DEAD_LETTER_ID
                )
        )
                .isSameAs(exception);

        verify(deadLetterService)
                .markPending(DEAD_LETTER_ID, CLAIM_TOKEN);

        verify(deadLetterService, never())
                .markRecovered(anyLong(), anyString());
    }

    @Test
    void Pending_상태_복구에도_실패하면_원래_예외를_재전파한다() {

        // given
        DeadLetterQueue deadLetterQueue = createDeadLetter(
                DeadLetterAggregateType.SESSION,
                "session-123"
        );

        givenProcessingDeadLetter(deadLetterQueue);

        RuntimeException recoveryException =
                new RuntimeException("복구 실패");

        RuntimeException pendingException =
                new RuntimeException("Pending 전환 실패");

        doThrow(recoveryException)
                .when(forceLogoutRetryService)
                .recover("session-123");

        doThrow(pendingException)
                .when(deadLetterService)
                .markPending(DEAD_LETTER_ID, CLAIM_TOKEN);

        // when & then
        assertThatThrownBy(() ->
                adminDeadLetterService.recoverDeadLetter(
                        ADMIN_ID,
                        DEAD_LETTER_ID
                )
        )
                .isSameAs(recoveryException);

        verify(deadLetterService)
                .markPending(DEAD_LETTER_ID, CLAIM_TOKEN);

        verify(deadLetterService, never())
                .markRecovered(anyLong(), anyString());
    }

    private void givenProcessingDeadLetter(
            DeadLetterQueue deadLetterQueue
    ) {
        when(deadLetterService.markProcessing(DEAD_LETTER_ID))
                .thenReturn(CLAIM_TOKEN);

        when(deadLetterRepository
                .findByIdAndClaimTokenAndDeadLetterStatus(
                        DEAD_LETTER_ID,
                        CLAIM_TOKEN,
                        DeadLetterStatus.PROCESSING
                ))
                .thenReturn(Optional.of(deadLetterQueue));
    }

    private DeadLetterQueue createDeadLetter(
            DeadLetterAggregateType aggregateType,
            String aggregateId
    ) {
        return DeadLetterQueue.create(
                DeadLetterType.FORCE_LOGOUT,
                DeadLetterSource.FORCE_LOGOUT_RETRY_SCHEDULER,
                aggregateType,
                aggregateId,
                10L,
                "{}",
                "test reason",
                RuntimeException.class.getName(),
                0,
                UUID.randomUUID().toString()
        );
    }
}