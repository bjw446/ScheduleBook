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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserWithdrawProcessorTest {

    @Mock
    private FriendCleanupProcessor friendCleanupProcessor;

    @Mock
    private CommentCleanupProcessor commentCleanupProcessor;

    @Mock
    private ScheduleCleanupProcessor scheduleCleanupProcessor;

    @Mock
    private NotificationCleanupProcessor notificationCleanupProcessor;

    @Mock
    private ChatRoomCleanupProcessor chatRoomCleanupProcessor;

    @Mock
    private RedisCleanupProcessor redisCleanupProcessor;

    private UserWithdrawProcessor userWithdrawProcessor;

    private Long outboxId;
    private Long userId;
    private UserWithdrawEvent event;

    @BeforeEach
    void setUp() {
        userWithdrawProcessor = new UserWithdrawProcessor(
                friendCleanupProcessor,
                commentCleanupProcessor,
                scheduleCleanupProcessor,
                notificationCleanupProcessor,
                chatRoomCleanupProcessor,
                redisCleanupProcessor
        );

        outboxId = 100L;
        userId = 1L;
        event = new UserWithdrawEvent(
                "event-id",
                userId,
                "test123"
        );
    }

    @Test
    void 모든_탈퇴_후처리가_성공하면_정상_완료된다() {
        // given
        given(friendCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(commentCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(scheduleCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(notificationCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(chatRoomCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(redisCleanupProcessor.process(outboxId, userId)).willReturn(true);

        // when & then
        userWithdrawProcessor.process(outboxId, event);

        InOrder inOrder = inOrder(
                friendCleanupProcessor,
                commentCleanupProcessor,
                scheduleCleanupProcessor,
                notificationCleanupProcessor,
                chatRoomCleanupProcessor,
                redisCleanupProcessor
        );

        inOrder.verify(friendCleanupProcessor).process(outboxId, userId);
        inOrder.verify(commentCleanupProcessor).process(outboxId, userId);
        inOrder.verify(scheduleCleanupProcessor).process(outboxId, userId);
        inOrder.verify(notificationCleanupProcessor).process(outboxId, userId);
        inOrder.verify(chatRoomCleanupProcessor).process(outboxId, userId);
        inOrder.verify(redisCleanupProcessor).process(outboxId, userId);
    }

    @Test
    void 친구_후처리가_실패하면_예외를_발생시키고_이후_후처리는_실행하지_않는다() {
        // given
        given(friendCleanupProcessor.process(outboxId, userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() ->
                userWithdrawProcessor.process(outboxId, event)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.USER_WITHDRAW_PROCESS_FAILED);

        verify(friendCleanupProcessor).process(outboxId, userId);
        verify(commentCleanupProcessor, never()).process(outboxId, userId);
        verify(scheduleCleanupProcessor, never()).process(outboxId, userId);
        verify(notificationCleanupProcessor, never()).process(outboxId, userId);
        verify(chatRoomCleanupProcessor, never()).process(outboxId, userId);
        verify(redisCleanupProcessor, never()).process(outboxId, userId);
    }

    @Test
    void 댓글_후처리가_실패하면_예외를_발생시키고_이후_후처리는_실행하지_않는다() {
        // given
        given(friendCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(commentCleanupProcessor.process(outboxId, userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() ->
                userWithdrawProcessor.process(outboxId, event)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.USER_WITHDRAW_PROCESS_FAILED);

        verify(friendCleanupProcessor).process(outboxId, userId);
        verify(commentCleanupProcessor).process(outboxId, userId);

        verify(scheduleCleanupProcessor, never()).process(outboxId, userId);
        verify(notificationCleanupProcessor, never()).process(outboxId, userId);
        verify(chatRoomCleanupProcessor, never()).process(outboxId, userId);
        verify(redisCleanupProcessor, never()).process(outboxId, userId);
    }

    @Test
    void 일정_후처리가_실패하면_예외를_발생시키고_이후_후처리는_실행하지_않는다() {
        // given
        given(friendCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(commentCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(scheduleCleanupProcessor.process(outboxId, userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() ->
                userWithdrawProcessor.process(outboxId, event)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.USER_WITHDRAW_PROCESS_FAILED);

        verify(friendCleanupProcessor).process(outboxId, userId);
        verify(commentCleanupProcessor).process(outboxId, userId);
        verify(scheduleCleanupProcessor).process(outboxId, userId);

        verify(notificationCleanupProcessor, never()).process(outboxId, userId);
        verify(chatRoomCleanupProcessor, never()).process(outboxId, userId);
        verify(redisCleanupProcessor, never()).process(outboxId, userId);
    }

    @Test
    void 알림_후처리가_실패하면_예외를_발생시키고_이후_후처리는_실행하지_않는다() {
        // given
        given(friendCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(commentCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(scheduleCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(notificationCleanupProcessor.process(outboxId, userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() ->
                userWithdrawProcessor.process(outboxId, event)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.USER_WITHDRAW_PROCESS_FAILED);

        verify(friendCleanupProcessor).process(outboxId, userId);
        verify(commentCleanupProcessor).process(outboxId, userId);
        verify(scheduleCleanupProcessor).process(outboxId, userId);
        verify(notificationCleanupProcessor).process(outboxId, userId);

        verify(chatRoomCleanupProcessor, never()).process(outboxId, userId);
        verify(redisCleanupProcessor, never()).process(outboxId, userId);
    }

    @Test
    void 채팅방_후처리가_실패하면_예외를_발생시키고_이후_Redis_후처리는_실행하지_않는다() {
        // given
        given(friendCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(commentCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(scheduleCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(notificationCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(chatRoomCleanupProcessor.process(outboxId, userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() ->
                userWithdrawProcessor.process(outboxId, event)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.USER_WITHDRAW_PROCESS_FAILED);

        verify(friendCleanupProcessor).process(outboxId, userId);
        verify(commentCleanupProcessor).process(outboxId, userId);
        verify(scheduleCleanupProcessor).process(outboxId, userId);
        verify(notificationCleanupProcessor).process(outboxId, userId);
        verify(chatRoomCleanupProcessor).process(outboxId, userId);

        verify(redisCleanupProcessor, never()).process(outboxId, userId);
    }

    @Test
    void Redis_후처리가_실패하면_예외를_발생시킨다() {
        // given
        given(friendCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(commentCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(scheduleCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(notificationCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(chatRoomCleanupProcessor.process(outboxId, userId)).willReturn(true);
        given(redisCleanupProcessor.process(outboxId, userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() ->
                userWithdrawProcessor.process(outboxId, event)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.USER_WITHDRAW_PROCESS_FAILED);

        verify(friendCleanupProcessor).process(outboxId, userId);
        verify(commentCleanupProcessor).process(outboxId, userId);
        verify(scheduleCleanupProcessor).process(outboxId, userId);
        verify(notificationCleanupProcessor).process(outboxId, userId);
        verify(chatRoomCleanupProcessor).process(outboxId, userId);
        verify(redisCleanupProcessor).process(outboxId, userId);
    }
}