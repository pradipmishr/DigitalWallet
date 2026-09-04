package com.project.digitalwallet.entity;

import com.project.digitalwallet.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    private String firstName;

    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    private String phoneNumber;

   @Column(nullable = false)
    private String password;

    private LocalDate dateOfBirth;

    @OneToOne
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;  //default user

    public void assignWallet(Wallet wallet) {
        this.wallet = wallet;
        wallet.setUser(this);
    }
    @Column(nullable = true)
    private String transactionPin;

    @OneToOne(
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JoinColumn(name = "address_id")
    private Address address;

    @OneToOne
    @JoinColumn(name = "kyc_id")
    private KycDetails kyc;
}
