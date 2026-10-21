package com.flowpulse.core.service;

import com.flowpulse.core.domain.BatchStatus;
import com.flowpulse.core.domain.PayoutBatch;
import com.flowpulse.core.repository.PayoutBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchRecoveryService {

    private final PayoutBatchRepository batchRepository;

    /**
     * Runs every 10 minutes (600,000 milliseconds) to check for batches
     * that have been stuck in PROCESSING for more than 30 minutes.
     */
    @Scheduled(fixedRate = 600000)
    @Transactional
    public void recoverStuckBatches() {
        log.info("Running scheduled check for stuck payout batches...");

        // Define threshold: e.g., anything processing longer than 30 minutes ago
        OffsetDateTime thresholdTime = OffsetDateTime.now().minusMinutes(30);

        // Fetch batches stuck in PROCESSING updated before the threshold
        List<PayoutBatch> stuckBatches = batchRepository.findByStatusAndUpdatedAtBefore(BatchStatus.PROCESSING, thresholdTime);

        if (stuckBatches.isEmpty()) {
            return;
        }

        log.warn("Found {} stuck payout batches! Marking them as FAILED.", stuckBatches.size());

        for (PayoutBatch batch : stuckBatches) {
            batch.setStatus(BatchStatus.FAILED);
            batchRepository.save(batch);
            log.error("Batch ID: {} marked as FAILED due to processing timeout/server crash.", batch.getId());
        }
    }
}