package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.auth.entity.ForceLogoutRetry;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import com.example.schedulebook.domain.deadletter.service.DeadLetterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForceLogoutRetryStateServiceTest {

    @Mock
    private ForceLogoutRetryService forceLogoutRetryService;

    @Mock
    private DeadLetterService deadLetterService;

    @Mock
    private ForceLogoutRetry forceLogoutRetry;

    private ForceLogoutRetryStateService forceLogoutRetryStateService;

    private Long retryId;
    private Long userId;
    private String sessionId;
    private String payload;
    private String eventId;
    private String claimToken;
    private String reason;
    private Exception exception;

    @BeforeEach
    void setUp() {
        forceLogoutRetryStateService = new ForceLogoutRetryStateService(
                forceLogoutRetryService,
                deadLetterService
        );

        retryId = 1L;
        userId = 100L;
        sessionId = "session-123";
        payload = "{\"userId\":100,\"sessionId\":\"session-123\"}";
        eventId = "event-123";
        claimToken = "claim-token";
        reason = "강제 로그아웃 처리 실패";
        exception = new RuntimeException("Redis connection failed");
    }

    @Test
    void givenRetryAndFailureReason_whenCompleteFailure_thenMarkFailedAndSaveDeadLetter() {
        // given
        givenFailureRetry();

        // when
        forceLogoutRetryStateService.completeFailure(
                forceLogoutRetry,
                reason,
                claimToken,
                exception
        );

        // then
        verify(forceLogoutRetryService)
                .markFailed(retryId, reason, claimToken);

        verify(deadLetterService).save(
                DeadLetterType.FORCE_LOGOUT,
                DeadLetterSource.FORCE_LOGOUT_RETRY_SCHEDULER,
                DeadLetterAggregateType.SESSION,
                sessionId,
                userId,
                payload,
                exception.getMessage(),
                exception.getClass().getSimpleName(),
                3,
                eventId
        );
    }

    @Test
    void givenDeadLetterSaveFailure_whenCompleteFailure_thenThrowDeadLetterSaveFailed() {
        // given
        givenFailureRetry();

        RuntimeException dlqException =
                new RuntimeException("DLQ unavailable");

        doThrow(dlqException)
                .when(deadLetterService)
                .save(
                        DeadLetterType.FORCE_LOGOUT,
                        DeadLetterSource.FORCE_LOGOUT_RETRY_SCHEDULER,
                        DeadLetterAggregateType.SESSION,
                        sessionId,
                        userId,
                        payload,
                        exception.getMessage(),
                        exception.getClass().getSimpleName(),
                        3,
                        eventId
                );

        // when & then
        assertThatThrownBy(() ->
                forceLogoutRetryStateService.completeFailure(
                        forceLogoutRetry,
                        reason,
                        claimToken,
                        exception
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.DEAD_LETTER_SAVE_FAILED);

        // 실패 상태 변경은 DLQ 저장보다 먼저 수행되어야 한다.
        verify(forceLogoutRetryService)
                .markFailed(retryId, reason, claimToken);

        verify(deadLetterService).save(
                DeadLetterType.FORCE_LOGOUT,
                DeadLetterSource.FORCE_LOGOUT_RETRY_SCHEDULER,
                DeadLetterAggregateType.SESSION,
                sessionId,
                userId,
                payload,
                exception.getMessage(),
                exception.getClass().getSimpleName(),
                3,
                eventId
        );
    }

    @Test
    void givenRetry_whenCompleteSuccess_thenMarkSuccessWithClaimToken() {
        // given
        when(forceLogoutRetry.getId()).thenReturn(retryId);

        // when
        forceLogoutRetryStateService.completeSuccess(
                forceLogoutRetry,
                claimToken
        );

        // then
        verify(forceLogoutRetryService)
                .markSuccess(retryId, claimToken);

        verifyNoInteractions(deadLetterService);
    }

    private void givenFailureRetry() {
        when(forceLogoutRetry.getId()).thenReturn(retryId);
        when(forceLogoutRetry.getUserId()).thenReturn(userId);
        when(forceLogoutRetry.getSessionId()).thenReturn(sessionId);
        when(forceLogoutRetry.getPayload()).thenReturn(payload);
        when(forceLogoutRetry.getRetryCount()).thenReturn(2);
        when(forceLogoutRetry.getEventId()).thenReturn(eventId);
    }
}