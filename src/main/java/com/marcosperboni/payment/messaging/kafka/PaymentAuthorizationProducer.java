package com.marcosperboni.payment.messaging.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentAuthorizationProducer {

    private final KafkaTemplate<String, PaymentAuthorizationRequested> kafkaTemplate;

    public PaymentAuthorizationProducer(KafkaTemplate<String, PaymentAuthorizationRequested> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void requestAuthorization(PaymentAuthorizationRequested event) {
        UUID paymentId = event.paymentId();
        kafkaTemplate.send(KafkaTopics.AUTHORIZATION_REQUESTED, paymentId.toString(), event);
    }
}
