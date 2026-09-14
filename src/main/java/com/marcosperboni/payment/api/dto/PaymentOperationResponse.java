package com.marcosperboni.payment.api.dto;

import com.marcosperboni.payment.domain.model.OperationType;
import com.marcosperboni.payment.domain.model.PaymentOperation;
import com.marcosperboni.payment.domain.model.PaymentStatus;

import java.time.Instant;

public record PaymentOperationResponse(
        OperationType type,
        PaymentStatus resultingStatus,
        String details,
        Instant occurredAt
) {
    public static PaymentOperationResponse from(PaymentOperation operation) {
        return new PaymentOperationResponse(
                operation.getType(), operation.getResultingStatus(), operation.getDetails(), operation.getOccurredAt());
    }
}
