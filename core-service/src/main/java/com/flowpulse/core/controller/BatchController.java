package com.flowpulse.core.controller;

import com.flowpulse.core.domain.AuditLog;
import com.flowpulse.core.domain.User;
import com.flowpulse.core.dto.response.PayoutBatchResponseDto;
import com.flowpulse.core.repository.UserRepository;
import com.flowpulse.core.service.BatchProducerService;
import com.flowpulse.core.service.BatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;
    private final UserRepository userRepository;
    private final BatchProducerService batchProducerService;

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<PayoutBatchResponseDto> uploadBatch(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a valid CSV file.");
        }

        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database: " + email));

        PayoutBatchResponseDto savedBatch = batchService.uploadAndProcessBatch(file, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedBatch);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ACCOUNTANT')")
    public ResponseEntity<Page<PayoutBatchResponseDto>> getAllBatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PayoutBatchResponseDto> batches = batchService.getBatches(pageable);
        return ResponseEntity.ok(batches);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ACCOUNTANT')")
    public ResponseEntity<PayoutBatchResponseDto> getBatchById(@PathVariable Long id) {
        PayoutBatchResponseDto batch = batchService.getBatchResponseById(id);
        return ResponseEntity.ok(batch);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<PayoutBatchResponseDto> approveBatch(
            @PathVariable Long id,
            Authentication authentication) {

        String email = authentication.getName();
        User currentManager = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database: " + email));

        PayoutBatchResponseDto approvedBatch = batchService.approveBatch(id, currentManager);
        return ResponseEntity.ok(approvedBatch);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<PayoutBatchResponseDto> rejectBatch(
            @PathVariable Long id,
            Authentication authentication) {

        String email = authentication.getName();
        User currentManager = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database: " + email));

        PayoutBatchResponseDto rejectedBatch = batchService.rejectBatch(id, currentManager);
        return ResponseEntity.ok(rejectedBatch);
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<String> triggerBatchProcessing(@PathVariable Long id) {
//        batchService.processBatchAsync(id);
        // Publish message to RabbitMQ instead of local thread pool
        batchProducerService.sendBatchForProcessing(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("Batch processing initiated successfully in the background for ID: " + id);
    }

    @GetMapping("/{id}/audit-logs")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ACCOUNTANT')")
    public ResponseEntity<List<AuditLog>> getBatchAuditLogs(@PathVariable Long id) {
        List<AuditLog> auditLogs = batchService.getBatchAuditLogs(id);
        return ResponseEntity.ok(auditLogs);
    }
}