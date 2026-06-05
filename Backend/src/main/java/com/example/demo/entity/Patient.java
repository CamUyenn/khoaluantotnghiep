package com.example.demo.entity;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PATIENTS")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Họ tên
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "hometown", length = 200)
    private String hometown;

    // CCCD
    @Column(name = "national_id", unique = true, length = 20)
    private String nationalId;

    // BHYT
    @Column(name = "health_insurance_number", length = 20)
    private String healthInsuranceNumber;

    // SDT
    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "gmail", length = 100)
    private String gmail;

    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

}
