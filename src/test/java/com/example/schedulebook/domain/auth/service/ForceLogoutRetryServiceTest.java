package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.auth.entity.ForceLogoutRetry;
import com.example.schedulebook.domain.auth.enums.ForceLogoutRetryStatus;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.example.schedulebook.domain.auth.repository.ForceLogoutRetryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForceLogoutRetryServiceTest {

    @Mock
    private ForceLogoutRetryRepository forceLogoutRetryRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ForceLogoutSessionEvent event;

    @Mock
    private ForceLogoutRetry forceLogoutRetry;

    @Mock
    private Page<ForceLogoutRetry> retryPage;

    private ForceLogoutRetryService forceLogoutRetryService;

    private Long retryId;
    private Long userId;
    private String sessionId;
    private String eventId;
    private String payload;
    private String reason;
    private String claimToken;

    @BeforeEach
    void setUp() {
        forceLogoutRetryService = new ForceLogoutRetryService(
                forceLogoutRetryRepository,
                objectMapper
        );

        retryId = 1L;
        userId = 100L;
        sessionId = "session-123";
        eventId = "event-123";
        payload = "{\"eventId\":\"event-123\",\"sessionId\":\"session-123\",\"userId\":100}";
        reason = "강제 로그아웃 처리 실패";
        claimToken = "claim-token";
    }

// =========================================================
// save
// =========================================================

    @Test
    void givenNewEvent_whenSave_thenSerializeAndSaveRetry() throws Exception {
        // given
        givenEvent();

        when(forceLogoutRetryRepository.findByEventId(eventId))
                .thenReturn(Optional.empty());

        when(objectMapper.writeValueAsString(event))
                .thenReturn(payload);

        // when
        forceLogoutRetryService.save(event, reason);

        // then
        verify(forceLogoutRetryRepository)
                .findByEventId(eventId);

        verify(objectMapper)
                .writeValueAsString(event);

        ArgumentCaptor<ForceLogoutRetry> captor =
                ArgumentCaptor.forClass(ForceLogoutRetry.class);

        verify(forceLogoutRetryRepository)
                .save(captor.capture());

        ForceLogoutRetry savedRetry = captor.getValue();

        assertThat(savedRetry.getEventId())
                .isEqualTo(eventId);
        assertThat(savedRetry.getSessionId())
                .isEqualTo(sessionId);
        assertThat(savedRetry.getUserId())
                .isEqualTo(userId);
        assertThat(savedRetry.getPayload())
                .isEqualTo(payload);
        assertThat(savedRetry.getForceLogoutRetryStatus())
                .isEqualTo(ForceLogoutRetryStatus.PENDING);
        assertThat(savedRetry.getRetryCount())
                .isZero();
        assertThat(savedRetry.getReason())
                .isEqualTo(reason);
        assertThat(savedRetry.getNextRetryAt())
                .isNotNull();
    }

    @Test
    void givenExistingEvent_whenSave_thenSkipSavingDuplicateRetry() {
        // given
        when(event.eventId())
                .thenReturn(eventId);

        when(forceLogoutRetryRepository.findByEventId(eventId))
                .thenReturn(Optional.of(forceLogoutRetry));

        // when
        forceLogoutRetryService.save(event, reason);

        // then
        verify(forceLogoutRetryRepository)
                .findByEventId(eventId);

        verifyNoInteractions(objectMapper);

        verify(forceLogoutRetryRepository, never())
                .save(any(ForceLogoutRetry.class));
    }

    @Test
    void givenSerializationFailure_whenSave_thenThrowJsonSerializationFailed() throws Exception {
        // given
        when(event.eventId())
                .thenReturn(eventId);

        when(forceLogoutRetryRepository.findByEventId(eventId))
                .thenReturn(Optional.empty());

        JsonProcessingException exception =
                new JsonProcessingException("serialization failed") {};

        when(objectMapper.writeValueAsString(event))
                .thenThrow(exception);

        // when & then
        assertThatThrownBy(() ->
                forceLogoutRetryService.save(event, reason)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.JSON_SERIALIZATION_FAILED);

        verify(forceLogoutRetryRepository)
                .findByEventId(eventId);

        verify(objectMapper)
                .writeValueAsString(event);

        verify(forceLogoutRetryRepository, never())
                .save(any(ForceLogoutRetry.class));
    }

// =========================================================
// markSuccess
// =========================================================

    @Test
    void givenExistingRetry_whenMarkSuccess_thenUpdateSuccess() {
        // given
        when(forceLogoutRetryRepository.markSuccess(retryId, claimToken))
                .thenReturn(1);

        // when
        forceLogoutRetryService.markSuccess(retryId, claimToken);

        // then
        verify(forceLogoutRetryRepository)
                .markSuccess(retryId, claimToken);
    }

    @Test
    void givenMissingRetryOrClaimMismatch_whenMarkSuccess_thenThrowRetryNotFound() {
        // given
        when(forceLogoutRetryRepository.markSuccess(retryId, claimToken))
                .thenReturn(0);

        // when & then
        assertThatThrownBy(() ->
                forceLogoutRetryService.markSuccess(retryId, claimToken)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.FORCE_LOGOUT_RETRY_NOT_FOUND);

        verify(forceLogoutRetryRepository)
                .markSuccess(retryId, claimToken);
    }

// =========================================================
// markFailed
// =========================================================

    @Test
    void givenExistingRetry_whenMarkFailed_thenUpdateFailedWithReasonAndClaimToken() {
        // given
        when(forceLogoutRetryRepository.markFailed(
                retryId,
                reason,
                claimToken
        )).thenReturn(1);

        // when
        forceLogoutRetryService.markFailed(
                retryId,
                reason,
                claimToken
        );

        // then
        verify(forceLogoutRetryRepository)
                .markFailed(retryId, reason, claimToken);
    }

    @Test
    void givenMissingRetryOrClaimMismatch_whenMarkFailed_thenThrowRetryNotFound() {
        // given
        when(forceLogoutRetryRepository.markFailed(
                retryId,
                reason,
                claimToken
        )).thenReturn(0);

        // when & then
        assertThatThrownBy(() ->
                forceLogoutRetryService.markFailed(
                        retryId,
                        reason,
                        claimToken
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.FORCE_LOGOUT_RETRY_NOT_FOUND);

        verify(forceLogoutRetryRepository)
                .markFailed(retryId, reason, claimToken);
    }

// =========================================================
// markProcessing
// =========================================================

    @Test
    void givenAvailableRetryTarget_whenMarkProcessing_thenReturnGeneratedClaimToken() {
        // given
        when(forceLogoutRetryRepository.markProcessing(
                eq(retryId),
                anyString(),
                any(LocalDateTime.class)
        )).thenReturn(1);

        // when
        String result =
                forceLogoutRetryService.markProcessing(retryId);

        // then
        assertThat(result)
                .isNotNull()
                .isNotBlank();

        verify(forceLogoutRetryRepository)
                .markProcessing(
                        eq(retryId),
                        eq(result),
                        any(LocalDateTime.class)
                );
    }

    @Test
    void givenUnavailableRetryTarget_whenMarkProcessing_thenReturnNull() {
        // given
        when(forceLogoutRetryRepository.markProcessing(
                eq(retryId),
                anyString(),
                any(LocalDateTime.class)
        )).thenReturn(0);

        // when
        String result =
                forceLogoutRetryService.markProcessing(retryId);

        // then
        assertThat(result)
                .isNull();

        verify(forceLogoutRetryRepository)
                .markProcessing(
                        eq(retryId),
                        anyString(),
                        any(LocalDateTime.class)
                );
    }

    @Test
    void givenRetry_whenMarkProcessing_thenUseTimeoutTenMinutesBeforeNow() {
        // given
        ArgumentCaptor<LocalDateTime> timeoutCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);

        when(forceLogoutRetryRepository.markProcessing(
                eq(retryId),
                anyString(),
                any(LocalDateTime.class)
        )).thenReturn(0);

        LocalDateTime before = LocalDateTime.now().minusMinutes(10);

        // when
        forceLogoutRetryService.markProcessing(retryId);

        LocalDateTime after = LocalDateTime.now().minusMinutes(10);

        // then
        verify(forceLogoutRetryRepository)
                .markProcessing(
                        eq(retryId),
                        anyString(),
                        timeoutCaptor.capture()
                );

        assertThat(timeoutCaptor.getValue())
                .isBetween(before.minusSeconds(1), after.plusSeconds(1));
    }

// =========================================================
// markRetry
// =========================================================

    @Test
    void givenRetryFailure_whenMarkRetry_thenUpdatePendingWithCalculatedNextRetryTime() {
        // given
        int retryCount = 2;

        when(forceLogoutRetryRepository.markRetry(
                eq(retryId),
                eq(reason),
                any(LocalDateTime.class),
                eq(claimToken)
        )).thenReturn(1);

        ArgumentCaptor<LocalDateTime> nextRetryAtCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);

        LocalDateTime before = LocalDateTime.now();

        long expectedDelaySeconds =
                CommonConst.NEXT_RETRY_DELAY * (1L << (retryCount + 1));

        LocalDateTime expectedNextRetryAt =
                before.plusSeconds(expectedDelaySeconds);

        // when
        forceLogoutRetryService.markRetry(
                retryId,
                reason,
                retryCount,
                claimToken
        );

        LocalDateTime after = LocalDateTime.now();

        // then
        verify(forceLogoutRetryRepository)
                .markRetry(
                        eq(retryId),
                        eq(reason),
                        nextRetryAtCaptor.capture(),
                        eq(claimToken)
                );

        assertThat(nextRetryAtCaptor.getValue())
                .isBetween(
                        expectedNextRetryAt.minusSeconds(1),
                        after.plusSeconds(expectedDelaySeconds + 1)
                );
    }

    @Test
    void givenDifferentRetryCount_whenMarkRetry_thenApplyDifferentRetryDelay() {
        // given
        ArgumentCaptor<LocalDateTime> nextRetryAtCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);

        when(forceLogoutRetryRepository.markRetry(
                eq(retryId),
                eq(reason),
                any(LocalDateTime.class),
                eq(claimToken)
        )).thenReturn(1);

        // when
        LocalDateTime before = LocalDateTime.now();

        forceLogoutRetryService.markRetry(
                retryId,
                reason,
                0,
                claimToken
        );

        forceLogoutRetryService.markRetry(
                retryId,
                reason,
                1,
                claimToken
        );

        forceLogoutRetryService.markRetry(
                retryId,
                reason,
                2,
                claimToken
        );

        // then
        verify(forceLogoutRetryRepository, times(3))
                .markRetry(
                        eq(retryId),
                        eq(reason),
                        nextRetryAtCaptor.capture(),
                        eq(claimToken)
                );

        List<LocalDateTime> nextRetryAtValues =
                nextRetryAtCaptor.getAllValues();

        long delay0 =
                CommonConst.NEXT_RETRY_DELAY * (1L << 1);

        long delay1 =
                CommonConst.NEXT_RETRY_DELAY * (1L << 2);

        long delay2 =
                CommonConst.NEXT_RETRY_DELAY * (1L << 3);

        assertThat(nextRetryAtValues.get(0))
                .isBetween(
                        before.plusSeconds(delay0).minusSeconds(1),
                        before.plusSeconds(delay0).plusSeconds(2)
                );

        assertThat(nextRetryAtValues.get(1))
                .isBetween(
                        before.plusSeconds(delay1).minusSeconds(1),
                        before.plusSeconds(delay1).plusSeconds(2)
                );

        assertThat(nextRetryAtValues.get(2))
                .isBetween(
                        before.plusSeconds(delay2).minusSeconds(1),
                        before.plusSeconds(delay2).plusSeconds(2)
                );

        assertThat(nextRetryAtValues.get(1))
                .isAfter(nextRetryAtValues.get(0));

        assertThat(nextRetryAtValues.get(2))
                .isAfter(nextRetryAtValues.get(1));
    }

    @Test
    void givenMissingRetryOrClaimMismatch_whenMarkRetry_thenThrowRetryNotFound() {
        // given
        when(forceLogoutRetryRepository.markRetry(
                eq(retryId),
                eq(reason),
                any(LocalDateTime.class),
                eq(claimToken)
        )).thenReturn(0);

        // when & then
        assertThatThrownBy(() ->
                forceLogoutRetryService.markRetry(
                        retryId,
                        reason,
                        2,
                        claimToken
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.FORCE_LOGOUT_RETRY_NOT_FOUND);

        verify(forceLogoutRetryRepository)
                .markRetry(
                        eq(retryId),
                        eq(reason),
                        any(LocalDateTime.class),
                        eq(claimToken)
                );
    }

// =========================================================
// findRetryTargets
// =========================================================

    @Test
    void givenRetryTargetSize_whenFindRetryTargets_thenReturnRepositoryContent() {
        // given
        List<ForceLogoutRetry> retries =
                List.of(forceLogoutRetry);

        when(retryPage.getContent())
                .thenReturn(retries);

        when(forceLogoutRetryRepository.findRetryTargets(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(retryPage);

        // when
        List<ForceLogoutRetry> result =
                forceLogoutRetryService.findRetryTargets(10);

        // then
        assertThat(result)
                .containsExactly(forceLogoutRetry);

        verify(retryPage)
                .getContent();
    }

    @Test
    void givenRetryTargetSize_whenFindRetryTargets_thenUseFirstPageAndRequestedSize() {
        // given
        when(retryPage.getContent())
                .thenReturn(List.of());

        ArgumentCaptor<LocalDateTime> timeoutCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        when(forceLogoutRetryRepository.findRetryTargets(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(retryPage);

        // when
        forceLogoutRetryService.findRetryTargets(20);

        // then
        verify(forceLogoutRetryRepository)
                .findRetryTargets(
                        timeoutCaptor.capture(),
                        pageableCaptor.capture()
                );

        assertThat(pageableCaptor.getValue())
                .isEqualTo(PageRequest.of(0, 20));

        LocalDateTime expected =
                LocalDateTime.now().minusMinutes(10);

        assertThat(timeoutCaptor.getValue())
                .isBetween(
                        expected.minusSeconds(1),
                        expected.plusSeconds(1)
                );
    }

// =========================================================
// findById
// =========================================================

    @Test
    void givenExistingRetryId_whenFindById_thenReturnRetry() {
        // given
        when(forceLogoutRetryRepository.findById(retryId))
                .thenReturn(Optional.of(forceLogoutRetry));

        // when
        ForceLogoutRetry result =
                forceLogoutRetryService.findById(retryId);

        // then
        assertThat(result)
                .isSameAs(forceLogoutRetry);

        verify(forceLogoutRetryRepository)
                .findById(retryId);
    }

    @Test
    void givenMissingRetryId_whenFindById_thenThrowRetryNotFound() {
        // given
        when(forceLogoutRetryRepository.findById(retryId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                forceLogoutRetryService.findById(retryId)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.FORCE_LOGOUT_RETRY_NOT_FOUND);

        verify(forceLogoutRetryRepository)
                .findById(retryId);
    }

// =========================================================
// deserialize
// =========================================================

    @Test
    void givenValidPayload_whenDeserialize_thenReturnEvent() throws Exception {
        // given
        when(forceLogoutRetry.getPayload())
                .thenReturn(payload);

        when(objectMapper.readValue(
                payload,
                ForceLogoutSessionEvent.class
        )).thenReturn(event);

        // when
        ForceLogoutSessionEvent result =
                forceLogoutRetryService.deserialize(forceLogoutRetry);

        // then
        assertThat(result)
                .isSameAs(event);

        verify(forceLogoutRetry)
                .getPayload();

        verify(objectMapper)
                .readValue(payload, ForceLogoutSessionEvent.class);
    }

    @Test
    void givenInvalidPayload_whenDeserialize_thenThrowJsonDeserializationFailed() throws Exception {
        // given
        when(forceLogoutRetry.getPayload())
                .thenReturn(payload);

        when(forceLogoutRetry.getId())
                .thenReturn(retryId);

        JsonProcessingException exception =
                new JsonProcessingException("deserialization failed") {};

        when(objectMapper.readValue(
                payload,
                ForceLogoutSessionEvent.class
        )).thenThrow(exception);

        // when & then
        assertThatThrownBy(() ->
                forceLogoutRetryService.deserialize(forceLogoutRetry)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.JSON_DESERIALIZATION_FAILED);

        verify(objectMapper)
                .readValue(payload, ForceLogoutSessionEvent.class);
    }

// =========================================================
// recover
// =========================================================

    @Test
    void givenPendingRetry_whenRecover_thenSkipRecovery() {
        // given
        when(forceLogoutRetryRepository.findBySessionId(sessionId))
                .thenReturn(Optional.of(forceLogoutRetry));

        when(forceLogoutRetry.getForceLogoutRetryStatus())
                .thenReturn(ForceLogoutRetryStatus.PENDING);

        // when
        forceLogoutRetryService.recover(sessionId);

        // then
        verify(forceLogoutRetryRepository)
                .findBySessionId(sessionId);

        verify(forceLogoutRetryRepository, never())
                .updateRecover(anyLong(), anyString());
    }

    @Test
    void givenFailedRetry_whenRecover_thenUpdateToPending() {
        // given
        when(forceLogoutRetryRepository.findBySessionId(sessionId))
                .thenReturn(Optional.of(forceLogoutRetry));

        when(forceLogoutRetry.getForceLogoutRetryStatus())
                .thenReturn(ForceLogoutRetryStatus.FAILED);

        when(forceLogoutRetry.getId())
                .thenReturn(retryId);

        when(forceLogoutRetry.getClaimToken())
                .thenReturn(claimToken);

        when(forceLogoutRetryRepository.updateRecover(
                retryId,
                claimToken
        )).thenReturn(1);

        // when
        forceLogoutRetryService.recover(sessionId);

        // then
        verify(forceLogoutRetryRepository)
                .findBySessionId(sessionId);

        verify(forceLogoutRetryRepository)
                .updateRecover(retryId, claimToken);
    }

    @Test
    void givenNonFailedAndNonPendingRetry_whenRecover_thenThrowInvalidStatus() {
        // given
        when(forceLogoutRetryRepository.findBySessionId(sessionId))
                .thenReturn(Optional.of(forceLogoutRetry));

        when(forceLogoutRetry.getForceLogoutRetryStatus())
                .thenReturn(ForceLogoutRetryStatus.PROCESSING);

        // when & then
        assertThatThrownBy(() ->
                forceLogoutRetryService.recover(sessionId)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.INVALID_FORCE_LOGOUT_RETRY_STATUS);

        verify(forceLogoutRetryRepository)
                .findBySessionId(sessionId);

        verify(forceLogoutRetryRepository, never())
                .updateRecover(anyLong(), anyString());
    }

    @Test
    void givenMissingRetrySession_whenRecover_thenThrowRetryNotFound() {
        // given
        when(forceLogoutRetryRepository.findBySessionId(sessionId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                forceLogoutRetryService.recover(sessionId)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.FORCE_LOGOUT_RETRY_NOT_FOUND);

        verify(forceLogoutRetryRepository)
                .findBySessionId(sessionId);

        verifyNoInteractions(forceLogoutRetry);
    }

    @Test
    void givenFailedRetryAndUpdateFailure_whenRecover_thenThrowRecoverFailed() {
        // given
        when(forceLogoutRetryRepository.findBySessionId(sessionId))
                .thenReturn(Optional.of(forceLogoutRetry));

        when(forceLogoutRetry.getForceLogoutRetryStatus())
                .thenReturn(ForceLogoutRetryStatus.FAILED);

        when(forceLogoutRetry.getId())
                .thenReturn(retryId);

        when(forceLogoutRetry.getClaimToken())
                .thenReturn(claimToken);

        when(forceLogoutRetryRepository.updateRecover(
                retryId,
                claimToken
        )).thenReturn(0);

        // when & then
        assertThatThrownBy(() ->
                forceLogoutRetryService.recover(sessionId)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.DEAD_LETTER_RECOVER_FAILED);

        verify(forceLogoutRetryRepository)
                .findBySessionId(sessionId);

        verify(forceLogoutRetryRepository)
                .updateRecover(retryId, claimToken);
    }

// =========================================================
// helper
// =========================================================

    private void givenEvent() {
        when(event.eventId())
                .thenReturn(eventId);

        when(event.sessionId())
                .thenReturn(sessionId);

        when(event.userId())
                .thenReturn(userId);
    }
}
