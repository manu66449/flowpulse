package com.flowpulse.core.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PayoutItemResponseDto(
        Long id,
        BigDecimal amount,
        String accountNumber,
        String status,
        String failureReason,
        OffsetDateTime processedAt
) {}