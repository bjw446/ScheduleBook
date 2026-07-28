package com.example.schedulebook.domain.notification.entity;

import com.example.schedulebook.common.entity.ModifyEntity;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.enums.RetryStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "notification_retry")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationRetry extends ModifyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outbox_id", nullable = false)
    private Long outboxId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "retry_status", nullable = false)
    private RetryStatus retryStatus;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    public static NotificationRetry create(
            Long outboxId,
            Long receiverId,
            NotificationType notificationType,
            String payload,
            String reason
    ) {
        NotificationRetry notificationRetry = new NotificationRetry();

        notificationRetry.outboxId = outboxId;
        notificationRetry.receiverId = receiverId;
        notificationRetry.notificationType = notificationType;
        notificationRetry.payload = payload;
        notificationRetry.retryCount = 0;
        notificationRetry.retryStatus = RetryStatus.PENDING;
        notificationRetry.reason = reason;
        notificationRetry.nextRetryAt = LocalDateTime.now().plusSeconds(30);

        return notificationRetry;
    }
}
