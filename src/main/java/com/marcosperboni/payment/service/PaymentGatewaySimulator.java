package com.marcosperboni.payment.service;

import com.marcosperboni.payment.messaging.kafka.KafkaTopics;
import com.marcosperboni.payment.messaging.kafka.PaymentAuthorizationRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stands in for a real acquirer/processor. Any amount matching the
 * configured threshold is deterministically declined so the failure path
 * is reproducible in demos and tests; every other amount is approved.
 */
@Component
public class PaymentGatewaySimulator {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewaySimulator.class);

    private final PaymentService paymentService;
    private final long processingDelayMs;
    private final BigDecimal declineAmountThreshold;

    public PaymentGatewaySimulator(PaymentService paymentService,
                                    @Value("${payments.gateway-simulator.processing-delay-ms}") long processingDelayMs,
                                    @Value("${payments.gateway-simulator.decline-amount-threshold}") BigDecimal declineAmountThreshold) {
        this.paymentService = paymentService;
        this.processingDelayMs = processingDelayMs;
        this.declineAmountThreshold = declineAmountThreshold;
    }

    @KafkaListener(topics = KafkaTopics.AUTHORIZATION_REQUESTED)
    public void onAuthorizationRequested(PaymentAuthorizationRequested request) throws InterruptedException {
        Thread.sleep(processingDelayMs);

        boolean approved = declineAmountThreshold.compareTo(request.amount()) != 0;
        String gatewayReference = "GW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("Gateway simulator {} payment {} (amount={})",
                approved ? "approved" : "declined", request.paymentId(), request.amount());

        paymentService.applyAuthorizationResult(request.paymentId(), approved, gatewayReference);
    }
}
