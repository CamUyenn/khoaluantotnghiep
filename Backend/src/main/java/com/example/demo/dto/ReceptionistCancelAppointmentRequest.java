package com.example.demo.dto;

import lombok.Data;

@Data
public class ReceptionistCancelAppointmentRequest {

    private String cancellationReason;

    // Default true for systems that require receptionist to provide a reason.
    private Boolean requireReason;
}