package com.example.schedulebook.domain.notification.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.notification.entity.NotificationRetry;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.repository.NotificationRetryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationRetryService {
    private final NotificationRetryRepository notificationRetryRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(Long outboxId, Long receiverId, NotificationType notificationType, Object event, String reason) {
        try {
            String json = objectMapper.writeValueAsString(event);

            notificationRetryRepository.save(NotificationRetry.create(
                    outboxId,
                    receiverId,
                    notificationType,
                    json,
                    reason
            ));

        }  catch (JsonProcessingException exception) {
            throw new BaseException(ErrorEnum.JSON_SERIALIZATION_FAILED);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long notificationRetryId) {
        if (notificationRetryRepository.markSuccess(notificationRetryId) != 1) {
            throw new BaseException(ErrorEnum.NOTIFICATION_RETRY_NOT_FOUND);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long notificationRetryId, String reason) {
        if (notificationRetryRepository.markFailed(notificationRetryId, reason) != 1) {
            throw new BaseException(ErrorEnum.NOTIFICATION_RETRY_NOT_FOUND);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markProcessing(Long notificationRetryId) {
        return notificationRetryRepository.markProcessing(notificationRetryId) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRetry(Long notificationRetryId, String reason, int retryCount) {
        LocalDateTime delay = LocalDateTime.now().plusSeconds(nextDelaySeconds(retryCount));

        if (notificationRetryRepository.markRetry(notificationRetryId, reason, delay) != 1) {
            throw new BaseException(ErrorEnum.NOTIFICATION_RETRY_NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationRetry> findRetryTargets() {
        return notificationRetryRepository.findRetryTargets(LocalDateTime.now().minusMinutes(10));
    }

    private long nextDelaySeconds(int retryCount) {
        return Math.min(30L * (1L << retryCount), 3600L);
    }
}
