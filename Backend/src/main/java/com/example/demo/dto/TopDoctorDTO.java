package com.example.demo.dto;

import lombok.Data;

@Data
public class TopDoctorDTO {

    private Long doctorId;
    private Long totalAppointments;

    public TopDoctorDTO() {
    }

    public TopDoctorDTO(Long doctorId, Long totalAppointments) {
        this.doctorId = doctorId;
        this.totalAppointments = totalAppointments;
    }
}
