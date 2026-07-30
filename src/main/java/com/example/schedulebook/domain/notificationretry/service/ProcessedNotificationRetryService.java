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

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessedNotificationRetryService {
    private final ProcessedNotificationRetryRepository processedNotificationRetryRepository;
    private final ProcessedNotificationRetryCreateService processedNotificationRetryCreateService;

    @Transactional(readOnly = true)
    public ProcessedNotificationRetry findByOutboxIdAndReceiverId(Long outboxId, Long receiverId) {
        return processedNotificationRetryRepository.findByOutboxIdAndReceiverId(outboxId, receiverId).orElse(null);
    }

    @Transactional
    public void markFailed(Long outboxId, Long receiverId, String owner) {
        int updated = processedNotificationRetryRepository.markFailed(outboxId, receiverId, owner);

        if (updated != 1) {
            log.warn("ProcessedNotificationRetry 실패 상태 변경 실패 outboxId = {}, receiverId = {}", outboxId, receiverId);

            throw new BaseException(ErrorEnum.PROCESSED_NOTIFICATION_RETRY_STATUS_CHANGE_FAILED);
        }

        log.debug("ProcessedNotificationRetry 실패 상태 변경 outboxId = {}, receiverId = {}", outboxId, receiverId);
    }

    @Transactional
    public void markSuccess(Long outboxId, Long receiverId, String owner) {
        int updated = processedNotificationRetryRepository.markSuccess(outboxId, receiverId, owner);

        if (updated != 1) {
            throw new BaseException(ErrorEnum.PROCESSED_NOTIFICATION_RETRY_STATUS_CHANGE_FAILED);
        }

        log.debug("ProcessedNotificationRetry 성공 상태 변경 outboxId = {}, receiverId = {}", outboxId, receiverId);
    }

    @Transactional
    public int markRetry(Long outboxId, Long receiverId, String owner) {
        int updated = processedNotificationRetryRepository.markRetry(outboxId, receiverId, owner);

        if (updated != 1) {
            log.warn("ProcessedNotificationRetry 재시도 상태 변경 실패 outboxId = {}, receiverId = {}", outboxId, receiverId);

            return updated;
        }

        log.debug("실패 한 ProcessedNotificationRetry 재시도 outboxId = {}, receiverId = {}", outboxId, receiverId);

        return updated;
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
                        notificationRetry.getReceiverId(),
                        notificationRetry.getClaimToken()
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
            int updated = markRetry(
                    notificationRetry.getOutboxId(),
                    notificationRetry.getReceiverId(),
                    notificationRetry.getClaimToken()
            );

            if (updated != 1) {
                return true;
            }

            return false;
        }

        if (processedNotificationRetry.getStatus() == ProcessedNotificationRetryStatus.PROCESSING) {
            if (!Objects.equals(processedNotificationRetry.getProcessingOwner(), notificationRetry.getClaimToken())) {
                return true;
            }

            return false;
        }

        return false;
    }
}
