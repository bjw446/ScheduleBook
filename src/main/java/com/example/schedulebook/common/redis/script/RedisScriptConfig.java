package com.example.schedulebook.common.redis.script;

import com.example.schedulebook.common.consts.RedisConst;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

@Configuration
public class RedisScriptConfig {

    @Bean
    public RedisScript<Long> refreshRotateScript() {
        return RedisScript.of(RedisConst.REFRESH_ROTATE_SCRIPT, Long.class);
    }

    @Bean
    public RedisScript<Long> rateLimitScript() {
        return RedisScript.of(RedisConst.RATE_LIMIT_SCRIPT, Long.class);
    }

    @Bean
    public RedisScript<Long> removeSessionScript() {
        return RedisScript.of(RedisConst.REMOVE_SESSION_SCRIPT, Long.class);
    }

    @Bean
    public RedisScript<Long> deleteAllSessionsScript() {
        return RedisScript.of(RedisConst.DELETE_ALL_SESSIONS_SCRIPT, Long.class);
    }

    @Bean
    public RedisScript<Long> updateLastAccessScript() {
        return RedisScript.of(RedisConst.UPDATE_LAST_ACCESS_SCRIPT, Long.class);
    }

    @Bean
    public RedisScript<Long> presenceCountScript() {
        return RedisScript.of(RedisConst.PRESENCE_COUNT_SCRIPT, Long.class);
    }

    @Bean
    public RedisScript<List> presenceSessionsScript() {
        return RedisScript.of(RedisConst.PRESENCE_SESSIONS_SCRIPT, List.class);
    }

    @Bean
    public RedisScript<Long> presenceRefreshScript() {
        return RedisScript.of(RedisConst.PRESENCE_REFRESH_SCRIPT, Long.class);
    }

    @Bean
    public RedisScript<Long> presenceRemoveScript() {
        return RedisScript.of(RedisConst.PRESENCE_REMOVE_SCRIPT, Long.class);
    }

    @Bean
    public RedisScript<Long> presenceRegisterScript() {
        return RedisScript.of(RedisConst.PRESENCE_REGISTER_SCRIPT, Long.class);
    }

    @Bean
    public RedisScript<Long> deleteAllPresenceScript() {
        return RedisScript.of(RedisConst.DELETE_ALL_PRESENCE_SCRIPT, Long.class);
    }

    @Bean
    public RedisScript<Long> addSessionIfAvailableScript() {
        return RedisScript.of(RedisConst.ADD_SESSION_IF_AVAILABLE_SCRIPT, Long.class);
    }

    @Bean
    public RedisScript<Long> replaceSessionIfAvailableScript() {
        return RedisScript.of(RedisConst.REPLACE_SESSION_IF_AVAILABLE_SCRIPT, Long.class);
    }

    @Bean
    public RedisScript<Long> revertReplaceSessionScript() {
        return RedisScript.of(RedisConst.REVERT_REPLACE_SESSION_SCRIPT, Long.class);
    }

    @Bean
    public RedisScript<Long> deleteReplacePendingIfOwnerScript() {
        return RedisScript.of(RedisConst.DELETE_REPLACE_PENDING_IF_OWNER_SCRIPT, Long.class);
    }
}