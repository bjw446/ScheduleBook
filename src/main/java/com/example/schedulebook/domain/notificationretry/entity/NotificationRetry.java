package com.example.schedulebook.domain.notificationretry.entity;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.entity.ModifyEntity;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notificationretry.enums.NotificationRetryStatus;
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

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

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
    private NotificationRetryStatus notificationRetryStatus;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "claim_token")
    private String claimToken;

    public static NotificationRetry create(
            String eventId,
            Long outboxId,
            Long receiverId,
            NotificationType notificationType,
            String payload,
            String reason
    ) {
        NotificationRetry notificationRetry = new NotificationRetry();

        notificationRetry.eventId = eventId;
        notificationRetry.outboxId = outboxId;
        notificationRetry.receiverId = receiverId;
        notificationRetry.notificationType = notificationType;
        notificationRetry.payload = payload;
        notificationRetry.retryCount = 0;
        notificationRetry.notificationRetryStatus = NotificationRetryStatus.PENDING;
        notificationRetry.reason = reason;
        notificationRetry.nextRetryAt = LocalDateTime.now().plusSeconds(CommonConst.NEXT_RETRY_DELAY);

        return notificationRetry;
    }
}
