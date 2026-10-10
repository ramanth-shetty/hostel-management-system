package com.hostel.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hostel.dto.StudentRequestDTO;
import com.hostel.dto.StudentResponseDTO;
import com.hostel.entity.PaymentRecord;
import com.hostel.entity.Room;
import com.hostel.entity.Student;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.exception.RoomFullException;
import com.hostel.repository.PaymentRecordRepository;
import com.hostel.repository.RoomRepository;
import com.hostel.repository.StudentRepository;

@Service
public class StudentService {

    private static final Pattern ROOM_NUMBER_PATTERN = Pattern.compile("^[A-Z]-[1-9][0-9]{2}$");

    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final SettingService settingService;

    public StudentService(StudentRepository studentRepository, RoomRepository roomRepository,
            PaymentRecordRepository paymentRecordRepository, SettingService settingService) {
        this.studentRepository = studentRepository;
        this.roomRepository = roomRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.settingService = settingService;
    }

    // ------------------------------------------------------------------ create

    @Transactional
    public StudentResponseDTO createStudent(StudentRequestDTO dto) {
        requireValidRoomNumber(dto.roomNumber());

        Room room = findOrCreateRoomForUpdate(dto.roomNumber());
        ensureRoomHasCapacity(room);

        Student student = new Student();
        student.setName(dto.name().trim());
        student.setRoomNumber(dto.roomNumber());
        student.setAdmissionDate(dto.admissionDate());
        student.setStatus(Student.STATUS_ACTIVE);
        Student saved = studentRepository.save(student);

        // An initial payment becomes a real PaymentRecord, so "fees paid" and "pending"
        // are always derived from the same source of truth.
        BigDecimal initial = dto.initialPayment();
        if (initial != null && initial.compareTo(BigDecimal.ZERO) > 0) {
            String mode = dto.paymentMode() == null ? "CASH" : dto.paymentMode();
            paymentRecordRepository.save(new PaymentRecord(
                    saved.getId(), initial.setScale(2, RoundingMode.HALF_UP), LocalDateTime.now(), mode));
        }

        refreshRoomState(room);
        return toDTO(saved);
    }

    // -------------------------------------------------------------------- read

    @Transactional(readOnly = true)
    public List<StudentResponseDTO> getAllStudents() {
        BigDecimal fee = settingService.getStandardMonthlyFee();
        Map<Long, BigDecimal> paid = paidTotalsByStudent();
        return studentRepository.findAll(Sort.by("id")).stream()
                .map(s -> toDTO(s, fee, paid.getOrDefault(s.getId(), BigDecimal.ZERO)))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentResponseDTO getStudentById(Long id) {
        return toDTO(findStudent(id));
    }

    @Transactional(readOnly = true)
    public List<StudentResponseDTO> getPendingFeesReport(BigDecimal minAmount) {
        return getAllStudents().stream()
                .filter(s -> s.pendingFees().compareTo(minAmount) > 0)
                .toList();
    }

    // ------------------------------------------------------------------ update

    @Transactional
    public StudentResponseDTO updateStudent(Long id, StudentRequestDTO dto) {
        Student student = findStudent(id);
        requireValidRoomNumber(dto.roomNumber());

        if (!Student.STATUS_ACTIVE.equals(student.getStatus())) {
            throw new IllegalArgumentException("Checked-out residents cannot be edited");
        }

        Room previousRoom = null;
        Room destinationRoom = null;
        if (!student.getRoomNumber().equals(dto.roomNumber())) {
            destinationRoom = findOrCreateRoomForUpdate(dto.roomNumber());
            ensureRoomHasCapacity(destinationRoom);
            previousRoom = roomRepository.findForUpdate(student.getRoomNumber()).orElse(null);
            student.setRoomNumber(dto.roomNumber());
        }

        student.setName(dto.name().trim());
        student.setAdmissionDate(dto.admissionDate());
        Student updated = studentRepository.save(student);

        refreshRoomState(previousRoom);
        refreshRoomState(destinationRoom);
        return toDTO(updated);
    }

    // ---------------------------------------------------------------- checkout

    /** DELETE /api/students/{id} is a soft delete: the profile and payments stay for audit. */
    @Transactional
    public void deleteStudent(Long id) {
        checkoutStudent(id);
    }

    @Transactional
    public StudentResponseDTO checkoutStudent(Long id) {
        Student student = findStudent(id);
        if (Student.STATUS_ACTIVE.equals(student.getStatus())) {
            Room room = roomRepository.findForUpdate(student.getRoomNumber()).orElse(null);
            student.setStatus(Student.STATUS_CHECKED_OUT);
            studentRepository.save(student);
            refreshRoomState(room);
        }
        return toDTO(student);
    }

    // ----------------------------------------------------------------- helpers

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
    }

    private StudentResponseDTO toDTO(Student student) {
        return toDTO(student, settingService.getStandardMonthlyFee(), totalPaid(student.getId()));
    }

    private StudentResponseDTO toDTO(Student student, BigDecimal fee, BigDecimal paid) {
        BigDecimal pending = fee.subtract(paid).max(BigDecimal.ZERO);
        return new StudentResponseDTO(student.getId(), student.getName(), student.getRoomNumber(),
                student.getAdmissionDate(), paid, pending, student.getStatus());
    }

    private BigDecimal totalPaid(Long studentId) {
        BigDecimal total = paymentRecordRepository.sumAmountByStudentId(studentId);
        return total == null ? BigDecimal.ZERO : total;
    }

    private Map<Long, BigDecimal> paidTotalsByStudent() {
        Map<Long, BigDecimal> totals = new HashMap<>();
        for (Object[] row : paymentRecordRepository.sumAmountGroupedByStudent()) {
            totals.put(((Number) row[0]).longValue(), new BigDecimal(row[1].toString()));
        }
        return totals;
    }

    private Room findOrCreateRoomForUpdate(String roomNumber) {
        return roomRepository.findForUpdate(roomNumber)
                .orElseGet(() -> roomRepository.saveAndFlush(new Room(roomNumber)));
    }

    private void ensureRoomHasCapacity(Room room) {
        if (activeOccupancy(room.getRoomNumber()) >= Room.MAX_CAPACITY) {
            throw new RoomFullException("Room " + room.getRoomNumber() + " is full. Cannot register resident.");
        }
    }

    /** Recounts ACTIVE residents and updates the stored AVAILABLE / FULL status. */
    private void refreshRoomState(Room room) {
        if (room == null) {
            return;
        }
        int occupancy = activeOccupancy(room.getRoomNumber());
        room.setCurrentOccupancy(occupancy);
        room.setStatus(occupancy >= Room.MAX_CAPACITY ? Room.STATUS_FULL : Room.STATUS_AVAILABLE);
        roomRepository.save(room);
    }

    private int activeOccupancy(String roomNumber) {
        return (int) studentRepository.countByRoomNumberAndStatus(roomNumber, Student.STATUS_ACTIVE);
    }

    private void requireValidRoomNumber(String roomNumber) {
        if (roomNumber == null || !ROOM_NUMBER_PATTERN.matcher(roomNumber).matches()) {
            throw new IllegalArgumentException("Invalid room number format. Expected format: A-101, B-205, etc.");
        }
    }
}
