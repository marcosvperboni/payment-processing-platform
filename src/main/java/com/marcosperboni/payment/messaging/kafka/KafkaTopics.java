package com.marcosperboni.payment.messaging.kafka;

public final class KafkaTopics {

    public static final String AUTHORIZATION_REQUESTED = "payment.authorization.requested";
    public static final String AUTHORIZATION_REQUESTED_DLT = AUTHORIZATION_REQUESTED + ".DLT";

    private KafkaTopics() {
    }
}
