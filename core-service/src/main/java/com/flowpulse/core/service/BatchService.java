package com.flowpulse.core.service;

import com.flowpulse.core.domain.*;
import com.flowpulse.core.repository.PayoutBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.OffsetDateTime;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final PayoutBatchRepository batchRepository;

    @Transactional
    public PayoutBatch uploadAndProcessBatch(MultipartFile file, User currentUser) {
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
        return batchRepository.save(batch);
    }


    public Page<PayoutBatch> getBatches(Pageable pageable) {
        return batchRepository.findAll(pageable);
    }

    public PayoutBatch getBatchById(Long id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payout batch not found with ID: " + id));
    }


    @Transactional
    public PayoutBatch approveBatch(Long batchId, User currentManager) {
        PayoutBatch batch = getBatchById(batchId);

        // Ensure the batch is in a state that can be approved
        if (batch.getStatus() != BatchStatus.DRAFT && batch.getStatus() != BatchStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Batch cannot be approved because its current status is: " + batch.getStatus());
        }

        batch.setStatus(BatchStatus.APPROVED);
        batch.setApprovedBy(currentManager);
        batch.setApprovedAt(OffsetDateTime.now());

        return batchRepository.save(batch);
    }

    @Transactional
    public PayoutBatch rejectBatch(Long batchId, User currentManager) {
        PayoutBatch batch = getBatchById(batchId);

        // Ensure the batch is in a state that can be rejected
        if (batch.getStatus() != BatchStatus.DRAFT && batch.getStatus() != BatchStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Batch cannot be rejected because its current status is: " + batch.getStatus());
        }

        batch.setStatus(BatchStatus.REJECTED);
        // Note: updatedAt will be automatically refreshed by your entity's @PreUpdate lifecycle hook

        return batchRepository.save(batch);
    }
}