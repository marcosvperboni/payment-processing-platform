package com.marcosperboni.payment.service;

import com.marcosperboni.payment.domain.exception.WebhookEndpointNotFoundException;
import com.marcosperboni.payment.domain.model.WebhookEndpoint;
import com.marcosperboni.payment.repository.WebhookEndpointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WebhookEndpointService {

    private final WebhookEndpointRepository webhookEndpointRepository;

    public WebhookEndpointService(WebhookEndpointRepository webhookEndpointRepository) {
        this.webhookEndpointRepository = webhookEndpointRepository;
    }

    public WebhookEndpoint register(String merchantReference, String url) {
        return webhookEndpointRepository.save(new WebhookEndpoint(merchantReference, url));
    }

    @Transactional(readOnly = true)
    public WebhookEndpoint get(UUID id) {
        return webhookEndpointRepository.findById(id)
                .orElseThrow(() -> new WebhookEndpointNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<WebhookEndpoint> listAll() {
        return webhookEndpointRepository.findAll();
    }

    public WebhookEndpoint updateUrl(UUID id, String url) {
        WebhookEndpoint endpoint = get(id);
        endpoint.updateUrl(url);
        return webhookEndpointRepository.save(endpoint);
    }

    public void delete(UUID id) {
        WebhookEndpoint endpoint = get(id);
        webhookEndpointRepository.delete(endpoint);
    }
}
