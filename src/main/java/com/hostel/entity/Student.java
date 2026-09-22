package com.hostel.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Room number is required")
    private String roomNumber;

    @NotNull(message = "Admission date is required")
    private LocalDate admissionDate;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal feesPaid;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal pendingFees;

    public Student() {}

    public Student(Integer id, String name, String roomNumber, LocalDate admissionDate, BigDecimal feesPaid, BigDecimal pendingFees) {
        this.id = id;
        this.name = name;
        this.roomNumber = roomNumber;
        this.admissionDate = admissionDate;
        this.feesPaid = feesPaid;
        this.pendingFees = pendingFees;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public LocalDate getAdmissionDate() { return admissionDate; }
    public void setAdmissionDate(LocalDate admissionDate) { this.admissionDate = admissionDate; }

    public BigDecimal getFeesPaid() { return feesPaid; }
    public void setFeesPaid(BigDecimal feesPaid) { this.feesPaid = feesPaid; }

    public BigDecimal getPendingFees() { return pendingFees; }
    public void setPendingFees(BigDecimal pendingFees) { this.pendingFees = pendingFees; }
}