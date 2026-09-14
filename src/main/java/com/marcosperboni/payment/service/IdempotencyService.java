package com.marcosperboni.payment.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Fast-path duplicate check backed by Redis. The database's unique
 * constraint on idempotency_key remains the source of truth for
 * correctness under concurrent requests; Redis only avoids the extra
 * round trip on the common case.
 */
@Service
public class IdempotencyService {

    private static final Duration TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "idempotency:payment:";

    private final StringRedisTemplate redisTemplate;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<UUID> findExistingPayment(String idempotencyKey) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey);
        return Optional.ofNullable(value).map(UUID::fromString);
    }

    public void remember(String idempotencyKey, UUID paymentId) {
        redisTemplate.opsForValue().set(KEY_PREFIX + idempotencyKey, paymentId.toString(), TTL);
    }
}
