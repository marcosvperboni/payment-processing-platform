package com.marcosperboni.payment.messaging.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic authorizationRequestedTopic() {
        return TopicBuilder.name(KafkaTopics.AUTHORIZATION_REQUESTED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic authorizationRequestedDeadLetterTopic() {
        return TopicBuilder.name(KafkaTopics.AUTHORIZATION_REQUESTED_DLT)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
