package com.project.digitalwallet.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AdminTransactionSearchRequest {
    private String referenceNumber;
    private String senderPhoneNumber;
    private String receiverPhoneNumber;
    private LocalDate date; // Searches for transactions created on this specific day

    private int page = 0;
    private int size = 20;
}