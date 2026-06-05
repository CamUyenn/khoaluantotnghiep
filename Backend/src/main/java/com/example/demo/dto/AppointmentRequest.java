package com.example.demo.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppointmentRequest {

    @NotNull(message = "patientId là bắt buộc")
    private Long patientId;

    @NotNull(message = "doctorId là bắt buộc")
    private Long doctorId;

    @NotNull(message = "appointmentDate là bắt buộc")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;

    @NotBlank(message = "timeSlot là bắt buộc")
    private String timeSlot;
}
