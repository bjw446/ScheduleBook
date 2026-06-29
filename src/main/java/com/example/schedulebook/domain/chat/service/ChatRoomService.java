package com.example.schedulebook.domain.chat.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.chat.dto.request.ChatRoomInviteRequest;
import com.example.schedulebook.domain.chat.dto.request.GroupChatRoomCreateRequest;
import com.example.schedulebook.domain.chat.dto.request.ChatRoomUpdateNameRequest;
import com.example.schedulebook.domain.chat.dto.response.*;
import com.example.schedulebook.domain.chat.entity.ChatMessage;
import com.example.schedulebook.domain.chat.entity.ChatRoom;
import com.example.schedulebook.domain.chat.entity.ChatRoomMember;
import com.example.schedulebook.domain.chat.entity.DirectChatRoom;
import com.example.schedulebook.domain.chat.enums.ChatMessageType;
import com.example.schedulebook.domain.chat.enums.ChatRoomType;
import com.example.schedulebook.domain.chat.enums.SystemMessageType;
import com.example.schedulebook.domain.chat.projection.MemberReadStatusProjection;
import com.example.schedulebook.domain.chat.projection.OpponentInfoProjection;
import com.example.schedulebook.domain.chat.repository.*;
import com.example.schedulebook.domain.friend.enums.FriendStatus;
import com.example.schedulebook.domain.friend.repository.FriendRepository;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.schedulebook.domain.chat.consts.ChatConst.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final DirectChatRoomRepository directChatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatUnreadCountManager chatUnreadCountManager;

    public ChatRoomResponse createDirectRoom(Long currentUserId, Long friendId) {
        validateMyself(currentUserId, friendId);

        User currentUser = getUser(currentUserId);

        User friendUser = getUser(friendId);

        validateFriend(currentUserId, friendId);

        try {
            Optional<DirectChatRoom> existingRoom = findDirectRoom(currentUserId, friendId);

            if (existingRoom.isPresent()) {
                return ChatRoomResponse.from(existingRoom.get().getChatRoom());
            }

            ChatRoom chatRoom = ChatRoom.direct();

            chatRoomRepository.save(chatRoom);

            ChatRoomMember member1 = ChatRoomMember.of(chatRoom, currentUser, LocalDateTime.now());

            ChatRoomMember member2 = ChatRoomMember.of(chatRoom, friendUser, LocalDateTime.now());

            chatRoomMemberRepository.save(member1);

            chatRoomMemberRepository.save(member2);

            directChatRoomRepository.save(DirectChatRoom.of(currentUserId, friendId, chatRoom));

            return ChatRoomResponse.from(chatRoom);
        } catch (DataIntegrityViolationException e) {
            log.info("1:1 채팅방 중복 생성 발생, user1 = {}, user2 = {}", currentUserId, friendId);

            DirectChatRoom directChatRoom = findDirectRoom(currentUserId, friendId).orElseThrow(
                    () -> new BaseException(ErrorEnum.CHAT_ROOM_NOT_FOUND)
            );

            return ChatRoomResponse.from(directChatRoom.getChatRoom());
        }
    }

    public ChatRoomResponse createGroupRoom(Long currentUserId, GroupChatRoomCreateRequest request) {
        validateInviteMembers(currentUserId, request.memberIds());

        User owner = getUser(currentUserId);

        List<User> members = userRepository.findAllById(request.memberIds());

        ChatRoom chatRoom = ChatRoom.group(request.name());

        chatRoomRepository.save(chatRoom);

        ChatRoomMember ownerMember = ChatRoomMember.of(chatRoom, owner, LocalDateTime.now());

        chatRoomMemberRepository.save(ownerMember);

        for (User member : members) {
            ChatRoomMember chatRoomMember = ChatRoomMember.of(chatRoom, member, LocalDateTime.now());

            chatRoomMemberRepository.save(chatRoomMember);
        }

        createGroupRoomSystemMessage(chatRoom, owner);

        return ChatRoomResponse.from(chatRoom);
    }

    public ChatRoomResponse inviteMembers(Long currentUserId, Long roomId, ChatRoomInviteRequest request) {
        ChatRoom chatRoom = validateChatRoom(roomId);

        validateChatRoomMember(currentUserId, roomId);

        validateChatRoomType(chatRoom);

        validateInviteMembers(currentUserId, request.memberIds());

        List<User> users = userRepository.findAllById(request.memberIds());

        List<User> invitedUsers = new ArrayList<>();

        for (User user : users) {
            boolean invited = processInvitation(chatRoom, user, LocalDateTime.now());

            if (invited) {
                invitedUsers.add(user);
            }
        }

        if (!invitedUsers.isEmpty()) {
            ChatMessage inviteMessage = createInviteSystemMessage(chatRoom, getUser(currentUserId), invitedUsers);

            updateUnreadCount(chatRoom, currentUserId);

            int unreadCount = chatUnreadCountManager.calculateUnreadCount(inviteMessage, currentUserId, readStatuses(chatRoom.getId()));

            publishSystemMessage(chatRoom, inviteMessage, unreadCount);
        }

        return ChatRoomResponse.from(chatRoom);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomListResponse> findMyChatRooms(Long currentUserId) {
        return chatRoomMemberRepository.findMyChatRooms(currentUserId).stream()
                .map(ChatRoomListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatRoomDetailResponse findChatRoom(Long currentUserId, Long roomId) {
        ChatRoomMember chatRoomMember = validateChatRoomMember(currentUserId, roomId);

        ChatRoom chatRoom = chatRoomMember.getChatRoom();

        String roomName = resolveRoomName(chatRoom, currentUserId);

        List<MemberReadStatusResponse> readStatuses = chatRoomMemberRepository.findReadStatuses(roomId)
                .stream()
                .filter(status -> !status.getUserId().equals(currentUserId))
                .map(MemberReadStatusResponse::from)
                .toList();

        return ChatRoomDetailResponse.from(chatRoom, chatRoomMember, roomName, readStatuses);
    }

    public ChatRoomResponse updateRoomName(Long currentUserId, Long roomId, ChatRoomUpdateNameRequest request) {
        ChatRoom chatRoom = validateChatRoom(roomId);

        validateChatRoomMember(currentUserId, roomId);

        validateChatRoomType(chatRoom);

        validateUpdateName(chatRoom, request.name().trim());

        String oldName = chatRoom.getName();

        chatRoom.updateName(request.name().trim());

        ChatMessage systemMessage = createUpdateNameSystemMessage(chatRoom, getUser(currentUserId), oldName, request.name().trim());

        updateUnreadCount(chatRoom, currentUserId);

        int unreadCount = chatUnreadCountManager.calculateUnreadCount(systemMessage, currentUserId, readStatuses(chatRoom.getId()));

        publishSystemMessage(chatRoom, systemMessage, unreadCount);

        return ChatRoomResponse.from(chatRoom);
    }

    public void leaveChatRoom(Long currentUserId, Long roomId) {
        ChatRoomMember chatRoomMember = validateChatRoomMember(currentUserId, roomId);

        ChatRoom chatRoom = chatRoomMember.getChatRoom();

        handleLeave(chatRoom, chatRoomMember);
    }

    private ChatRoomMember validateChatRoomMember(Long currentUserId, Long roomId) {
        return chatRoomMemberRepository.findActiveByChatRoomIdAndUserId(roomId, currentUserId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_ROOM_FORBIDDEN)
        );
    }

    private void validateFriend(Long currentUserId, Long friendId) {
        if (!friendRepository.existsAcceptedFriend(currentUserId, friendId, FriendStatus.ACCEPTED)) {
            throw new BaseException(ErrorEnum.FRIEND_NOT_FOUND);
        }
    }

    private void validateMyself(Long currentUserId, Long friendId) {
        if (currentUserId.equals(friendId)) {
            throw new BaseException(ErrorEnum.INVALID_CHAT_TARGET);
        }
    }

    private ChatRoom validateChatRoom(Long roomId) {
        return chatRoomRepository.findById(roomId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_ROOM_NOT_FOUND)
        );
    }

    private void validateInviteMembers(Long currentUserId, List<Long> memberIds) {
        if (memberIds.contains(currentUserId)) {
            throw new BaseException(ErrorEnum.INVALID_CHAT_TARGET);
        }

        Set<Long> uniqueIds = new HashSet<>(memberIds);

        if (uniqueIds.size() != memberIds.size()) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }

        List<User> users = userRepository.findAllById(memberIds);

        if (users.size() != memberIds.size()) {
            throw new BaseException(ErrorEnum.USER_NOT_FOUND);
        }

        long friendCount = friendRepository.countAcceptedFriends(currentUserId, memberIds, FriendStatus.ACCEPTED);

        if (friendCount != memberIds.size()) {
            throw new BaseException(ErrorEnum.FRIEND_NOT_FOUND);
        }
    }

    private void validateChatRoomType(ChatRoom chatRoom) {
        if (chatRoom.getChatRoomType() != ChatRoomType.GROUP) {
            throw new BaseException(ErrorEnum.INVALID_CHAT_ROOM_TYPE);
        }
    }

    private void validateUpdateName(ChatRoom chatRoom, String newName) {
        if (chatRoom.getName().trim().equals(newName.trim())) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }
    }

    private boolean processInvitation(ChatRoom chatRoom, User user, LocalDateTime now) {
        Optional<ChatRoomMember> memberOpt = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoom.getId(), user.getId());

        if (memberOpt.isPresent()) {
            ChatRoomMember chatRoomMember = memberOpt.get();

            if (chatRoomMember.getDeletedAt() == null) {
                return false;
            }

            chatRoomMember.rejoin(now);

            return true;
        }

        ChatRoomMember chatRoomMember = ChatRoomMember.of(chatRoom, user, now);

        chatRoomMemberRepository.save(chatRoomMember);

        return true;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new BaseException(ErrorEnum.USER_NOT_FOUND)
        );
    }

    private Optional<DirectChatRoom> findDirectRoom(Long userId1, Long userId2) {
        long min = Math.min(userId1, userId2);

        long max = Math.max(userId1, userId2);

        return directChatRoomRepository.findByUser1IdAndUser2Id(min, max);
    }

    private String resolveRoomName(ChatRoom chatRoom, Long currentUserId) {
        if (chatRoom.getChatRoomType() == ChatRoomType.DIRECT) {
            OpponentInfoProjection opponent = chatRoomMemberRepository.findOpponentInfo(
                    chatRoom.getId(),
                    currentUserId
            );

            if (opponent == null || opponent.getUserDeletedAt() != null) {
                return UNKNOWN_NICKNAME;
            }

            return opponent.getNickname();
        }

        return chatRoom.getName();
    }

    private void leaveDirectRoom(ChatRoomMember chatRoomMember) {
        chatRoomMember.leaveChatRoom();
    }

    private void leaveGroupRoom(ChatRoom chatRoom, ChatRoomMember chatRoomMember) {
        chatRoomMember.leaveChatRoom();

        chatRoom.decreaseMemberCount();

        ChatMessage leaveMessage = createLeaveSystemMessage(chatRoom, chatRoomMember);

        updateUnreadCount(chatRoom, chatRoomMember.getUser().getId());

        int unreadCount = chatUnreadCountManager.calculateUnreadCount(
                leaveMessage,
                chatRoomMember.getUser().getId(),
                readStatuses(chatRoom.getId())
        );

        publishSystemMessage(chatRoom, leaveMessage, unreadCount);
    }

    private void handleLeave(ChatRoom chatRoom, ChatRoomMember chatRoomMember) {
        switch (chatRoom.getChatRoomType()) {
            case DIRECT -> leaveDirectRoom(chatRoomMember);
            case GROUP -> leaveGroupRoom(chatRoom, chatRoomMember);
        }
    }

    private void publishSystemMessage(ChatRoom chatRoom, ChatMessage chatMessage, int unreadMemberCount) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                simpMessagingTemplate.convertAndSend(
                        "/topic/chat/" + chatRoom.getId(),
                        ChatMessageResponse.from(chatMessage, unreadMemberCount)
                );
            }
        });
    }

    private List<MemberReadStatusProjection> readStatuses(Long roomId) {
        return chatRoomMemberRepository.findReadStatuses(roomId);
    }

    private void updateUnreadCount(ChatRoom chatRoom, Long userId) {
        chatRoomMemberRepository.increaseUnreadCount(chatRoom.getId(), userId);
    }

    private ChatMessage createLeaveSystemMessage(ChatRoom chatRoom, ChatRoomMember chatRoomMember){
        ChatMessage chatMessage = ChatMessage.of(
                chatRoom,
                null,
                SystemMessageType.USER_LEAVE.format(chatRoomMember.getUser().getNickname()),
                ChatMessageType.SYSTEM,
                null
        );

        chatMessageRepository.save(chatMessage);

        chatRoom.updateLastMessage(chatMessage);

        return chatMessage;
    }

    private void createGroupRoomSystemMessage(ChatRoom chatRoom, User owner) {
        ChatMessage chatMessage = ChatMessage.of(
                chatRoom,
                null,
                SystemMessageType.GROUP_ROOM_CREATED.format(owner.getNickname()),
                ChatMessageType.SYSTEM,
                null
        );

        chatMessageRepository.save(chatMessage);

        chatRoom.updateLastMessage(chatMessage);
    }

    private ChatMessage createInviteSystemMessage(ChatRoom chatRoom, User inviter, List<User> users) {
        String invitedNames = users
                .stream()
                .map(user -> user.getNickname() + "님")
                .collect(Collectors.joining(", "));

        String content = SystemMessageType.USER_INVITED.format(inviter.getNickname(), invitedNames);

        ChatMessage chatMessage = ChatMessage.of(
                chatRoom,
                null,
                content,
                ChatMessageType.SYSTEM,
                null
        );

        chatMessageRepository.save(chatMessage);

        chatRoom.updateLastMessage(chatMessage);

        return chatMessage;
    }

    private ChatMessage createUpdateNameSystemMessage(ChatRoom chatRoom, User user, String oldName, String newName) {
        ChatMessage chatMessage = ChatMessage.of(
                chatRoom,
                null,
                SystemMessageType.ROOM_NAME_UPDATED.format(
                        user.getNickname(),
                        oldName,
                        newName
                ),
                ChatMessageType.SYSTEM,
                null
        );

        chatMessageRepository.save(chatMessage);

        chatRoom.updateLastMessage(chatMessage);

        return chatMessage;
    }
}
