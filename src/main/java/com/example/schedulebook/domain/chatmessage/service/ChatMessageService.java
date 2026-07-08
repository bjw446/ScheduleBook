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
import com.example.schedulebook.domain.chatmessage.validator.ChatMessageValidator;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import com.example.schedulebook.domain.chatroom.entity.ChatRoomMember;
import com.example.schedulebook.domain.chatmessage.enums.ChatMessageType;
import com.example.schedulebook.domain.chatroom.enums.ChatRoomType;
import com.example.schedulebook.domain.chatroom.projection.MemberReadStatusProjection;
import com.example.schedulebook.domain.chatmessage.repository.ChatMessageRepository;
import com.example.schedulebook.domain.chatroom.repository.ChatRoomMemberRepository;
import com.example.schedulebook.domain.chatroom.validator.ChatRoomValidator;
import com.example.schedulebook.domain.schedule.validator.ScheduleValidator;
import com.example.schedulebook.domain.scheduleparticipant.validator.ScheduleParticipantValidator;
import com.example.schedulebook.domain.schedulesnapshot.dto.response.SchedulePreviewDetailResponse;
import com.example.schedulebook.domain.schedulesnapshot.dto.response.ScheduleSnapshotDiffResponse;
import com.example.schedulebook.domain.schedulesnapshot.dto.response.ScheduleSnapshotHistoryResponse;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.scheduleparticipant.entity.ScheduleParticipant;
import com.example.schedulebook.domain.schedulesnapshot.entity.ScheduleSnapshot;
import com.example.schedulebook.domain.scheduleparticipant.publisher.ScheduleParticipantPublisher;
import com.example.schedulebook.domain.scheduleshare.publisher.ScheduleSharePublisher;
import com.example.schedulebook.domain.scheduleparticipant.repository.ScheduleParticipantRepository;
import com.example.schedulebook.domain.schedulesnapshot.repository.ScheduleSnapshotHistoryRepository;
import com.example.schedulebook.domain.schedulesnapshot.service.ScheduleSnapshotComparator;
import com.example.schedulebook.domain.schedulesnapshot.service.ScheduleSnapshotHistoryManager;
import com.example.schedulebook.domain.scheduleshare.entity.ScheduleShare;
import com.example.schedulebook.domain.scheduleshare.repository.ScheduleShareRepository;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.validator.UserValidator;
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
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ScheduleSharePublisher scheduleSharePublisher;
    private final ChatMessagePublisher chatMessagePublisher;
    private final ScheduleSnapshotHistoryRepository scheduleSnapshotHistoryRepository;
    private final ScheduleSnapshotComparator scheduleSnapshotComparator;
    private final ScheduleSnapshotHistoryManager scheduleSnapshotHistoryManager;
    private final ChatUnreadCountManager chatUnreadCountManager;
    private final ScheduleShareRepository scheduleShareRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final ScheduleParticipantPublisher scheduleParticipantPublisher;
    private final UserValidator userValidator;
    private final ScheduleValidator scheduleValidator;
    private final ChatRoomValidator chatRoomValidator;
    private final ChatMessageValidator chatMessageValidator;
    private final ScheduleParticipantValidator scheduleParticipantValidator;

    public void sendMessage(Long currentUserId, ChatMessageSendRequest request) {
        ChatRoom chatRoom = chatRoomValidator.validateChatRoom(request.roomId());

        User sender = chatRoomValidator.validateMember(chatRoom.getId(), currentUserId);

        String content = chatMessageValidator.validateContent(request.content());

        ChatMessage replyMessage = chatMessageValidator.validateReplyMessage(request.replyMessageId(), chatRoom.getId());

        ChatMessage chatMessage = ChatMessage.of(
                chatRoom,
                sender,
                content,
                ChatMessageType.TEXT,
                replyMessage
        );

        int unreadCount = saveMessage(chatRoom, chatMessage, currentUserId, ChatRoomType.DIRECT);

        chatMessagePublisher.publishMessage(chatMessage, unreadCount);
    }

    @Transactional(readOnly = true)
    public ChatMessageSliceResponse findMessages(Long currentUserId, Long roomId, ChatMessageSearchRequest request) {
        chatRoomValidator.validateChatRoom(roomId);

        ChatRoomMember chatRoomMember = chatRoomValidator.validateChatRoomMember(currentUserId, roomId);

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
        ChatRoomMember chatRoomMember = chatRoomValidator.validateChatRoomMember(currentUserId, roomId);

        ChatMessage chatMessage = chatMessageValidator.validateChatMessageInRoom(lastReadMessageId, roomId);

        chatMessageValidator.validateReadableMessage(chatRoomMember, chatMessage);

        chatRoomMember.updateLastRead(lastReadMessageId);

        long unreadCount = unreadCount(chatRoomMember);

        chatRoomMember.updateUnreadCount((int) unreadCount);

        chatMessagePublisher.publishReadMessageAfterCommit(roomId, currentUserId, chatRoomMember.getLastReadMessageId());
    }

    public void deleteMessage(Long currentUserId, Long roomId, Long messageId) {
        chatRoomValidator.validateChatRoomMember(currentUserId, roomId);

        ChatMessage chatMessage = chatMessageValidator.validateChatMessageInRoom(messageId, roomId);

        chatMessageValidator.validateDeleteMessage(chatMessage);

        chatMessage.deleteMessage(currentUserId);

        chatMessagePublisher.publishDeleteMessageAfterCommit(roomId, messageId);
    }

    public void shareSchedule(Long currentUserId, ChatMessageScheduleShareRequest request) {
        ChatRoom chatRoom = chatRoomValidator.validateChatRoom(request.roomId());

        User user = chatRoomValidator.validateMember(request.roomId(), currentUserId);

        Schedule schedule = scheduleValidator.validateSchedule(request.scheduleId(), currentUserId);

        ChatMessage chatMessage = ChatMessage.schedule(chatRoom, user, schedule);

        int unreadCount = saveMessage(chatRoom, chatMessage, currentUserId);

        scheduleSharePublisher.publishScheduleShared(chatMessage, unreadCount);
    }

    public void acceptSharedSchedule(Long currentUserId, Long messageId) {
        ChatMessage chatMessage = chatMessageValidator.validateReadableScheduleMessage(currentUserId, messageId);

        Schedule schedule = scheduleValidator.findSchedule(chatMessage.getScheduleId());

        userValidator.validateShareMyself(currentUserId, schedule.getUser().getId());

        scheduleParticipantValidator.validateAlreadyParticipated(schedule.getId(), currentUserId);

        User currentUser = userValidator.validateActiveUser(currentUserId);

        ScheduleShare scheduleShare = ScheduleShare.create(schedule, currentUser);

        scheduleShareRepository.save(scheduleShare);

        createParticipant(schedule, currentUser);

        scheduleSharePublisher.publishAcceptSharedSchedule(chatMessage);

        scheduleParticipantPublisher.publishParticipantsUpdated(schedule.getId());
    }

    public void cancelScheduleShare(Long currentUserId, Long messageId) {
        ChatMessage chatMessage = chatMessageValidator.validateChatMessage(messageId);

        chatMessageValidator.validateScheduleMessageType(chatMessage);

        chatMessage.cancelScheduleShare(currentUserId);

        int unreadCount = unreadCount(chatMessage.getChatRoom(), chatMessage);

        scheduleSharePublisher.publishScheduleShareCanceled(chatMessage, unreadCount);
    }


    @Transactional(readOnly = true)
    public SchedulePreviewDetailResponse findSharedSchedule(Long currentUserId, Long messageId) {
        ChatMessage chatMessage = chatMessageValidator.validateReadableScheduleMessage(currentUserId, messageId);

        boolean shared = scheduleParticipantValidator.isAlreadyScheduleShared(chatMessage.getScheduleId(), currentUserId);

        return SchedulePreviewDetailResponse.from(chatMessage, false, false, shared);
    }

    @Transactional(readOnly = true)
    public List<ScheduleSnapshotHistoryResponse> findScheduleSnapshotHistory(Long currentUserId, Long messageId) {
        ChatMessage chatMessage = chatMessageValidator.validateReadableScheduleMessage(currentUserId, messageId);

        return scheduleSnapshotHistoryRepository.findAllByChatMessageId(chatMessage.getId())
                .stream()
                .map(ScheduleSnapshotHistoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScheduleSnapshotDiffResponse findScheduleSnapshotDiff(Long currentUserId, Long messageId, Long fromVersion, Long toVersion) {
        ChatMessage chatMessage = chatMessageValidator.validateReadableScheduleMessage(currentUserId, messageId);

        ScheduleSnapshot before = scheduleSnapshotHistoryManager.findSnapshot(chatMessage, fromVersion);

        ScheduleSnapshot after = scheduleSnapshotHistoryManager.findSnapshot(chatMessage, toVersion);

        return scheduleSnapshotComparator.compare(before, after);
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

    private void createParticipant(Schedule schedule, User user) {
        ScheduleParticipant scheduleParticipant = ScheduleParticipant.of(schedule, user);

        scheduleParticipantRepository.save(scheduleParticipant);
    }

    private int saveMessage(ChatRoom chatRoom, ChatMessage chatMessage, Long senderId) {
        chatMessageRepository.save(chatMessage);

        chatRoom.updateLastMessage(chatMessage);

        updateUnreadCount(chatRoom, senderId);

        return unreadCount(chatRoom, chatMessage);
    }

    private int saveMessage(ChatRoom chatRoom, ChatMessage chatMessage, Long senderId, ChatRoomType chatRoomType) {
        chatMessageRepository.save(chatMessage);

        chatRoom.updateLastMessage(chatMessage);

        if (chatRoom.getChatRoomType() == chatRoomType) {
            rejoinLeftMembers(chatRoom.getId(), senderId, chatMessage.getCreatedAt());
        }

        updateUnreadCount(chatRoom, senderId);

        return unreadCount(chatRoom, chatMessage);
    }

    private int unreadCount(ChatRoom chatRoom, ChatMessage chatMessage) {
        return chatUnreadCountManager.calculateUnreadCount(chatMessage, readStatuses(chatRoom.getId()));
    }

    private long unreadCount(ChatRoomMember chatRoomMember) {
        return chatUnreadCountManager.recalculateUnreadCount(chatRoomMember);
    }
}
