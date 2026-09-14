package com.marcosperboni.payment.api.dto;

import com.marcosperboni.payment.domain.model.WebhookEndpoint;

import java.time.Instant;
import java.util.UUID;

public record WebhookEndpointResponse(
        UUID id,
        String merchantReference,
        String url,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static WebhookEndpointResponse from(WebhookEndpoint endpoint) {
        return new WebhookEndpointResponse(endpoint.getId(), endpoint.getMerchantReference(), endpoint.getUrl(),
                endpoint.isActive(), endpoint.getCreatedAt(), endpoint.getUpdatedAt());
    }
}
