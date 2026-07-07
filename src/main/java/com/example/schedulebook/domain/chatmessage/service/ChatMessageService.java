package com.example.schedulebook.domain.chatmessage.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.chatmessage.dto.request.ChatMessageScheduleShareRequest;
import com.example.schedulebook.domain.chatmessage.dto.request.ChatMessageSearchRequest;
import com.example.schedulebook.domain.chatmessage.dto.request.ChatMessageSendRequest;
import com.example.schedulebook.domain.chatmessage.dto.response.ChatMessageResponse;
import com.example.schedulebook.domain.chatmessage.dto.response.ChatMessageSliceResponse;
import com.example.schedulebook.domain.chatmessage.publisher.ChatMessagePublisher;
import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import com.example.schedulebook.domain.chatroom.entity.ChatRoomMember;
import com.example.schedulebook.domain.chatmessage.enums.ChatMessageType;
import com.example.schedulebook.domain.chatroom.enums.ChatRoomType;
import com.example.schedulebook.domain.chatroom.projection.MemberReadStatusProjection;
import com.example.schedulebook.domain.chatmessage.repository.ChatMessageRepository;
import com.example.schedulebook.domain.chatroom.repository.ChatRoomMemberRepository;
import com.example.schedulebook.domain.chatroom.repository.ChatRoomRepository;
import com.example.schedulebook.domain.schedulesnapshot.dto.response.SchedulePreviewDetailResponse;
import com.example.schedulebook.domain.schedulesnapshot.dto.response.ScheduleSnapshotDiffResponse;
import com.example.schedulebook.domain.schedulesnapshot.dto.response.ScheduleSnapshotHistoryResponse;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.scheduleparticipant.entity.ScheduleParticipant;
import com.example.schedulebook.domain.schedulesnapshot.entity.ScheduleSnapshot;
import com.example.schedulebook.domain.scheduleparticipant.publisher.ScheduleParticipantPublisher;
import com.example.schedulebook.domain.scheduleshare.publisher.ScheduleSharePublisher;
import com.example.schedulebook.domain.scheduleparticipant.repository.ScheduleParticipantRepository;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.schedulesnapshot.repository.ScheduleSnapshotHistoryRepository;
import com.example.schedulebook.domain.schedulesnapshot.service.ScheduleSnapshotComparator;
import com.example.schedulebook.domain.schedulesnapshot.service.ScheduleSnapshotHistoryManager;
import com.example.schedulebook.domain.scheduleshare.entity.ScheduleShare;
import com.example.schedulebook.domain.scheduleshare.enums.ScheduleShareStatus;
import com.example.schedulebook.domain.scheduleshare.repository.ScheduleShareRepository;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.enums.UserStatus;
import com.example.schedulebook.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.schedulebook.common.consts.CommonConst.MAX_PAGE_SIZE;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleSharePublisher scheduleSharePublisher;
    private final ChatMessagePublisher chatMessagePublisher;
    private final ScheduleSnapshotHistoryRepository scheduleSnapshotHistoryRepository;
    private final ScheduleSnapshotComparator scheduleSnapshotComparator;
    private final ScheduleSnapshotHistoryManager scheduleSnapshotHistoryManager;
    private final ChatUnreadCountManager chatUnreadCountManager;
    private final ScheduleShareRepository scheduleShareRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final UserRepository userRepository;
    private final ScheduleParticipantPublisher scheduleParticipantPublisher;

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

        int unreadCount = chatUnreadCountManager.calculateUnreadCount(chatMessage, readStatuses(chatRoom.getId()));

        chatMessagePublisher.publishMessage(chatMessage, unreadCount);
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
                    int unreadCount = chatUnreadCountManager.calculateUnreadCount(
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

        long unreadCount = chatUnreadCountManager.recalculateUnreadCount(chatRoomMember);

        chatRoomMember.updateUnreadCount((int) unreadCount);

        chatMessagePublisher.publishReadMessageAfterCommit(roomId, currentUserId, chatRoomMember.getLastReadMessageId());
    }

    public void deleteMessage(Long currentUserId, Long roomId, Long messageId) {
        validateChatRoomMember(currentUserId, roomId);

        ChatMessage chatMessage = validateChatMessage(messageId, roomId);

        validateDeleteMessage(chatMessage);

        chatMessage.deleteMessage(currentUserId);

        chatMessagePublisher.publishDeleteMessageAfterCommit(roomId, messageId);
    }

    public void shareSchedule(Long currentUserId, ChatMessageScheduleShareRequest request) {
        ChatRoom chatRoom = validateChatRoom(request.roomId());

        User user = validateMember(request.roomId(), currentUserId);

        Schedule schedule = validateSchedule(currentUserId, request.scheduleId());

        ChatMessage chatMessage = ChatMessage.schedule(chatRoom, user, schedule);

        chatMessageRepository.save(chatMessage);

        chatRoom.updateLastMessage(chatMessage);

        updateUnreadCount(chatRoom, currentUserId);

        int unreadCount = chatUnreadCountManager.calculateUnreadCount(chatMessage, readStatuses(chatRoom.getId()));

        scheduleSharePublisher.publishScheduleShared(chatMessage, unreadCount);
    }

    public void acceptSharedSchedule(Long currentUserId, Long messageId) {
        ChatMessage chatMessage = validateChatMessage(messageId);

        ChatRoomMember chatRoomMember = validateChatRoomMember(currentUserId, chatMessage.getChatRoom().getId());

        validateReadableMessage(chatRoomMember, chatMessage);

        validateScheduleMessageType(chatMessage);

        validateReadableSharedSchedule(chatMessage);

        Schedule schedule = findSchedule(chatMessage.getScheduleId());

        if (schedule.getUser().getId().equals(currentUserId)) {
            throw new BaseException(ErrorEnum.CANNOT_SHARE_MYSELF);
        }

        validateAlreadyParticipated(schedule.getId(), currentUserId);

        User currentUser = validateUser(currentUserId);

        ScheduleShare scheduleShare = ScheduleShare.create(schedule, currentUser);

        scheduleShareRepository.save(scheduleShare);

        createParticipant(schedule, currentUser);

        scheduleSharePublisher.publishAcceptSharedSchedule(chatMessage);

        scheduleParticipantPublisher.publishParticipantsUpdated(schedule.getId());
    }

    public void cancelScheduleShare(Long currentUserId, Long messageId) {
        ChatMessage chatMessage = validateChatMessage(messageId);

        validateScheduleMessageType(chatMessage);

        chatMessage.cancelScheduleShare(currentUserId);

        int unreadCount = chatUnreadCountManager.calculateUnreadCount(chatMessage, readStatuses(chatMessage.getChatRoom().getId()));

        scheduleSharePublisher.publishScheduleShareCanceled(chatMessage, unreadCount);
    }


    @Transactional(readOnly = true)
    public SchedulePreviewDetailResponse findSharedSchedule(Long currentUserId, Long messageId) {
        ChatMessage chatMessage = validateChatMessage(messageId);

        ChatRoomMember chatRoomMember = validateChatRoomMember(currentUserId, chatMessage.getChatRoom().getId());

        validateReadableMessage(chatRoomMember, chatMessage);

        validateScheduleMessageType(chatMessage);

        validateReadableSharedSchedule(chatMessage);

        boolean shared = isAlreadyScheduleShared(chatMessage.getScheduleId(), currentUserId);

        return SchedulePreviewDetailResponse.from(chatMessage, false, false, shared);
    }

    @Transactional(readOnly = true)
    public List<ScheduleSnapshotHistoryResponse> findScheduleSnapshotHistory(Long currentUserId, Long messageId) {
        ChatMessage chatMessage = validateChatMessage(messageId);

        ChatRoomMember chatRoomMember = validateChatRoomMember(currentUserId, chatMessage.getChatRoom().getId());

        validateReadableMessage(chatRoomMember, chatMessage);

        validateScheduleMessageType(chatMessage);

        validateReadableSharedSchedule(chatMessage);

        return scheduleSnapshotHistoryRepository.findAllByChatMessageId(chatMessage.getId())
                .stream()
                .map(ScheduleSnapshotHistoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScheduleSnapshotDiffResponse findScheduleSnapshotDiff(Long currentUserId, Long messageId, Long fromVersion, Long toVersion) {
        ChatMessage chatMessage = validateChatMessage(messageId);

        ChatRoomMember chatRoomMember = validateChatRoomMember(currentUserId, chatMessage.getChatRoom().getId());

        validateReadableMessage(chatRoomMember, chatMessage);

        validateScheduleMessageType(chatMessage);

        validateReadableSharedSchedule(chatMessage);

        ScheduleSnapshot before = scheduleSnapshotHistoryManager.findSnapshot(chatMessage, fromVersion);

        ScheduleSnapshot after = scheduleSnapshotHistoryManager.findSnapshot(chatMessage, toVersion);

        return scheduleSnapshotComparator.compare(before, after);
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

        return chatMessageRepository.findByIdWithChatRoom(messageId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_MESSAGE_NOT_FOUND)
        );
    }

    private void validateReadableMessage(ChatRoomMember chatRoomMember, ChatMessage chatMessage) {
        if (chatMessage.getCreatedAt().isBefore(chatRoomMember.getJoinedAt())) {
            throw new BaseException(ErrorEnum.CHAT_MESSAGE_FORBIDDEN);
        }
    }

    private void validateReadableSharedSchedule(ChatMessage chatMessage) {
        if (chatMessage.isScheduleShareCanceled()) {
            throw new BaseException(ErrorEnum.SCHEDULE_SHARE_CANCELED);
        }
    }

    private User validateUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BaseException(ErrorEnum.USER_NOT_FOUND)
        );

        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new BaseException(ErrorEnum.USER_NOT_ACTIVE);
        }

        return user;
    }

    private void validateAlreadyParticipated(Long scheduleId, Long currentUserId) {
        if (scheduleParticipantRepository.existsBySchedule_IdAndUser_Id(scheduleId, currentUserId)) {
            throw new BaseException(ErrorEnum.SCHEDULE_ALREADY_PARTICIPATED);
        }

        if (isAlreadyScheduleShared(scheduleId, currentUserId)) {
            throw new BaseException(ErrorEnum.SCHEDULE_ALREADY_SHARED);
        }
    }

    private boolean isAlreadyScheduleShared(Long scheduleId, Long currentUserId) {
        return scheduleShareRepository.existsBySchedule_IdAndSharedUser_IdAndScheduleShareStatus(
                scheduleId,
                currentUserId,
                ScheduleShareStatus.ACTIVE
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

    private List<MemberReadStatusProjection> readStatuses(Long roomId) {
        return chatRoomMemberRepository.findReadStatuses(roomId);
    }

    private Schedule findSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId).orElseThrow(
                () -> new BaseException(ErrorEnum.SCHEDULE_NOT_FOUND)
        );
    }

    private void createParticipant(Schedule schedule, User user) {
        ScheduleParticipant scheduleParticipant = ScheduleParticipant.of(schedule, user);

        scheduleParticipantRepository.save(scheduleParticipant);
    }
}
