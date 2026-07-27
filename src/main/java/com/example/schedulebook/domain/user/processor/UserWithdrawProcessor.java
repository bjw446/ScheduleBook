package com.example.schedulebook.domain.user.processor;

import com.example.schedulebook.common.redis.processor.RedisCleanupProcessor;
import com.example.schedulebook.domain.chatroom.processor.ChatRoomCleanupProcessor;
import com.example.schedulebook.domain.comment.processor.CommentCleanupProcessor;
import com.example.schedulebook.domain.friend.processor.FriendCleanupProcessor;
import com.example.schedulebook.domain.notification.processor.NotificationCleanupProcessor;
import com.example.schedulebook.domain.schedule.processor.ScheduleCleanupProcessor;
import com.example.schedulebook.domain.user.event.UserWithdrawEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserWithdrawProcessor {
    private final FriendCleanupProcessor friendCleanupProcessor;
    private final CommentCleanupProcessor commentCleanupProcessor;
    private final ScheduleCleanupProcessor scheduleCleanupProcessor;
    private final NotificationCleanupProcessor notificationCleanupProcessor;
    private final ChatRoomCleanupProcessor chatRoomCleanupProcessor;
    private final RedisCleanupProcessor redisCleanupProcessor;


    public void process(UserWithdrawEvent event) {
        log.info("회원 탈퇴 후처리 시작 userId = {}", event.userId());

        friendCleanupProcessor.process(event.userId());

        commentCleanupProcessor.process(event.userId());

        scheduleCleanupProcessor.process(event.userId());

        notificationCleanupProcessor.process(event.userId());

        chatRoomCleanupProcessor.process(event.userId());

        redisCleanupProcessor.process(event.userId());
    }
}
