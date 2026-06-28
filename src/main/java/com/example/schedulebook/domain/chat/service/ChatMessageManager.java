package com.example.schedulebook.domain.chat.service;

import com.example.schedulebook.domain.chat.dto.request.PublishChatMessage;
import com.example.schedulebook.domain.chat.entity.ChatMessage;
import com.example.schedulebook.domain.chat.entity.ChatRoom;
import com.example.schedulebook.domain.chat.enums.SystemMessageType;
import com.example.schedulebook.domain.chat.projection.MemberReadStatusProjection;
import com.example.schedulebook.domain.chat.repository.ChatMessageRepository;
import com.example.schedulebook.domain.chat.repository.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatMessageManager {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatUnreadCountManager chatUnreadCountManager;

    public PublishChatMessage createSystemPublishMessage(ChatRoom chatRoom, SystemMessageType systemMessageType) {
        ChatMessage systemMessage = ChatMessage.system(chatRoom, systemMessageType);

        chatRoom.updateLastMessage(systemMessage);

        chatMessageRepository.save(systemMessage);

        chatRoomMemberRepository.increaseUnreadCountAll(chatRoom.getId());

        List<MemberReadStatusProjection> readStatuses = chatRoomMemberRepository.findReadStatuses(chatRoom.getId());

        int unreadCount = chatUnreadCountManager.calculateUnreadCount(systemMessage, readStatuses);

        return new PublishChatMessage(systemMessage, unreadCount);
    }
}
