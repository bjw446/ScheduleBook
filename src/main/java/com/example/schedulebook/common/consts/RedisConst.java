package com.example.schedulebook.common.consts;

import java.time.Duration;

public final class RedisConst {
    private RedisConst() {}

    public static final String NOTIFICATION = "notification";
    public static final String COMMENT = "comment";
    public static final String FORCE_LOGOUT = "auth:force-logout";
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
            local sessionKey = KEYS[1]
            
            local pendingKey = KEYS[2]
            
            local sessionId = ARGV[1]
            
            redis.call('SREM', sessionKey, sessionId)
            
            redis.call('DEL', pendingKey)
            
            return 1
            """;
    public static final String DELETE_ALL_SESSIONS_SCRIPT = """
            local sessions = redis.call('SMEMBERS', KEYS[1])
            
            for i, sessionId in ipairs(sessions) do
            
            local refreshKey = ARGV[1] .. sessionId
            
            local infoKey = ARGV[2] .. sessionId
            
            redis.call("DEL", refreshKey, infoKey)
            
            end
            
            redis.call('DEL', KEYS[1])
            
            return #sessions;
            """;
    public static final String SESSION_INFO_PREFIX = "session:info:";
    public static final Duration LAST_ACCESS_UPDATE_INTERVAL = Duration.ofSeconds(30);
    public static final String UPDATE_LAST_ACCESS_SCRIPT = """
            local key = KEYS[1]
            
            local interval = tonumber(ARGV[1])
            
            local now = tonumber(ARGV[2])
            
            local last = redis.call('HGET', key, 'lastAccessAt')
            
            if (not last) then return 0
            
            end
            
            last = tonumber(last)
            
            if (now - last < interval) then return 1
            
            end
            
            redis.call('HSET', key, 'lastAccessAt', now)
            
            return 2
            """;
    public static final String FORCE_LOGOUT_SESSION = "auth:force-logout";
    public static final String PRESENCE = "presence:user:";
    public static String getPresenceKey(Long userId) {
        return PRESENCE + userId;
    }
    public static final Duration PRESENCE_TTL = Duration.ofHours(2);
    public static final String PRESENCE_SESSION = "presence:session:";
    public static String getPresenceSessionKey(String sessionId) {
        return PRESENCE_SESSION + sessionId;
    }
    public static final String PRESENCE_COUNT_SCRIPT = """
            local key = KEYS[1]
            
            local now = tonumber(ARGV[1])
            
            redis.call("ZREMRANGEBYSCORE", key, "-inf", now)
            
            return redis.call("ZCARD", key)
            """;
    public static final String PRESENCE_SESSIONS_SCRIPT = """
            local key = KEYS[1]
            
            local now = tonumber(ARGV[1])
            
            redis.call("ZREMRANGEBYSCORE", key, "-inf", now)
            
            return redis.call("ZRANGE", key, 0, -1)
            """;
    public static final String PRESENCE_REFRESH_SCRIPT = """
            local userKey = KEYS[1]
            
            local sessionKey = KEYS[2]
            
            local sessionId = ARGV[1]
            
            local expireTime = tonumber(ARGV[2])
            
            local ttl = tonumber(ARGV[3])
            
            if redis.call("EXISTS", sessionKey) == 0 then return 0
            
            end
            
            redis.call("ZADD", userKey, expireTime, sessionId)
            
            redis.call("PEXPIRE", userKey, ttl)
            
            redis.call("PEXPIRE", sessionKey, ttl)
            
            return 1
            """;
    public static final String PRESENCE_REMOVE_SCRIPT = """
            local userKey = KEYS[1]
            
            local sessionKey = KEYS[2]
            
            local sessionId = ARGV[1]
            
            local deleted = redis.call("DEL", sessionKey)
            
            redis.call("ZREM", userKey, sessionId)
            
            return deleted
            """;
    public static final String PRESENCE_REGISTER_SCRIPT = """
            local userKey = KEYS[1]
            
            local sessionKey = KEYS[2]
            
            local sessionId = ARGV[1]
            
            local userId = ARGV[2]
            
            local expireTime = tonumber(ARGV[3])
            
            local ttl = tonumber(ARGV[4])
            
            redis.call("ZADD", userKey, expireTime, sessionId)
            
            redis.call("SET", sessionKey, userId, "PX", ttl)
            
            redis.call("PEXPIRE", userKey, ttl)
            
            return 1
            """;
    public static final String DELETE_ALL_PRESENCE_SCRIPT = """
            local userKey = KEYS[1]
            
            local sessions = redis.call("ZRANGE", userKey, 0, -1)
            
            for _, sessionId in ipairs(sessions) do
            
            redis.call("DEL", ARGV[1] .. sessionId)
            
            end
            
            redis.call("DEL", userKey)
            
            return #sessions
            """;
    public static final String ADD_SESSION_IF_AVAILABLE_SCRIPT = """
            local key = KEYS[1]
            
            local sessionId = ARGV[1]
            
            local limit = tonumber(ARGV[2])
            
            local expiration = tonumber(ARGV[3])
            
            local count = redis.call('SCARD', key)
            
            if count >= limit then return 0
            end
            
            redis.call('SADD', key, sessionId)
            
            redis.call('PEXPIRE', key, expiration)
            
            return 1
            """;
    public static final String REPLACE_SESSION_IF_AVAILABLE_SCRIPT = """
            local sessionKey = KEYS[1]

            local pendingKey = KEYS[2]

            local generationKey = KEYS[3]

            local oldSessionId = ARGV[1]

            local newSessionId = ARGV[2]

            local operationId = ARGV[3]

            local limit = tonumber(ARGV[4])

            local pendingExpiration = tonumber(ARGV[5])

            local sessionExpiration = tonumber(ARGV[6])

            if redis.call('SISMEMBER', sessionKey, oldSessionId) == 0 then return 0
        
            end

            if redis.call('EXISTS', pendingKey) == 1 then return 0
        
            end

            local count = redis.call('SCARD', sessionKey)

            if count > limit then return 0
        
            end

            local currentGeneration = redis.call('GET', generationKey)

            if currentGeneration == false then currentGeneration = "0"
        
            end

            local pendingValue = operationId .. ":" .. currentGeneration

            redis.call('SET', pendingKey, pendingValue, 'PX', pendingExpiration)

            redis.call('SREM', sessionKey, oldSessionId)

            redis.call('SADD', sessionKey, newSessionId)

            redis.call('PEXPIRE', sessionKey, sessionExpiration)

            return 1
            """;
    public static final String REVERT_REPLACE_SESSION_SCRIPT = """
            local sessionKey = KEYS[1]

            local pendingKey = KEYS[2]
            
            local generationKey = KEYS[3]

            local oldSessionId = ARGV[1]

            local newSessionId = ARGV[2]

            local operationId = ARGV[3]

            local expiration = tonumber(ARGV[4])

            local pendingValue = redis.call('GET', pendingKey)

            if pendingValue == false then return 0
        
            end
            
            local separator = string.find(pendingValue, ":")
            
            if separator == nil then redis.call('DEL', pendingKey) return 0
            
            end
            
            local pendingOperationId = string.sub(pendingValue, 1, separator - 1)
            
            local pendingGeneration = string.sub(pendingValue, separator + 1)

            if pendingOperationId ~= operationId then return 0
        
            end
            
            local currentGeneration = redis.call('GET', generationKey)
            
            if currentGeneration == false then currentGeneration = "0"
            
            end
            
            if pendingGeneration ~= currentGeneration then redis.call('DEL', pendingKey) return 0
            
            end

            redis.call('SREM', sessionKey, newSessionId)
        
            redis.call('SADD', sessionKey, oldSessionId)

            redis.call('PEXPIRE', sessionKey, expiration)

            redis.call('DEL', pendingKey)

            return 1
            """;
    public static final String SESSION_REPLACE_PENDING_PREFIX = "session:replace:pending:";
    public static final Duration SESSION_REPLACE_PENDING_EXPIRATION = Duration.ofSeconds(30);
    public static final String SESSION_GENERATION_PREFIX = "session:generation:";
    public static final String DELETE_REPLACE_PENDING_IF_OWNER_SCRIPT = """
            local pendingKey = KEYS[1]

            local operationId = ARGV[1]

            local pendingValue = redis.call('GET', pendingKey)

            if pendingValue == false then return 0
        
            end

            local separator = string.find(pendingValue, ":")

            if separator == nil then return 0
        
            end

            local pendingOperationId = string.sub(pendingValue, 1, separator - 1)

            if pendingOperationId ~= operationId then return 0
        
            end

            redis.call('DEL', pendingKey)

            return 1
            """;
}