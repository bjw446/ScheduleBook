package com.example.schedulebook.domain.notification.entity;

import com.example.schedulebook.common.entity.DeleteEntity;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "notifications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_receiver_target_type",
                        columnNames = {"receiver_id", "target_id", "notification_type"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends DeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false, name = "is_read")
    private boolean isRead;

    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    public static Notification create(User receiver, NotificationType notificationType, String title, String content, Long targetId) {
        Notification notification = new Notification();

        notification.receiver = receiver;
        notification.notificationType = notificationType;
        notification.title = title;
        notification.content = content;
        notification.isRead = false;
        notification.targetId = targetId;

        return notification;
    }

    public void read() {
        if (isRead) {
            throw new BaseException(ErrorEnum.NOTIFICATION_ALREADY_READ);
        }

        this.isRead = true;
    }
}
