package com.example.schedulebook.domain.chat.service;

import com.example.schedulebook.domain.chat.entity.ChatMessage;
import com.example.schedulebook.domain.chat.entity.ChatRoom;
import com.example.schedulebook.domain.chat.enums.SystemMessageType;
import com.example.schedulebook.domain.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMessageManager {
    private final ChatMessageRepository chatMessageRepository;

    public ChatMessage createScheduleUpdatedMessage(ChatRoom chatRoom) {
        ChatMessage chatMessage = ChatMessage.system(chatRoom, SystemMessageType.SCHEDULE_UPDATED);

        return chatMessageRepository.save(chatMessage);
    }

    public ChatMessage createScheduleDeletedMessage(ChatRoom chatRoom) {
        ChatMessage chatMessage = ChatMessage.system(chatRoom, SystemMessageType.SCHEDULE_DELETED);

        return chatMessageRepository.save(chatMessage);
    }

    public ChatMessage createScheduleCanceledMessage(ChatRoom chatRoom) {
        ChatMessage chatMessage = ChatMessage.system(chatRoom, SystemMessageType.SCHEDULE_SHARE_CANCELED);

        return chatMessageRepository.save(chatMessage);
    }
}
