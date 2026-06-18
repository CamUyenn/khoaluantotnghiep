package com.example.demo.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.example.demo.exception.AppException;

import com.example.demo.dto.DoctorPatientServiceItemResponse;
import com.example.demo.dto.PatientMedicalRecordDetailResponse;
import com.example.demo.dto.PatientMedicalRecordHistoryItemResponse;
import com.example.demo.dto.PatientPrescriptionHistoryItemResponse;
import com.example.demo.entity.Appointment;
import com.example.demo.entity.Invoice;
import com.example.demo.entity.MedicalRecord;
import com.example.demo.entity.MedicalRecordServiceDetail;
import com.example.demo.entity.Patient;
import com.example.demo.entity.PrescriptionDetail;
import com.example.demo.entity.User;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.InvoiceRepository;
import com.example.demo.repository.MedicalRecordRepository;
import com.example.demo.repository.MedicalRecordServiceDetailRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.PrescriptionDetailRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PatientService {

        private final PatientRepository patientRepository;
        private final UserRepository userRepository;
        private final MedicalRecordRepository medicalRecordRepository;
        private final MedicalRecordServiceDetailRepository medicalRecordServiceDetailRepository;
        private final PrescriptionDetailRepository prescriptionDetailRepository;
        private final InvoiceRepository invoiceRepository;
        private final AppointmentRepository appointmentRepository;

        // LẤY PATIENT TỪ USERNAME (CỰC QUAN TRỌNG)
        // Chức năng: xử lý get patient from username.
        public Patient getPatientFromUsername(String username) {

                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

                return patientRepository.findByUserId(user.getId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy bệnh nhân"));
        }

        // Chức năng: xử lý lấy lịch sử bệnh án của bệnh nhân đăng nhập.
        public List<PatientMedicalRecordHistoryItemResponse> getMyMedicalRecordHistory(String username) {
                Patient patient = getPatientFromUsername(username);

                ensureMedicalRecordsForCompletedAppointments(patient.getId());

                return medicalRecordRepository.findByAppointment_Patient_Id(patient.getId()).stream()
                                .sorted(Comparator.comparing(
                                                record -> record.getAppointment() == null
                                                                ? null
                                                                : record.getAppointment().getAppointmentTime(),
                                                Comparator.nullsLast(Comparator.reverseOrder())))
                                .map(this::toHistoryItemResponse)
                                .toList();
        }

        // Chức năng: xử lý lấy chi tiết bệnh án và đơn thuốc của bệnh nhân đăng nhập.
        public PatientMedicalRecordDetailResponse getMyMedicalRecordDetail(String username, Long medicalRecordId) {
                Patient patient = getPatientFromUsername(username);

                MedicalRecord medicalRecord = medicalRecordRepository.findById(medicalRecordId)
                                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy bệnh án"));

                Appointment appointment = medicalRecord.getAppointment();
                if (appointment == null || appointment.getPatient() == null
                                || !patient.getId().equals(appointment.getPatient().getId())) {
                        throw AppException.of(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập bệnh án này");
                }

                List<PatientPrescriptionHistoryItemResponse> prescriptionItems = prescriptionDetailRepository
                                .findByMedicalRecord_Id(medicalRecordId)
                                .stream()
                                .map(this::toPrescriptionItemResponse)
                                .toList();

                List<DoctorPatientServiceItemResponse> services = medicalRecordServiceDetailRepository
                                .findByMedicalRecord_Id(medicalRecordId)
                                .stream()
                                .map(this::toServiceItemResponse)
                                .toList();

                Invoice invoice = invoiceRepository.findByMedicalRecord_Id(medicalRecordId).orElse(null);
                BigDecimal totalServiceFee = invoice == null || invoice.getTotalServiceFee() == null
                                ? BigDecimal.ZERO
                                : invoice.getTotalServiceFee();
                BigDecimal totalAmount = invoice == null || invoice.getGrandTotal() == null
                                ? totalServiceFee
                                : invoice.getGrandTotal();

                return new PatientMedicalRecordDetailResponse(
                                medicalRecord.getId(),
                                appointment.getId(),
                                appointment.getAppointmentTime(),
                                appointment.getStatus(),
                                appointment.getDoctor() == null ? null : appointment.getDoctor().getUsername(),
                                medicalRecord.getDiagnosis(),
                                medicalRecord.getDoctorAdvice(),
                                resolveCompletedAt(medicalRecord),
                                invoice == null ? null : invoice.getId(),
                                totalServiceFee,
                                totalAmount,
                                invoice != null && Boolean.TRUE.equals(invoice.getIsPaid()),
                                invoice == null ? null : invoice.getPaidAt(),
                                invoice == null ? null : invoice.getPaymentMethod(),
                                invoice == null ? null : invoice.getPaymentReference(),
                                services,
                                prescriptionItems);
        }

        // Chức năng: xử lý ánh xạ bệnh án sang DTO lịch sử.
        private PatientMedicalRecordHistoryItemResponse toHistoryItemResponse(MedicalRecord medicalRecord) {
                Appointment appointment = medicalRecord.getAppointment();
                int prescriptionItemCount = prescriptionDetailRepository.findByMedicalRecord_Id(medicalRecord.getId())
                                .size();
                Invoice invoice = invoiceRepository.findByMedicalRecord_Id(medicalRecord.getId()).orElse(null);

                BigDecimal totalServiceFee = invoice == null || invoice.getTotalServiceFee() == null
                                ? BigDecimal.ZERO
                                : invoice.getTotalServiceFee();
                BigDecimal totalAmount = invoice == null || invoice.getGrandTotal() == null
                                ? totalServiceFee
                                : invoice.getGrandTotal();

                return new PatientMedicalRecordHistoryItemResponse(
                                medicalRecord.getId(),
                                appointment == null ? null : appointment.getId(),
                                appointment == null ? null : appointment.getAppointmentTime(),
                                appointment == null ? null : appointment.getStatus(),
                                (appointment == null || appointment.getDoctor() == null) ? null
                                                : appointment.getDoctor().getUsername(),
                                medicalRecord.getDiagnosis(),
                                medicalRecord.getDoctorAdvice(),
                                resolveCompletedAt(medicalRecord),
                                prescriptionItemCount,
                                invoice == null ? null : invoice.getId(),
                                totalServiceFee,
                                totalAmount,
                                invoice != null && Boolean.TRUE.equals(invoice.getIsPaid()),
                                invoice == null ? null : invoice.getPaidAt(),
                                invoice == null ? null : invoice.getPaymentMethod(),
                                invoice == null ? null : invoice.getPaymentReference());
        }

        private java.time.LocalDateTime resolveCompletedAt(MedicalRecord medicalRecord) {
                if (medicalRecord == null) {
                        return null;
                }
                return medicalRecord.getCompletedAt();
        }

        // Chức năng: xử lý ánh xạ chi tiết đơn thuốc sang DTO lịch sử.
        private PatientPrescriptionHistoryItemResponse toPrescriptionItemResponse(PrescriptionDetail detail) {
                BigDecimal unitPrice = (detail.getMedicine() != null && detail.getMedicine().getSellingPrice() != null)
                                ? detail.getMedicine().getSellingPrice()
                                : BigDecimal.ZERO;
                Integer quantityValue = detail.getQuantity();
                int normalizedQuantity = quantityValue == null ? 0 : quantityValue;
                BigDecimal quantity = BigDecimal.valueOf(normalizedQuantity);

                return new PatientPrescriptionHistoryItemResponse(
                                detail.getMedicine() == null ? null : detail.getMedicine().getId(),
                                detail.getMedicine() == null ? null : detail.getMedicine().getMedicineName(),
                                detail.getMedicine() == null ? null : detail.getMedicine().getUnit(),
                                quantityValue,
                                detail.getUsageInstructions(),
                                unitPrice,
                                unitPrice.multiply(quantity));
        }

        private DoctorPatientServiceItemResponse toServiceItemResponse(MedicalRecordServiceDetail detail) {
                return new DoctorPatientServiceItemResponse(
                                detail.getService() == null ? null : detail.getService().getId(),
                                detail.getService() == null ? null : detail.getService().getServiceName(),
                                detail.getQuantity(),
                                detail.getActualPrice(),
                                detail.getResultNote());
        }

        private void ensureMedicalRecordsForCompletedAppointments(Long patientId) {
                if (patientId == null) {
                        return;
                }

                List<Appointment> completedAppointments = appointmentRepository
                                .findByPatient_IdOrderByAppointmentTimeDesc(patientId)
                                .stream()
                                .filter(appointment -> "COMPLETED".equalsIgnoreCase(appointment.getStatus()))
                                .toList();

                for (Appointment appointment : completedAppointments) {
                        if (appointment.getId() == null) {
                                continue;
                        }
                        if (medicalRecordRepository.existsByAppointment_Id(appointment.getId())) {
                                continue;
                        }

                        MedicalRecord record = new MedicalRecord();
                        record.setAppointment(appointment);
                        record.setDiagnosis(null);
                        record.setDoctorAdvice(null);
                        record.setCreatedAt(appointment.getAppointmentTime() == null
                                        ? java.time.LocalDateTime.now()
                                        : appointment.getAppointmentTime());
                        medicalRecordRepository.save(record);
                }
        }
}
