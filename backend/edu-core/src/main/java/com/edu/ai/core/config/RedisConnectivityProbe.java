package com.edu.ai.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Startup probe that pings Redis and downgrades gracefully: if Redis is
 * unreachable, a WARN is logged but the application context still starts
 * (Story 1.1 T3 / BLIND-ERROR-002). Cache-dependent code paths will see
 * Redis errors at call time and must implement their own fallback.
 */
@Component
public class RedisConnectivityProbe {

    private static final Logger log = LoggerFactory.getLogger(RedisConnectivityProbe.class);

    private final RedisConnectionFactory connectionFactory;

    public RedisConnectivityProbe(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void pingRedis() {
        try {
            String reply = connectionFactory.getConnection().ping();
            log.info("Redis ping: {}", reply);
        } catch (Exception ex) {
            log.warn("Redis unreachable at startup — degraded mode engaged: {}", ex.getMessage());
        }
    }
}
