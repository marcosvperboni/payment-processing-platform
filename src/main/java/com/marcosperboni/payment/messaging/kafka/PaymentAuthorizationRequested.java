package com.marcosperboni.payment.messaging.kafka;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentAuthorizationRequested(UUID paymentId, BigDecimal amount, String currency) {
}
