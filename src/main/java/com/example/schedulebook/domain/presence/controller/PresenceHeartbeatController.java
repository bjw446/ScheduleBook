package com.example.schedulebook.domain.presence.controller;

import com.example.schedulebook.common.redis.service.RedisPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class PresenceHeartbeatController {
    private final RedisPresenceService redisPresenceService;

    @MessageMapping("/presence/ping")
    public void ping(Principal principal, SimpMessageHeaderAccessor accessor) {
        if (principal == null) {
            return;
        }

        Long userId = Long.valueOf(principal.getName());

        redisPresenceService.refresh(userId, accessor.getSessionId());
    }
}
