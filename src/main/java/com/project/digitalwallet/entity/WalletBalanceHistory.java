package com.project.digitalwallet.entity;



import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalletBalanceHistory extends BaseEntity {


    @ManyToOne
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;


    private BigDecimal previousBalance;


    private BigDecimal newBalance;


    private BigDecimal changeAmount;


    private String reason;
}