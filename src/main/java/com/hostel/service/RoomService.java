package com.hostel.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hostel.dto.RoomResponseDTO;
import com.hostel.entity.Room;
import com.hostel.entity.Student;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.RoomRepository;
import com.hostel.repository.StudentRepository;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;

    public RoomService(RoomRepository roomRepository, StudentRepository studentRepository) {
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<RoomResponseDTO> getAllRooms() {
        Map<String, List<String>> residentsByRoom = studentRepository.findByStatus(Student.STATUS_ACTIVE).stream()
                .collect(Collectors.groupingBy(Student::getRoomNumber,
                        Collectors.mapping(Student::getName, Collectors.toList())));
        return roomRepository.findAllByOrderByRoomNumberAsc().stream()
                .map(room -> toDTO(room, residentsByRoom.getOrDefault(room.getRoomNumber(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomResponseDTO getRoom(String roomNumber) {
        Room room = roomRepository.findById(roomNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomNumber));
        List<String> residents = studentRepository.findByRoomNumberAndStatus(roomNumber, Student.STATUS_ACTIVE)
                .stream().map(Student::getName).toList();
        return toDTO(room, residents);
    }

    private RoomResponseDTO toDTO(Room room, List<String> residents) {
        int occupancy = residents.size();
        String status = occupancy >= Room.MAX_CAPACITY ? Room.STATUS_FULL : Room.STATUS_AVAILABLE;
        return new RoomResponseDTO(room.getRoomNumber(), room.getCapacity(), occupancy, status, residents);
    }
}
