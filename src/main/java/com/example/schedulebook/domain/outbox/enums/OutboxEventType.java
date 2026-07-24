package com.example.schedulebook.domain.outbox.enums;

import com.example.schedulebook.domain.comment.event.CommentCreatedEvent;
import com.example.schedulebook.domain.friend.event.FriendAcceptedEvent;
import com.example.schedulebook.domain.schedule.event.ScheduleCanceledEvent;
import com.example.schedulebook.domain.schedule.event.ScheduleDeletedEvent;
import com.example.schedulebook.domain.schedule.event.ScheduleUpdatedEvent;
import com.example.schedulebook.domain.user.event.UserWithdrawEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OutboxEventType {
    COMMENT_CREATED(CommentCreatedEvent.class),
    SCHEDULE_Canceled(ScheduleCanceledEvent.class),
    SCHEDULE_UPDATED(ScheduleUpdatedEvent.class),
    SCHEDULE_DELETED(ScheduleDeletedEvent.class),
    FRIEND_ACCEPTED(FriendAcceptedEvent.class),
    USER_WITHDRAW(UserWithdrawEvent.class);

    private final Class<?> eventClass;
}
