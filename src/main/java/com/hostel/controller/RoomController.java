package com.hostel.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hostel.dto.RoomResponseDTO;
import com.hostel.service.RoomService;

import jakarta.validation.constraints.Pattern;

@RestController
@Validated
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public ResponseEntity<List<RoomResponseDTO>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    @GetMapping("/{roomNumber}")
    public ResponseEntity<RoomResponseDTO> getRoom(
            @PathVariable @Pattern(regexp = "^[A-Z]-[1-9][0-9]{2}$", message = "Room number must match the format A-101") String roomNumber) {
        return ResponseEntity.ok(roomService.getRoom(roomNumber));
    }
}
