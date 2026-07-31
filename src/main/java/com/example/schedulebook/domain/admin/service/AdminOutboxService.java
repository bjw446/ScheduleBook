package com.example.schedulebook.domain.admin.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.response.PageResponse;
import com.example.schedulebook.domain.admin.dto.response.CleanupOutboxResponse;
import com.example.schedulebook.domain.admin.dto.response.OutboxResponse;
import com.example.schedulebook.domain.admin.dto.response.OutboxStatsResponse;
import com.example.schedulebook.domain.outbox.entity.Outbox;
import com.example.schedulebook.domain.outbox.enums.OutboxStatus;
import com.example.schedulebook.domain.outbox.repository.OutboxRepository;
import com.example.schedulebook.domain.outbox.service.OutboxCleanupService;
import com.example.schedulebook.domain.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminOutboxService {
    private final OutboxRepository outboxRepository;
    private final UserValidator userValidator;
    private final OutboxCleanupService outboxCleanupService;

    @Transactional(readOnly = true)
    public PageResponse<OutboxResponse> findAllDeadOutboxes(Long currentUserId, Pageable pageable) {
        userValidator.validateActiveAdmin(currentUserId);

        Page<Outbox> outboxPage = outboxRepository.findAllByStatus(OutboxStatus.DEAD, pageable);

        return PageResponse.register(outboxPage.map(OutboxResponse::from));
    }

    public void retryOutbox(Long currentUserId, Long outboxId) {
        userValidator.validateActiveAdmin(currentUserId);

        Outbox outbox = outboxRepository.findById(outboxId).orElseThrow(
                () -> new BaseException(ErrorEnum.OUTBOX_NOT_FOUND)
        );

        outbox.retry();

        log.info("Outbox {} 수동 Retry", outboxId);
    }

    public CleanupOutboxResponse deleteSuccessOutbox(Long currentUserId, int days) {
        userValidator.validateActiveAdmin(currentUserId);

        int deleted = outboxCleanupService.cleanup(days);

        log.info("{}개의 SUCCESS Outbox 삭제", deleted);

        return new CleanupOutboxResponse(deleted);
    }

    @Transactional(readOnly = true)
    public OutboxStatsResponse getStats(Long currentUserId) {
        userValidator.validateActiveAdmin(currentUserId);

        Object[] result = outboxRepository.countStats();

        return new OutboxStatsResponse(
                toLong(result[0]),
                toLong(result[1]),
                toLong(result[2]),
                toLong(result[3]),
                toLong(result[4])
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<OutboxResponse> findAllFailedOutboxes(Long currentUserId, Pageable pageable) {
        userValidator.validateActiveAdmin(currentUserId);

        Page<Outbox> outboxPage = outboxRepository.findAllByStatus(OutboxStatus.FAILED, pageable);

        return PageResponse.register(outboxPage.map(OutboxResponse::from));
    }

    private long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
