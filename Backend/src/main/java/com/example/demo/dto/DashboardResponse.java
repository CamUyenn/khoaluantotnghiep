package com.example.demo.dto;

import java.util.List;

import lombok.Data;

@Data
public class DashboardResponse {

    private long totalPatients;
    private long totalDoctors;
    private long totalAppointments;
    private long totalMedicalRecords;

    private long todayAppointments;

    private long pendingAppointments;
    private long confirmedAppointments;
    private long cancelledAppointments;

    private long thisMonthAppointments;
    private List<TopDoctorDTO> topDoctors;
}
