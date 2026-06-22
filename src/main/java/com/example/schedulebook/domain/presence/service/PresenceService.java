package com.example.schedulebook.domain.presence.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.websocket.WebSocketSessionRegistry;
import com.example.schedulebook.domain.friend.enums.FriendStatus;
import com.example.schedulebook.domain.friend.repository.FriendRepository;
import com.example.schedulebook.domain.presence.dto.response.UserPresenceResponse;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.enums.UserStatus;
import com.example.schedulebook.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PresenceService {
    private final WebSocketSessionRegistry webSocketSessionRegistry;
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserPresenceResponse findPresence(Long currentUserId, Long targetUserId) {
        if (!currentUserId.equals(targetUserId) && !friendRepository.existsAcceptedFriend(
                currentUserId,
                targetUserId,
                FriendStatus.ACCEPTED
        )) {
            throw new BaseException(ErrorEnum.PRESENCE_ACCESS_DENIED);
        }

        validateTargetUser(targetUserId);

        return new UserPresenceResponse(
                targetUserId,
                webSocketSessionRegistry.isOnline(targetUserId),
                webSocketSessionRegistry.getSessionCount(targetUserId)
        );
    }

    private void validateTargetUser(Long targetUserId) {
        User user = userRepository.findById(targetUserId).orElseThrow(
                () -> new BaseException(ErrorEnum.USER_NOT_FOUND)
        );

        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new BaseException(ErrorEnum.USER_NOT_ACTIVE);
        }
    }
}
