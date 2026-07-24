package com.example.schedulebook.domain.outbox.enums;

import com.example.schedulebook.domain.comment.event.CommentCreatedEvent;
import com.example.schedulebook.domain.friend.event.FriendAcceptedEvent;
import com.example.schedulebook.domain.friend.event.FriendRequestedEvent;
import com.example.schedulebook.domain.scheduleshare.event.ScheduleSharedEvent;
import com.example.schedulebook.domain.user.event.UserWithdrawEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OutboxEventType {
    COMMENT_CREATED(CommentCreatedEvent.class),
    SCHEDULE_SHARED(ScheduleSharedEvent.class),
    FRIEND_ACCEPTED(FriendAcceptedEvent.class),
    FRIEND_REQUESTED(FriendRequestedEvent.class),
    USER_WITHDRAW(UserWithdrawEvent.class);

    private final Class<?> eventClass;
}
