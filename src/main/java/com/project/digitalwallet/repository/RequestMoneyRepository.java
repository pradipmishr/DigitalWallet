package com.project.digitalwallet.repository;

import com.project.digitalwallet.common.enums.RequestMoneyStatus;
import com.project.digitalwallet.entity.RequestMoney;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RequestMoneyRepository extends JpaRepository<RequestMoney, Long> {

    List<RequestMoney> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    List<RequestMoney> findByPayerIdOrderByCreatedAtDesc(Long payerId);

    // Hard-delete expired pending requests
    @Modifying
    @Query("DELETE FROM RequestMoney r WHERE r.status = :status AND r.expiresAt <= :now")
    int deleteExpiredRequests(RequestMoneyStatus status, LocalDateTime now);
}