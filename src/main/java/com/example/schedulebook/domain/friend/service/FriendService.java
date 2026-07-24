package com.example.schedulebook.domain.friend.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.redis.service.RedisPresenceService;
import com.example.schedulebook.domain.friend.dto.request.FriendRequest;
import com.example.schedulebook.domain.friend.dto.response.ReceivedFriendRequestResponse;
import com.example.schedulebook.domain.friend.dto.response.FriendResponse;
import com.example.schedulebook.domain.friend.dto.response.FriendSummaryResponse;
import com.example.schedulebook.domain.friend.dto.response.SentFriendRequestResponse;
import com.example.schedulebook.domain.friend.entity.Friend;
import com.example.schedulebook.domain.friend.enums.FriendStatus;
import com.example.schedulebook.domain.friend.event.FriendAcceptedEvent;
import com.example.schedulebook.domain.friend.event.FriendRequestedEvent;
import com.example.schedulebook.domain.friend.repository.FriendRepository;
import com.example.schedulebook.domain.friend.validator.FriendValidator;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.event.OutboxSaveEvent;
import com.example.schedulebook.domain.outbox.service.OutboxPublishService;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FriendService {
    private final FriendRepository friendRepository;
    private final UserValidator userValidator;
    private final FriendValidator friendValidator;
    private final RedisPresenceService redisPresenceService;
    private final OutboxPublishService outboxPublishService;

    public FriendResponse requestFriend(FriendRequest request, Long currentUserId) {
        friendValidator.validateMyself(request.receiverId(), currentUserId);

        User requester = userValidator.validateActiveUser(currentUserId);

        User receiver = userValidator.validateActiveUser(request.receiverId());

        Friend existing = friendRepository.findRelation(requester.getId(), receiver.getId()).orElse(null);

        if (existing != null) {
            friendValidator.validateFriendStatus(existing);

            existing.reRequest(requester, receiver);

            return completeFriendRequest(existing, requester, receiver);
        }

        Friend savedFriend;

        try {
            Friend friend = Friend.request(requester, receiver);

            savedFriend = friendRepository.save(friend);

        } catch (DataIntegrityViolationException e) {
            log.warn("친구 요청 생성 중 중복 에러 발생 (requesterId={}, receiverId={}): {}",
                    requester.getId(), receiver.getId(), e.getMessage(), e);
            throw new BaseException(ErrorEnum.FRIEND_ALREADY_EXISTS);
        }

        return completeFriendRequest(savedFriend, requester, receiver);
    }

    @Transactional(readOnly = true)
    public List<FriendSummaryResponse> findAllFriends(Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        List<Friend> friends = friendRepository.findAcceptedFriends(currentUserId, FriendStatus.ACCEPTED);

        List<Long> friendIds = friends.stream()
                .map(friend ->
                        friendValidator.extractFriendUser(friend, currentUserId).getId()
                )
                .toList();

        Map<Long, Boolean> onlineMap = redisPresenceService.getOnlineStatuses(friendIds);

        return friends.stream()
                .map(friend -> {
                    User friendUser = friendValidator.extractFriendUser(friend, currentUserId);

                    return FriendSummaryResponse.from(
                            friend.getId(),
                            friendUser,
                            onlineMap.getOrDefault(friendUser.getId(), false)
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

        FriendAcceptedEvent friendAcceptedEvent = new FriendAcceptedEvent(
                friend.getRequester().getId(),
                friend.getReceiver().getNickname(),
                friend.getId()
        );

        outboxPublishService.publish(new OutboxSaveEvent(
                OutboxAggregateType.FRIEND,
                friend.getId(),
                OutboxEventType.FRIEND_ACCEPTED,
                friendAcceptedEvent
        ));

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

    public void removeAllFriendRelations(Long userId) {
        friendRepository.deleteAllByUserId(userId);
    }

    private FriendResponse completeFriendRequest(Friend friend, User requester, User receiver) {
        FriendRequestedEvent friendRequestedEvent = new FriendRequestedEvent(
                receiver.getId(),
                requester.getNickname(),
                friend.getId()
        );

        outboxPublishService.publish(new OutboxSaveEvent(
                OutboxAggregateType.FRIEND,
                friend.getId(),
                OutboxEventType.FRIEND_REQUESTED,
                friendRequestedEvent
        ));

        return FriendResponse.from(friend);
    }
}
