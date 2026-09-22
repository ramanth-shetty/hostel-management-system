package com.hostel.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hostel.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Integer> {
    List<Student> findByPendingFeesGreaterThan(BigDecimal amount);
    List<Student> findByRoomNumberContainingIgnoreCase(String roomNumber);
}