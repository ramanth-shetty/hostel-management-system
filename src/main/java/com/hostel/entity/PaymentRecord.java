package com.hostel.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "payment_records", indexes = @Index(name = "idx_payment_student", columnList = "student_id"))
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(name = "amount_paid", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @NotNull
    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @NotNull
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @NotBlank
    @Column(name = "payment_mode", length = 32, nullable = false)
    private String paymentMode;

    public PaymentRecord() {
    }

    public PaymentRecord(Long studentId, BigDecimal amount, LocalDateTime paymentDate, String paymentMode) {
        this.studentId = studentId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMode = paymentMode;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getPaymentMode() {
        return paymentMode;
    }
}
