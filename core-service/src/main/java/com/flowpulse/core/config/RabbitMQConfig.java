package com.flowpulse.core.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

//@Configuration
//public class RabbitMQConfig {
//
//    public static final String PAYOUT_EXCHANGE = "payout.exchange";
//    public static final String PAYOUT_QUEUE = "payout.batch.queue";
//    public static final String PAYOUT_ROUTING_KEY = "payout.batch.routing.key";
//
//    @Bean
//    public DirectExchange payoutExchange() {
//        return new DirectExchange(PAYOUT_EXCHANGE);
//    }
//
//    @Bean
//    public Queue payoutQueue() {
//        // QueueBuilder.durable() ensures messages survive a RabbitMQ server restart!
//        return QueueBuilder.durable(PAYOUT_QUEUE).build();
//    }
//
//    @Bean
//    public Binding payoutBinding(Queue payoutQueue, DirectExchange payoutExchange) {
//        return BindingBuilder.bind(payoutQueue).to(payoutExchange).with(PAYOUT_ROUTING_KEY);
//    }
//}

@Configuration
public class RabbitMQConfig {

    // Main Payout Queue Constants
    public static final String PAYOUT_EXCHANGE = "payout.exchange";
    public static final String PAYOUT_QUEUE = "payout.batch.queue";
    public static final String PAYOUT_ROUTING_KEY = "payout.batch.routing.key";

    // Dead Letter Queue (DLQ) Constants
    public static final String DLX_EXCHANGE = "payout.dlx.exchange";
    public static final String DLQ_QUEUE = "payout.dlq.queue";
    public static final String DLQ_ROUTING_KEY = "payout.dlq.routing.key";

    @Bean
    public DirectExchange payoutExchange() {
        return new DirectExchange(PAYOUT_EXCHANGE);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ_ROUTING_KEY);
    }

    @Bean
    public Queue payoutQueue() {
        // Configuring the Main Queue to forward dead-lettered messages to our DLX
        return QueueBuilder.durable(PAYOUT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding payoutBinding(Queue payoutQueue, DirectExchange payoutExchange) {
        return BindingBuilder.bind(payoutQueue).to(payoutExchange).with(PAYOUT_ROUTING_KEY);
    }

    // Configures Spring Retry: 3 max attempts, starting at 2s delay with a 2.0 multiplier
    @Bean
    public RetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(2000, 2.0, 10000) // Initial: 2s, Multiplier: 2.0, Max: 10s
                .recoverer(new RejectAndDontRequeueRecoverer()) // Sends exhausted messages to DLX
                .build();
    }

    // Attaches the retry interceptor to the RabbitMQ listener container
    //keep rabbitListenerContainerFactory method name same as spring find this bean matching this name / OR/ pass new name in consumer   @RabbitListener(queues = "..", containerFactory = "myCustomFactory")
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            RetryOperationsInterceptor retryInterceptor,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAdviceChain(retryInterceptor);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }


//  Because it's binary data, RabbitMQ's log formatter prints [serialized object] instead of plaintext.
//  If an administrator looks inside the Dead Letter Queue (DLQ) in the RabbitMQ web UI, they won't see a clean number like 10; they will see raw binary bytes, which makes debugging hard.
//  In production microservices, Java native serialization is strongly discouraged because it's slow, unsafe, and language-dependent. Instead, we use JSON.
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}