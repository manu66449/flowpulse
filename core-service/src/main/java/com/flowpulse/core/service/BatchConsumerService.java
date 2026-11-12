package com.flowpulse.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchConsumerService {

    private final BatchService batchService;

    @RabbitListener(queues = "payout.batch.queue")
    public void consumeBatchMessage(Long batchId) {
        log.info("Received batch ID {} from RabbitMQ queue. Starting background execution...", batchId);

        // Execute the processing logic
        batchService.processBatch(batchId);

        log.info("Finished processing batch ID {} consumed from queue.", batchId);
    }
}