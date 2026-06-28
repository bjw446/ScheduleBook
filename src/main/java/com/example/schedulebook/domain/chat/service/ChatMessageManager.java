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

    public ChatMessage createSystemMessage(ChatRoom chatRoom, SystemMessageType systemMessageType) {
        ChatMessage chatMessage = ChatMessage.system(chatRoom, systemMessageType);

        return chatMessageRepository.save(chatMessage);
    }
}
