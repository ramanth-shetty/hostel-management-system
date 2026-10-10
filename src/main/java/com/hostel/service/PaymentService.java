package com.hostel.service;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hostel.dto.PaymentRequestDTO;
import com.hostel.dto.PaymentResponseDTO;
import com.hostel.entity.PaymentRecord;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.PaymentRecordRepository;
import com.hostel.repository.StudentRepository;

@Service
public class PaymentService {

    private final PaymentRecordRepository paymentRecordRepository;
    private final StudentRepository studentRepository;

    public PaymentService(PaymentRecordRepository paymentRecordRepository, StudentRepository studentRepository) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public PaymentResponseDTO recordPayment(PaymentRequestDTO dto) {
        requireStudent(dto.studentId());
        PaymentRecord saved = paymentRecordRepository.save(new PaymentRecord(
                dto.studentId(),
                dto.amount().setScale(2, RoundingMode.HALF_UP),
                LocalDateTime.now(),
                dto.paymentMode()));
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getPaymentsForStudent(Long studentId) {
        requireStudent(studentId);
        return paymentRecordRepository.findByStudentIdOrderByPaymentDateDescIdDesc(studentId).stream()
                .map(this::toDTO)
                .toList();
    }

    private void requireStudent(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
    }

    private PaymentResponseDTO toDTO(PaymentRecord record) {
        return new PaymentResponseDTO(record.getId(), record.getStudentId(), record.getAmount(),
                record.getPaymentDate(), record.getPaymentMode());
    }
}
