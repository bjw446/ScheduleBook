package com.example.schedulebook.domain.friend.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.websocket.WebSocketSessionRegistry;
import com.example.schedulebook.domain.friend.dto.request.FriendRequest;
import com.example.schedulebook.domain.friend.dto.response.ReceivedFriendRequestResponse;
import com.example.schedulebook.domain.friend.dto.response.FriendResponse;
import com.example.schedulebook.domain.friend.dto.response.FriendSummaryResponse;
import com.example.schedulebook.domain.friend.dto.response.SentFriendRequestResponse;
import com.example.schedulebook.domain.friend.entity.Friend;
import com.example.schedulebook.domain.friend.enums.FriendStatus;
import com.example.schedulebook.domain.friend.event.FriendAcceptedEvent;
import com.example.schedulebook.domain.friend.event.FriendRequestEvent;
import com.example.schedulebook.domain.friend.repository.FriendRepository;
import com.example.schedulebook.domain.friend.validator.FriendValidator;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;
    private final WebSocketSessionRegistry webSocketSessionRegistry;
    private final UserValidator userValidator;
    private final FriendValidator friendValidator;

    public FriendResponse requestFriend(FriendRequest request, Long currentUserId) {
        friendValidator.validateMyself(request.receiverId(), currentUserId);

        User requester = userValidator.validateActiveUser(currentUserId);

        User receiver = userValidator.validateActiveUser(request.receiverId());

        Friend existing = friendRepository.findRelation(requester.getId(), receiver.getId()).orElse(null);

        if (existing != null) {
            friendValidator.validateFriendStatus(existing);

            existing.reRequest(requester, receiver);

            eventPublisher.publishEvent(new FriendRequestEvent(receiver.getId(), requester.getNickname(), existing.getId()));

            return FriendResponse.from(existing);
        }

        try {
            Friend friend = Friend.request(requester, receiver);

            Friend savedFriend = friendRepository.save(friend);

            eventPublisher.publishEvent(new FriendRequestEvent(receiver.getId(), requester.getNickname(), savedFriend.getId()));

            return FriendResponse.from(savedFriend);

        } catch (DataIntegrityViolationException e) {
            log.warn("친구 요청 생성 중 중복 에러 발생 (requesterId={}, receiverId={}): {}",
                    requester.getId(), receiver.getId(), e.getMessage(), e);
            throw new BaseException(ErrorEnum.FRIEND_ALREADY_EXISTS);
        }
    }

    @Transactional(readOnly = true)
    public List<FriendSummaryResponse> findAllFriends(Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        List<Friend> friends = friendRepository.findAcceptedFriends(currentUserId, FriendStatus.ACCEPTED);

        return friends.stream()
                .map(friend -> {
                    User friendUser = friendValidator.extractFriendUser(friend, currentUserId);

                    boolean online = webSocketSessionRegistry.isOnline(friendUser.getId());

                    return FriendSummaryResponse.from(
                            friend.getId(),
                            friendUser,
                            online
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReceivedFriendRequestResponse> findReceivedRequests(Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        List<Friend> friends = friendRepository.findReceivedRequests(currentUserId, FriendStatus.PENDING);

        return friends.stream()
                .map(ReceivedFriendRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SentFriendRequestResponse> findSentRequests(Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        List<Friend> friends = friendRepository.findSentRequests(currentUserId, FriendStatus.PENDING);

        return friends.stream()
                .map(SentFriendRequestResponse::from)
                .toList();
    }

    public FriendResponse acceptFriend(Long friendId, Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        Friend friend = friendValidator.validateFriend(friendId);

        friendValidator.validateReceiver(friend, currentUserId);

        friendValidator.validatePending(friend);

        friend.acceptFriend();

        eventPublisher.publishEvent(new FriendAcceptedEvent(friend.getRequester().getId(), friend.getReceiver().getNickname(), friend.getId()));

        return FriendResponse.from(friend);
    }

    public void rejectFriend(Long friendId, Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        Friend friend = friendValidator.validateFriend(friendId);

        friendValidator.validateReceiver(friend, currentUserId);

        friendValidator.validatePending(friend);

        friend.rejectFriend();
    }

    public void blockFriend(Long friendId, Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        Friend friend = friendValidator.validateFriend(friendId);

        friendValidator.validateFriendOwner(friend, currentUserId);

        friend.block();
    }

    public void deleteFriend(Long friendId, Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        Friend friend = friendValidator.validateFriend(friendId);

        friendValidator.validateFriendOwner(friend, currentUserId);

        friendValidator.validateDeletable(friend);

        friend.deleteFriend();
    }
}
