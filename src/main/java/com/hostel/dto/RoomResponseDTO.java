package com.hostel.dto;

import java.util.List;

public record RoomResponseDTO(
        String roomNumber,
        int capacity,
        int currentOccupancy,
        String status,
        List<String> residents) {
}
