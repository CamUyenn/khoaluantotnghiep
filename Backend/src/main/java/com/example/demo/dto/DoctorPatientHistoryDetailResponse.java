package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DoctorPatientHistoryDetailResponse {

    private Long medicalRecordId;
    private Long appointmentId;
    private LocalDateTime appointmentTime;
    private String doctorName;
    private String diagnosis;
    private String doctorAdvice;
    private String previousSymptoms;
    private List<DoctorPatientPrescriptionItemResponse> prescriptions;
    private List<DoctorPatientServiceItemResponse> services;
}