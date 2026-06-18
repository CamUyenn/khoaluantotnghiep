package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReceptionistDoctorOptionResponse {

    private Long doctorId;
    private String doctorUsername;
    private Long roomId;
    private String specialty;
    private String roomName;
}