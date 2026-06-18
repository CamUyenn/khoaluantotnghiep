package com.example.demo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReceptionistApproveRequest {

    private Long doctorId;

    private Long assignedRoomId;

    @Size(max = 100, message = "Chuyên khoa tối đa 100 ký tự")
    private String specialty;
}
