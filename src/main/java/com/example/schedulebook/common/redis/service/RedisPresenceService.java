package com.example.schedulebook.common.redis.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RedisPresenceService {

    void register(Long userId, String sessionId);

    void remove(Long userId, String sessionId);

    boolean isOnline(Long userId);

    int getSessionCount(Long userId);

    Long findUser(String sessionId);

    Set<String> getSessionIds(Long userId);

    void refresh(Long userId, String sessionId);

    Map<Long, Boolean> getOnlineStatuses(List<Long> userIds);
}
