package com.example.demo.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.DoctorClinicRoomRequest;
import com.example.demo.dto.AdminMedicalServiceResponse;
import com.example.demo.dto.DoctorMedicineResponse;
import com.example.demo.dto.DoctorPatientHistoryDetailResponse;
import com.example.demo.dto.DoctorPatientHistoryResponse;
import com.example.demo.dto.DoctorResponse;
import com.example.demo.dto.PrescriptionWorkspaceResponse;
import com.example.demo.entity.Appointment;
import com.example.demo.service.AdminService;
import com.example.demo.service.DoctorService;
import com.example.demo.service.MedicalRecordService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctor", description = "Nghiệp vụ bac si: danh sach bac si, hang doi cho kham, cap nhat phong va xem lich su benh an.")
public class DoctorController {

    private final DoctorService doctorService;
    private final MedicalRecordService medicalRecordService;
    private final AdminService adminService;

    @GetMapping
    @Operation(summary = "Danh sách bác sĩ", description = "Lấy danh sách bác sĩ hiện có.")
    public List<DoctorResponse> getAllDoctors() {
        return doctorService.getAllDoctors();
    }

    @GetMapping("/me/waiting-patients")
    @Operation(summary = "Danh sách bệnh nhân đang chờ", description = "Lấy danh sách bệnh nhân đang chờ khám của bác sĩ đang đăng nhập.")
    public List<Appointment> getMyWaitingPatients(
            Authentication authentication,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return doctorService.getMyWaitingPatients(authentication.getName(), date);
    }

    @GetMapping("/me/completed-patients")
    @Operation(summary = "Danh sách bệnh nhân đã khám", description = "Lấy danh sách bệnh nhân đã hoàn tất khám của bác sĩ đang đăng nhập.")
    public List<Appointment> getMyCompletedPatients(
            Authentication authentication,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return doctorService.getMyCompletedPatients(authentication.getName(), date);
    }

    @GetMapping("/me/services")
    @Operation(summary = "Danh sách dịch vụ khám", description = "Lấy danh sách dịch vụ để bác sĩ chọn trong ca khám.")
    public List<AdminMedicalServiceResponse> getAvailableServicesForDoctor() {
        return adminService.getAllMedicalServices();
    }

    @GetMapping("/me/medicines")
    @Operation(summary = "Danh sách thuốc khám", description = "Lấy danh sách thuốc active để bác sĩ kê đơn.")
    public List<DoctorMedicineResponse> getAvailableMedicinesForDoctor() {
        return doctorService.getMyMedicines();
    }

    @PutMapping("/{doctorId}/clinic-room")
    @Operation(summary = "Cập nhật phòng kham", description = "Cập nhật thông tin phòng khám phụ trách của bác sĩ.")
    public DoctorResponse updateClinicRoom(
            @PathVariable Long doctorId,
            @Valid @RequestBody DoctorClinicRoomRequest request) {
        return doctorService.updateClinicRoom(doctorId, request.getClinicRoom());
    }

    @GetMapping("/appointments/{appointmentId}/patient-history")
    @Operation(summary = "Lịch sử bệnh án tổng quan", description = "Lấy tổng quan lịch sử bệnh án của bệnh nhân theo cuộc hẹn.")
    public DoctorPatientHistoryResponse getPatientHistorySummary(
            Authentication authentication,
            @PathVariable(name = "appointmentId") Long appointmentId) {
        return medicalRecordService.getPatientHistorySummaryForDoctor(authentication.getName(), appointmentId);
    }

    @GetMapping("/appointments/{appointmentId}/patient-history/{medicalRecordId}")
    @Operation(summary = "Lịch sử bệnh án chi tiết", description = "Lấy chi tiết một bệnh án cụ thể để bác sĩ đối chiếu khi khám.")
    public DoctorPatientHistoryDetailResponse getPatientHistoryDetail(
            Authentication authentication,
            @PathVariable(name = "appointmentId") Long appointmentId,
            @PathVariable(name = "medicalRecordId") Long medicalRecordId) {
        return medicalRecordService.getPatientHistoryDetailForDoctor(
                authentication.getName(),
                appointmentId,
                medicalRecordId);
    }

    @PostMapping("/medical-records/{medicalRecordId}/prescriptions/autopopulate")
    @Operation(summary = "Auto-populate prescriptions", description = "Gợi ý đơn thuốc dựa trên DiagnosisTemplate và tuổi bệnh nhân.")
    public PrescriptionWorkspaceResponse autoPopulatePrescriptions(
            Authentication authentication,
            @PathVariable(name = "medicalRecordId") Long medicalRecordId,
            @RequestParam(name = "diagnosisId") Long diagnosisId) {
        return medicalRecordService.autoPopulatePrescriptionsFromDiagnosis(authentication.getName(), medicalRecordId,
                diagnosisId);
    }
}
