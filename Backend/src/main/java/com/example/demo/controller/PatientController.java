package com.example.demo.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AppointmentFeeEstimateResponse;
import com.example.demo.dto.PatientAppointmentRequest;
import com.example.demo.dto.PatientMedicalRecordDetailResponse;
import com.example.demo.dto.PatientMedicalRecordHistoryItemResponse;
import com.example.demo.dto.PatientPrefillResponse;
import com.example.demo.entity.Appointment;
import com.example.demo.service.AppointmentService;
import com.example.demo.service.PatientService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
@Tag(name = "Patient", description = "Nghiệp vụ bệnh nhân: xem hồ sơ, đặt lịch, theo dõi lịch hẹn và lịch sử bệnh án.")
public class PatientController {

    private final AppointmentService appointmentService;
    private final PatientService patientService;

    @GetMapping("/profile")
    @Operation(summary = "Thông tin hồ sơ bệnh nhân")
    public PatientPrefillResponse getProfile(@RequestParam(name = "patientId", required = false) Long patientId) {
        return appointmentService.getPatientPrefill(patientId);
    }

    @PostMapping("/appointments")
    @Operation(summary = "Đặt lịch khám")
    public Appointment createAppointment(@Valid @RequestBody PatientAppointmentRequest request) {
        return appointmentService.createAppointmentForPatient(request);
    }

    @GetMapping("/appointments/estimate-fee")
    @Operation(summary = "Ước tính phí khám theo triệu chứng")
    public AppointmentFeeEstimateResponse estimateAppointmentFee(
            @RequestParam(name = "symptomIds") List<Long> symptomIds) {
        return appointmentService.estimateFeeForSymptoms(symptomIds);
    }

    @GetMapping("/appointments")
    @Operation(summary = "Danh sách lịch hẹn cua toi")
    public List<Appointment> getMyAppointments(Authentication authentication) {
        return appointmentService.getMyAppointments(authentication.getName());
    }

    @GetMapping("/medical-records")
    @Operation(summary = "Lịch sử khám bệnh và bệnh án")
    public List<PatientMedicalRecordHistoryItemResponse> getMyMedicalRecords(Authentication authentication) {
        return patientService.getMyMedicalRecordHistory(authentication.getName());
    }

    @GetMapping("/medical-records/{medicalRecordId}")
    @Operation(summary = "Chi tiết bệnh án và đơn thuốc")
    public PatientMedicalRecordDetailResponse getMyMedicalRecordDetail(
            Authentication authentication,
            @PathVariable Long medicalRecordId) {
        return patientService.getMyMedicalRecordDetail(authentication.getName(), medicalRecordId);
    }

    @PutMapping("/appointments/{appointmentId}/cancel")
    @Operation(summary = "Hủy lịch hẹn của tôi")
    public Appointment cancelMyAppointment(
            @PathVariable Long appointmentId,
            Authentication authentication) {
        return appointmentService.cancelMyAppointment(appointmentId, authentication.getName());
    }
}
