package com.marcosperboni.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService(redisTemplate);
    }

    @Test
    void returnsEmptyWhenKeyNotSeenBefore() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotency:payment:unknown-key")).thenReturn(null);

        Optional<UUID> result = idempotencyService.findExistingPayment("unknown-key");

        assertThat(result).isEmpty();
    }

    @Test
    void returnsCachedPaymentIdWhenKeySeenBefore() {
        UUID paymentId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotency:payment:known-key")).thenReturn(paymentId.toString());

        Optional<UUID> result = idempotencyService.findExistingPayment("known-key");

        assertThat(result).contains(paymentId);
    }

    @Test
    void remembersKeyWithTtl() {
        UUID paymentId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        idempotencyService.remember("some-key", paymentId);

        org.mockito.Mockito.verify(valueOperations)
                .set(eq("idempotency:payment:some-key"), eq(paymentId.toString()), any(Duration.class));
    }
}
