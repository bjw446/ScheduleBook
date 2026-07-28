package com.example.schedulebook.domain.comment.publisher;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.domain.comment.event.CommentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentPublisher {
    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(CommentEvent event) {
        try {
            redisTemplate.convertAndSend(
                    RedisConst.COMMENT,
                    event
            );

        } catch (Exception e) {
            log.error("COMMENT Redis Pub/Sub 발행 실패", e);

            throw e;
        }
    }
}
