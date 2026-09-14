package com.marcosperboni.payment.domain.model;

import com.marcosperboni.payment.domain.exception.InvalidPaymentStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Payment aggregate root. All state transitions go through the methods below
 * so invalid transitions are rejected at the domain layer, not the API layer.
 */
@Entity
@Table(name = "payments")
public class Payment {

    private static final Set<PaymentStatus> CANCELLABLE_FROM =
            EnumSet.of(PaymentStatus.CREATED, PaymentStatus.PROCESSING, PaymentStatus.APPROVED);

    protected Payment() {
    }

    @Id
    private UUID id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "merchant_reference", length = 100)
    private String merchantReference;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "gateway_reference", length = 100)
    private String gatewayReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public static Payment create(BigDecimal amount, Currency currency, PaymentMethod method,
                                  String merchantReference, String idempotencyKey) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID();
        payment.amount = amount;
        payment.currency = currency.getCurrencyCode();
        payment.method = method;
        payment.merchantReference = merchantReference;
        payment.idempotencyKey = idempotencyKey;
        payment.status = PaymentStatus.CREATED;
        Instant now = Instant.now();
        payment.createdAt = now;
        payment.updatedAt = now;
        return payment;
    }

    public void markAuthorizationRequested() {
        requireStatus(PaymentStatus.CREATED, "request authorization");
        transitionTo(PaymentStatus.PROCESSING);
    }

    public void approve(String gatewayReference) {
        requireStatus(PaymentStatus.PROCESSING, "approve");
        this.gatewayReference = gatewayReference;
        transitionTo(PaymentStatus.APPROVED);
    }

    public void decline(String gatewayReference) {
        requireStatus(PaymentStatus.PROCESSING, "decline");
        this.gatewayReference = gatewayReference;
        transitionTo(PaymentStatus.DECLINED);
    }

    public void capture() {
        requireStatus(PaymentStatus.APPROVED, "capture");
        transitionTo(PaymentStatus.SETTLED);
    }

    public void cancel() {
        if (!CANCELLABLE_FROM.contains(status)) {
            throw new InvalidPaymentStateException(status, "cancel");
        }
        transitionTo(PaymentStatus.CANCELLED);
    }

    public void refund() {
        requireStatus(PaymentStatus.SETTLED, "refund");
        transitionTo(PaymentStatus.REFUNDED);
    }

    private void requireStatus(PaymentStatus required, String operation) {
        if (this.status != required) {
            throw new InvalidPaymentStateException(this.status, operation);
        }
    }

    private void transitionTo(PaymentStatus next) {
        this.status = next;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getMerchantReference() {
        return merchantReference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
