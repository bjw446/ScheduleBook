package com.example.schedulebook.domain.chat.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.chat.dto.request.ChatMessageSearchRequest;
import com.example.schedulebook.domain.chat.dto.request.ChatMessageSendRequest;
import com.example.schedulebook.domain.chat.dto.response.ChatMessageResponse;
import com.example.schedulebook.domain.chat.dto.response.ChatMessageSliceResponse;
import com.example.schedulebook.domain.chat.entity.ChatMessage;
import com.example.schedulebook.domain.chat.entity.ChatRoom;
import com.example.schedulebook.domain.chat.entity.ChatRoomMember;
import com.example.schedulebook.domain.chat.enums.ChatMessageType;
import com.example.schedulebook.domain.chat.enums.ChatRoomType;
import com.example.schedulebook.domain.chat.repository.ChatMessageRepository;
import com.example.schedulebook.domain.chat.repository.ChatRoomMemberRepository;
import com.example.schedulebook.domain.chat.repository.ChatRoomRepository;
import com.example.schedulebook.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.schedulebook.domain.chat.consts.ChatConst.MAX_PAGE_SIZE;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public void sendMessage(Long currentUserId, ChatMessageSendRequest request) {
        ChatRoom chatRoom = validateChatRoom(request.roomId());

        User sender = validateMember(chatRoom.getId(), currentUserId);

        String content = validateContent(request.content());

        ChatMessage replyMessage = validateReplyMessage(request.replyMessageId(), chatRoom.getId());

        ChatMessage chatMessage = ChatMessage.of(
                chatRoom,
                sender,
                content,
                ChatMessageType.TEXT,
                replyMessage
        );

        chatMessageRepository.save(chatMessage);

        chatRoom.updateLastMessage(chatMessage);

        if (chatRoom.getChatRoomType() == ChatRoomType.DIRECT) {
            rejoinLeftMembers(chatRoom.getId(), currentUserId, chatMessage.getCreatedAt());
        }

        updateUnreadCount(chatRoom, currentUserId);

        ChatMessageResponse response = ChatMessageResponse.from(chatMessage);

        String destination = "/topic/chat/" + chatRoom.getId();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                simpMessagingTemplate.convertAndSend(destination, response);
            }
        });
    }

    @Transactional(readOnly = true)
    public ChatMessageSliceResponse findMessages(Long currentUserId, Long roomId, ChatMessageSearchRequest request) {
        validateChatRoom(roomId);

        validateMember(roomId, currentUserId);

        ChatRoomMember chatRoomMember = validateChatRoomMember(currentUserId, roomId);

        int requestedSize = request.size() == null ? 30 : request.size();

        int pageSize = requestedSize <= 0 ? 30 : Math.min(requestedSize, MAX_PAGE_SIZE);

        List<ChatMessage> messages = chatMessageRepository.findMessages(
                roomId,
                chatRoomMember.getJoinedAt(),
                request.cursor(),
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = messages.size() > pageSize;

        if (hasNext) {
            messages.remove(pageSize);
        }

        Long nextCursor = null;

        if (hasNext && !messages.isEmpty()) {
            nextCursor = messages.get(messages.size() - 1).getId();
        }

        return new ChatMessageSliceResponse(
                messages.stream()
                        .map(ChatMessageResponse::from)
                        .toList(),
                nextCursor,
                hasNext
        );
    }

    public void readMessage(Long currentUserId, Long roomId, Long lastReadMessageId) {
        ChatRoomMember chatRoomMember = chatRoomMemberRepository.findActiveByChatRoomIdAndUserId(roomId, currentUserId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_ROOM_FORBIDDEN)
        );

        validateChatMessage(lastReadMessageId, roomId);

        chatRoomMember.updateLastRead(lastReadMessageId);

        chatRoomMember.clearUnreadCount();
    }

    private ChatRoom validateChatRoom(Long roomId) {
        return chatRoomRepository.findById(roomId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_ROOM_NOT_FOUND)
        );
    }

    private ChatRoomMember validateChatRoomMember(Long currentUserId, Long roomId) {
        return chatRoomMemberRepository.findActiveByChatRoomIdAndUserId(roomId, currentUserId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_ROOM_FORBIDDEN)
        );
    }

    private User validateMember(Long roomId, Long currentUserId) {
        return chatRoomMemberRepository.findUserInRoom(roomId, currentUserId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_ROOM_FORBIDDEN)
        );
    }

    private String validateContent(String content) {
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

    private ChatMessage validateReplyMessage(Long replyMessageId, Long roomId) {
        if (replyMessageId == null) {
            return null;
        }

        return chatMessageRepository.findByIdAndChatRoomId(replyMessageId, roomId).orElseThrow(
                () -> new BaseException(ErrorEnum.INVALID_REPLY_MESSAGE)
        );
    }

    private void validateChatMessage(Long messageId, Long roomId) {
        if (messageId == null) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }

        chatMessageRepository.findByIdAndChatRoomId(messageId, roomId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_MESSAGE_NOT_FOUND)
        );
    }

    private void updateUnreadCount(ChatRoom chatRoom, Long currentUserId) {
        chatRoomMemberRepository.increaseUnreadCount(chatRoom.getId(), currentUserId);
    }

    private void rejoinLeftMembers(Long roomId, Long senderId, LocalDateTime joinedAt) {
        List<ChatRoomMember> leftMembers = chatRoomMemberRepository.findDeletedMembers(roomId, senderId);

        for (ChatRoomMember chatRoomMember : leftMembers) {
            chatRoomMember.rejoin(joinedAt);
        }
    }
}
