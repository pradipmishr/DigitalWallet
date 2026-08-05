package com.project.digitalwallet.entity;

import com.project.digitalwallet.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

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

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Wallet wallet;
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;  //default user

    public void assignWallet(Wallet wallet) {
        this.wallet = wallet;
        wallet.setUser(this);
    }
    @Column(nullable = true)
    private String transactionPin;
}
