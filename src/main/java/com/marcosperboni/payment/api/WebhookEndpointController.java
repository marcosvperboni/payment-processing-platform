package com.marcosperboni.payment.api;

import com.marcosperboni.payment.api.dto.WebhookEndpointRequest;
import com.marcosperboni.payment.api.dto.WebhookEndpointResponse;
import com.marcosperboni.payment.api.dto.WebhookEndpointUpdateRequest;
import com.marcosperboni.payment.service.WebhookEndpointService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhook-endpoints")
@Tag(name = "Webhook Endpoints")
public class WebhookEndpointController {

    private final WebhookEndpointService webhookEndpointService;

    public WebhookEndpointController(WebhookEndpointService webhookEndpointService) {
        this.webhookEndpointService = webhookEndpointService;
    }

    @PostMapping
    public ResponseEntity<WebhookEndpointResponse> create(@Valid @RequestBody WebhookEndpointRequest request) {
        var endpoint = webhookEndpointService.register(request.merchantReference(), request.url());
        return ResponseEntity.status(HttpStatus.CREATED).body(WebhookEndpointResponse.from(endpoint));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebhookEndpointResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(WebhookEndpointResponse.from(webhookEndpointService.get(id)));
    }

    @GetMapping
    public ResponseEntity<List<WebhookEndpointResponse>> list() {
        List<WebhookEndpointResponse> endpoints = webhookEndpointService.listAll().stream()
                .map(WebhookEndpointResponse::from)
                .toList();
        return ResponseEntity.ok(endpoints);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebhookEndpointResponse> update(@PathVariable UUID id,
                                                           @Valid @RequestBody WebhookEndpointUpdateRequest request) {
        var endpoint = webhookEndpointService.updateUrl(id, request.url());
        return ResponseEntity.ok(WebhookEndpointResponse.from(endpoint));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        webhookEndpointService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
