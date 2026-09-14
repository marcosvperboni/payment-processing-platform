package com.marcosperboni.payment.domain.model;

import com.marcosperboni.payment.domain.exception.InvalidPaymentStateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private static final Currency USD = Currency.getInstance("USD");

    private Payment newPayment() {
        return Payment.create(new BigDecimal("100.00"), USD, PaymentMethod.CREDIT_CARD, "order-1", "idem-1");
    }

    @Test
    void createsPaymentInCreatedStatus() {
        Payment payment = newPayment();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(payment.getAmount()).isEqualByComparingTo("100.00");
        assertThat(payment.getCurrency()).isEqualTo("USD");
        assertThat(payment.getIdempotencyKey()).isEqualTo("idem-1");
        assertThat(payment.getId()).isNotNull();
    }

    @Test
    void happyPathGoesThroughEveryExpectedStatus() {
        Payment payment = newPayment();

        payment.markAuthorizationRequested();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);

        payment.approve("GW-1");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getGatewayReference()).isEqualTo("GW-1");

        payment.capture();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SETTLED);

        payment.refund();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void declinedPaymentStaysDeclined() {
        Payment payment = newPayment();
        payment.markAuthorizationRequested();

        payment.decline("GW-2");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DECLINED);
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"CREATED", "PROCESSING", "APPROVED"})
    void canBeCancelledBeforeSettlement(PaymentStatus status) {
        Payment payment = newPayment();
        moveTo(payment, status);

        payment.cancel();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void cannotBeCancelledAfterSettlement() {
        Payment payment = newPayment();
        moveTo(payment, PaymentStatus.SETTLED);

        assertThatThrownBy(payment::cancel).isInstanceOf(InvalidPaymentStateException.class);
    }

    @Test
    void cannotCaptureBeforeApproval() {
        Payment payment = newPayment();

        assertThatThrownBy(payment::capture).isInstanceOf(InvalidPaymentStateException.class);
    }

    @Test
    void cannotCaptureTwice() {
        Payment payment = newPayment();
        moveTo(payment, PaymentStatus.APPROVED);
        payment.capture();

        assertThatThrownBy(payment::capture).isInstanceOf(InvalidPaymentStateException.class);
    }

    @Test
    void cannotRefundBeforeSettlement() {
        Payment payment = newPayment();
        moveTo(payment, PaymentStatus.APPROVED);

        assertThatThrownBy(payment::refund).isInstanceOf(InvalidPaymentStateException.class);
    }

    @Test
    void cannotApproveTwice() {
        Payment payment = newPayment();
        moveTo(payment, PaymentStatus.APPROVED);

        assertThatThrownBy(() -> payment.approve("GW-3")).isInstanceOf(InvalidPaymentStateException.class);
    }

    private void moveTo(Payment payment, PaymentStatus status) {
        if (status == PaymentStatus.CREATED) {
            return;
        }
        payment.markAuthorizationRequested();
        if (status == PaymentStatus.PROCESSING) {
            return;
        }
        payment.approve("GW-setup");
        if (status == PaymentStatus.APPROVED) {
            return;
        }
        payment.capture();
    }
}
