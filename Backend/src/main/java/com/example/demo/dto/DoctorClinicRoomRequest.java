package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DoctorClinicRoomRequest {

    @NotBlank(message = "Phòng khám là bắt buộc")
    private String clinicRoom;
}
