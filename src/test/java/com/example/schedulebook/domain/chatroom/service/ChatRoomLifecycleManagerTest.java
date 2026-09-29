package com.example.schedulebook.domain.chatroom.service;

import com.example.schedulebook.domain.chatmessage.dto.request.PublishChatMessage;
import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;
import com.example.schedulebook.domain.chatmessage.publisher.ChatMessagePublisher;
import com.example.schedulebook.domain.chatmessage.service.ChatMessageManager;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import com.example.schedulebook.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatRoomLifecycleManagerTest {

    private ChatMessageManager chatMessageManager;
    private ChatMessagePublisher chatMessagePublisher;
    private ChatRoomLifecycleManager chatRoomLifecycleManager;

    private ChatRoom chatRoom;
    private User owner;
    private User inviter;
    private User invitedUser;
    private ChatMessage chatMessage;
    private PublishChatMessage publishChatMessage;

    @BeforeEach
    void setUp() {
        chatMessageManager = mock(ChatMessageManager.class);
        chatMessagePublisher = mock(ChatMessagePublisher.class);

        chatRoomLifecycleManager = new ChatRoomLifecycleManager(
                chatMessageManager,
                chatMessagePublisher
        );

        chatRoom = mock(ChatRoom.class);
        owner = mock(User.class);
        inviter = mock(User.class);
        invitedUser = mock(User.class);

        chatMessage = mock(ChatMessage.class);
        publishChatMessage = mock(PublishChatMessage.class);

        when(publishChatMessage.chatMessage()).thenReturn(chatMessage);
        when(publishChatMessage.unreadCount()).thenReturn(3);
    }

    @Test
    void afterRoomCreated_그룹_채팅방_생성_시스템_메시지를_생성하고_발행한다() {
        // given
        when(chatMessageManager.createGroupRoomSystemMessage(chatRoom, owner))
                .thenReturn(publishChatMessage);

        // when
        chatRoomLifecycleManager.afterRoomCreated(chatRoom, owner);

        // then
        verify(chatMessageManager)
                .createGroupRoomSystemMessage(chatRoom, owner);

        verify(chatMessagePublisher)
                .publishMessage(chatMessage, 3);
    }

    @Test
    void afterMemberInvited_초대_시스템_메시지를_생성하고_발행한다() {
        // given
        List<User> invitedUsers = List.of(invitedUser);

        when(chatMessageManager.createInviteSystemMessage(
                chatRoom,
                inviter,
                invitedUsers
        )).thenReturn(publishChatMessage);

        // when
        chatRoomLifecycleManager.afterMemberInvited(
                chatRoom,
                inviter,
                invitedUsers
        );

        // then
        verify(chatMessageManager)
                .createInviteSystemMessage(
                        chatRoom,
                        inviter,
                        invitedUsers
                );

        verify(chatMessagePublisher)
                .publishMessage(chatMessage, 3);
    }

    @Test
    void afterRoomNameUpdated_채팅방_이름_변경_시스템_메시지를_생성하고_발행한다() {
        // given
        String oldName = "기존 채팅방";
        String newName = "새 채팅방";

        when(chatMessageManager.createUpdateNameSystemMessage(
                chatRoom,
                inviter,
                oldName,
                newName
        )).thenReturn(publishChatMessage);

        // when
        chatRoomLifecycleManager.afterRoomNameUpdated(
                chatRoom,
                inviter,
                oldName,
                newName
        );

        // then
        verify(chatMessageManager)
                .createUpdateNameSystemMessage(
                        chatRoom,
                        inviter,
                        oldName,
                        newName
                );

        verify(chatMessagePublisher)
                .publishMessage(chatMessage, 3);
    }

    @Test
    void afterMemberLeft_퇴장_시스템_메시지를_생성하고_발행한다() {
        // given
        String nickname = "사용자";

        when(chatMessageManager.createLeaveSystemMessage(chatRoom, nickname))
                .thenReturn(publishChatMessage);

        // when
        chatRoomLifecycleManager.afterMemberLeft(chatRoom, nickname);

        // then
        verify(chatMessageManager)
                .createLeaveSystemMessage(chatRoom, nickname);

        verify(chatMessagePublisher)
                .publishMessage(chatMessage, 3);
    }

    @Test
    void 시스템_메시지_생성이_실패하면_메시지를_발행하지_않는다() {
        // given
        when(chatMessageManager.createLeaveSystemMessage(chatRoom, "사용자"))
                .thenThrow(new RuntimeException("system message creation failed"));

        // when & then
        assertThatThrownBy(
                () -> chatRoomLifecycleManager.afterMemberLeft(chatRoom, "사용자")
        ).isInstanceOf(RuntimeException.class);

        verify(chatMessagePublisher, never())
                .publishMessage(any(), anyInt());
    }
}