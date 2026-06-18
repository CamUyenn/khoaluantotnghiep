package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminRoomCreateRequest {

    @NotBlank(message = "Tên phòng là bắt buộc")
    @Size(max = 100, message = "Tên phòng tối đa 100 ký tự")
    private String roomName;
}