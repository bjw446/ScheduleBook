package com.example.schedulebook.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class AppConfig implements AsyncConfigurer {

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, obj) -> {
            log.error("비동기 에러 발생, 메서드 : {}", method.getName());
            log.error("에러 메세지 : {}", throwable.getMessage(), throwable);
            for (Object param : obj) {
                log.error("파라미터 정보 : {}", param);
            };
        };
    }
}
