package com.example.schedulebook.common.redis.script;

import com.example.schedulebook.common.consts.RedisConst;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisScript {

    @Bean
    public org.springframework.data.redis.core.script.RedisScript<Long> refreshRotateScript() {
        return org.springframework.data.redis.core.script.RedisScript.of(RedisConst.REFRESH_ROTATE_SCRIPT, Long.class);
    }

    @Bean
    public org.springframework.data.redis.core.script.RedisScript<Long> rateLimitScript() {
        return org.springframework.data.redis.core.script.RedisScript.of(RedisConst.RATE_LIMIT_SCRIPT, Long.class);
    }

    @Bean
    public org.springframework.data.redis.core.script.RedisScript<Long> removeSessionScript() {
        return org.springframework.data.redis.core.script.RedisScript.of(RedisConst.REMOVE_SESSION_SCRIPT, Long.class);
    }

    @Bean
    public org.springframework.data.redis.core.script.RedisScript<Long> deleteAllSessionsScript() {
        return org.springframework.data.redis.core.script.RedisScript.of(RedisConst.DELETE_ALL_SESSIONS_SCRIPT, Long.class);
    }

    @Bean
    public org.springframework.data.redis.core.script.RedisScript<Long> updateLastAccessScript() {
        return org.springframework.data.redis.core.script.RedisScript.of(RedisConst.UPDATE_LAST_ACCESS_SCRIPT, Long.class);
    }
}
