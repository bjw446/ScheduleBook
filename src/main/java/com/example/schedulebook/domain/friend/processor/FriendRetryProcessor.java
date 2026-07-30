package com.example.schedulebook.domain.friend.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.friend.event.FriendAcceptedEvent;
import com.example.schedulebook.domain.friend.event.FriendRequestedEvent;
import com.example.schedulebook.domain.notificationretry.entity.NotificationRetry;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.notificationretry.service.ProcessedNotificationRetryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FriendRetryProcessor {
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final ProcessedNotificationRetryService processedNotificationRetryService;

    public void process(NotificationRetry notificationRetry) {
        if (processedNotificationRetryService.prepareProcessedNotificationRetry(notificationRetry)) {
            return;
        }

        try {
            switch (notificationRetry.getNotificationType()) {
                case FRIEND_ACCEPTED -> {
                    FriendAcceptedEvent event = objectMapper.readValue(
                            notificationRetry.getPayload(),
                            FriendAcceptedEvent.class
                    );

                    notificationService.createFriendAcceptedNotification(
                            notificationRetry.getReceiverId(),
                            event.accepterNickname(),
                            event.friendId()
                    );
                }

                case FRIEND_REQUEST -> {
                    FriendRequestedEvent event = objectMapper.readValue(
                            notificationRetry.getPayload(),
                            FriendRequestedEvent.class
                    );

                    notificationService.createFriendRequestNotification(
                            notificationRetry.getReceiverId(),
                            event.requesterNickname(),
                            event.friendId()
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