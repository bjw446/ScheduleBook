package com.example.schedulebook.common.consts;

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

        if (saved ~= oldToken) then return 0
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
}
