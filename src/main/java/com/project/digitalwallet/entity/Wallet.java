package com.project.digitalwallet.entity;

import com.project.digitalwallet.common.enums.WalletStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Wallet extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;


    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;


    @Enumerated(EnumType.STRING)
    private WalletStatus status;
}
