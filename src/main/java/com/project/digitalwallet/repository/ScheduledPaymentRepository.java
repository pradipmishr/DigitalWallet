package com.project.digitalwallet.repository;

import com.project.digitalwallet.common.enums.ScheduledPaymentStatus;
import com.project.digitalwallet.entity.ScheduledPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, Long> {

    List<ScheduledPayment> findByUserId(Long userId);

    // Finds all active payments due for execution on or before the current timestamp
    @Query("SELECT s FROM ScheduledPayment s WHERE s.status = :status AND s.nextRunAt <= :now")
    List<ScheduledPayment> findDuePayments(
            @Param("status") ScheduledPaymentStatus status,
            @Param("now") LocalDateTime now
    );
    List<ScheduledPayment> findByStatusAndNextRunAtLessThanEqual(
            ScheduledPaymentStatus status,
            LocalDateTime currentTime
    );
}