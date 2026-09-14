package com.marcosperboni.payment.messaging.rabbit;

import java.io.Serializable;
import java.util.UUID;

public record WebhookDeliveryMessage(UUID webhookEndpointId, String url, UUID paymentId, String status)
        implements Serializable {
}
