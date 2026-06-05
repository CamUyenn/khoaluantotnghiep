package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ReceptionistApproveRequest;
import com.example.demo.dto.ReceptionistApproveResponse;
import com.example.demo.dto.ReceptionistCancelAppointmentRequest;
import com.example.demo.dto.ReceptionistDoctorOptionResponse;
import com.example.demo.dto.ReceptionistRoomSuggestionResponse;
import com.example.demo.dto.ReceptionistRoomScheduleOptionResponse;
import com.example.demo.dto.ReceptionistWaitingStatusUpdateRequest;
import com.example.demo.entity.Appointment;
import com.example.demo.service.ReceptionistService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/receptionist")
@RequiredArgsConstructor
@Tag(name = "Receptionist", description = "Nghiệp vụ le tan: duyet lich hen, phan cong bac si, quan ly hang doi cho kham va huy lich.")
public class ReceptionistController {

    private final ReceptionistService receptionistService;

    @GetMapping("/appointments/today")
    @Operation(summary = "Lịch hẹn hôm nay")
    public List<Appointment> getTodayAppointments() {
        return receptionistService.getTodayAppointments();
    }

    @GetMapping("/appointments/from-booking")
    @Operation(summary = "Lịch hẹn từ kênh đặt lịch")
    public List<Appointment> getFromBookingList() {
        return receptionistService.getAppointmentsFromBookingList();
    }

    @GetMapping("/doctors/by-specialty")
    @Operation(summary = "Danh sách bác sĩ theo chuyen khoa")
    public List<ReceptionistDoctorOptionResponse> getDoctorsBySpecialty(
            @RequestParam(name = "specialty", required = false) String specialty) {
        return receptionistService.getDoctorsBySpecialty(specialty);
    }

    @GetMapping("/rooms/schedules")
    @Operation(summary = "Danh sách phòng theo lịch phân công")
    public List<ReceptionistRoomScheduleOptionResponse> getRoomSchedules(
            @RequestParam(name = "date", required = false) String date) {
        java.time.LocalDate scheduleDate = null;
        if (date != null && !date.isBlank()) {
            scheduleDate = java.time.LocalDate.parse(date.trim());
        }
        return receptionistService.getRoomSchedules(scheduleDate);
    }

    @GetMapping("/appointments/{appointmentId}/suggested-room")
    @Operation(summary = "Phòng khám gợi ý theo lịch hẹn")
    public ReceptionistRoomSuggestionResponse getSuggestedRoom(
            @PathVariable("appointmentId") Long appointmentId) {
        return receptionistService.getSuggestedRoom(appointmentId);
    }

    @PutMapping("/appointments/{appointmentId}/approve")
    @Operation(summary = "Duyệt lịch hẹn")
    public ReceptionistApproveResponse approveAppointment(
            @PathVariable("appointmentId") Long appointmentId,
            @Valid @RequestBody ReceptionistApproveRequest request) {
        return receptionistService.approveAppointment(
                appointmentId,
                request.getDoctorId(),
                request.getAssignedRoomId(),
                request.getSpecialty());
    }

    @PutMapping("/appointments/{appointmentId}/assign-doctor")
    @Operation(summary = "Phân công bác sĩ va chuyen hang cho")
    public ReceptionistApproveResponse assignDoctorAndMoveToWaiting(
            @PathVariable("appointmentId") Long appointmentId,
            @Valid @RequestBody ReceptionistApproveRequest request) {
        return receptionistService.assignDoctorAndMoveToWaiting(appointmentId, request.getDoctorId(),
                request.getSpecialty());
    }

    @GetMapping("/appointments/waiting")
    @Operation(summary = "Hang doi cho kham")
    public List<Appointment> getWaitingQueue(@RequestParam(name = "status", required = false) String status) {
        return receptionistService.getWaitingQueue(status);
    }

    @PutMapping("/appointments/{appointmentId}/waiting-status")
    @Operation(summary = "Cập nhật trạng thái hàng chờ")
    public Appointment updateWaitingStatus(
            @PathVariable("appointmentId") Long appointmentId,
            @Valid @RequestBody ReceptionistWaitingStatusUpdateRequest request) {
        return receptionistService.updateWaitingStatus(appointmentId, request.getStatus());
    }

    @PutMapping("/appointments/{appointmentId}/cancel")
    @Operation(summary = "Hủy lịch hẹn bởi lễ tân")
    public Appointment cancelAppointmentByReceptionist(
            @PathVariable("appointmentId") Long appointmentId,
            @RequestBody ReceptionistCancelAppointmentRequest request) {
        return receptionistService.cancelAppointmentByReceptionist(
                appointmentId,
                request == null ? null : request.getCancellationReason(),
                request == null ? null : request.getRequireReason());
    }
}