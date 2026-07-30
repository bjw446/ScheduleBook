package com.example.schedulebook.domain.notificationretry.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.notificationretry.entity.NotificationRetry;
import com.example.schedulebook.domain.notificationretry.entity.ProcessedNotificationRetry;
import com.example.schedulebook.domain.notificationretry.enums.ProcessedNotificationRetryStatus;
import com.example.schedulebook.domain.notificationretry.repository.ProcessedNotificationRetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProcessedNotificationRetryService {
    private final ProcessedNotificationRetryRepository processedNotificationRetryRepository;
    private final ProcessedNotificationRetryCreateService processedNotificationRetryCreateService;

    public ProcessedNotificationRetry findByOutboxIdAndReceiverId(Long outboxId, Long receiverId) {
        return processedNotificationRetryRepository.findByOutboxIdAndReceiverId(outboxId, receiverId).orElse(null);
    }

    @Transactional
    public void markFailed(Long outboxId, Long receiverId) {
        int updated = processedNotificationRetryRepository.markFailed(outboxId, receiverId);

        if (updated != 1) {
            log.warn("ProcessedNotificationRetry 실패 상태 변경 실패 outboxId = {}, receiverId = {}", outboxId, receiverId);

            throw new BaseException(ErrorEnum.PROCESSED_NOTIFICATION_RETRY_STATUS_CHANGE_FAILED);
        }

        log.debug("ProcessedNotificationRetry 실패 상태 변경 outboxId = {}, receiverId = {}", outboxId, receiverId);
    }

    @Transactional
    public void markSuccess(Long outboxId, Long receiverId) {
        int updated = processedNotificationRetryRepository.markSuccess(outboxId, receiverId);

        if (updated != 1) {
            log.warn("ProcessedNotificationRetry 이미 성공 상태 outboxId = {}, receiverId = {}", outboxId, receiverId);

            return;
        }

        log.debug("ProcessedNotificationRetry 성공 상태 변경 outboxId = {}, receiverId = {}", outboxId, receiverId);
    }

    @Transactional
    public void markRetry(Long outboxId, Long receiverId) {
        int updated = processedNotificationRetryRepository.markRetry(outboxId, receiverId);

        if (updated != 1) {
            log.warn("ProcessedNotificationRetry 재시도 상태 변경 실패 outboxId = {}, receiverId = {}", outboxId, receiverId);

            throw new BaseException(ErrorEnum.PROCESSED_NOTIFICATION_RETRY_STATUS_CHANGE_FAILED);
        }

        log.debug("실패 한 ProcessedNotificationRetry 재시도 outboxId = {}, receiverId = {}", outboxId, receiverId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean prepareProcessedNotificationRetry(NotificationRetry notificationRetry) {
        ProcessedNotificationRetry processedNotificationRetry = findByOutboxIdAndReceiverId(
                notificationRetry.getOutboxId(),
                notificationRetry.getReceiverId()
        );

        if (processedNotificationRetry == null) {
            try {
                processedNotificationRetry = processedNotificationRetryCreateService.create(
                        notificationRetry.getOutboxId(),
                        notificationRetry.getReceiverId()
                );

            } catch (DataIntegrityViolationException e) {
                processedNotificationRetry = findByOutboxIdAndReceiverId(notificationRetry.getOutboxId(), notificationRetry.getReceiverId());

                log.debug("ProcessedNotificationRetry가 이미 생성 되었습니다. outboxId = {}, receiverId = {}",
                        notificationRetry.getOutboxId(),
                        notificationRetry.getReceiverId()
                );
            }
        }

        if (processedNotificationRetry == null) {
            throw new BaseException(ErrorEnum.PROCESSED_NOTIFICATION_RETRY_NOT_FOUND);
        }

        if (processedNotificationRetry.getStatus() == ProcessedNotificationRetryStatus.SUCCESS) {
            return true;
        }

        if (processedNotificationRetry.getStatus() == ProcessedNotificationRetryStatus.FAILED) {
            markRetry(notificationRetry.getOutboxId(), notificationRetry.getReceiverId());
            return false;
        }

        return false;
    }
}
