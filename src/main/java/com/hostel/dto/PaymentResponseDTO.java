package com.hostel.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponseDTO(
        Long id,
        Long studentId,
        BigDecimal amount,
        LocalDateTime paymentDate,
        String paymentMode) {
}
