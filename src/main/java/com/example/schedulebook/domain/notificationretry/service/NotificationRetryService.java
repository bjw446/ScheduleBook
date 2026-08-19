package com.example.schedulebook.domain.notificationretry.service;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.notificationretry.entity.NotificationRetry;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notificationretry.repository.NotificationRetryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryService {
    private final NotificationRetryRepository notificationRetryRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(String eventId, Long outboxId, Long receiverId, NotificationType notificationType, Object event, String reason) {
        try {
            String json = objectMapper.writeValueAsString(event);

            notificationRetryRepository.save(NotificationRetry.create(
                    eventId,
                    outboxId,
                    receiverId,
                    notificationType,
                    json,
                    reason
            ));

        }  catch (JsonProcessingException exception) {
            log.error("알림 재시도 payload 직렬화 실패", exception);
            throw new BaseException(ErrorEnum.JSON_SERIALIZATION_FAILED);
        }
    }

    @Transactional
    public void markSuccess(Long notificationRetryId, String claimToken) {
        if (notificationRetryRepository.markSuccess(notificationRetryId, claimToken) != 1) {
            throw new BaseException(ErrorEnum.NOTIFICATION_RETRY_NOT_FOUND);
        }
    }

    @Transactional
    public void markFailed(Long notificationRetryId, String reason, String claimToken) {
        if (notificationRetryRepository.markFailed(notificationRetryId, reason, claimToken) != 1) {
            throw new BaseException(ErrorEnum.NOTIFICATION_RETRY_NOT_FOUND);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String markProcessing(Long notificationRetryId) {
        String claimToken = UUID.randomUUID().toString();

        boolean claimed = notificationRetryRepository.markProcessing(
                notificationRetryId,
                claimToken,
                LocalDateTime.now().minusMinutes(10)
        ) == 1;

        if (claimed) {
            return claimToken;
        }

        return null;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRetry(Long notificationRetryId, String reason, int retryCount, String claimToken) {
        LocalDateTime delay = LocalDateTime.now().plusSeconds(nextDelaySeconds(retryCount + 1));

        if (notificationRetryRepository.markRetry(notificationRetryId, reason, delay, claimToken) != 1) {
            throw new BaseException(ErrorEnum.NOTIFICATION_RETRY_NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationRetry> findRetryTargets(int size) {
        return notificationRetryRepository.findRetryTargets(
                LocalDateTime.now().minusMinutes(10),
                PageRequest.of(0, size)
        ).getContent();
    }

    @Transactional(readOnly = true)
    public NotificationRetry findById(Long notificationRetryId) {
        return notificationRetryRepository.findById(notificationRetryId).orElseThrow(
                () -> new BaseException(ErrorEnum.NOTIFICATION_RETRY_NOT_FOUND)
        );
    }

    private long nextDelaySeconds(int retryCount) {
        int safeRetry = Math.max(0, Math.min(retryCount, 10));

        return Math.min(CommonConst.NEXT_RETRY_DELAY * (1L << safeRetry), 3600L);
    }
}
