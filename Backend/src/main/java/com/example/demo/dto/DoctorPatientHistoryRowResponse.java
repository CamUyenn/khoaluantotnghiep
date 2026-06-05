package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DoctorPatientHistoryRowResponse {

    private Long medicalRecordId;
    private Long appointmentId;
    private LocalDateTime appointmentTime;
    private String doctorName;
    private String oldDiagnosis;
    private List<String> usedMedicines;
}