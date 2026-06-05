package com.example.demo.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AddPrescriptionDetailRequest;
import com.example.demo.dto.CreateMedicalRecordRequest;
import com.example.demo.dto.PrescriptionAutosaveResponse;
import com.example.demo.dto.PrescriptionMedicineCatalogResponse;
import com.example.demo.dto.PrescriptionWorkspaceResponse;
import com.example.demo.dto.QuickAddPrescriptionMedicineRequest;
import com.example.demo.dto.UpdatePrescriptionDetailRequest;
import com.example.demo.dto.UpsertMedicalRecordServiceResultRequest;
import com.example.demo.entity.MedicalRecord;
import com.example.demo.entity.MedicalRecordServiceDetail;
import com.example.demo.entity.PrescriptionDetail;
import com.example.demo.service.MedicalRecordService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
@Tag(name = "MedicalRecord", description = "Nghiệp vụ benh an: tao benh an, don thuoc, ket qua dich vu can lam sang va hoan tat ho so kham.")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @GetMapping
    @Operation(summary = "Danh sách bệnh án")
    public List<MedicalRecord> getAll() {
        return medicalRecordService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết bệnh án")
    public MedicalRecord getById(@PathVariable("id") Long id) {
        return medicalRecordService.getById(id);
    }

    @GetMapping("/appointment/{appointmentId}")
    @Operation(summary = "Lấy bệnh án theo lịch hẹn")
    public MedicalRecord getByAppointment(@PathVariable("appointmentId") Long appointmentId) {
        return medicalRecordService.getByAppointmentId(appointmentId);
    }

    @PostMapping
    @Operation(summary = "Tạo bệnh án")
    public MedicalRecord create(
            @RequestParam(name = "appointmentId") Long appointmentId,
            @RequestBody MedicalRecord request) {
        return medicalRecordService.create(appointmentId, request);
    }

    @PostMapping("/doctor")
    @Operation(summary = "Bác sĩ tạo bệnh án")
    public MedicalRecord createByDoctor(
            Authentication authentication,
            @Valid @RequestBody CreateMedicalRecordRequest request) {
        return medicalRecordService.createByDoctor(authentication.getName(), request);
    }

    @PostMapping("/{medicalRecordId}/prescription-details")
    @Operation(summary = "Thêm thuốc vào đơn")
    public PrescriptionDetail addPrescriptionDetail(
            Authentication authentication,
            @PathVariable("medicalRecordId") Long medicalRecordId,
            @Valid @RequestBody AddPrescriptionDetailRequest request) {
        return medicalRecordService.addMedicineToCurrentMedicalRecord(
                authentication.getName(),
                medicalRecordId,
                request);
    }

    @GetMapping("/{medicalRecordId}/prescription-details")
    @Operation(summary = "Danh sách thuốc trong don")
    public List<PrescriptionDetail> getPrescriptionDetails(
            Authentication authentication,
            @PathVariable("medicalRecordId") Long medicalRecordId) {
        return medicalRecordService.getPrescriptionDetailsByMedicalRecord(
                authentication.getName(),
                medicalRecordId);
    }

    @PostMapping("/{medicalRecordId}/service-results")
    @Operation(summary = "Cập nhật kết quả dịch vụ")
    public MedicalRecordServiceDetail upsertServiceResult(
            Authentication authentication,
            @PathVariable("medicalRecordId") Long medicalRecordId,
            @Valid @RequestBody UpsertMedicalRecordServiceResultRequest request) {
        return medicalRecordService.upsertMedicalRecordServiceResult(
                authentication.getName(),
                medicalRecordId,
                request);
    }

    @GetMapping("/{medicalRecordId}/prescription-workspace")
    @Operation(summary = "Không gian kê đơn")
    // Chức năng: xử lý get prescription workspace.
    public PrescriptionWorkspaceResponse getPrescriptionWorkspace(
            Authentication authentication,
            @PathVariable("medicalRecordId") Long medicalRecordId) {
        return medicalRecordService.getPrescriptionWorkspace(authentication.getName(), medicalRecordId);
    }

    @GetMapping("/{medicalRecordId}/medicine-catalog")
    @Operation(summary = "Danh mục thuốc để kê đơn")
    public PrescriptionMedicineCatalogResponse getMedicineCatalogByGroup(
            Authentication authentication,
            @PathVariable("medicalRecordId") Long medicalRecordId,
            @RequestParam(name = "group", required = false) String group) {
        return medicalRecordService.getMedicineCatalogForPrescription(
                authentication.getName(),
                medicalRecordId,
                group);
    }

    @PostMapping("/{medicalRecordId}/prescription-details/quick-add")
    @Operation(summary = "Thêm nhanh thuốc vào đơn")
    public PrescriptionWorkspaceResponse quickAddMedicineToPrescription(
            Authentication authentication,
            @PathVariable("medicalRecordId") Long medicalRecordId,
            @Valid @RequestBody QuickAddPrescriptionMedicineRequest request) {
        return medicalRecordService.quickAddMedicineToPrescription(
                authentication.getName(),
                medicalRecordId,
                request.getMedicineId());
    }

    @PutMapping("/{medicalRecordId}/prescription-details/{medicineId}")
    @Operation(summary = "Cập nhật chi tiết thuốc")
    public PrescriptionDetail updatePrescriptionDetail(
            Authentication authentication,
            @PathVariable("medicalRecordId") Long medicalRecordId,
            @PathVariable("medicineId") Long medicineId,
            @Valid @RequestBody UpdatePrescriptionDetailRequest request) {
        return medicalRecordService.updatePrescriptionDetail(
                authentication.getName(),
                medicalRecordId,
                medicineId,
                request);
    }

    @PutMapping("/{medicalRecordId}/prescription-details/{medicineId}/autosave")
    @Operation(summary = "Tự động lưu đơn thuốc")
    public PrescriptionAutosaveResponse autosavePrescriptionDetail(
            Authentication authentication,
            @PathVariable("medicalRecordId") Long medicalRecordId,
            @PathVariable("medicineId") Long medicineId,
            @Valid @RequestBody UpdatePrescriptionDetailRequest request) {
        return medicalRecordService.autosavePrescriptionDetail(
                authentication.getName(),
                medicalRecordId,
                medicineId,
                request);
    }

    @DeleteMapping("/{medicalRecordId}/prescription-details/{medicineId}")
    @Operation(summary = "Xóa thuốc khỏi đơn")
    public PrescriptionWorkspaceResponse removePrescriptionDetail(
            Authentication authentication,
            @PathVariable("medicalRecordId") Long medicalRecordId,
            @PathVariable("medicineId") Long medicineId) {
        return medicalRecordService.removePrescriptionDetail(
                authentication.getName(),
                medicalRecordId,
                medicineId);
    }

    @PostMapping("/{medicalRecordId}/prescription-save")
    @Operation(summary = "Lưu đơn thuốc")
    public PrescriptionWorkspaceResponse savePrescription(
            Authentication authentication,
            @PathVariable("medicalRecordId") Long medicalRecordId) {
        return medicalRecordService.savePrescription(authentication.getName(), medicalRecordId);
    }

    @PutMapping("/{medicalRecordId}/complete")
    @Operation(summary = "Hoàn tất bệnh án")
    public MedicalRecord completeMedicalRecord(
            Authentication authentication,
            @PathVariable("medicalRecordId") Long medicalRecordId) {
        return medicalRecordService.completeMedicalRecord(authentication.getName(), medicalRecordId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật bệnh án")
    public MedicalRecord update(@PathVariable("id") Long id, @RequestBody MedicalRecord request) {
        return medicalRecordService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa bệnh án")
    public void delete(@PathVariable("id") Long id) {
        medicalRecordService.delete(id);
    }
}
