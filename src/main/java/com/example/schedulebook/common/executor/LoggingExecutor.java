package com.example.schedulebook.common.executor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoggingExecutor {
    public void execute(String name, Runnable action) {
        try {
            action.run();

            log.info("{} 완료", name);

        } catch (Exception e) {
            log.error("{} 실패 : {}", name, e.getMessage(), e);

            // TODO Outbox 저장
            // TODO Retry Queue 등록
        }
    }
}
