package com.example.schedulebook.domain.auth.dispatcher;

import com.example.schedulebook.common.consts.WebSocketDestination;
import com.example.schedulebook.common.redis.service.RedisPresenceService;
import com.example.schedulebook.domain.auth.dto.response.ForceLogoutResponse;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.example.schedulebook.domain.auth.service.SessionBlockStore;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ForceLogoutDispatcherImpl implements ForceLogoutDispatcher {
    private final SessionBlockStore sessionBlockStore;
    private final RedisPresenceService redisPresenceService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public void dispatch(ForceLogoutSessionEvent event) {
        sessionBlockStore.block(event.sessionId(), event.accessTokenExpiration());

        if (!redisPresenceService.isOnline(event.userId())) {
            return;
        }

        simpMessagingTemplate.convertAndSendToUser(
                event.userId().toString(),
                WebSocketDestination.FORCE_LOGOUT,
                ForceLogoutResponse.from(event)
        );
    }
}
