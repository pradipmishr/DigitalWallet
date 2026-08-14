package com.project.digitalwallet.service;

import com.project.digitalwallet.dto.TransferRequest;
import com.project.digitalwallet.entity.Wallet;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class TransferPessimisticLockTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Concurrent transfers should prevent double spending")
    void testConcurrentTransfersPreventDoubleSpend() throws InterruptedException {
        // Arrange
        Long senderUserId = 1L;   // Ensure user exists in test DB with $100 balance
        Long recipientUserId = 2L; // Recipient user phone: "9800000000"

        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1); // Ready, set, go signal
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        TransferRequest request = new TransferRequest();
        request.setRecipientPhoneNumber("9800000000");
        request.setAmount(new BigDecimal("100.00")); // Full balance
        request.setPin("1234");

        // Act
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    latch.await(); // Wait for all threads to be ready
                    walletService.transfer(senderUserId, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown(); // Fire all 10 requests at the exact same millisecond
        doneLatch.await(); // Wait for all threads to finish execution

        // Assert
        Wallet senderWallet = walletRepository.findByUserId(senderUserId).orElseThrow();

        assertEquals(1, successCount.get(), "Only 1 transfer should succeed.");
        assertEquals(9, failureCount.get(), "9 transfers should fail due to insufficient balance.");
        assertEquals(0, new BigDecimal("0.00").compareTo(senderWallet.getBalance()), "Final balance should be $0");
    }
}