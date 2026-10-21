package com.flowpulse.core.controller;

import com.flowpulse.core.domain.PayoutBatch;
import com.flowpulse.core.domain.User;
import com.flowpulse.core.repository.UserRepository;
import com.flowpulse.core.service.BatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<PayoutBatch> uploadBatch(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) { // Get standard Authentication object

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a valid CSV file.");
        }

        // Safely extract email and fetch the real database User entity
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database: " + email));

        PayoutBatch savedBatch = batchService.uploadAndProcessBatch(file, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedBatch);
    }
}