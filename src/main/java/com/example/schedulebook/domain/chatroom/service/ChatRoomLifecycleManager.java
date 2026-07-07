package com.example.schedulebook.domain.chatroom.service;

import com.example.schedulebook.domain.chatmessage.dto.request.PublishChatMessage;
import com.example.schedulebook.domain.chatmessage.service.ChatMessageManager;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import com.example.schedulebook.domain.chatroom.entity.ChatRoomMember;
import com.example.schedulebook.domain.chatmessage.publisher.ChatMessagePublisher;
import com.example.schedulebook.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ChatRoomLifecycleManager {
    private final ChatMessageManager chatMessageManager;
    private final ChatMessagePublisher chatMessagePublisher;

    public void afterRoomCreated(ChatRoom chatRoom, User owner) {
        publish(chatMessageManager.createGroupRoomSystemMessage(chatRoom, owner));
    }

    public void afterMemberInvited(ChatRoom chatRoom, User inviter, List<User> invitedUsers) {
        publish(chatMessageManager.createInviteSystemMessage(chatRoom, inviter, invitedUsers));
    }

    public void afterRoomNameUpdated(ChatRoom chatRoom, User updater, String oldName, String newName) {
        publish(chatMessageManager.createUpdateNameSystemMessage(chatRoom, updater, oldName, newName));
    }

    public void afterMemberLeft(ChatRoom chatRoom, ChatRoomMember chatRoomMember) {
        publish(chatMessageManager.createLeaveSystemMessage(chatRoom, chatRoomMember));
    }

    private void publish(PublishChatMessage publishChatMessage) {
        chatMessagePublisher.publishMessage(publishChatMessage.chatMessage(), publishChatMessage.unreadCount());
    }
}
