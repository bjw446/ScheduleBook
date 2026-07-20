package com.example.schedulebook.common.redis.config;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.common.redis.delegate.CommentRedisMessageDelegate;
import com.example.schedulebook.common.redis.delegate.ForceLogoutRedisMessageDelegate;
import com.example.schedulebook.common.redis.delegate.NotificationRedisMessageDelegate;
import com.example.schedulebook.common.redis.subscriber.RedisSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.TimeoutOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(3))
                .clientOptions(ClientOptions.builder()
                        .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(3)))
                        .build())
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(redisHost, redisPort), clientConfiguration
        );

        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

        redisTemplate.setConnectionFactory(connectionFactory);

        redisTemplate.setKeySerializer(new StringRedisSerializer());

        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        return redisTemplate;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter notificationListenerAdapter,
            MessageListenerAdapter commentListenerAdapter,
            MessageListenerAdapter forceLogoutListenerAdapter
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();

        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(notificationListenerAdapter, new PatternTopic(RedisConst.NOTIFICATION));

        container.addMessageListener(commentListenerAdapter, new PatternTopic(RedisConst.COMMENT));

        container.addMessageListener(forceLogoutListenerAdapter, new ChannelTopic(RedisConst.FORCE_LOGOUT_SESSION));

        return container;
    }

    @Bean
    public MessageListenerAdapter notificationListenerAdapter(
            RedisSubscriber redisSubscriber,
            ObjectMapper objectMapper
    ) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(
                new NotificationRedisMessageDelegate(redisSubscriber, objectMapper), "handleMessage"
        );

        adapter.setSerializer(new StringRedisSerializer());

        return adapter;
    }

    @Bean
    public MessageListenerAdapter commentListenerAdapter(
            RedisSubscriber redisSubscriber,
            ObjectMapper objectMapper
    ) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(
                new CommentRedisMessageDelegate(redisSubscriber, objectMapper), "handleMessage"
        );

        adapter.setSerializer(new StringRedisSerializer());

        return adapter;
    }

    @Bean
    public MessageListenerAdapter forceLogoutListenerAdapter(
            RedisSubscriber redisSubscriber,
            ObjectMapper objectMapper
    ) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(
                new ForceLogoutRedisMessageDelegate(redisSubscriber, objectMapper), "handleMessage"
        );

        adapter.setSerializer(new StringRedisSerializer());

        return adapter;
    }
}
