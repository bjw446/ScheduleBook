package com.example.schedulebook.domain.comment.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.comment.event.CommentCreatedEvent;
import com.example.schedulebook.domain.notificationretry.entity.NotificationRetry;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.notificationretry.service.ProcessedNotificationRetryTransactionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentRetryProcessor {
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final ProcessedNotificationRetryTransactionService processedNotificationRetryTransactionService;

    public void process(NotificationRetry notificationRetry) {
        if (processedNotificationRetryTransactionService.prepareProcessedNotificationRetry(notificationRetry)) {
            return;
        }

        try {
            CommentCreatedEvent event = objectMapper.readValue(notificationRetry.getPayload(), CommentCreatedEvent.class);

            switch (notificationRetry.getNotificationType()) {
                case SCHEDULE_COMMENT -> {
                    notificationService.createScheduleCommentNotification(
                            notificationRetry.getReceiverId(),
                            event.writerNickname(),
                            event.scheduleId()
                    );

                    processedNotificationRetryTransactionService.markSuccess(
                            notificationRetry.getOutboxId(),
                            notificationRetry.getReceiverId()
                    );
                }

                case COMMENT_REPLY -> {
                    notificationService.createCommentReplyNotification(
                            notificationRetry.getReceiverId(),
                            event.writerNickname(),
                            event.scheduleId()
                    );

                    processedNotificationRetryTransactionService.markSuccess(
                            notificationRetry.getOutboxId(),
                            notificationRetry.getReceiverId()
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
