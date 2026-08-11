package com.project.digitalwallet.repository;

import com.project.digitalwallet.common.enums.KycStatus;
import com.project.digitalwallet.entity.KycDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Repository
public interface KycDetailsRepository extends JpaRepository<KycDetails, Long> {
    Optional<KycDetails> findByUserId(Long userId);
    Page<KycDetails> findByStatus(KycStatus status, Pageable pageable);
}