package com.example.schedulebook.domain.admin.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.response.PageResponse;
import com.example.schedulebook.domain.admin.dto.response.CleanupOutboxResponse;
import com.example.schedulebook.domain.admin.dto.response.OutboxResponse;
import com.example.schedulebook.domain.admin.dto.response.OutboxStatsResponse;
import com.example.schedulebook.domain.outbox.entity.Outbox;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.enums.OutboxStatus;
import com.example.schedulebook.domain.outbox.repository.OutboxRepository;
import com.example.schedulebook.domain.outbox.service.OutboxCleanupService;
import com.example.schedulebook.domain.user.validator.UserValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOutboxServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private UserValidator userValidator;

    @Mock
    private OutboxCleanupService outboxCleanupService;

    @InjectMocks
    private AdminOutboxService adminOutboxService;

    private static final Long ADMIN_ID = 1L;
    private static final Long OUTBOX_ID = 100L;

    @Test
    void Dead_Outbox_목록을_조회하면_페이지_응답을_반환한다() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Outbox outbox = mockOutbox(OutboxStatus.DEAD);

        Page<Outbox> page =
                new PageImpl<>(List.of(outbox), pageable, 1);

        when(outboxRepository.findAllByStatus(
                OutboxStatus.DEAD,
                pageable
        )).thenReturn(page);

        // when
        PageResponse<OutboxResponse> result =
                adminOutboxService.findAllDeadOutboxes(
                        ADMIN_ID,
                        pageable
                );

        // then
        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verify(outboxRepository).findAllByStatus(
                OutboxStatus.DEAD,
                pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.currentPage()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.isLast()).isTrue();

        OutboxResponse response = result.content().get(0);

        assertThat(response.id()).isEqualTo(OUTBOX_ID);
        assertThat(response.aggregateType())
                .isEqualTo(OutboxAggregateType.SESSION);
        assertThat(response.aggregateId()).isEqualTo("10");
        assertThat(response.eventType())
                .isEqualTo(OutboxEventType.FORCE_LOGOUT);
        assertThat(response.status()).isEqualTo(OutboxStatus.DEAD);
        assertThat(response.payloadPreview()).isEqualTo("{}");
        assertThat(response.retryCount()).isEqualTo(2);
        assertThat(response.errorMessage()).isEqualTo("test error");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void 관리자_권한_검증에_실패하면_Dead_Outbox를_조회하지_않는다() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        BaseException exception =
                new BaseException(ErrorEnum.ADMIN_NOT_FOUND);

        doThrow(exception)
                .when(userValidator)
                .validateActiveAdmin(ADMIN_ID);

        // when & then
        assertThatThrownBy(() ->
                adminOutboxService.findAllDeadOutboxes(
                        ADMIN_ID,
                        pageable
                )
        ).isSameAs(exception);

        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verifyNoInteractions(outboxRepository);
    }

    @Test
    void Failed_Outbox_목록을_조회하면_페이지_응답을_반환한다() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Outbox outbox = mockOutbox(OutboxStatus.FAILED);

        Page<Outbox> page =
                new PageImpl<>(List.of(outbox), pageable, 1);

        when(outboxRepository.findAllByStatus(
                OutboxStatus.FAILED,
                pageable
        )).thenReturn(page);

        // when
        PageResponse<OutboxResponse> result =
                adminOutboxService.findAllFailedOutboxes(
                        ADMIN_ID,
                        pageable
                );

        // then
        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verify(outboxRepository).findAllByStatus(
                OutboxStatus.FAILED,
                pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.currentPage()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.isLast()).isTrue();

        OutboxResponse response = result.content().get(0);

        assertThat(response.id()).isEqualTo(OUTBOX_ID);
        assertThat(response.status()).isEqualTo(OutboxStatus.FAILED);
        assertThat(response.payloadPreview()).isEqualTo("{}");
        assertThat(response.retryCount()).isEqualTo(2);
        assertThat(response.errorMessage()).isEqualTo("test error");
    }

    @Test
    void Outbox를_Retry하면_retry를_호출한다() {
        // given
        Outbox outbox = org.mockito.Mockito.mock(Outbox.class);

        when(outboxRepository.findById(OUTBOX_ID))
                .thenReturn(Optional.of(outbox));

        // when
        adminOutboxService.retryOutbox(
                ADMIN_ID,
                OUTBOX_ID
        );

        // then
        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verify(outboxRepository).findById(OUTBOX_ID);
        verify(outbox).retry();
    }

    @Test
    void 존재하지_않는_Outbox를_Retry하면_예외가_발생한다() {
        // given
        when(outboxRepository.findById(OUTBOX_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                adminOutboxService.retryOutbox(
                        ADMIN_ID,
                        OUTBOX_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.OUTBOX_NOT_FOUND);

        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verify(outboxRepository).findById(OUTBOX_ID);
    }

    @Test
    void 관리자_권한_검증에_실패하면_Retry하지_않는다() {
        // given
        BaseException exception =
                new BaseException(ErrorEnum.ADMIN_NOT_FOUND);

        doThrow(exception)
                .when(userValidator)
                .validateActiveAdmin(ADMIN_ID);

        // when & then
        assertThatThrownBy(() ->
                adminOutboxService.retryOutbox(
                        ADMIN_ID,
                        OUTBOX_ID
                )
        ).isSameAs(exception);

        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verifyNoInteractions(outboxRepository);
    }

    @Test
    void SUCCESS_Outbox를_정리하면_삭제된_개수를_응답한다() {
        // given
        int days = 30;
        int deletedCount = 42;

        when(outboxCleanupService.cleanup(days))
                .thenReturn(deletedCount);

        // when
        CleanupOutboxResponse result =
                adminOutboxService.deleteSuccessOutbox(
                        ADMIN_ID,
                        days
                );

        // then
        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verify(outboxCleanupService).cleanup(days);

        assertThat(result).isNotNull();
        assertThat(result.deletedCount())
                .isEqualTo(deletedCount);
    }

    @Test
    void 관리자_권한_검증에_실패하면_Outbox_정리를_수행하지_않는다() {
        // given
        BaseException exception =
                new BaseException(ErrorEnum.ADMIN_NOT_FOUND);

        doThrow(exception)
                .when(userValidator)
                .validateActiveAdmin(ADMIN_ID);

        // when & then
        assertThatThrownBy(() ->
                adminOutboxService.deleteSuccessOutbox(
                        ADMIN_ID,
                        30
                )
        ).isSameAs(exception);

        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verifyNoInteractions(outboxCleanupService);
    }

    @Test
    void Outbox_통계를_조회하면_전체_통계값을_반환한다() {
        // given
        Object[] stats = {
                10L, // pending
                20L, // processing
                30L, // failed
                40L, // dead
                50L  // success
        };

        when(outboxRepository.countStats())
                .thenReturn(stats);

        // when
        OutboxStatsResponse result =
                adminOutboxService.getStats(ADMIN_ID);

        // then
        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verify(outboxRepository).countStats();

        assertThat(result.pending()).isEqualTo(10L);
        assertThat(result.processing()).isEqualTo(20L);
        assertThat(result.failed()).isEqualTo(30L);
        assertThat(result.dead()).isEqualTo(40L);
        assertThat(result.success()).isEqualTo(50L);
    }

    @Test
    void Outbox_통계값이_null이면_0으로_변환한다() {
        // given
        Object[] stats = {
                null,
                20L,
                null,
                40L,
                null
        };

        when(outboxRepository.countStats())
                .thenReturn(stats);

        // when
        OutboxStatsResponse result =
                adminOutboxService.getStats(ADMIN_ID);

        // then
        assertThat(result.pending()).isZero();
        assertThat(result.processing()).isEqualTo(20L);
        assertThat(result.failed()).isZero();
        assertThat(result.dead()).isEqualTo(40L);
        assertThat(result.success()).isZero();
    }

    @Test
    void 관리자_권한_검증에_실패하면_통계를_조회하지_않는다() {
        // given
        BaseException exception =
                new BaseException(ErrorEnum.ADMIN_NOT_FOUND);

        doThrow(exception)
                .when(userValidator)
                .validateActiveAdmin(ADMIN_ID);

        // when & then
        assertThatThrownBy(() ->
                adminOutboxService.getStats(ADMIN_ID)
        ).isSameAs(exception);

        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verifyNoInteractions(outboxRepository);
    }

    private Outbox mockOutbox(OutboxStatus status) {
        Outbox outbox = org.mockito.Mockito.mock(Outbox.class);

        LocalDateTime createdAt =
                LocalDateTime.of(2026, 9, 14, 10, 0);

        LocalDateTime updatedAt =
                LocalDateTime.of(2026, 9, 14, 11, 0);

        when(outbox.getId()).thenReturn(OUTBOX_ID);
        when(outbox.getAggregateType())
                .thenReturn(OutboxAggregateType.SESSION);
        when(outbox.getAggregateId())
                .thenReturn("10");
        when(outbox.getEventType())
                .thenReturn(OutboxEventType.FORCE_LOGOUT);
        when(outbox.getStatus()).thenReturn(status);
        when(outbox.getPayload()).thenReturn("{}");
        when(outbox.getRetryCount()).thenReturn(2);
        when(outbox.getErrorMessage())
                .thenReturn("test error");
        when(outbox.getCreatedAt()).thenReturn(createdAt);
        when(outbox.getUpdatedAt()).thenReturn(updatedAt);

        return outbox;
    }
}