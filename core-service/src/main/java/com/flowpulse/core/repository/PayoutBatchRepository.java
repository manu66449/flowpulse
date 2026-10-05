package com.flowpulse.core.repository;

import com.flowpulse.core.domain.BatchStatus;
import com.flowpulse.core.domain.PayoutBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayoutBatchRepository extends JpaRepository<PayoutBatch, Long> {
    Page<PayoutBatch> findByStatus(BatchStatus status, Pageable pageable);
}
