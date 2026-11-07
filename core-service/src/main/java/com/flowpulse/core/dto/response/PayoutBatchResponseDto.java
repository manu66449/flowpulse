package com.flowpulse.core.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

//RECORD (Not Class) (Feature introduced in JAVA 14)
public record PayoutBatchResponseDto(
        Long id,
        String title,
        BigDecimal totalAmount,
        Integer totalRecords,
        String status,
        OffsetDateTime createdAt,
        List<PayoutItemResponseDto> items
) {}