package com.example.schedulebook.domain.friend.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.friend.dto.request.FriendRequest;
import com.example.schedulebook.domain.friend.dto.response.ReceivedFriendRequestResponse;
import com.example.schedulebook.domain.friend.dto.response.FriendResponse;
import com.example.schedulebook.domain.friend.dto.response.FriendSummaryResponse;
import com.example.schedulebook.domain.friend.dto.response.SentFriendRequestResponse;
import com.example.schedulebook.domain.friend.entity.Friend;
import com.example.schedulebook.domain.friend.enums.FriendStatus;
import com.example.schedulebook.domain.friend.repository.FriendRepository;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.enums.UserStatus;
import com.example.schedulebook.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FriendService {
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    public FriendResponse requestFriend(FriendRequest request, Long currentUserId) {
        validateMyself(request.receiverId(), currentUserId);

        User requester = validateUser(currentUserId);

        User receiver = validateUser(request.receiverId());

        boolean exists = friendRepository.existsFriendRelation(requester.getId(), receiver.getId());

        if (exists) {
            throw new BaseException(ErrorEnum.FRIEND_ALREADY_EXISTS);
        }

        try {
            Friend friend = Friend.request(requester, receiver);

            Friend savedFriend = friendRepository.save(friend);

            return FriendResponse.from(savedFriend);

        } catch (DataIntegrityViolationException e) {
            log.warn("친구 요청 생성 중 중복 에러 발생 : {}", e.getMessage());
            throw new BaseException(ErrorEnum.FRIEND_ALREADY_EXISTS);
        }
    }

    @Transactional(readOnly = true)
    public List<FriendSummaryResponse> findAllFriends(Long currentUserId) {
        validateUser(currentUserId);

        List<Friend> friends = friendRepository.findAcceptedFriends(currentUserId);

        return friends.stream()
                .map(friend -> FriendSummaryResponse.from(friend.getId(), extractFriendUser(friend, currentUserId)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReceivedFriendRequestResponse> findReceivedRequests(Long currentUserId) {
        validateUser(currentUserId);

        List<Friend> friends = friendRepository.findReceivedRequests(currentUserId);

        return friends.stream()
                .map(ReceivedFriendRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SentFriendRequestResponse> findSentRequests(Long currentUserId) {
        validateUser(currentUserId);

        List<Friend> friends = friendRepository.findSentRequests(currentUserId);

        return friends.stream()
                .map(SentFriendRequestResponse::from)
                .toList();
    }

    public FriendResponse acceptFriend(Long friendId, Long currentUserId) {
        Friend friend = validateFriend(friendId);

        validateReceiver(friend, currentUserId);

        validatePending(friend);

        friend.acceptFriend();

        return FriendResponse.from(friend);
    }

    public void rejectFriend(Long friendId, Long currentUserId) {
        Friend friend = validateFriend(friendId);

        validateReceiver(friend, currentUserId);

        validatePending(friend);

        friend.rejectFriend();
    }

    public void deleteFriend(Long friendId, Long currentUserId) {
        Friend friend = validateFriend(friendId);

        validateFriendOwner(friend, currentUserId);

        validateDeletable(friend);

        friend.deleteFriend();
    }

    private User validateUser(Long receiverId) {
        User user = userRepository.findById(receiverId).orElseThrow(
                () -> new BaseException(ErrorEnum.USER_NOT_FOUND)
        );

        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new BaseException(ErrorEnum.USER_NOT_ACTIVE);
        }

        return user;
    }

    private Friend validateFriend(Long friendId) {
        Friend friend = friendRepository.findByIdWithUsers(friendId).orElseThrow(
                () -> new BaseException(ErrorEnum.FRIEND_NOT_FOUND)
        );

        return friend;
    }

    private void validateReceiver(Friend friend, Long currentUserId) {
        if (!friend.getReceiver().getId().equals(currentUserId)) {
            throw new BaseException(ErrorEnum.FRIEND_FORBIDDEN);
        }
    }

    private void validatePending(Friend friend) {
        if (friend.getFriendStatus() != FriendStatus.PENDING) {
            throw new BaseException(ErrorEnum.INVALID_FRIEND_STATUS);
        }
    }

    private void validateMyself(Long friendId, Long currentUserId) {
        if (currentUserId.equals(friendId)) {
            throw new BaseException(ErrorEnum.CANNOT_ADD_MYSELF);
        }
    }

    private void validateFriendOwner(Friend friend, Long currentUserId) {
        boolean owner = friend.getRequester().getId().equals(currentUserId) || friend.getReceiver().getId().equals(currentUserId);

        if (!owner) {
            throw new BaseException(ErrorEnum.FRIEND_FORBIDDEN);
        }
    }

    private void validateDeletable(Friend friend) {
        if (friend.getFriendStatus() != FriendStatus.ACCEPTED) {
            throw new BaseException(ErrorEnum.INVALID_FRIEND_STATUS);
        }
    }

    private User extractFriendUser(Friend friend, Long currentUserId) {
        return friend.getRequester().getId().equals(currentUserId) ? friend.getReceiver() : friend.getRequester();
    }
}
