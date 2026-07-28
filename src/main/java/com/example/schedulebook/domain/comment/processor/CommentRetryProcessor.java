package com.example.schedulebook.domain.comment.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.comment.event.CommentCreatedEvent;
import com.example.schedulebook.domain.notification.entity.NotificationRetry;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.outbox.service.ProcessedOutboxTransactionService;
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
    private final ProcessedOutboxTransactionService processedOutboxTransactionService;

    public void process(NotificationRetry notificationRetry) {
        if (processedOutboxTransactionService.isAlreadyProcessed(notificationRetry.getOutboxId())) {
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

                    processedOutboxTransactionService.markProcessedOutboxSuccess(notificationRetry.getOutboxId());
                }

                case COMMENT_REPLY -> {
                    notificationService.createCommentReplyNotification(
                            notificationRetry.getReceiverId(),
                            event.writerNickname(),
                            event.scheduleId()
                    );

                    processedOutboxTransactionService.markProcessedOutboxSuccess(notificationRetry.getOutboxId());
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
