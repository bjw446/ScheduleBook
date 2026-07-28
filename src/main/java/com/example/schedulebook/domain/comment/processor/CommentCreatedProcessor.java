package com.example.schedulebook.domain.comment.processor;

import com.example.schedulebook.domain.comment.entity.Comment;
import com.example.schedulebook.domain.comment.event.CommentCreatedEvent;
import com.example.schedulebook.domain.comment.repository.CommentRepository;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.processor.NotificationEventProcessor;
import com.example.schedulebook.domain.notification.service.NotificationRetryService;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.scheduleparticipant.repository.ScheduleParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentCreatedProcessor implements NotificationEventProcessor<CommentCreatedEvent> {
    private final NotificationService notificationService;
    private final CommentRepository commentRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final NotificationRetryService notificationRetryService;

    @Override
    public Class<CommentCreatedEvent> supports() {
        return CommentCreatedEvent.class;
    }

    @Override
    public void process(Long outboxId, CommentCreatedEvent event) {
        if (event.parentCommentId() == null) {
            notifyScheduleParticipants(outboxId, event);
        } else {
            notifyParentWriter(outboxId, event);
        }
    }

    private void notifyScheduleParticipants(Long outboxId, CommentCreatedEvent event) {
        Schedule schedule = scheduleRepository.findWithOwner(event.scheduleId()).orElseThrow();

        Set<Long> receivers = new HashSet<>();

        receivers.add(schedule.getUser().getId());

        receivers.addAll(scheduleParticipantRepository.findParticipantIds(event.scheduleId()));

        receivers.remove(event.writerId());

        for (Long receiverId : receivers) {
            try {
                notificationService.createScheduleCommentNotification(receiverId, event.writerNickname(), schedule.getId());

            } catch (Exception e) {
                saveNotificationRetry(outboxId, receiverId, NotificationType.SCHEDULE_COMMENT, event, e);
            }
        }
    }

    private void notifyParentWriter(Long outboxId, CommentCreatedEvent event) {
        Comment parent = commentRepository.findWithWriter(event.parentCommentId()).orElseThrow();

        Long receiverId = parent.getWriter().getId();

        if (receiverId.equals(event.writerId())) {
            return;
        }

        try {
            notificationService.createCommentReplyNotification(receiverId, event.writerNickname(), event.scheduleId());

        } catch (Exception e) {
            saveNotificationRetry(outboxId, receiverId, NotificationType.COMMENT_REPLY, event, e);
        }
    }

    private void saveNotificationRetry(
            Long outboxId,
            Long receiverId,
            NotificationType notificationType,
            Object event,
            Exception e
    ) {
        try {
            log.error("Notification Retry 저장 outboxId = {}, receiverId = {}, type = {}", outboxId, receiverId, notificationType, e);

            notificationRetryService.save(
                    outboxId,
                    receiverId,
                    notificationType,
                    event,
                    e.getMessage()
            );

        } catch (Exception ex) {
            log.error("일정 댓글 생성 Retry 저장 실패", ex);
        }
    }
}
