package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReceptionistRoomScheduleOptionResponse {
    private Long roomId;
    private String roomName;
    private Long doctorId;
    private String doctorUsername;
    private String startTime;
    private String endTime;
}
