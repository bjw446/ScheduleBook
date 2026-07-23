package com.example.schedulebook.domain.auth.listener;

import com.example.schedulebook.domain.auth.event.RefreshReplayDetectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshReplayDetectedListener {

    @EventListener
    public void handle(RefreshReplayDetectedEvent event) {
        try {
            // TODO : Notification, monitoring 등 이벤트 추가 예정

        } catch (Exception e) {
            log.error("리프레시 재사용 감시 전달 실패 : {}", e.getMessage(), e);
        }
    }
}
