package com.marcosperboni.payment.messaging.rabbit;

public final class RabbitTopology {

    public static final String WEBHOOK_EXCHANGE = "webhook.exchange";
    public static final String WEBHOOK_QUEUE = "webhook.delivery.queue";
    public static final String WEBHOOK_ROUTING_KEY = "webhook.delivery";

    public static final String WEBHOOK_DLX = "webhook.dlx";
    public static final String WEBHOOK_DLQ = "webhook.delivery.dlq";

    private RabbitTopology() {
    }
}
