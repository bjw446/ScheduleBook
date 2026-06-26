package com.example.schedulebook.domain.chat.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.chat.dto.request.ChatMessageScheduleShareRequest;
import com.example.schedulebook.domain.chat.dto.request.ChatMessageSearchRequest;
import com.example.schedulebook.domain.chat.dto.request.ChatMessageSendRequest;
import com.example.schedulebook.domain.chat.dto.response.ChatMessageDeletedEvent;
import com.example.schedulebook.domain.chat.dto.response.ChatMessageResponse;
import com.example.schedulebook.domain.chat.dto.response.ChatMessageSliceResponse;
import com.example.schedulebook.domain.chat.dto.response.ReadMessageEvent;
import com.example.schedulebook.domain.chat.entity.ChatMessage;
import com.example.schedulebook.domain.chat.entity.ChatRoom;
import com.example.schedulebook.domain.chat.entity.ChatRoomMember;
import com.example.schedulebook.domain.chat.enums.ChatMessageType;
import com.example.schedulebook.domain.chat.enums.ChatRoomType;
import com.example.schedulebook.domain.chat.projection.MemberReadStatusProjection;
import com.example.schedulebook.domain.chat.repository.ChatMessageRepository;
import com.example.schedulebook.domain.chat.repository.ChatRoomMemberRepository;
import com.example.schedulebook.domain.chat.repository.ChatRoomRepository;
import com.example.schedulebook.domain.schedule.dto.response.SchedulePreviewResponse;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.schedulebook.domain.chat.consts.ChatConst.DELETE_MESSAGE;
import static com.example.schedulebook.domain.chat.consts.ChatConst.MAX_PAGE_SIZE;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

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

        int unreadCount = calculateUnreadCount(chatMessage, readStatuses(chatRoom.getId()));

        ChatMessageResponse response = ChatMessageResponse.from(chatMessage, unreadCount);

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

        List<MemberReadStatusProjection> readStatusProjections = readStatuses(roomId);

        List<ChatMessageResponse> responses = messages.stream()
                .map(message -> {
                    int unreadCount = calculateUnreadCount(
                            message,
                            readStatusProjections
                    );

                    return ChatMessageResponse.from(message, unreadCount);

                })
                .toList();

        return new ChatMessageSliceResponse(responses, nextCursor, hasNext);
    }

    public void readMessage(Long currentUserId, Long roomId, Long lastReadMessageId) {
        ChatRoomMember chatRoomMember = validateChatRoomMember(currentUserId, roomId);

        ChatMessage chatMessage = validateChatMessage(lastReadMessageId, roomId);

        validateReadableMessage(chatRoomMember, chatMessage);

        chatRoomMember.updateLastRead(lastReadMessageId);

        long unreadCount = recalculateUnreadCount(chatRoomMember);

        chatRoomMember.updateUnreadCount((int) unreadCount);

        publishAfterCommit(roomId, currentUserId, chatRoomMember.getLastReadMessageId());
    }

    public void deleteMessage(Long currentUserId, Long roomId, Long messageId) {
        validateChatRoomMember(currentUserId, roomId);

        ChatMessage chatMessage = validateChatMessage(messageId, roomId);

        validateDeleteMessage(chatMessage);

        chatMessage.deleteMessage(currentUserId);

        publishDeleteMessageAfterCommit(roomId, messageId);
    }

    public void shareSchedule(Long currentUserId, ChatMessageScheduleShareRequest request) {
        ChatRoom chatRoom = validateChatRoom(request.roomId());

        validateChatRoomMember(currentUserId, request.roomId());

        User user = getUser(currentUserId);

        Schedule schedule = validateSchedule(currentUserId, request.scheduleId());

        ChatMessage chatMessage = ChatMessage.schedule(chatRoom, user, schedule.getId());

        chatMessageRepository.save(chatMessage);

        chatRoom.updateLastMessage(chatMessage);

        updateUnreadCount(chatRoom, currentUserId);

        int unreadCount = calculateUnreadCount(chatMessage, readStatuses(chatRoom.getId()));

        ChatMessageResponse response = ChatMessageResponse.from(
                chatMessage,
                unreadCount,
                SchedulePreviewResponse.from(
                        chatMessage.getId(),
                        schedule,
                        false,
                        false,
                        false)
        );

        String destination = "/topic/chat/" + chatRoom.getId();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                simpMessagingTemplate.convertAndSend(destination, response);
            }
        });
    }

    public void cancelScheduleShare(Long currentUserId, Long messageId) {
        ChatMessage chatMessage = validateChatMessage(messageId);

        validateScheduleMessageType(chatMessage);

        chatMessage.cancelScheduleShare(currentUserId);

        Schedule schedule = validateSchedule(currentUserId, chatMessage.getScheduleId());

        int unreadCount = calculateUnreadCount(chatMessage, readStatuses(chatMessage.getChatRoom().getId()));

        publishScheduleCancelAfterCommit(chatMessage, unreadCount, schedule);
    }

    private Schedule validateSchedule(Long currentUserId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(
                () -> new BaseException(ErrorEnum.SCHEDULE_NOT_FOUND)
        );

        if (!schedule.getUser().getId().equals(currentUserId)) {
            throw new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN);
        }

        return schedule;
    }

    private void validateScheduleMessageType(ChatMessage chatMessage) {
        if (chatMessage.getChatMessageType() != ChatMessageType.SCHEDULE) {
            throw new BaseException(ErrorEnum.INVALID_MESSAGE_TYPE);
        }
    }

    private void validateDeleteMessage(ChatMessage chatMessage) {
        if (chatMessage.isDeleted()) {
            throw new BaseException(ErrorEnum.CHAT_MESSAGE_ALREADY_DELETE);
        }

        if (chatMessage.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new BaseException(ErrorEnum.CHAT_MESSAGE_DELETE_NOT_ALLOWED);
        }
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

    private ChatMessage validateChatMessage(Long messageId, Long roomId) {
        if (messageId == null) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }

        return chatMessageRepository.findByIdAndChatRoomId(messageId, roomId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_MESSAGE_NOT_FOUND)
        );
    }

    private ChatMessage validateChatMessage(Long messageId) {
        if (messageId == null) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }

        return chatMessageRepository.findById(messageId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_MESSAGE_NOT_FOUND)
        );
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new BaseException(ErrorEnum.USER_NOT_FOUND)
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

    private void publishAfterCommit(Long roomId, Long currentUserId, Long lastReadMessageId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                simpMessagingTemplate.convertAndSend(
                        "/topic/chat/" + roomId + "/read",
                        ReadMessageEvent.from(roomId, currentUserId, lastReadMessageId)
                );
            }
        });
    }

    private void publishDeleteMessageAfterCommit(Long roomId, Long messageId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                simpMessagingTemplate.convertAndSend(
                        "/topic/chat/" + roomId + "/delete",
                        new ChatMessageDeletedEvent(
                                roomId,
                                messageId,
                                DELETE_MESSAGE
                        )
                );
            }
        });
    }

    private void publishScheduleCancelAfterCommit(ChatMessage chatMessage, int unreadCount, Schedule schedule) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                simpMessagingTemplate.convertAndSend(
                        "/topic/chat/" + chatMessage.getChatRoom().getId(),
                        ChatMessageResponse.from(
                                chatMessage,
                                unreadCount,
                                SchedulePreviewResponse.from(
                                        chatMessage.getId(),
                                        schedule,
                                        false,
                                        true,
                                        false)

                        )
                );
            }
        });
    }

    private List<MemberReadStatusProjection> readStatuses(Long roomId) {
        return chatRoomMemberRepository.findReadStatuses(roomId);
    }

    private int calculateUnreadCount(ChatMessage chatMessage, List<MemberReadStatusProjection> members) {
        Long senderId = chatMessage.getSender() == null ? null : chatMessage.getSender().getId();

        return (int) members.stream()
                .filter(member ->
                        senderId == null
                                || !member.getUserId().equals(senderId)
                )
                .filter(member ->
                        member.getLastReadMessageId() == null
                                || member.getLastReadMessageId() < chatMessage.getId()
                )
                .filter(member ->
                        !chatMessage.getCreatedAt().isBefore(member.getJoinedAt()))
                .count();
    }

    private void validateReadableMessage(ChatRoomMember chatRoomMember, ChatMessage chatMessage) {
        if (chatMessage.getCreatedAt().isBefore(chatRoomMember.getJoinedAt())) {
            throw new BaseException(ErrorEnum.CHAT_MESSAGE_FORBIDDEN);
        }
    }

    private long recalculateUnreadCount(ChatRoomMember chatRoomMember) {
        return chatMessageRepository.countUnreadMessages(
                chatRoomMember.getChatRoom().getId(),
                chatRoomMember.getLastReadMessageId() == null
                        ? 0L : chatRoomMember.getLastReadMessageId(),
                chatRoomMember.getUser().getId(),
                chatRoomMember.getJoinedAt()
        );
    }
}
