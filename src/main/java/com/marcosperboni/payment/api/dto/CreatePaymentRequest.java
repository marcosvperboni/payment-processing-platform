package com.marcosperboni.payment.api.dto;

import com.marcosperboni.payment.domain.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePaymentRequest(

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "currency is required")
        @Pattern(regexp = "[A-Z]{3}", message = "currency must be a 3-letter ISO 4217 code")
        String currency,

        @NotNull(message = "method is required")
        PaymentMethod method,

        @Size(max = 100, message = "merchantReference must be at most 100 characters")
        String merchantReference
) {
}
