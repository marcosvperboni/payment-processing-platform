package com.marcosperboni.payment.domain.exception;

import java.util.UUID;

public class WebhookEndpointNotFoundException extends RuntimeException {

    public WebhookEndpointNotFoundException(UUID id) {
        super("Webhook endpoint not found: " + id);
    }
}
