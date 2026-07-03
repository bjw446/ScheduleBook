package com.example.schedulebook.domain.notification.event;

import com.example.schedulebook.domain.comment.entity.Comment;
import com.example.schedulebook.domain.comment.event.CommentCreatedEvent;
import com.example.schedulebook.domain.comment.repository.CommentRepository;
import com.example.schedulebook.domain.friend.event.FriendAcceptedEvent;
import com.example.schedulebook.domain.friend.event.FriendRequestEvent;
import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
import com.example.schedulebook.domain.notification.enums.NotificationEventType;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.repository.NotificationRepository;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.event.ScheduleSharedEvent;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.schedule.repository.ScheduleParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventHandler {
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final NotificationEventPublisher notificationEventPublisher;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final ScheduleRepository scheduleRepository;
    private final CommentRepository commentRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFriendRequest(FriendRequestEvent event) {
        try {
            notificationService.createFriendRequestNotification(event.receiverId(), event.requesterNickname(), event.friendId());

            publishCreatedEvent(event.receiverId(), NotificationType.FRIEND_REQUEST, event.requesterNickname());
        } catch (Exception e) {
            log.error("친구 신청 알림 비동기 처리 중 오류 발생 Event : {}", event, e);

            throw e;
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFriendAccepted(FriendAcceptedEvent event) {
        try {
            notificationService.createFriendAcceptedNotification(event.requesterId(), event.accepterNickname(), event.friendId());

            publishCreatedEvent(event.requesterId(), NotificationType.FRIEND_ACCEPTED, event.accepterNickname());
        } catch (Exception e) {
            log.error("친구 수락 알림 비동기 처리 중 오류 발생 Event : {}", event, e);

            throw e;
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleScheduleShared(ScheduleSharedEvent event) {
        try {
            notificationService.createScheduleSharedNotification(event.receiverId(), event.ownerNickname(), event.shareId());

            publishCreatedEvent(event.receiverId(), NotificationType.SCHEDULE_SHARED, event.ownerNickname());
        } catch (Exception e) {
            log.error("일정 공유 알림 비동기 처리 중 오류 발생 Event : {}", event, e);

            throw e;
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleScheduleComment(CommentCreatedEvent event) {
        try {
            if (event.parentCommentId() == null) {
                notifyScheduleParticipants(event);
            } else {
                notifyParentWriter(event);
            }
        } catch (Exception e) {
            log.error("댓글 알림 알림 비동기 처리 중 오류 발생 Event : {}", event, e);

            throw e;
        }
    }

    private void publishCreatedEvent(Long receiverId, NotificationType notificationType, String senderNickname) {
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

    private void notifyScheduleParticipants(CommentCreatedEvent event) {
        Schedule schedule = scheduleRepository.findWithOwner(event.scheduleId()).orElseThrow();

        Set<Long> receivers = new HashSet<>();

        receivers.add(schedule.getUser().getId());

        receivers.addAll(scheduleParticipantRepository.findParticipantIds(event.scheduleId()));

        receivers.remove(event.writerId());

        for (Long receiverId : receivers) {
            try {
                notificationService.createScheduleCommentNotification(receiverId, event.writerNickname(), schedule.getId());

                publishCreatedEvent(receiverId, NotificationType.SCHEDULE_COMMENT, event.writerNickname());
            } catch (Exception e) {
                log.error("일정 댓글 알림 생성 실패 receiverId : {}, scheduleId : {}", receiverId, schedule.getId(), e);
            }
        }
    }

    private void notifyParentWriter(CommentCreatedEvent event) {
        Comment parent = commentRepository.findWithWriter(event.parentCommentId()).orElseThrow();

        Long receiverId = parent.getWriter().getId();

        if (receiverId.equals(event.writerId())) {
            return;
        }

        notificationService.createCommentReplyNotification(receiverId, event.writerNickname(), event.scheduleId());

        publishCreatedEvent(receiverId, NotificationType.COMMENT_REPLY, event.writerNickname());
    }
}
