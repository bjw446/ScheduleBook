package com.example.schedulebook.common.redis;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.consts.RedisConst;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class RedisLoginLockService {
    private final StringRedisTemplate stringRedisTemplate;

    public int increaseFail(String loginId) {
        String key = RedisConst.LOGIN_FAIL_PREFIX + loginId;

        Long count = stringRedisTemplate.opsForValue().increment(key);

        stringRedisTemplate.expire(key, RedisConst.LOGIN_LOCK_DURATION);

        return count != null ? count.intValue() : 0;
    }

    public void lock(String loginId) {
        stringRedisTemplate.opsForValue().set(
                RedisConst.LOGIN_LOCK_PREFIX + loginId,
                "LOCK",
                RedisConst.LOGIN_LOCK_DURATION
        );
    }

    public boolean isLocked(String loginId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisConst.LOGIN_LOCK_PREFIX + loginId));
    }

    public void clear(String loginId) {
        stringRedisTemplate.delete(RedisConst.LOGIN_FAIL_PREFIX + loginId);

        stringRedisTemplate.delete(RedisConst.LOGIN_LOCK_PREFIX + loginId);
    }

    public void recordFailure(String loginId) {
        int failCount = increaseFail(loginId);

        if (failCount >= CommonConst.MAX_LOGIN_FAIL) {
            lock(loginId);
        }
    }
}
