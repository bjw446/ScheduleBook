package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.consts.WebSocketDestination;
import com.example.schedulebook.common.redis.service.RedisPresenceService;
import com.example.schedulebook.domain.auth.dto.response.ForceLogoutResponse;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ForceLogoutHandler {
    private final SessionBlockStore sessionBlockStore;
    private final RedisPresenceService redisPresenceService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public void handle(ForceLogoutSessionEvent event) {
        sessionBlockStore.block(event.sessionId(), event.accessTokenExpiration());

        log.info("강제 로그아웃 처리 : userId = {}, sessionId = {}", event.userId(), event.sessionId());

        if (!redisPresenceService.isOnline(event.userId())) {
            return;
        }

        try {
            simpMessagingTemplate.convertAndSendToUser(
                    event.userId().toString(),
                    WebSocketDestination.FORCE_LOGOUT,
                    ForceLogoutResponse.from(event)
            );

        } catch (Exception e) {
            log.error("웹소켓 전송 실패 : {}", WebSocketDestination.FORCE_LOGOUT, e);
        }
    }
}
