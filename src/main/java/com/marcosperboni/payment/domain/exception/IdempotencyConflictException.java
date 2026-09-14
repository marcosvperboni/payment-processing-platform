package com.marcosperboni.payment.domain.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency key '%s' was already used with a different request payload".formatted(idempotencyKey));
    }
}
