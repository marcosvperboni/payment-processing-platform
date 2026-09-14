package com.marcosperboni.payment.messaging.rabbit;

import com.marcosperboni.payment.domain.model.OperationType;
import com.marcosperboni.payment.domain.model.PaymentOperation;
import com.marcosperboni.payment.domain.model.PaymentStatus;
import com.marcosperboni.payment.repository.PaymentOperationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Records permanently failed webhook deliveries so they show up in the
 * payment history and can be replayed manually later.
 */
@Component
public class WebhookDeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeadLetterConsumer.class);

    private final PaymentOperationRepository paymentOperationRepository;

    public WebhookDeadLetterConsumer(PaymentOperationRepository paymentOperationRepository) {
        this.paymentOperationRepository = paymentOperationRepository;
    }

    @RabbitListener(queues = RabbitTopology.WEBHOOK_DLQ)
    public void handle(WebhookDeliveryMessage message) {
        log.error("Webhook delivery exhausted retries for payment {} to {}", message.paymentId(), message.url());
        paymentOperationRepository.save(new PaymentOperation(message.paymentId(),
                OperationType.WEBHOOK_FAILED, PaymentStatus.valueOf(message.status()),
                "Delivery to " + message.url() + " failed after retries"));
    }
}
