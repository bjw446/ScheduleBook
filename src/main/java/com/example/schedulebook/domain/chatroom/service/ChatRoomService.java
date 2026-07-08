package com.example.schedulebook.domain.chatroom.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.chatroom.dto.request.ChatRoomInviteRequest;
import com.example.schedulebook.domain.chatroom.dto.request.GroupChatRoomCreateRequest;
import com.example.schedulebook.domain.chatroom.dto.request.ChatRoomUpdateNameRequest;
import com.example.schedulebook.domain.chatmessage.dto.response.*;
import com.example.schedulebook.domain.chatroom.dto.response.ChatRoomDetailResponse;
import com.example.schedulebook.domain.chatroom.dto.response.ChatRoomListResponse;
import com.example.schedulebook.domain.chatroom.dto.response.ChatRoomResponse;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import com.example.schedulebook.domain.chatroom.repository.ChatRoomMemberRepository;
import com.example.schedulebook.domain.chatroom.repository.ChatRoomRepository;
import com.example.schedulebook.domain.chatroom.repository.DirectChatRoomRepository;
import com.example.schedulebook.domain.chatroom.entity.ChatRoomMember;
import com.example.schedulebook.domain.chatroom.entity.DirectChatRoom;
import com.example.schedulebook.domain.chatroom.enums.ChatRoomType;
import com.example.schedulebook.domain.chatroom.projection.OpponentInfoProjection;
import com.example.schedulebook.domain.chatroom.validator.ChatRoomValidator;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static com.example.schedulebook.common.consts.CommonConst.UNKNOWN_NICKNAME;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final DirectChatRoomRepository directChatRoomRepository;
    private final ChatRoomLifecycleManager chatRoomLifecycleManager;
    private final UserValidator userValidator;
    private final ChatRoomValidator chatRoomValidator;

    public ChatRoomResponse createDirectRoom(Long currentUserId, Long friendId) {
        chatRoomValidator.validateMyself(currentUserId, friendId);

        User currentUser = userValidator.validateActiveUser(currentUserId);

        User friendUser = userValidator.validateActiveUser(friendId);

        chatRoomValidator.validateFriend(currentUserId, friendId);

        try {
            Optional<DirectChatRoom> existingRoom = findDirectRoom(currentUserId, friendId);

            if (existingRoom.isPresent()) {
                return ChatRoomResponse.from(existingRoom.get().getChatRoom());
            }

            ChatRoom chatRoom = ChatRoom.direct();

            chatRoomRepository.save(chatRoom);

            ChatRoomMember member1 = ChatRoomMember.of(chatRoom, currentUser, LocalDateTime.now());

            ChatRoomMember member2 = ChatRoomMember.of(chatRoom, friendUser, LocalDateTime.now());

            chatRoomMemberRepository.saveAll(List.of(member1, member2));

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
        User owner = userValidator.validateActiveUser(currentUserId);

        List<User> members = chatRoomValidator.validateInviteMembers(currentUserId, request.memberIds());

        ChatRoom chatRoom = ChatRoom.group(request.name());

        chatRoomRepository.save(chatRoom);

        List<ChatRoomMember> chatRoomMembers = new ArrayList<>();

        ChatRoomMember ownerMember = ChatRoomMember.of(chatRoom, owner, LocalDateTime.now());

        chatRoomMembers.add(ownerMember);

        members.forEach(member -> chatRoomMembers.add(
                ChatRoomMember.of(chatRoom, member, LocalDateTime.now())
        ));

        chatRoomMemberRepository.saveAll(chatRoomMembers);

        chatRoomLifecycleManager.afterRoomCreated(chatRoom, owner);

        return ChatRoomResponse.from(chatRoom);
    }

    public ChatRoomResponse inviteMembers(Long currentUserId, Long roomId, ChatRoomInviteRequest request) {
        ChatRoom chatRoom = chatRoomValidator.validateChatRoom(roomId);

        chatRoomValidator.validateChatRoomMember(currentUserId, roomId);

        chatRoomValidator.validateChatRoomType(chatRoom);

        List<User> users = chatRoomValidator.validateInviteMembers(currentUserId, request.memberIds());

        List<User> invitedUsers = new ArrayList<>();

        for (User user : users) {
            boolean invited = processInvitation(chatRoom, user, LocalDateTime.now());

            if (invited) {
                invitedUsers.add(user);
            }
        }

        if (!invitedUsers.isEmpty()) {
            chatRoomLifecycleManager.afterMemberInvited(chatRoom, userValidator.validateActiveUser(currentUserId), invitedUsers);
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
        ChatRoomMember chatRoomMember = chatRoomValidator.validateChatRoomMember(currentUserId, roomId);

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
        ChatRoom chatRoom = chatRoomValidator.validateChatRoom(roomId);

        chatRoomValidator.validateChatRoomMember(currentUserId, roomId);

        chatRoomValidator.validateChatRoomType(chatRoom);

        chatRoomValidator.validateUpdateName(chatRoom, request.name().trim());

        String oldName = chatRoom.getName();

        chatRoom.updateName(request.name().trim());

        chatRoomLifecycleManager.afterRoomNameUpdated(chatRoom, userValidator.validateActiveUser(currentUserId), oldName, request.name().trim());

        return ChatRoomResponse.from(chatRoom);
    }

    public void leaveChatRoom(Long currentUserId, Long roomId) {
        ChatRoomMember chatRoomMember = chatRoomValidator.validateChatRoomMember(currentUserId, roomId);

        ChatRoom chatRoom = chatRoomMember.getChatRoom();

        handleLeave(chatRoom, chatRoomMember);
    }

    private boolean processInvitation(ChatRoom chatRoom, User user, LocalDateTime now) {
        Optional<ChatRoomMember> memberOpt = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoom.getId(), user.getId());

        if (memberOpt.isEmpty()) {
            ChatRoomMember chatRoomMember = ChatRoomMember.of(chatRoom, user, now);

            chatRoomMemberRepository.save(chatRoomMember);

            return true;
        }

        ChatRoomMember chatRoomMember = memberOpt.get();

        if (chatRoomMember.getDeletedAt() == null) {
            return false;
        }

        chatRoomMember.rejoin(now);

        return true;
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

        chatRoomLifecycleManager.afterMemberLeft(chatRoom, chatRoomMember);
    }

    private void handleLeave(ChatRoom chatRoom, ChatRoomMember chatRoomMember) {
        switch (chatRoom.getChatRoomType()) {
            case DIRECT -> leaveDirectRoom(chatRoomMember);
            case GROUP -> leaveGroupRoom(chatRoom, chatRoomMember);
        }
    }
}
