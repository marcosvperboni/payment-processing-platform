package com.marcosperboni.payment.domain.model;

public enum OperationType {
    CREATED,
    AUTHORIZATION_REQUESTED,
    APPROVED,
    DECLINED,
    CAPTURED,
    CANCELLED,
    REFUNDED,
    WEBHOOK_DELIVERED,
    WEBHOOK_FAILED
}
