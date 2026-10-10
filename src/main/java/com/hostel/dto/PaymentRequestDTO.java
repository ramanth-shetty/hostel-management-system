package com.hostel.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record PaymentRequestDTO(

        @NotNull(message = "Student is required")
        @Positive(message = "Student id must be positive")
        Long studentId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @Digits(integer = 10, fraction = 2, message = "Use at most 2 decimal places")
        BigDecimal amount,

        @NotBlank(message = "Payment mode is required")
        @Pattern(regexp = "^(CASH|UPI|CARD|BANK_TRANSFER)$", message = "Payment mode must be CASH, UPI, CARD or BANK_TRANSFER")
        String paymentMode) {
}
