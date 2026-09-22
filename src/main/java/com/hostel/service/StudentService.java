package com.hostel.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hostel.dto.StudentRequestDTO;
import com.hostel.dto.StudentResponseDTO;
import com.hostel.entity.Student;
import com.hostel.repository.StudentRepository;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
    this.studentRepository = studentRepository;
}

    @Transactional
    public StudentResponseDTO createStudent(StudentRequestDTO dto) {
        Student student = new Student();
        student.setName(dto.getName());
        student.setRoomNumber(dto.getRoomNumber());
        student.setAdmissionDate(dto.getAdmissionDate());
        student.setFeesPaid(dto.getFeesPaid());
        student.setPendingFees(dto.getPendingFees());

        Student saved = studentRepository.save(student);
        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<StudentResponseDTO> getAllStudents() {
        return studentRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentResponseDTO getStudentById(Integer id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        return mapToDTO(student);
    }

    @Transactional
    public StudentResponseDTO updateStudent(Integer id, StudentRequestDTO dto) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));

        student.setName(dto.getName());
        student.setRoomNumber(dto.getRoomNumber());
        student.setAdmissionDate(dto.getAdmissionDate());
        student.setFeesPaid(dto.getFeesPaid());
        student.setPendingFees(dto.getPendingFees());

        Student updated = studentRepository.save(student);
        return mapToDTO(updated);
    }

    @Transactional
    public void deleteStudent(Integer id) {
        if (!studentRepository.existsById(id)) {
            throw new RuntimeException("Student not found with id: " + id);
        }
        studentRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<StudentResponseDTO> getPendingFeesReport(BigDecimal minAmount) {
        return studentRepository.findByPendingFeesGreaterThan(minAmount)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private StudentResponseDTO mapToDTO(Student student) {
        return new StudentResponseDTO(
                student.getId(),
                student.getName(),
                student.getRoomNumber(),
                student.getAdmissionDate(),
                student.getFeesPaid(),
                student.getPendingFees()
        );
    }
}