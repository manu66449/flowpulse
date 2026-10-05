package com.flowpulse.core.repository;

import com.flowpulse.core.domain.ItemStatus;
import com.flowpulse.core.domain.PayoutItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PayoutItemRepository extends JpaRepository<PayoutItem, Long> {
    List<PayoutItem> findByBatchId(Long batchId);
    Page<PayoutItem> findByBatchIdAndStatus(Long batchId, ItemStatus status, Pageable pageable);
}
