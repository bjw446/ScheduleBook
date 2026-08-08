package com.example.schedulebook.common.websocket.publisher;

import com.example.schedulebook.common.executor.AfterCommitExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketPublisher {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final AfterCommitExecutor afterCommitExecutor;

    public void sendAfterCommit(String destination, Object payload) {
        afterCommitExecutor.execute(() -> {
            try {
                simpMessagingTemplate.convertAndSend(destination, payload);
            } catch (Exception e) {
                log.error("웹소켓 전송 실패 : {}", destination, e);
            }
        });
    }

    public void send(String destination, Object payload) {
        try {
            simpMessagingTemplate.convertAndSend(destination, payload);
        } catch (Exception e) {
            log.error("웹소켓 전송 실패 : {}", destination, e);
        }
    }
}