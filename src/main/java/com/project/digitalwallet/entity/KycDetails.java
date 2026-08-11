package com.project.digitalwallet.entity;

import com.project.digitalwallet.common.enums.DocumentType;
import com.project.digitalwallet.common.enums.KycStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "kyc_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycDetails extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column(nullable = false)
    private String documentNumber;

    @Column(nullable = false)
    private LocalDate issueDate;

    private LocalDate dateOfBirth;

    @Column(name = "front_image_path", nullable = false)
    private String frontImagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus status;

    private String adminRemarks;

    @Column(name = "verified_at")
    private LocalDate verifiedAt;
}