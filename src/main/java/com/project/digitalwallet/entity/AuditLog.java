package com.project.digitalwallet.entity;



import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog extends BaseEntity {


    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;


    private String action;


    private String description;


    private String ipAddress;
}
