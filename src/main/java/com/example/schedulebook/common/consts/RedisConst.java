package com.example.schedulebook.common.consts;

import java.time.Duration;

public final class RedisConst {
    private RedisConst() {}

    public static final String NOTIFICATION = "notification";
    public static final String COMMENT = "comment";
    public static final String REFRESH_PREFIX = "refresh:";
    public static final String BLACKLIST_PREFIX = "blacklist:";
    public static final String REFRESH_ROTATE_SCRIPT = """
        local key = KEYS[1]

        local oldToken = ARGV[1]
        local newToken = ARGV[2]
        local expiration = tonumber(ARGV[3])

        local saved = redis.call('GET', key)

        if (not saved) then return 0
        end

        if (saved ~= oldToken) then return 2
        end

        redis.call('SET', key, newToken, 'PX', expiration)

        return 1
        """;
    public static final String RATE_LIMIT_SCRIPT = """
            local key = KEYS[1]
            
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            local member = ARGV[4]
            
            redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
            
            local count = redis.call('ZCARD', key)
            
            if count >= limit then return 0
            
            end
            
            redis.call('ZADD', key, now, member)
            
            redis.call('PEXPIRE', key, window)
            
            return 1
            """;
    public static final String LOGIN_IP_PREFIX = "rate:login:ip:";
    public static final String LOGIN_ID_PREFIX = "rate:login:id:";
    public static final int MAX_BODY_SIZE = 1024 * 8;
    public static final int BUFFER_SIZE = 4096;
    public static final String LOGIN_FAIL_PREFIX = "login:fail:";
    public static final String LOGIN_LOCK_PREFIX = "login:lock:";
    public static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(30);
    public static final String USER_SESSION_PREFIX = "user:session:";

    public static final String REMOVE_SESSION_SCRIPT = """
            redis.call('SREM', KEYS[1], ARGV[1])
            
            local size = redis.call('SCARD', KEYS[1])
            
            if size == 0 then redis.call('DEL', KEYS[1])
            
            end
            
            return size
            """;
    public static final String DELETE_ALL_SESSIONS_SCRIPT = """
            local sessions = redis.call('SMEMBERS', KEYS[1])
            
            for i, sessionId in ipairs(sessions) do
            
            local refreshKey = ARGV[1] .. sessionId
            
            redis.call('DEL', refreshKey)
            
            end
            
            redis.call('DEL', KEYS[1])
            
            return #sessions;
            """;
}
