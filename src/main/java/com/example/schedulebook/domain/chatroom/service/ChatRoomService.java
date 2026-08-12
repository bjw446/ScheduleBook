package com.example.schedulebook.domain.chatroom.service;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.chatroom.dto.request.ChatRoomInviteRequest;
import com.example.schedulebook.domain.chatroom.dto.request.GroupChatRoomCreateRequest;
import com.example.schedulebook.domain.chatroom.dto.request.ChatRoomUpdateNameRequest;
import com.example.schedulebook.domain.chatmessage.dto.response.*;
import com.example.schedulebook.domain.chatroom.dto.response.*;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import com.example.schedulebook.domain.chatroom.projection.ChatRoomListProjection;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;


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

            int updateCount = chatRoomRepository.increaseMemberCount(chatRoom.getId(), 2);

            if (updateCount != 1) {
                throw new BaseException(ErrorEnum.CHAT_ROOM_MEMBER_COUNT_UPDATE_FAILED);
            }

            ChatRoom updatedChatRoom = chatRoomValidator.validateChatRoom(chatRoom.getId());

            directChatRoomRepository.save(DirectChatRoom.of(currentUserId, friendId, updatedChatRoom));

            return ChatRoomResponse.from(updatedChatRoom);
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

        int updateCount = chatRoomRepository.increaseMemberCount(chatRoom.getId(), chatRoomMembers.size());

        if (updateCount != 1) {
            throw new BaseException(ErrorEnum.CHAT_ROOM_MEMBER_COUNT_UPDATE_FAILED);
        }

        ChatRoom updatedChatRoom = chatRoomValidator.validateChatRoom(chatRoom.getId());

        chatRoomLifecycleManager.afterRoomCreated(updatedChatRoom, owner);

        return ChatRoomResponse.from(updatedChatRoom);
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

        ChatRoom updatedChatRoom = chatRoomValidator.validateChatRoom(chatRoom.getId());

        if (!invitedUsers.isEmpty()) {
            chatRoomLifecycleManager.afterMemberInvited(updatedChatRoom, userValidator.validateActiveUser(currentUserId), invitedUsers);
        }

        return ChatRoomResponse.from(updatedChatRoom);
    }

    @Transactional(readOnly = true)
    public ChatRoomSliceResponse findMyChatRooms(Long currentUserId, LocalDateTime cursorTime, Long cursorRoomId, int size) {
        if (cursorTime != null && cursorRoomId == null) {
            throw new BaseException(ErrorEnum.INVALID_CURSOR);
        }

        int pageSize = Math.max(1, Math.min(size, CommonConst.MAX_PAGE_SIZE));

        List<ChatRoomListProjection> projections = chatRoomMemberRepository.findMyChatRooms(
                currentUserId,
                cursorTime,
                cursorRoomId,
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = projections.size() > pageSize;

        if (hasNext) {
            projections = projections.subList(0, pageSize);
        }

        List<ChatRoomListResponse> responses = projections.stream()
                .map(ChatRoomListResponse::from)
                .toList();

        ChatRoomCursor nextCursor = null;

        if (hasNext && !responses.isEmpty()) {
            ChatRoomListProjection last = projections.get(projections.size() - 1);

            nextCursor = new ChatRoomCursor(last.getLastMessageAt(), last.getRoomId());
        }

        return new ChatRoomSliceResponse(responses, nextCursor, hasNext);
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

    public void removeAllChatRelations(Long userId) {
        List<ChatRoomMember> chatRoomMembers = chatRoomMemberRepository.findAllByUserId(userId);

        LocalDateTime deleteAt = LocalDateTime.now();

        for (ChatRoomMember chatRoomMember : chatRoomMembers) {
            int leaveCount = chatRoomMemberRepository.leave(chatRoomMember.getId(), deleteAt);

            if (leaveCount != 1) {
                continue;
            }

            ChatRoom chatRoom = chatRoomMember.getChatRoom();

            if (chatRoom.getChatRoomType() == ChatRoomType.GROUP) {
                int updateCount = chatRoomRepository.decreaseMemberCount(chatRoom.getId());

                if (updateCount != 1) {
                    throw new BaseException(ErrorEnum.CHAT_ROOM_MEMBER_COUNT_UPDATE_FAILED);
                }
            }
        }

        directChatRoomRepository.deleteDirectChatRoomByUserId(userId);
    }

    private boolean processInvitation(ChatRoom chatRoom, User user, LocalDateTime now) {
        Optional<ChatRoomMember> memberOpt = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoom.getId(), user.getId());

        if (memberOpt.isEmpty()) {
            ChatRoomMember chatRoomMember = ChatRoomMember.of(chatRoom, user, now);

            chatRoomMemberRepository.save(chatRoomMember);

            int updateCount = chatRoomRepository.increaseMemberCount(chatRoom.getId(), 1);

            if (updateCount != 1) {
                throw new BaseException(ErrorEnum.CHAT_ROOM_MEMBER_COUNT_UPDATE_FAILED);
            }

            return true;
        }

        ChatRoomMember chatRoomMember = memberOpt.get();

        if (chatRoomMember.getDeletedAt() == null) {
            return false;
        }

        int rejoinCount = chatRoomMemberRepository.rejoin(chatRoomMember.getId(), now);

        if (rejoinCount != 1) {
            return false;
        }

        int updateCount = chatRoomRepository.increaseMemberCount(chatRoom.getId(), 1);

        if (updateCount != 1) {
            throw new BaseException(ErrorEnum.CHAT_ROOM_MEMBER_COUNT_UPDATE_FAILED);
        }

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
                return CommonConst.UNKNOWN_NICKNAME;
            }

            return opponent.getNickname();
        }

        return chatRoom.getName();
    }

    private void leaveDirectRoom(ChatRoomMember chatRoomMember) {
        chatRoomMemberRepository.leave(chatRoomMember.getId(), LocalDateTime.now());
    }

    private void leaveGroupRoom(ChatRoom chatRoom, ChatRoomMember chatRoomMember) {
        Long memberId = chatRoomMember.getId();

        String nickname = chatRoomMember.getUser().getNickname();

        int leaveCount = chatRoomMemberRepository.leave(memberId, LocalDateTime.now());

        if (leaveCount != 1) {
            return;
        }

        int updateCount = chatRoomRepository.decreaseMemberCount(chatRoom.getId());

        if (updateCount != 1) {
            throw new BaseException(ErrorEnum.CHAT_ROOM_MEMBER_COUNT_UPDATE_FAILED);
        }

        ChatRoom updatedChatRoom = chatRoomValidator.validateChatRoom(chatRoom.getId());

        chatRoomLifecycleManager.afterMemberLeft(updatedChatRoom, nickname);
    }

    private void handleLeave(ChatRoom chatRoom, ChatRoomMember chatRoomMember) {
        switch (chatRoom.getChatRoomType()) {
            case DIRECT -> leaveDirectRoom(chatRoomMember);
            case GROUP -> leaveGroupRoom(chatRoom, chatRoomMember);
        }
    }
}
