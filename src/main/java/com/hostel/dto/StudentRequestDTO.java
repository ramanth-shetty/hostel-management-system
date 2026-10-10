package com.hostel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body for POST /api/students and PUT /api/students/{id}.
 * Fees are NOT editable here. initialPayment/paymentMode are only used on create
 * (they become a PaymentRecord); later payments go through POST /api/payments.
 */
public record StudentRequestDTO(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Room number is required")
        @Pattern(regexp = "^[A-Z]-[1-9][0-9]{2}$", message = "Room number must match the format A-101")
        String roomNumber,

        @NotNull(message = "Admission date is required")
        LocalDate admissionDate,

        @DecimalMin(value = "0.00", message = "Initial payment cannot be negative")
        @Digits(integer = 10, fraction = 2, message = "Use at most 2 decimal places")
        BigDecimal initialPayment,

        @Pattern(regexp = "^(CASH|UPI|CARD|BANK_TRANSFER)$", message = "Payment mode must be CASH, UPI, CARD or BANK_TRANSFER")
        String paymentMode) {
}
