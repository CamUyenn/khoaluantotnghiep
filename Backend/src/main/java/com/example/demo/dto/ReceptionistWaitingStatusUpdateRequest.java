package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReceptionistWaitingStatusUpdateRequest {

    @NotBlank(message = "Trạng thái là bắt buộc")
    private String status;
}
