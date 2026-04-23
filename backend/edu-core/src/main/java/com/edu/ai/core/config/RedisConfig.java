package com.edu.ai.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis wiring for the edu-core service.
 *
 * <p>Key contract (Story 1.1 BLIND-RESOURCE-001):
 * <ul>
 *   <li>{@code keySerializer} / {@code hashKeySerializer} = {@link StringRedisSerializer}</li>
 *   <li>{@code valueSerializer} / {@code hashValueSerializer} = {@link Jackson2JsonRedisSerializer}</li>
 * </ul>
 * These serializers prevent the default JDK binary serializer from polluting
 * Redis with cross-version-fragile bytes.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                                                       ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
