package com.example.demo.dto;

import lombok.Data;

@Data
public class PatientPrefillResponse {
    private Long patientId;
    private String fullName;
    private String gender;
    private java.time.LocalDate dateOfBirth;
    private String hometown;
    private String nationalId;
    private String phoneNumber;
    private String healthInsuranceNumber;
    private String gmail;
}
