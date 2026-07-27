package com.example.schedulebook.domain.user.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
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


    public void process(Long outboxId, UserWithdrawEvent event) {
        log.info("회원 탈퇴 후처리 시작 outboxId = {}, userId = {}", outboxId, event.userId());

        if (!friendCleanupProcessor.process(outboxId, event.userId())) {
            throwException("FriendCleanup", outboxId, event.userId());
        }

        if (!commentCleanupProcessor.process(outboxId, event.userId())) {
            throwException("CommentCleanup", outboxId, event.userId());
        }

        if (!scheduleCleanupProcessor.process(outboxId, event.userId())) {
            throwException("ScheduleCleanup", outboxId, event.userId());
        }

        if (!notificationCleanupProcessor.process(outboxId, event.userId())) {
            throwException("NotificationCleanup", outboxId, event.userId());
        }

        if (!chatRoomCleanupProcessor.process(outboxId, event.userId())) {
            throwException("ChatRoomCleanup", outboxId, event.userId());
        }

        if (!redisCleanupProcessor.process(outboxId, event.userId())) {
            throwException("RedisCleanup", outboxId, event.userId());
        }

        log.info("회원 탈퇴 후처리 완료 outboxId = {}, userId = {}", outboxId, event.userId());
    }

    private void throwException(String processName, Long outboxId, Long userId) {
        log.error("회원 탈퇴 후처리 {} 실패 outboxId = {}, userId = {}", processName, outboxId, userId);

        throw new BaseException(ErrorEnum.USER_WITHDRAW_PROCESS_FAILED);
    }
}
