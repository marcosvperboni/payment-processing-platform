package com.marcosperboni.payment.service;

import com.marcosperboni.payment.domain.exception.PaymentNotFoundException;
import com.marcosperboni.payment.domain.model.OperationType;
import com.marcosperboni.payment.domain.model.Payment;
import com.marcosperboni.payment.domain.model.PaymentMethod;
import com.marcosperboni.payment.domain.model.PaymentOperation;
import com.marcosperboni.payment.domain.model.PaymentStatus;
import com.marcosperboni.payment.messaging.kafka.PaymentAuthorizationProducer;
import com.marcosperboni.payment.messaging.kafka.PaymentAuthorizationRequested;
import com.marcosperboni.payment.repository.PaymentOperationRepository;
import com.marcosperboni.payment.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentOperationRepository paymentOperationRepository;
    private final IdempotencyService idempotencyService;
    private final PaymentAuthorizationProducer paymentAuthorizationProducer;
    private final WebhookNotifier webhookNotifier;

    public PaymentService(PaymentRepository paymentRepository,
                           PaymentOperationRepository paymentOperationRepository,
                           IdempotencyService idempotencyService,
                           PaymentAuthorizationProducer paymentAuthorizationProducer,
                           WebhookNotifier webhookNotifier) {
        this.paymentRepository = paymentRepository;
        this.paymentOperationRepository = paymentOperationRepository;
        this.idempotencyService = idempotencyService;
        this.paymentAuthorizationProducer = paymentAuthorizationProducer;
        this.webhookNotifier = webhookNotifier;
    }

    public Payment createPayment(BigDecimal amount, Currency currency, PaymentMethod method,
                                  String merchantReference, String idempotencyKey) {
        Payment existing = idempotencyService.findExistingPayment(idempotencyKey)
                .flatMap(paymentRepository::findById)
                .or(() -> paymentRepository.findByIdempotencyKey(idempotencyKey))
                .orElse(null);

        if (existing != null) {
            idempotencyService.remember(idempotencyKey, existing.getId());
            return existing;
        }

        Payment payment = Payment.create(amount, currency, method, merchantReference, idempotencyKey);
        paymentRepository.save(payment);
        recordOperation(payment, OperationType.CREATED, "Payment created");

        payment.markAuthorizationRequested();
        paymentRepository.save(payment);
        recordOperation(payment, OperationType.AUTHORIZATION_REQUESTED, "Sent to gateway for authorization");

        idempotencyService.remember(idempotencyKey, payment.getId());
        paymentAuthorizationProducer.requestAuthorization(
                new PaymentAuthorizationRequested(payment.getId(), payment.getAmount(), payment.getCurrency()));

        return payment;
    }

    public void applyAuthorizationResult(UUID paymentId, boolean approved, String gatewayReference) {
        Payment payment = getPayment(paymentId);

        if (approved) {
            payment.approve(gatewayReference);
            recordOperation(payment, OperationType.APPROVED, "Authorized by gateway " + gatewayReference);
        } else {
            payment.decline(gatewayReference);
            recordOperation(payment, OperationType.DECLINED, "Declined by gateway " + gatewayReference);
        }

        paymentRepository.save(payment);
        webhookNotifier.notifyStatusChange(payment.getId(), payment.getStatus());
    }

    public Payment capturePayment(UUID paymentId) {
        Payment payment = getPayment(paymentId);
        payment.capture();
        paymentRepository.save(payment);
        recordOperation(payment, OperationType.CAPTURED, "Payment captured and settled");
        webhookNotifier.notifyStatusChange(payment.getId(), payment.getStatus());
        return payment;
    }

    public Payment cancelPayment(UUID paymentId) {
        Payment payment = getPayment(paymentId);
        payment.cancel();
        paymentRepository.save(payment);
        recordOperation(payment, OperationType.CANCELLED, "Payment cancelled");
        webhookNotifier.notifyStatusChange(payment.getId(), payment.getStatus());
        return payment;
    }

    public Payment refundPayment(UUID paymentId) {
        Payment payment = getPayment(paymentId);
        payment.refund();
        paymentRepository.save(payment);
        recordOperation(payment, OperationType.REFUNDED, "Payment refunded");
        webhookNotifier.notifyStatusChange(payment.getId(), payment.getStatus());
        return payment;
    }

    @Transactional(readOnly = true)
    public Payment getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    @Transactional(readOnly = true)
    public Page<Payment> listPayments(PaymentStatus status, Pageable pageable) {
        return status == null
                ? paymentRepository.findAll(pageable)
                : paymentRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public List<PaymentOperation> getHistory(UUID paymentId) {
        getPayment(paymentId);
        return paymentOperationRepository.findByPaymentIdOrderByOccurredAtAsc(paymentId);
    }

    private void recordOperation(Payment payment, OperationType type, String details) {
        paymentOperationRepository.save(new PaymentOperation(payment.getId(), type, payment.getStatus(), details));
    }
}
