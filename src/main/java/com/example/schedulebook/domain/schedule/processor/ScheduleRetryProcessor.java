package com.example.schedulebook.domain.schedule.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.notificationretry.entity.NotificationRetry;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.notificationretry.service.ProcessedNotificationRetryService;
import com.example.schedulebook.domain.scheduleshare.event.ScheduleSharedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleRetryProcessor {
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final ProcessedNotificationRetryService processedNotificationRetryService;

    public void process(NotificationRetry notificationRetry) {
        if (processedNotificationRetryService.prepareProcessedNotificationRetry(notificationRetry)) {
            return;
        }

        try {
            ScheduleSharedEvent event = objectMapper.readValue(
                    notificationRetry.getPayload(),
                    ScheduleSharedEvent.class
            );

            switch (notificationRetry.getNotificationType()) {
                case SCHEDULE_SHARED -> {
                    notificationService.createScheduleSharedNotification(
                            notificationRetry.getReceiverId(),
                            event.ownerNickname(),
                            event.shareId()
                    );
                }

                default ->
                        throw new BaseException(ErrorEnum.INVALID_NOTIFICATION_TYPE);
            }

        } catch (JsonProcessingException e) {
            log.error("알림 재시도 payload 역직렬화 실패", e);

            throw new BaseException(ErrorEnum.JSON_DESERIALIZATION_FAILED);
        }
    }
}

