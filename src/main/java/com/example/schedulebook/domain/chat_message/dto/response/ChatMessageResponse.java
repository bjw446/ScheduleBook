package com.example.schedulebook.domain.chat_message.dto.response;

import com.example.schedulebook.domain.chat_message.entity.ChatMessage;
import com.example.schedulebook.domain.chat_message.enums.ChatMessageType;
import com.example.schedulebook.domain.schedule_snapshot.dto.response.SchedulePreviewResponse;
import com.example.schedulebook.domain.user.entity.User;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long roomId,
        Long senderId,
        String senderNickname,
        String content,
        ChatMessageType chatMessageType,
        ReplyMessageResponse replyMessageResponse,
        boolean edited,
        int unreadMemberCount,
        LocalDateTime createdAt,
        SchedulePreviewResponse schedulePreviewResponse

) {
    public static ChatMessageResponse from(
            ChatMessage chatMessage,
            int unreadMemberCount
    ) {
        User sender = chatMessage.getSender();

        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getChatRoom().getId(),
                chatMessage.getSender() == null ? null : sender.getId(),
                chatMessage.getSender() == null ? null : sender.getNickname(),
                chatMessage.getContent(),
                chatMessage.getChatMessageType(),
                chatMessage.getReplyMessage() == null ? null : ReplyMessageResponse.from(chatMessage.getReplyMessage()),
                chatMessage.isEdited(),
                unreadMemberCount,
                chatMessage.getCreatedAt(),
                null
        );
    }

    public static ChatMessageResponse from(
            ChatMessage chatMessage,
            int unreadMemberCount,
            SchedulePreviewResponse schedulePreviewResponse
    ) {
        User sender = chatMessage.getSender();

        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getChatRoom().getId(),
                chatMessage.getSender() == null ? null : sender.getId(),
                chatMessage.getSender() == null ? null : sender.getNickname(),
                chatMessage.getContent(),
                chatMessage.getChatMessageType(),
                chatMessage.getReplyMessage() == null ? null : ReplyMessageResponse.from(chatMessage.getReplyMessage()),
                chatMessage.isEdited(),
                unreadMemberCount,
                chatMessage.getCreatedAt(),
                schedulePreviewResponse
        );
    }
}
