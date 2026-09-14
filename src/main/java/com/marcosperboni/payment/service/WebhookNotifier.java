package com.marcosperboni.payment.service;

import com.marcosperboni.payment.domain.model.PaymentStatus;
import com.marcosperboni.payment.messaging.rabbit.WebhookDeliveryMessage;
import com.marcosperboni.payment.messaging.rabbit.WebhookDeliveryProducer;
import com.marcosperboni.payment.repository.WebhookEndpointRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WebhookNotifier {

    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookDeliveryProducer webhookDeliveryProducer;

    public WebhookNotifier(WebhookEndpointRepository webhookEndpointRepository,
                            WebhookDeliveryProducer webhookDeliveryProducer) {
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.webhookDeliveryProducer = webhookDeliveryProducer;
    }

    public void notifyStatusChange(UUID paymentId, PaymentStatus status) {
        webhookEndpointRepository.findByActiveTrue().forEach(endpoint ->
                webhookDeliveryProducer.enqueue(new WebhookDeliveryMessage(
                        endpoint.getId(), endpoint.getUrl(), paymentId, status.name())));
    }
}
