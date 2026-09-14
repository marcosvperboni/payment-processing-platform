package com.marcosperboni.payment.service;

import com.marcosperboni.payment.domain.exception.InvalidPaymentStateException;
import com.marcosperboni.payment.domain.exception.PaymentNotFoundException;
import com.marcosperboni.payment.domain.model.Payment;
import com.marcosperboni.payment.domain.model.PaymentMethod;
import com.marcosperboni.payment.domain.model.PaymentStatus;
import com.marcosperboni.payment.messaging.kafka.PaymentAuthorizationProducer;
import com.marcosperboni.payment.messaging.kafka.PaymentAuthorizationRequested;
import com.marcosperboni.payment.repository.PaymentOperationRepository;
import com.marcosperboni.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentOperationRepository paymentOperationRepository;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private PaymentAuthorizationProducer paymentAuthorizationProducer;
    @Mock
    private WebhookNotifier webhookNotifier;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, paymentOperationRepository,
                idempotencyService, paymentAuthorizationProducer, webhookNotifier);
    }

    @Test
    void createPaymentPersistsAndRequestsAuthorization() {
        when(idempotencyService.findExistingPayment("idem-1")).thenReturn(Optional.empty());
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());

        Payment payment = paymentService.createPayment(new BigDecimal("50.00"), Currency.getInstance("USD"),
                PaymentMethod.PIX, "order-1", "idem-1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        verify(paymentRepository, org.mockito.Mockito.times(2)).save(payment);
        verify(idempotencyService).remember("idem-1", payment.getId());

        ArgumentCaptor<PaymentAuthorizationRequested> captor =
                ArgumentCaptor.forClass(PaymentAuthorizationRequested.class);
        verify(paymentAuthorizationProducer).requestAuthorization(captor.capture());
        assertThat(captor.getValue().paymentId()).isEqualTo(payment.getId());
    }

    @Test
    void createPaymentIsIdempotentOnRepeatedKey() {
        Payment existing = Payment.create(new BigDecimal("50.00"), Currency.getInstance("USD"),
                PaymentMethod.PIX, "order-1", "idem-1");
        when(idempotencyService.findExistingPayment("idem-1")).thenReturn(Optional.of(existing.getId()));
        when(paymentRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        Payment result = paymentService.createPayment(new BigDecimal("50.00"), Currency.getInstance("USD"),
                PaymentMethod.PIX, "order-1", "idem-1");

        assertThat(result).isSameAs(existing);
        verify(paymentRepository, never()).save(any());
        verify(paymentAuthorizationProducer, never()).requestAuthorization(any());
    }

    @Test
    void captureRequiresApprovedStatus() {
        Payment payment = Payment.create(new BigDecimal("50.00"), Currency.getInstance("USD"),
                PaymentMethod.PIX, "order-1", "idem-1");
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.capturePayment(payment.getId()))
                .isInstanceOf(InvalidPaymentStateException.class);
    }

    @Test
    void applyingResultToUnknownPaymentThrows() {
        UUID randomId = UUID.randomUUID();
        when(paymentRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.applyAuthorizationResult(randomId, true, "GW-1"))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void capturingNotifiesWebhooks() {
        Payment payment = Payment.create(new BigDecimal("50.00"), Currency.getInstance("USD"),
                PaymentMethod.PIX, "order-1", "idem-1");
        payment.markAuthorizationRequested();
        payment.approve("GW-1");
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        paymentService.capturePayment(payment.getId());

        verify(webhookNotifier).notifyStatusChange(payment.getId(), PaymentStatus.SETTLED);
    }
}
