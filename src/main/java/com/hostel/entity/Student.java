package com.hostel.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A resident. NOTE: feesPaid / pendingFees are NOT stored here any more.
 * They are always calculated from PaymentRecord rows + the standard monthly fee
 * (see StudentService), exactly as System.md requires.
 */
@Entity
@Table(name = "students")
public class Student {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CHECKED_OUT = "CHECKED_OUT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @NotBlank(message = "Room number is required")
    @Pattern(regexp = "^[A-Z]-[1-9][0-9]{2}$", message = "Room number must match the format A-101")
    @Column(name = "room_number", length = 5, nullable = false)
    private String roomNumber;

    @NotNull(message = "Admission date is required")
    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;

    @Column(name = "status", length = 12, nullable = false)
    private String status = STATUS_ACTIVE;

    public Student() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }

    public void setAdmissionDate(LocalDate admissionDate) {
        this.admissionDate = admissionDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
