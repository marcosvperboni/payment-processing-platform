package com.marcosperboni.payment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit trail of everything that happened to a payment.
 */
@Entity
@Table(name = "payment_operations")
public class PaymentOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OperationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "resulting_status", nullable = false, length = 20)
    private PaymentStatus resultingStatus;

    @Column(length = 500)
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected PaymentOperation() {
    }

    public PaymentOperation(UUID paymentId, OperationType type, PaymentStatus resultingStatus, String details) {
        this.paymentId = paymentId;
        this.type = type;
        this.resultingStatus = resultingStatus;
        this.details = details;
        this.occurredAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public OperationType getType() {
        return type;
    }

    public PaymentStatus getResultingStatus() {
        return resultingStatus;
    }

    public String getDetails() {
        return details;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
