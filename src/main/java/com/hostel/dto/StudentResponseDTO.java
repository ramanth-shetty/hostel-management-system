package com.hostel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StudentResponseDTO(
        Long id,
        String name,
        String roomNumber,
        LocalDate admissionDate,
        BigDecimal feesPaid,
        BigDecimal pendingFees,
        String status) {
}
