package com.marcosperboni.payment.repository;

import com.marcosperboni.payment.domain.model.PaymentOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentOperationRepository extends JpaRepository<PaymentOperation, UUID> {

    List<PaymentOperation> findByPaymentIdOrderByOccurredAtAsc(UUID paymentId);
}
