package com.marcosperboni.payment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WebhookEndpointRequest(

        @NotBlank(message = "merchantReference is required")
        @Size(max = 100, message = "merchantReference must be at most 100 characters")
        String merchantReference,

        @NotBlank(message = "url is required")
        @Pattern(regexp = "^https?://.+", message = "url must start with http:// or https://")
        @Size(max = 2048, message = "url must be at most 2048 characters")
        String url
) {
}
