package com.flowpulse.core.service;

import com.flowpulse.core.domain.ItemStatus;
import com.flowpulse.core.domain.PayoutItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BankGatewayService {

    /**
     * Simulates calling an external bank API.
     * If a RuntimeException occurs, Spring Retry catches it, waits, and retries up to 3 times.
     */
    @Retryable(
            retryFor = { RuntimeException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public void processBankTransfer(PayoutItem item) {
        log.info("Attempting bank transfer for Payout Item ID: {} (Amount: {})", item.getId(), item.getAmount());

        // Simulate network latency / bank gateway call
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // TODO: For testing retries later, you can temporarily throw an exception here:
//         if (true) { throw new RuntimeException("Gateway timeout"); }

        // If successful:
        item.setStatus(ItemStatus.SUCCESS);
        item.setProcessedAt(java.time.OffsetDateTime.now());
        log.info("Successfully processed Payout Item ID: {}", item.getId());
    }

    /**
     * Fallback method executed automatically if all 3 retry attempts fail completely.
     */
    @Recover
    public void recoverBankTransfer(RuntimeException e, PayoutItem item) {
        log.error("All 3 retry attempts exhausted for Payout Item ID: {}. Reason: {}", item.getId(), e.getMessage());
        item.setStatus(ItemStatus.FAILED);
        item.setFailureReason("Exhausted retries: " + e.getMessage());
    }
}