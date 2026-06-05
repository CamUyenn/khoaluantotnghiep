package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminRoomResponse {

    private Long id;
    private String roomName;
    private Long currentDoctorId;
    private String currentDoctorUsername;
}