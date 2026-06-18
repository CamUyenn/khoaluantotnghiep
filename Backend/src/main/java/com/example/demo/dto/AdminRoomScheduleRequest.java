package com.example.demo.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminRoomScheduleRequest {

    @NotNull(message = "roomId là bắt buộc")
    private Long roomId;

    @NotNull(message = "doctorId là bắt buộc")
    private Long doctorId;

    @NotNull(message = "scheduleDate là bắt buộc")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    @NotBlank(message = "timeSlot là bắt buộc")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d-([01]\\d|2[0-3]):[0-5]\\d$", message = "timeSlot phải đúng định dạng HH:mm-HH:mm")
    private String timeSlot;
}
