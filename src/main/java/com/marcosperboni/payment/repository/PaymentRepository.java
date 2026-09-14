package com.marcosperboni.payment.repository;

import com.marcosperboni.payment.domain.model.Payment;
import com.marcosperboni.payment.domain.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
}
