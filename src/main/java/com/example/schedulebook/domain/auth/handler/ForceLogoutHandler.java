package com.example.schedulebook.domain.auth.handler;

import com.example.schedulebook.common.consts.WebSocketDestination;
import com.example.schedulebook.domain.auth.dispatcher.ForceLogoutDispatcher;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.example.schedulebook.domain.auth.service.ForceLogoutRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ForceLogoutHandler {
    private final ForceLogoutDispatcher forceLogoutDispatcher;
    private final ForceLogoutRetryService forceLogoutRetryService;

    public void handle(ForceLogoutSessionEvent event) {
        try {
            log.info("강제 로그아웃 처리 : userId = {}, sessionId = {}", event.userId(), event.sessionId());

            forceLogoutDispatcher.dispatch(event);

        } catch (Exception e) {
            log.error("강제 로그아웃 처리 실패 : {}, userId ={}. sessionId = {}",
                    WebSocketDestination.FORCE_LOGOUT,
                    event.userId(),
                    event.sessionId(),
                    e
            );

            forceLogoutRetryService.save(event.sessionId(), event.userId(), event, e.getMessage());
        }
    }
}
