package com.example.schedulebook.domain.chatmessage.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;
import com.example.schedulebook.domain.chatmessage.enums.ChatMessageType;
import com.example.schedulebook.domain.chatmessage.repository.ChatMessageRepository;
import com.example.schedulebook.domain.chatroom.entity.ChatRoomMember;
import com.example.schedulebook.domain.chatroom.validator.ChatRoomValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ChatMessageValidator {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomValidator chatRoomValidator;

    public void validateScheduleMessageType(ChatMessage chatMessage) {
        if (chatMessage.getChatMessageType() != ChatMessageType.SCHEDULE) {
            throw new BaseException(ErrorEnum.INVALID_MESSAGE_TYPE);
        }
    }

    public void validateDeleteMessage(ChatMessage chatMessage) {
        if (chatMessage.isDeleted()) {
            throw new BaseException(ErrorEnum.CHAT_MESSAGE_ALREADY_DELETE);
        }

        if (chatMessage.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new BaseException(ErrorEnum.CHAT_MESSAGE_DELETE_NOT_ALLOWED);
        }
    }

    public String validateContent(String content) {
        if (content != null) {
            content = content.trim();
        }

        if (content == null || content.isBlank()) {
            throw new BaseException(ErrorEnum.CHAT_MESSAGE_EMPTY);
        }

        if (content.length() > 1000) {
            throw new BaseException(ErrorEnum.CHAT_MESSAGE_TOO_LONG);
        }

        return content;
    }

    public ChatMessage validateReplyMessage(Long replyMessageId, Long roomId) {
        if (replyMessageId == null) {
            return null;
        }

        return chatMessageRepository.findByIdAndChatRoomId(replyMessageId, roomId).orElseThrow(
                () -> new BaseException(ErrorEnum.INVALID_REPLY_MESSAGE)
        );
    }

    public ChatMessage validateChatMessageInRoom(Long messageId, Long roomId) {
        if (messageId == null) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }

        return chatMessageRepository.findByIdAndChatRoomId(messageId, roomId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_MESSAGE_NOT_FOUND)
        );
    }

    public ChatMessage validateChatMessage(Long messageId) {
        if (messageId == null) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }

        return chatMessageRepository.findByIdWithChatRoom(messageId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_MESSAGE_NOT_FOUND)
        );
    }

    public void validateReadableMessage(ChatRoomMember chatRoomMember, ChatMessage chatMessage) {
        if (chatMessage.getCreatedAt().isBefore(chatRoomMember.getJoinedAt())) {
            throw new BaseException(ErrorEnum.CHAT_MESSAGE_FORBIDDEN);
        }
    }

    public void validateReadableSharedSchedule(ChatMessage chatMessage) {
        if (chatMessage.isScheduleShareCanceled()) {
            throw new BaseException(ErrorEnum.SCHEDULE_SHARE_CANCELED);
        }
    }

    public ChatMessage validateReadableScheduleMessage(Long currentUserId, Long messageId) {
        ChatMessage chatMessage = validateChatMessage(messageId);

        ChatRoomMember chatRoomMember = chatRoomValidator.validateChatRoomMember(currentUserId, chatMessage.getChatRoom().getId());

        validateReadableMessage(chatRoomMember, chatMessage);

        validateScheduleMessageType(chatMessage);

        validateReadableSharedSchedule(chatMessage);

        return chatMessage;
    }
}
