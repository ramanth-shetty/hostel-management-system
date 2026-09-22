package com.hostel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StudentResponseDTO {

    private Integer id;
    private String name;
    private String roomNumber;
    private LocalDate admissionDate;
    private BigDecimal feesPaid;
    private BigDecimal pendingFees;

    public StudentResponseDTO() {}

    public StudentResponseDTO(Integer id, String name, String roomNumber, LocalDate admissionDate, BigDecimal feesPaid, BigDecimal pendingFees) {
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