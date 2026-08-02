package com.example.schedulebook.domain.outbox.enums;

import com.example.schedulebook.domain.auth.event.AuditEvent;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.example.schedulebook.domain.comment.dto.response.CommentEventResponse;
import com.example.schedulebook.domain.comment.event.CommentCreatedEvent;
import com.example.schedulebook.domain.friend.event.FriendAcceptedEvent;
import com.example.schedulebook.domain.friend.event.FriendRequestedEvent;
import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
import com.example.schedulebook.domain.schedule.event.ScheduleCanceledEvent;
import com.example.schedulebook.domain.schedule.event.ScheduleDeletedEvent;
import com.example.schedulebook.domain.schedule.event.ScheduleReminderEvent;
import com.example.schedulebook.domain.schedule.event.ScheduleUpdatedEvent;
import com.example.schedulebook.domain.scheduleshare.event.ScheduleSharedEvent;
import com.example.schedulebook.domain.user.event.UserWithdrawEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OutboxEventType {
    COMMENT_CREATED(CommentCreatedEvent.class),
    SCHEDULE_SHARED(ScheduleSharedEvent.class),
    SCHEDULE_UPDATED(ScheduleUpdatedEvent.class),
    SCHEDULE_CANCELED(ScheduleCanceledEvent.class),
    SCHEDULE_DELETED(ScheduleDeletedEvent.class),
    SCHEDULE_REMINDER(ScheduleReminderEvent.class),
    FRIEND_ACCEPTED(FriendAcceptedEvent.class),
    FRIEND_REQUESTED(FriendRequestedEvent.class),
    USER_WITHDRAW(UserWithdrawEvent.class),
    NOTIFICATION_EVENT(NotificationEventResponse.class),
    COMMENT_EVENT(CommentEventResponse.class),
    AUDIT_EVENT(AuditEvent.class);

    private final Class<?> eventClass;
}
