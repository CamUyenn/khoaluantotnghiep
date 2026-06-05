package com.example.demo.dto;

import com.example.demo.entity.Appointment;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReceptionistApproveResponse {

    private Appointment appointment;

    private String message;
}