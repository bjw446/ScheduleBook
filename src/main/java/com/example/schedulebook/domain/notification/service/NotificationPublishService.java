package com.example.schedulebook.domain.notification.service;

import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
import com.example.schedulebook.domain.notification.enums.NotificationEventType;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.publisher.NotificationEventPublisher;
import com.example.schedulebook.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationPublishService {
    private final NotificationRepository notificationRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    public void publish(Long receiverId, NotificationType notificationType, String senderNickname) {
        long unreadCount = notificationRepository.countUnreadNotifications(receiverId);

        String fullMessage = senderNickname + notificationType.getDefaultMessage();

        NotificationEventResponse response = new NotificationEventResponse(
                NotificationEventType.CREATED,
                receiverId,
                null,
                notificationType.name(),
                notificationType.getTitle(),
                fullMessage,
                unreadCount,
                System.currentTimeMillis()
        );

        notificationEventPublisher.publish(response);
    }
}
