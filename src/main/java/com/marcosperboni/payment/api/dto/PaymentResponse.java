package com.marcosperboni.payment.api.dto;

import com.marcosperboni.payment.domain.model.Payment;
import com.marcosperboni.payment.domain.model.PaymentMethod;
import com.marcosperboni.payment.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        PaymentStatus status,
        String merchantReference,
        String gatewayReference,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getMerchantReference(),
                payment.getGatewayReference(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}
