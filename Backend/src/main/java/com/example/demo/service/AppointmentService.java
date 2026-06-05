package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.example.demo.exception.AppException;

import com.example.demo.dto.AppointmentRequest;
import com.example.demo.dto.AppointmentFeeEstimateResponse;
import com.example.demo.dto.CashierServiceLineItemResponse;
import com.example.demo.entity.MedicalRecord;
import com.example.demo.repository.MedicalRecordRepository;
import com.example.demo.repository.MedicalRecordServiceDetailRepository;
import com.example.demo.repository.MedicalServiceRepository;
import com.example.demo.repository.PaymentTransactionRepository;
import com.example.demo.dto.PatientAppointmentRequest;
import com.example.demo.dto.PatientPrefillResponse;
import com.example.demo.entity.Appointment;
import com.example.demo.entity.MedicalCategory;
import com.example.demo.entity.MedicalService;
import com.example.demo.entity.Patient;
import com.example.demo.entity.Role;
import com.example.demo.entity.SymptomServiceMapping;
import com.example.demo.entity.SymptomTemplate;
import com.example.demo.entity.User;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.MedicalCategoryRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.SymptomServiceMappingRepository;
import com.example.demo.repository.SymptomTemplateRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.constants.PaymentStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AppointmentService {

    private static final String STATUS_PENDING_CONFIRMATION = "PENDING";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_CANCELLED_BY_CLINIC = "CANCELLED_BY_CLINIC";
    private static final Set<String> RELEASED_SLOT_STATUSES = Set.of(
            STATUS_CANCELLED,
            STATUS_CANCELLED_BY_CLINIC);

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final MedicalCategoryRepository medicalCategoryRepository;
    private final SymptomTemplateRepository symptomTemplateRepository;
    private final SymptomServiceMappingRepository symptomServiceMappingRepository;
    private final PatientService patientService;
    private final NotificationService notificationService;
    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordServiceDetailRepository medicalRecordServiceDetailRepository;
    private final MedicalServiceRepository medicalServiceRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final InvoiceService invoiceService;

    // Chức năng: xử lý lấy danh sách tất cả lịch hẹn.
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    // Chức năng: xử lý lấy danh sách lịch hẹn đang chờ phân công.
    public List<Appointment> getWaitingAssignmentAppointments() {
        return appointmentRepository.findByStatusInOrderByAppointmentTimeAsc(
                List.of(STATUS_PENDING_CONFIRMATION, "DRAFT", "PENDING", "PENDING_CONFIRMATION"));
    }

    // Chức năng: xử lý lấy thông tin trước cho bệnh nhân.
    public PatientPrefillResponse getPatientPrefill(Long patientId) {
        Patient patient = resolvePatient(patientId);

        PatientPrefillResponse response = new PatientPrefillResponse();
        response.setPatientId(patient.getId());
        response.setFullName(patient.getFullName());
        response.setGender(patient.getGender());
        response.setDateOfBirth(patient.getDateOfBirth());
        response.setHometown(patient.getHometown());
        response.setNationalId(patient.getNationalId());
        response.setPhoneNumber(patient.getPhoneNumber());
        response.setHealthInsuranceNumber(patient.getHealthInsuranceNumber());
        response.setGmail(patient.getGmail());
        return response;
    }

    // Chức năng: xử lý tạo lịch hẹn cho bệnh nhân.
    public Appointment createAppointmentForPatient(PatientAppointmentRequest request) {
        Patient patient = resolvePatient(request.getPatientId());
        LocalDateTime appointmentTime = resolvePatientAppointmentTime(request);
        String symptoms = resolveSymptoms(request);

        MedicalCategory category = medicalCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm bệnh"));

        List<Long> symptomIds = request.getSymptomIds();
        if (symptomIds == null || symptomIds.isEmpty()) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "symptomIds là bắt buộc");
        }

        List<SymptomTemplate> symptomTemplates = symptomTemplateRepository.findAllById(symptomIds);
        if (symptomTemplates.size() != symptomIds.size()) {
            throw AppException.of(HttpStatus.NOT_FOUND, "Có triệu chứng không tồn tại");
        }

        String symptomsText = symptomTemplates.stream()
                .map(SymptomTemplate::getSymptomName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse(null);

        BigDecimal estimatedTotalFee = estimateFeeFromSymptoms(symptomIds);
        BigDecimal advancePayment = request.getAdvancePayment() == null
                ? BigDecimal.ZERO
                : request.getAdvancePayment();

        String paymentStatus = resolvePaymentStatus(advancePayment, estimatedTotalFee);

        // If client provided a paymentReference, enforce idempotency: return
        // existing appointment with the same reference instead of creating a
        // duplicate. This prevents duplicate records when the client retries
        // or both polling and websocket events trigger creation.
        if (request.getPaymentReference() != null && !request.getPaymentReference().isBlank()) {
            var existingOpt = appointmentRepository
                    .findTopByPaymentReferenceOrderByIdDesc(request.getPaymentReference());
            if (existingOpt != null && existingOpt.isPresent()) {
                return existingOpt.get();
            }
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setCategory(category);
        appointment.setAppointmentTime(appointmentTime);
        appointment.setStatus(STATUS_PENDING_CONFIRMATION);
        appointment.setSymptoms(symptoms);
        appointment.setSymptomsText(symptomsText);
        appointment.setEstimatedTotalFee(estimatedTotalFee);
        appointment.setAdvancePayment(advancePayment);
        appointment.setPaymentMethod(request.getPaymentMethod());
        appointment.setPaymentStatus(paymentStatus);

        Appointment savedAppointment = appointmentRepository.save(appointment);
        // Create a medical record and persist suggested service lines based on symptoms
        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setAppointment(savedAppointment);
        medicalRecord.setCreatedAt(java.time.LocalDateTime.now());
        medicalRecord = medicalRecordRepository.save(medicalRecord);

        List<com.example.demo.dto.CashierServiceLineItemResponse> suggestedServices = collectServicesFromSymptoms(
                symptomIds);
        for (com.example.demo.dto.CashierServiceLineItemResponse svc : suggestedServices) {
            if (svc.getServiceId() == null)
                continue;
            com.example.demo.entity.MedicalRecordServiceDetail detail = new com.example.demo.entity.MedicalRecordServiceDetail();
            com.example.demo.entity.MedicalRecordServiceId id = new com.example.demo.entity.MedicalRecordServiceId();
            id.setMedicalRecordId(medicalRecord.getId());
            id.setServiceId(svc.getServiceId());
            detail.setId(id);
            detail.setMedicalRecord(medicalRecord);
            com.example.demo.entity.MedicalService serviceEntity = medicalServiceRepository.findById(svc.getServiceId())
                    .orElse(null);
            detail.setService(serviceEntity);
            detail.setQuantity(svc.getQuantity());
            detail.setActualPrice(svc.getUnitPrice());
            detail.setResultNote("Dịch vụ gợi ý khi đăng ký");
            medicalRecordServiceDetailRepository.save(detail);
        }

        if ("CHUYEN_KHOAN".equalsIgnoreCase(request.getPaymentMethod())) {
            String paymentReference = request.getPaymentReference();
            if (paymentReference == null || paymentReference.isBlank()) {
                paymentReference = buildPaymentReference(savedAppointment.getId());
            }
            savedAppointment.setPaymentReference(paymentReference);
            boolean alreadyPaid = paymentTransactionRepository.findTopByPaymentReferenceOrderByIdDesc(paymentReference)
                    .map(tx -> "SUCCESS".equalsIgnoreCase(tx.getStatus()))
                    .orElse(false);
            savedAppointment.setPaymentStatus(alreadyPaid ? PaymentStatus.FULLY_PAID : PaymentStatus.PENDING_TRANSFER);
            savedAppointment = appointmentRepository.save(savedAppointment);

            var invoice = invoiceService.createInvoiceForAppointment(savedAppointment);
            invoice.setPaymentReference(paymentReference);
            invoice = invoiceService.saveInvoice(invoice);

            if (alreadyPaid) {
                invoiceService.markTransferInvoiceAsPaid(invoice.getId(), "CHUYEN_KHOAN");
            }
        }

        notificationService.notifyReceptionistNewPatientBooking(savedAppointment);
        return savedAppointment;
    }

    // Chức năng: ước tính phí khám theo danh sách triệu chứng.
    public AppointmentFeeEstimateResponse estimateFeeForSymptoms(List<Long> symptomIds) {
        if (symptomIds == null || symptomIds.isEmpty()) {
            return new AppointmentFeeEstimateResponse(BigDecimal.ZERO, List.of());
        }

        List<CashierServiceLineItemResponse> services = collectServicesFromSymptoms(symptomIds);
        BigDecimal estimatedTotalFee = services.stream()
                .map(CashierServiceLineItemResponse::getLineTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AppointmentFeeEstimateResponse(estimatedTotalFee, services);
    }

    // Chức năng: xử lý tạo lịch hẹn.
    public Appointment createAppointment(AppointmentRequest request) {
        validateTimeSlotRange(request.getTimeSlot());

        if (request.getPatientId() == null || request.getDoctorId() == null) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "patientId và doctorId là bắt buộc");
        }

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy bệnh nhân"));

        User doctor = userRepository.findByIdAndRole(request.getDoctorId(), Role.DOCTOR)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy bác sĩ"));

        LocalDateTime appointmentTime = combineDateAndStartTime(request.getAppointmentDate(), request.getTimeSlot());

        boolean exists = appointmentRepository.existsByDoctor_IdAndAppointmentTimeAndStatusNotIn(
                doctor.getId(),
                appointmentTime,
                RELEASED_SLOT_STATUSES);
        if (exists) {
            throw AppException.of(HttpStatus.CONFLICT, "Bác sĩ đã có lịch hẹn vào thời điểm này");
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentTime(appointmentTime);
        appointment.setStatus(STATUS_PENDING_CONFIRMATION);

        Appointment savedAppointment = appointmentRepository.save(appointment);
        notificationService.notifyReceptionistNewPatientBooking(savedAppointment);
        return savedAppointment;
    }

    // Chức năng: xử lý chỉ định bác sĩ đến cuộc hẹn.
    public Appointment assignDoctorToAppointment(Long appointmentId, Long doctorId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn"));

        User doctor = userRepository.findByIdAndRole(doctorId, Role.DOCTOR)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy bác sĩ"));

        ensureDoctorIsAvailable(doctor.getId(), appointment.getAppointmentTime(), appointment.getId());

        appointment.setDoctor(doctor);
        appointment.setStatus(STATUS_PENDING_CONFIRMATION);

        return appointmentRepository.save(appointment);
    }

    // Chức năng: xử lý đảm bảo có bác sĩ.
    private void ensureDoctorIsAvailable(Long doctorId, LocalDateTime appointmentTime, Long appointmentId) {
        boolean exists = appointmentRepository.existsByDoctor_IdAndAppointmentTimeAndIdNotAndStatusNotIn(
                doctorId,
                appointmentTime,
                appointmentId,
                RELEASED_SLOT_STATUSES);
        if (exists) {
            throw AppException.of(HttpStatus.CONFLICT, "Bác sĩ đã có lịch hẹn vào thời điểm này");
        }
    }

    private BigDecimal estimateFeeFromSymptoms(List<Long> symptomIds) {
        List<SymptomServiceMapping> mappings = symptomServiceMappingRepository.findBySymptom_IdIn(symptomIds);
        if (mappings.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return mappings.stream()
                .map(SymptomServiceMapping::getService)
                .filter(service -> service != null && Boolean.TRUE.equals(service.getIsActive()))
                .map(MedicalService::getCurrentPrice)
                .filter(Objects::nonNull)
                .distinct()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<CashierServiceLineItemResponse> collectServicesFromSymptoms(List<Long> symptomIds) {
        List<SymptomServiceMapping> mappings = symptomServiceMappingRepository.findBySymptom_IdIn(symptomIds);
        if (mappings.isEmpty()) {
            return List.of();
        }

        Map<Long, CashierServiceLineItemResponse> serviceMap = new LinkedHashMap<>();
        for (SymptomServiceMapping mapping : mappings) {
            MedicalService service = mapping.getService();
            if (service == null || service.getId() == null || !Boolean.TRUE.equals(service.getIsActive())) {
                continue;
            }

            BigDecimal unitPrice = service.getCurrentPrice() == null ? BigDecimal.ZERO : service.getCurrentPrice();
            serviceMap.putIfAbsent(service.getId(), new CashierServiceLineItemResponse(
                    service.getId(),
                    service.getServiceName(),
                    1,
                    unitPrice,
                    unitPrice));
        }

        return List.copyOf(serviceMap.values());
    }

    private String resolvePaymentStatus(BigDecimal advancePayment, BigDecimal estimatedTotalFee) {
        BigDecimal advance = advancePayment == null ? BigDecimal.ZERO : advancePayment;
        BigDecimal total = estimatedTotalFee == null ? BigDecimal.ZERO : estimatedTotalFee;
        if (advance.compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentStatus.UNPAID;
        }
        if (advance.compareTo(total) >= 0 && total.compareTo(BigDecimal.ZERO) > 0) {
            return PaymentStatus.FULLY_PAID;
        }
        return PaymentStatus.PARTIALLY_PAID;
    }

    private String buildPaymentReference(Long appointmentId) {
        return "APT-" + appointmentId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Chức năng: xử lý giải quyết thời gian hẹn bệnh nhân.
    private LocalDateTime resolvePatientAppointmentTime(PatientAppointmentRequest request) {
        if (request.getAppointmentTime() != null) {
            return request.getAppointmentTime();
        }

        if (request.getAppointmentDate() != null && request.getTimeSlot() != null && !request.getTimeSlot().isBlank()) {
            validateTimeSlotRange(request.getTimeSlot());
            return combineDateAndStartTime(request.getAppointmentDate(), request.getTimeSlot());
        }

        throw AppException.of(HttpStatus.BAD_REQUEST, "appointmentTime là bắt buộc");
    }

    // Chức năng: xử lý giải quyết các triệu chứng.
    private String resolveSymptoms(PatientAppointmentRequest request) {
        if (request.getSymptoms() != null && !request.getSymptoms().isBlank()) {
            return request.getSymptoms().trim();
        }

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            return request.getNotes().trim();
        }

        throw AppException.of(HttpStatus.BAD_REQUEST, "Triệu chứng là bắt buộc");
    }

    // Chức năng: xử lý kết hợp ngày và giờ bắt đầu.
    private LocalDateTime combineDateAndStartTime(LocalDate appointmentDate, String timeSlot) {
        String[] parts = timeSlot.split("-");
        LocalTime start = LocalTime.parse(parts[0]);
        return appointmentDate.atTime(start);
    }

    // Chức năng: xử lý validate thứ tự .
    private void validateTimeSlotRange(String timeSlot) {
        String[] parts = timeSlot.split("-");
        if (parts.length != 2) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Định dạng timeSlot không hợp lệ");
        }

        try {
            LocalTime start = LocalTime.parse(parts[0]);
            LocalTime end = LocalTime.parse(parts[1]);
            if (!end.isAfter(start)) {
                throw AppException.of(HttpStatus.BAD_REQUEST,
                        "Thời gian kết thúc của timeSlot phải sau thời gian bắt đầu");
            }
        } catch (DateTimeParseException ex) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Giá trị timeSlot không hợp lệ");
        }
    }

    // Chức năng: xử lý giải quyết bệnh nhân.
    private Patient resolvePatient(Long patientId) {
        if (patientId != null) {
            return patientRepository.findById(patientId)
                    .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy bệnh nhân"));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw AppException.of(HttpStatus.BAD_REQUEST,
                    "patientId là bắt buộc khi người dùng chưa xác thực");
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        if (user.getPatient() == null) {
            throw AppException.of(HttpStatus.CONFLICT,
                    "Authenticated user is not linked to a patient profile");
        }

        return user.getPatient();
    }

    // Chức năng: xử lý lấy danh sách cuộc hẹn của bệnh nhân.
    public List<Appointment> getMyAppointments(String username) {
        Patient patient = patientService.getPatientFromUsername(username);
        return appointmentRepository.findByPatient_IdOrderByAppointmentTimeDesc(patient.getId());
    }

    // Chức năng: xử lý bệnh nhân hủy cuộc hẹn.
    public Appointment cancelMyAppointment(Long appointmentId, String username) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn"));

        Patient patient = patientService.getPatientFromUsername(username);
        if (appointment.getPatient() == null || !appointment.getPatient().getId().equals(patient.getId())) {
            throw AppException.of(HttpStatus.FORBIDDEN, "Bạn chỉ có thể hủy lịch hẹn của chính mình");
        }

        String normalizedStatus = normalizeStatus(appointment.getStatus());
        if (STATUS_CANCELLED.equals(normalizedStatus) || STATUS_CANCELLED_BY_CLINIC.equals(normalizedStatus)) {
            throw AppException.of(HttpStatus.CONFLICT, "Lịch hẹn đã được hủy");
        }

        if (STATUS_IN_PROGRESS.equals(normalizedStatus)) {
            throw AppException.of(HttpStatus.CONFLICT, "Không thể hủy vì lịch đang được thực hiện");
        }

        if (STATUS_COMPLETED.equals(normalizedStatus)
                || (appointment.getAppointmentTime() != null
                        && !appointment.getAppointmentTime().isAfter(LocalDateTime.now()))) {
            throw AppException.of(HttpStatus.CONFLICT, "Không thể hủy vì đã quá giờ hẹn");
        }

        appointment.setStatus(STATUS_CANCELLED);
        appointment.setDoctor(null);
        appointment.setCancellationReason(null);
        return appointmentRepository.save(appointment);
    }

    // Chức năng: hủy lịch hẹn bởi hệ thống khi thanh toán chuyển khoản thất bại.
    public Appointment cancelAppointmentBySystem(Long appointmentId, String reason) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn"));

        appointment.setStatus(STATUS_CANCELLED);
        appointment.setDoctor(null);
        appointment.setCancellationReason(reason);
        appointment.setPaymentStatus(PaymentStatus.UNPAID);
        return appointmentRepository.save(appointment);
    }

    // Chức năng: xử lý chuẩn hóa trạng thái.
    private String normalizeStatus(String status) {
        if (status == null) {
            return "";
        }
        return status.trim().toUpperCase();
    }
}
