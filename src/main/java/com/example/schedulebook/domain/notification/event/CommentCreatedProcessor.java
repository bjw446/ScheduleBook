package com.example.schedulebook.domain.notification.event;

import com.example.schedulebook.domain.comment.entity.Comment;
import com.example.schedulebook.domain.comment.event.CommentCreatedEvent;
import com.example.schedulebook.domain.comment.repository.CommentRepository;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.service.NotificationPublishService;
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
    private final NotificationPublishService notificationPublishService;

    @Override
    public Class<CommentCreatedEvent> supports() {
        return CommentCreatedEvent.class;
    }

    @Override
    public void process(CommentCreatedEvent event) {
        if (event.parentCommentId() == null) {
            notifyScheduleParticipants(event);
        } else {
            notifyParentWriter(event);
        }
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

                notificationPublishService.publish(receiverId, NotificationType.SCHEDULE_COMMENT, event.writerNickname());
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

        notificationPublishService.publish(receiverId, NotificationType.COMMENT_REPLY, event.writerNickname());
    }
}
