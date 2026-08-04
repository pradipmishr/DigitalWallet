package com.project.digitalwallet.entity;

import com.project.digitalwallet.common.enums.TransactionStatus;
import com.project.digitalwallet.common.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction extends BaseEntity {


    @ManyToOne
    @JoinColumn(name = "sender_wallet_id")
    private Wallet senderWallet;


    @ManyToOne
    @JoinColumn(name = "receiver_wallet_id")
    private Wallet receiverWallet;


    private BigDecimal amount;


    @Enumerated(EnumType.STRING)
    private TransactionType type;


    @Enumerated(EnumType.STRING)
    private TransactionStatus status;


    @Column(unique = true)
    private String referenceNumber;


    private String description;
}