package com.project.digitalwallet.repository;

import com.project.digitalwallet.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {


    List<Transaction> findBySenderWalletId(Long walletId);
    Optional<Transaction> findByReferenceNumber(String referenceNumber);


    List<Transaction> findByReceiverWalletId(Long walletId);
}
