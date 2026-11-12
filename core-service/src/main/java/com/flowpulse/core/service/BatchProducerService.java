package com.flowpulse.core.service;

import com.flowpulse.core.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchProducerService {

    private final RabbitTemplate rabbitTemplate;

    public void sendBatchForProcessing(Long batchId) {
        log.info("Publishing batch ID {} to RabbitMQ exchange '{}'...", batchId, RabbitMQConfig.PAYOUT_EXCHANGE);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYOUT_EXCHANGE,
                RabbitMQConfig.PAYOUT_ROUTING_KEY,
                batchId
        );

        log.info("Successfully published batch ID {} to queue.", batchId);
    }
}