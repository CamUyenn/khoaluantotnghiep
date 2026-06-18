package com.example.demo.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DoctorPatientHistoryResponse {

    private Long patientId;
    private String patientName;
    private String message;
    private List<DoctorPatientHistoryRowResponse> histories;
}