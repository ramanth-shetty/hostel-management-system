package com.hostel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "rooms")
public class Room {

    public static final int MAX_CAPACITY = 4;
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_FULL = "FULL";

    @Id
    @NotBlank
    @Pattern(regexp = "^[A-Z]-[1-9][0-9]{2}$")
    @Column(name = "room_number", length = 5, unique = true, nullable = false, updatable = false)
    private String roomNumber;

    @Column(name = "capacity", nullable = false, updatable = false)
    private int capacity = MAX_CAPACITY;

    @Column(name = "status", length = 9, nullable = false)
    private String status = STATUS_AVAILABLE;

    /** Not stored: filled in by the service from the count of ACTIVE residents. */
    @Transient
    private int currentOccupancy;

    public Room() {
    }

    public Room(String roomNumber) {
        this.roomNumber = roomNumber;
        this.capacity = MAX_CAPACITY;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCurrentOccupancy() {
        return currentOccupancy;
    }

    public void setCurrentOccupancy(int currentOccupancy) {
        this.currentOccupancy = Math.max(0, Math.min(currentOccupancy, MAX_CAPACITY));
    }
}
