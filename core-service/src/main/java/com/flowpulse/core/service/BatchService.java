package com.flowpulse.core.service;

import com.flowpulse.core.domain.*;
import com.flowpulse.core.dto.response.PayoutBatchResponseDto;
import com.flowpulse.core.dto.response.PayoutItemResponseDto;
import com.flowpulse.core.repository.AuditLogRepository;
import com.flowpulse.core.repository.PayoutBatchRepository;
import com.flowpulse.core.repository.PayoutItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.OffsetDateTime;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {

    private final PayoutBatchRepository batchRepository;
    private final PayoutItemRepository payoutItemRepository;
    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;
    private final BankGatewayService bankGatewayService;
    private final BatchProducerService batchProducer;

    /**
     * Centralized mapper helper to convert PayoutBatch entity to PayoutBatchResponseDto.
     * @param batch The entity to convert
     * @param includeItems Whether to include individual payout items (false for lists to optimize performance)
     */
    public PayoutBatchResponseDto mapToDto(PayoutBatch batch, boolean includeItems) {
        List<PayoutItemResponseDto> itemDtos = (!includeItems || batch.getItems() == null) ? List.of() : batch.getItems().stream()
                .map(item -> new PayoutItemResponseDto(
                        item.getId(),
                        item.getAmount(),
                        item.getAccountNumber(),
                        item.getStatus().name(),
                        item.getFailureReason(),
                        item.getProcessedAt()
                ))
                .toList();

        return new PayoutBatchResponseDto(
                batch.getId(),
                batch.getTitle(),
                batch.getTotalAmount(),
                batch.getTotalRecords(),
                batch.getStatus().name(),
                batch.getCreatedAt(),
                itemDtos
        );
    }

    @Transactional
    public PayoutBatchResponseDto uploadAndProcessBatch(MultipartFile file, User currentUser) {
        List<PayoutItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 1. Parse the file rows and compute totals
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false; // Skip CSV header row: vendor_name,account_number,ifsc_code,amount
                    continue;
                }

                String[] data = line.split(",");
                if (data.length < 4) {
                    throw new IllegalArgumentException("Invalid row format. Expected 4 columns: vendor_name, account_number, ifsc_code, amount");
                }

                BigDecimal amount = new BigDecimal(data[3].trim());
                totalAmount = totalAmount.add(amount);

                PayoutItem item = PayoutItem.builder()
                        .vendorName(data[0].trim())
                        .accountNumber(data[1].trim())
                        .ifscCode(data[2].trim())
                        .amount(amount)
                        .status(ItemStatus.PENDING)
                        .retryCount(0)
                        .build();

                items.add(item);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse payout file: " + e.getMessage(), e);
        }

        if (items.isEmpty()) {
            throw new IllegalArgumentException("The uploaded file contains no records.");
        }

        // 2. Build the PayoutBatch matching your exact entity fields and non-null constraints
        PayoutBatch batch = PayoutBatch.builder()
                .title(file.getOriginalFilename() != null ? file.getOriginalFilename() : "Payout Batch")
                .totalAmount(totalAmount)
                .totalRecords(items.size())
                .status(BatchStatus.DRAFT)
                .createdBy(currentUser)
                .build();

        // 3. Link items to batch (CascadeType.ALL will save them automatically)
        for (PayoutItem item : items) {
            item.setBatch(batch);
        }
        batch.getItems().addAll(items);

        // 4. Save parent batch (and cascading child items) to PostgreSQL
        PayoutBatch savedBatch = batchRepository.save(batch);
        auditService.logAction("PayoutBatch", savedBatch.getId(), "UPLOAD", currentUser, null, "{\"status\": \"DRAFT\"}");
//        auditService.logAction("PayoutBatch", savedBatch.getId(), "UPLOAD", currentUser, null, "Batch created with status DRAFT");

        return mapToDto(savedBatch, true);
    }

    @Transactional(readOnly = true)
    public Page<PayoutBatchResponseDto> getBatches(Pageable pageable) {
        Page<PayoutBatch> batchPage = batchRepository.findAll(pageable);
        // Map page of entities to page of DTOs (omitting items list for list views to prevent N+1 queries)
        return batchPage.map(batch -> mapToDto(batch, false));
    }

    public PayoutBatch getBatchById(Long id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payout batch not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public PayoutBatchResponseDto getBatchResponseById(Long id) {
        PayoutBatch batch = getBatchById(id);
        return mapToDto(batch, true); // Include items detail for single view
    }

    @Transactional
    public PayoutBatchResponseDto approveBatch(Long batchId, User currentManager) {
        PayoutBatch batch = getBatchById(batchId);
        String oldStatus = batch.getStatus().name();

        // Ensure the batch is in a state that can be approved
        if (batch.getStatus() != BatchStatus.DRAFT && batch.getStatus() != BatchStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Batch cannot be approved because its current status is: " + batch.getStatus());
        }

        batch.setStatus(BatchStatus.APPROVED);
        batch.setApprovedBy(currentManager);
        batch.setApprovedAt(OffsetDateTime.now());

        PayoutBatch updatedBatch = batchRepository.save(batch);
        auditService.logAction("PayoutBatch", batchId, "APPROVE", currentManager, "{\"status\": \"" + oldStatus + "\"}", "{\"status\": \"APPROVED\"}");
//        auditService.logAction("PayoutBatch", batchId, "APPROVE", currentManager, "Status: " + oldStatus, "Status: APPROVED");
        return mapToDto(updatedBatch, true);
    }

    @Transactional
    public PayoutBatchResponseDto rejectBatch(Long batchId, User currentManager) {
        PayoutBatch batch = getBatchById(batchId);
        String oldStatus = batch.getStatus().name();

        // Ensure the batch is in a state that can be rejected
        if (batch.getStatus() != BatchStatus.DRAFT && batch.getStatus() != BatchStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Batch cannot be rejected because its current status is: " + batch.getStatus());
        }

        batch.setStatus(BatchStatus.REJECTED);
        // Note: updatedAt will be automatically refreshed by your entity's @PreUpdate lifecycle hook

        PayoutBatch updatedBatch = batchRepository.save(batch);
        auditService.logAction("PayoutBatch", batchId, "REJECT", currentManager, "{\"status\": \"" + oldStatus + "\"}", "{\"status\": \"REJECTED\"}");

        return mapToDto(updatedBatch, true);
    }

    // this method performs the guard check before talking to RabbitMQ
    @Transactional
    public void initiateProcessing(Long batchId) {
        PayoutBatch batch = getBatchById(batchId);

        // Fail fast: Validate status before publishing to RabbitMQ
        if (batch.getStatus() != BatchStatus.APPROVED) {
            throw new IllegalStateException("Batch ID " + batchId + " cannot be processed because its current status is: " + batch.getStatus());
        }

        // Status is valid, now publish to RabbitMQ
        batchProducer.sendBatchForProcessing(batchId);
    }

//    @Async("payoutTaskExecutor")  : Now instead of manually handling batchProcessing using jvm threadpool, we are handling this to RabbitMQ
//    public void processBatchAsync(Long batchId) {
    @Transactional
    public void processBatch(Long batchId) {
        log.info("Starting background payout processing for batch ID: {} on thread: {}", batchId, Thread.currentThread().getName());

        PayoutBatch batch = getBatchById(batchId);

//        // 1. Verify batch is APPROVED before processing    -----already getting handled in initiateProcessing()
//        if (batch.getStatus() != BatchStatus.APPROVED) {
//            log.error("Batch ID {} cannot be processed because its status is {}", batchId, batch.getStatus());
//            return;
//        }

        // 2. Transition batch status to PROCESSING
        batch.setStatus(BatchStatus.PROCESSING);
        batchRepository.save(batch);

        boolean hasFailures = false;

        // 3. Iterate through each item and process via BankGatewayService (with built-in retry)
        for (PayoutItem item : batch.getItems()) {
            try {
                item.setStatus(ItemStatus.PROCESSING);

                // Delegate to BankGatewayService so Spring Retry proxy intercepts it
                bankGatewayService.processBankTransfer(item);

                if (item.getStatus() == ItemStatus.FAILED) {
                    hasFailures = true;
                }
                payoutItemRepository.save(item);

            } catch (Exception e) {
                log.error("Unexpected error processing payout item {}: {}", item.getId(), e.getMessage());
                item.setStatus(ItemStatus.FAILED);
                item.setFailureReason(e.getMessage());
                hasFailures = true;
                payoutItemRepository.save(item);
            }
        }

        // 4. Finalize batch status based on item results
        if (hasFailures) {
            batch.setStatus(BatchStatus.FAILED);
        } else {
            batch.setStatus(BatchStatus.COMPLETED);
        }

        batchRepository.save(batch);
        log.info("Finished processing batch ID {}. Final Status: {}", batchId, batch.getStatus());
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getBatchAuditLogs(Long batchId) {
        // Verify the batch exists first (throws exception if ID is invalid)
        getBatchById(batchId);

        // Fetch audit logs matching entity name "PayoutBatch" and the specific ID
        return auditLogRepository.findByEntityNameAndEntityIdOrderByTimestampDesc("PayoutBatch", batchId);
    }
}