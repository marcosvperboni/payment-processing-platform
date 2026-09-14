package com.marcosperboni.payment.messaging.rabbit;

import com.marcosperboni.payment.domain.model.OperationType;
import com.marcosperboni.payment.domain.model.PaymentOperation;
import com.marcosperboni.payment.domain.model.PaymentStatus;
import com.marcosperboni.payment.repository.PaymentOperationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class WebhookDeliveryConsumer {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryConsumer.class);

    private final RestClient restClient;
    private final PaymentOperationRepository paymentOperationRepository;

    public WebhookDeliveryConsumer(PaymentOperationRepository paymentOperationRepository) {
        this.paymentOperationRepository = paymentOperationRepository;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @RabbitListener(queues = RabbitTopology.WEBHOOK_QUEUE)
    public void handle(WebhookDeliveryMessage message) {
        try {
            restClient.post()
                    .uri(message.url())
                    .body(message)
                    .retrieve()
                    .toBodilessEntity();

            paymentOperationRepository.save(new PaymentOperation(message.paymentId(),
                    OperationType.WEBHOOK_DELIVERED, PaymentStatus.valueOf(message.status()),
                    "Delivered to " + message.url()));
        } catch (Exception ex) {
            log.warn("Webhook delivery failed for payment {} to {}: {}",
                    message.paymentId(), message.url(), ex.getMessage());
            throw ex;
        }
    }
}
