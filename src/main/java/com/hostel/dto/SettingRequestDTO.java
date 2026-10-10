package com.hostel.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record SettingRequestDTO(

        @NotNull(message = "Fee is required")
        @DecimalMin(value = "0.00", message = "Fee cannot be negative")
        @Digits(integer = 10, fraction = 2, message = "Use at most 2 decimal places")
        BigDecimal value) {
}
