package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AppointmentAssignDoctorRequest;
import com.example.demo.dto.AppointmentRequest;
import com.example.demo.entity.Appointment;
import com.example.demo.service.AppointmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(
    name = "Appointment",
    description = "Quản lý lịch hẹn chung: tạo lịch, xem danh sách lịch và phân công bác sĩ cho lịch chờ xử lý."
)
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    @Operation(summary = "Danh sách lịch hẹn")
    // Chức năng: xử lý lấy danh sách tất cả các lịch hẹn.
    public List<Appointment> getAll() {
        return appointmentService.getAllAppointments();
    }

    @GetMapping("/waiting-assignment")
    @Operation(summary = "Lịch hẹn chờ phân công")
    // Chức năng: xử lý lấy danh sách lịch hẹn chờ phân công.
    public List<Appointment> getWaitingAssignment() {
        return appointmentService.getWaitingAssignmentAppointments();
    }

    @PostMapping
    @Operation(summary = "Tạo lịch hẹn")
    // Chức năng: xử lý create.
    public Appointment create(@Valid @RequestBody AppointmentRequest request) {
        return appointmentService.createAppointment(request);
    }

    @PutMapping("/{appointmentId}/assign-doctor")
    @Operation(summary = "Phân công bác sĩ")
    // Chức năng: xử lý phân công bác sĩ.
    public Appointment assignDoctor(
            @PathVariable Long appointmentId,
            @Valid @RequestBody AppointmentAssignDoctorRequest request) {
        return appointmentService.assignDoctorToAppointment(appointmentId, request.getDoctorId());
    }
}

