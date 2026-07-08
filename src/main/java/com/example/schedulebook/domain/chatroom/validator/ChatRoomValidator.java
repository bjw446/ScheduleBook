package com.example.schedulebook.domain.chatroom.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import com.example.schedulebook.domain.chatroom.entity.ChatRoomMember;
import com.example.schedulebook.domain.chatroom.enums.ChatRoomType;
import com.example.schedulebook.domain.chatroom.repository.ChatRoomMemberRepository;
import com.example.schedulebook.domain.chatroom.repository.ChatRoomRepository;
import com.example.schedulebook.domain.friend.enums.FriendStatus;
import com.example.schedulebook.domain.friend.repository.FriendRepository;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ChatRoomValidator {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    public ChatRoomMember validateChatRoomMember(Long currentUserId, Long roomId) {
        return chatRoomMemberRepository.findActiveByChatRoomIdAndUserId(roomId, currentUserId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_ROOM_FORBIDDEN)
        );
    }

    public void validateFriend(Long currentUserId, Long friendId) {
        if (!friendRepository.existsAcceptedFriend(currentUserId, friendId, FriendStatus.ACCEPTED)) {
            throw new BaseException(ErrorEnum.FRIEND_NOT_FOUND);
        }
    }

    public void validateMyself(Long currentUserId, Long friendId) {
        if (currentUserId.equals(friendId)) {
            throw new BaseException(ErrorEnum.INVALID_CHAT_TARGET);
        }
    }

    public ChatRoom validateChatRoom(Long roomId) {
        return chatRoomRepository.findById(roomId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_ROOM_NOT_FOUND)
        );
    }

    public void validateInviteMembers(Long currentUserId, List<Long> memberIds) {
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

    public void validateChatRoomType(ChatRoom chatRoom) {
        if (chatRoom.getChatRoomType() != ChatRoomType.GROUP) {
            throw new BaseException(ErrorEnum.INVALID_CHAT_ROOM_TYPE);
        }
    }

    public void validateUpdateName(ChatRoom chatRoom, String newName) {
        if (chatRoom.getName().trim().equals(newName.trim())) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }
    }

    public User validateMember(Long roomId, Long currentUserId) {
        return chatRoomMemberRepository.findUserInRoom(roomId, currentUserId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_ROOM_FORBIDDEN)
        );
    }
}
