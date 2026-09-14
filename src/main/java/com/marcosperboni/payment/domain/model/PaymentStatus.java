package com.marcosperboni.payment.domain.model;

public enum PaymentStatus {
    CREATED,
    PROCESSING,
    APPROVED,
    DECLINED,
    SETTLED,
    REFUNDED,
    CANCELLED
}
