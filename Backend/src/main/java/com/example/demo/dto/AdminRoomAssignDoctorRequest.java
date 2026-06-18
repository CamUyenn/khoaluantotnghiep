package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminRoomAssignDoctorRequest {

    @NotNull(message = "doctorId là bắt buộc")
    private Long doctorId;
}