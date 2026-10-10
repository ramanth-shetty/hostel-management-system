package com.hostel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hostel.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByStatus(String status);

    List<Student> findByRoomNumberAndStatus(String roomNumber, String status);

    long countByRoomNumberAndStatus(String roomNumber, String status);
}
