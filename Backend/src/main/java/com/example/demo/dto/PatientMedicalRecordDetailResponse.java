package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PatientMedicalRecordDetailResponse {

    private Long medicalRecordId;
    private Long appointmentId;
    private LocalDateTime appointmentTime;
    private String appointmentStatus;
    private String doctorUsername;
    private String diagnosis;
    private String doctorAdvice;
    private LocalDateTime createdAt;
    private Long invoiceId;
    private BigDecimal totalServiceFee;
    private BigDecimal totalAmount;
    private Boolean paid;
    private LocalDateTime paidAt;
    private String paymentMethod;
    private String paymentReference;
    private List<DoctorPatientServiceItemResponse> services;
    private List<PatientPrescriptionHistoryItemResponse> prescriptionItems;
}