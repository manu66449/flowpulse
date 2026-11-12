package com.flowpulse.core.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PAYOUT_EXCHANGE = "payout.exchange";
    public static final String PAYOUT_QUEUE = "payout.batch.queue";
    public static final String PAYOUT_ROUTING_KEY = "payout.batch.routing.key";

    @Bean
    public DirectExchange payoutExchange() {
        return new DirectExchange(PAYOUT_EXCHANGE);
    }

    @Bean
    public Queue payoutQueue() {
        // QueueBuilder.durable() ensures messages survive a RabbitMQ server restart!
        return QueueBuilder.durable(PAYOUT_QUEUE).build();
    }

    @Bean
    public Binding payoutBinding(Queue payoutQueue, DirectExchange payoutExchange) {
        return BindingBuilder.bind(payoutQueue).to(payoutExchange).with(PAYOUT_ROUTING_KEY);
    }
}