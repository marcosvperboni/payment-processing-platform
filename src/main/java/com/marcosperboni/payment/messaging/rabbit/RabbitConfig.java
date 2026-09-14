package com.marcosperboni.payment.messaging.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Webhook deliveries that fail 3 times are dead-lettered into
 * {@value RabbitTopology#WEBHOOK_DLQ} for manual inspection/replay instead
 * of being retried forever or dropped.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public DirectExchange webhookExchange() {
        return new DirectExchange(RabbitTopology.WEBHOOK_EXCHANGE);
    }

    @Bean
    public DirectExchange webhookDeadLetterExchange() {
        return new DirectExchange(RabbitTopology.WEBHOOK_DLX);
    }

    @Bean
    public Queue webhookQueue() {
        return QueueBuilder.durable(RabbitTopology.WEBHOOK_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitTopology.WEBHOOK_DLX)
                .withArgument("x-dead-letter-routing-key", RabbitTopology.WEBHOOK_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue webhookDeadLetterQueue() {
        return QueueBuilder.durable(RabbitTopology.WEBHOOK_DLQ).build();
    }

    @Bean
    public Binding webhookBinding() {
        return BindingBuilder.bind(webhookQueue())
                .to(webhookExchange())
                .with(RabbitTopology.WEBHOOK_ROUTING_KEY);
    }

    @Bean
    public Binding webhookDeadLetterBinding() {
        return BindingBuilder.bind(webhookDeadLetterQueue())
                .to(webhookDeadLetterExchange())
                .with(RabbitTopology.WEBHOOK_ROUTING_KEY);
    }
}
