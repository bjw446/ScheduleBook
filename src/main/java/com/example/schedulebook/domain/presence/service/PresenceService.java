package com.example.schedulebook.domain.presence.service;

import com.example.schedulebook.common.websocket.WebSocketSessionRegistry;
import com.example.schedulebook.domain.friend.validator.FriendValidator;
import com.example.schedulebook.domain.presence.dto.response.UserPresenceResponse;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PresenceService {
    private final WebSocketSessionRegistry webSocketSessionRegistry;
    private final UserValidator userValidator;
    private final FriendValidator friendValidator;

    @Transactional(readOnly = true)
    public UserPresenceResponse findPresence(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            userValidator.validateActiveUser(targetUserId);

        } else {
            friendValidator.validateFriendRelation(currentUserId, targetUserId);

            userValidator.validateActiveUser(targetUserId);
        }

        return new UserPresenceResponse(
                targetUserId,
                webSocketSessionRegistry.isOnline(targetUserId),
                webSocketSessionRegistry.getSessionCount(targetUserId)
        );
    }
}
