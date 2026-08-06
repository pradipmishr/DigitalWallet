package com.project.digitalwallet.repository;

import com.project.digitalwallet.common.enums.WalletStatus;
import com.project.digitalwallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);
    Optional<Wallet> findByUserPhoneNumber(String phoneNumber);
    // Inside WalletRepository.java
    long countByStatus(WalletStatus status);

    @Query("SELECT SUM(w.balance) FROM Wallet w")
    Optional<BigDecimal> findTotalSystemBalance();
}
