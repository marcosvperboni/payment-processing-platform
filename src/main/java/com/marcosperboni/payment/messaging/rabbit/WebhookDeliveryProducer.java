package com.marcosperboni.payment.messaging.rabbit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebhookDeliveryProducer {

    private final RabbitTemplate rabbitTemplate;

    public WebhookDeliveryProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enqueue(WebhookDeliveryMessage message) {
        rabbitTemplate.convertAndSend(RabbitTopology.WEBHOOK_EXCHANGE, RabbitTopology.WEBHOOK_ROUTING_KEY, message);
    }
}
