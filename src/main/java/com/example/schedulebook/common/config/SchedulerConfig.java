package com.example.schedulebook.common.config;

import com.example.schedulebook.common.consts.CommonConst;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    @Bean(name = "sessionBlockTaskScheduler")
    public TaskScheduler sessionBlockTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix(CommonConst.SESSION_BLOCK);
        scheduler.initialize();
        return scheduler;
    }
}
