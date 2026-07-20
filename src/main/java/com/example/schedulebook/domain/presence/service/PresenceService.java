package com.example.schedulebook.domain.presence.service;

import com.example.schedulebook.common.redis.service.RedisPresenceService;
import com.example.schedulebook.domain.friend.validator.FriendValidator;
import com.example.schedulebook.domain.presence.dto.response.UserPresenceResponse;
import com.example.schedulebook.domain.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PresenceService {
    private final UserValidator userValidator;
    private final FriendValidator friendValidator;
    private final RedisPresenceService redisPresenceService;

    @Transactional(readOnly = true)
    public UserPresenceResponse findPresence(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            userValidator.validateActiveUser(targetUserId);

        } else {
            friendValidator.validatePresenceAccess(currentUserId, targetUserId);

            userValidator.validateActiveUser(targetUserId);
        }

        return new UserPresenceResponse(
                targetUserId,
                redisPresenceService.isOnline(targetUserId),
                redisPresenceService.getSessionCount(targetUserId)
        );
    }
}
