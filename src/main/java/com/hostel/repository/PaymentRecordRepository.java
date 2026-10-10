package com.hostel.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hostel.entity.PaymentRecord;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    List<PaymentRecord> findByStudentIdOrderByPaymentDateDescIdDesc(Long studentId);

    /** Returns null when the student has no payments yet (the service converts that to zero). */
    @Query("select sum(p.amount) from PaymentRecord p where p.studentId = :studentId")
    BigDecimal sumAmountByStudentId(@Param("studentId") Long studentId);

    /** One row per student: [studentId, totalPaid]. Used by list screens to avoid one query per student. */
    @Query("select p.studentId, sum(p.amount) from PaymentRecord p group by p.studentId")
    List<Object[]> sumAmountGroupedByStudent();
}
