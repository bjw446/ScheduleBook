package com.example.schedulebook.domain.friend.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.friend.entity.Friend;
import com.example.schedulebook.domain.friend.enums.FriendStatus;
import com.example.schedulebook.domain.friend.repository.FriendRepository;
import com.example.schedulebook.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendValidator {
    private final FriendRepository friendRepository;

    public Friend validateFriend(Long friendId) {
        return friendRepository.findByIdWithUsers(friendId).orElseThrow(
                () -> new BaseException(ErrorEnum.FRIEND_NOT_FOUND)
        );
    }

    public void validateReceiver(Friend friend, Long currentUserId) {
        if (!friend.getReceiver().getId().equals(currentUserId)) {
            throw new BaseException(ErrorEnum.FRIEND_FORBIDDEN);
        }
    }

    public void validatePending(Friend friend) {
        if (friend.getFriendStatus() != FriendStatus.PENDING) {
            throw new BaseException(ErrorEnum.INVALID_FRIEND_STATUS);
        }
    }

    public void validateMyself(Long friendId, Long currentUserId) {
        if (currentUserId.equals(friendId)) {
            throw new BaseException(ErrorEnum.CANNOT_ADD_MYSELF);
        }
    }

    public void validateFriendOwner(Friend friend, Long currentUserId) {
        boolean owner = friend.getRequester().getId().equals(currentUserId) || friend.getReceiver().getId().equals(currentUserId);

        if (!owner) {
            throw new BaseException(ErrorEnum.FRIEND_FORBIDDEN);
        }
    }

    public void validateDeletable(Friend friend) {
        if (friend.getFriendStatus() != FriendStatus.ACCEPTED) {
            throw new BaseException(ErrorEnum.INVALID_FRIEND_STATUS);
        }
    }

    public User extractFriendUser(Friend friend, Long currentUserId) {
        return friend.getRequester().getId().equals(currentUserId) ? friend.getReceiver() : friend.getRequester();
    }

    public void validateFriendStatus(Friend friend) {
        if (friend.getFriendStatus() != FriendStatus.REJECTED && friend.getFriendStatus() != FriendStatus.DELETED) {
            throw new BaseException(ErrorEnum.FRIEND_ALREADY_EXISTS);
        }
    }

    public void validateFriendRelation(Long currentUserId, Long friendId) {
        boolean exists = friendRepository.existsAcceptedFriend(currentUserId, friendId, FriendStatus.ACCEPTED);

        if (!exists) {
            throw new BaseException(ErrorEnum.FRIEND_NOT_FOUND);
        }
    }

    public void validatePresenceAccess(Long currentUserId, Long targetUserId) {
        if (!currentUserId.equals(targetUserId) && !friendRepository.existsAcceptedFriend(
                currentUserId,
                targetUserId,
                FriendStatus.ACCEPTED
        )) {
            throw new BaseException(ErrorEnum.PRESENCE_ACCESS_DENIED);
        }
    }
}
