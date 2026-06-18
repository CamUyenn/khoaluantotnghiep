package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateMedicalRecordRequest {

    @NotNull(message = "appointmentId là bắt buộc")
    private Long appointmentId;

    @NotBlank(message = "Chẩn đoán là bắt buộc")
    private String diagnosis;

    @NotBlank(message = "Lời dặn của bác sĩ là bắt buộc")
    private String doctorAdvice;
}
