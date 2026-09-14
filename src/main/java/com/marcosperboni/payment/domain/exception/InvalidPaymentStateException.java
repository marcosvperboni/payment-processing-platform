package com.marcosperboni.payment.domain.exception;

import com.marcosperboni.payment.domain.model.PaymentStatus;

public class InvalidPaymentStateException extends RuntimeException {

    public InvalidPaymentStateException(PaymentStatus current, String attemptedOperation) {
        super("Cannot perform '%s' while payment is in status %s".formatted(attemptedOperation, current));
    }
}
