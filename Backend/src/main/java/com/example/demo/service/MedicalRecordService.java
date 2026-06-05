package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.exception.AppException;
import com.example.demo.constants.AppointmentStatus;

import com.example.demo.dto.AddPrescriptionDetailRequest;
import com.example.demo.dto.CreateMedicalRecordRequest;
import com.example.demo.dto.DoctorPatientHistoryDetailResponse;
import com.example.demo.dto.DoctorPatientHistoryResponse;
import com.example.demo.dto.DoctorPatientHistoryRowResponse;
import com.example.demo.dto.DoctorPatientPrescriptionItemResponse;
import com.example.demo.dto.DoctorPatientServiceItemResponse;
import com.example.demo.dto.PrescriptionAutosaveResponse;
import com.example.demo.dto.PrescriptionCatalogMedicineResponse;
import com.example.demo.dto.PrescriptionLineResponse;
import com.example.demo.dto.PrescriptionMedicineCatalogResponse;
import com.example.demo.dto.PrescriptionWorkspaceResponse;
import com.example.demo.dto.UpdatePrescriptionDetailRequest;
import com.example.demo.dto.UpsertMedicalRecordServiceResultRequest;
import com.example.demo.entity.Appointment;
import com.example.demo.entity.MedicalRecord;
import com.example.demo.entity.MedicalRecordServiceDetail;
import com.example.demo.entity.MedicalRecordServiceId;
import com.example.demo.entity.MedicalService;
import com.example.demo.entity.Medicine;
import com.example.demo.entity.Patient;
import com.example.demo.entity.PrescriptionDetail;
import com.example.demo.entity.PrescriptionDetailId;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DiagnosisTemplateRepository;
import com.example.demo.repository.MedicalRecordRepository;
import com.example.demo.repository.MedicalRecordServiceDetailRepository;
import com.example.demo.repository.MedicalServiceRepository;
import com.example.demo.repository.MedicineRepository;
import com.example.demo.repository.PrescriptionDetailRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class MedicalRecordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MedicalRecordService.class);

    private static final String STATUS_WAITING = "WAITING";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private static final String GROUP_ALL = "Tat ca";
    private static final String GROUP_ANTIBIOTIC = "Khang sinh";
    private static final String GROUP_PAIN_FEVER = "Giam dau ha sot";
    private static final String GROUP_COUGH = "Thuoc ho";
    private static final String GROUP_ALLERGY = "Chong di ung";
    private static final String GROUP_DIGESTIVE = "Tieu hoa";
    private static final String GROUP_OTHER = "Khac";

    private static final String DEFAULT_USAGE_PLACEHOLDER = "Chua cap nhat";

    private static final Pattern CONCENTRATION_PATTERN = Pattern.compile(
            "(\\d+(?:[\\.,]\\d+)?\\s?(?:mg|g|mcg|ug|ml|iu|%))",
            Pattern.CASE_INSENSITIVE);

    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final MedicineRepository medicineRepository;
    private final MedicalServiceRepository medicalServiceRepository;
    private final PrescriptionDetailRepository prescriptionDetailRepository;
    private final MedicalRecordServiceDetailRepository medicalRecordServiceDetailRepository;
    private final RoomRepository roomRepository;
    private final com.example.demo.repository.DiagnosisMedicineRuleRepository diagnosisMedicineRuleRepository;
    private final DiagnosisTemplateRepository diagnosisTemplateRepository;
    private final PatientService patientService;
    private final InvoiceService invoiceService;

    // Chức năng: xử lý get all.
    public List<MedicalRecord> getAll() {
        return medicalRecordRepository.findAll();
    }

    // Chức năng: xử lý Cập nhật kết quả dịch vụ hồ sơ y tế từ phòng khám của bác sĩ
    // được chỉ định.
    public MedicalRecordServiceDetail upsertMedicalRecordServiceResult(
            String username,
            Long medicalRecordId,
            UpsertMedicalRecordServiceResultRequest request) {
        User doctor = userRepository.findByUsernameAndRole(username, Role.DOCTOR)
                .orElseThrow(
                        () -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản bác sĩ"));

        MedicalRecord medicalRecord = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy bệnh án"));

        ensureDoctorCanUpdateCrossRoomServiceResult(doctor, medicalRecord);

        MedicalService medicalService = medicalServiceRepository.findById(request.getServiceId())
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy dịch vụ y tế"));

        MedicalRecordServiceId id = new MedicalRecordServiceId();
        id.setMedicalRecordId(medicalRecordId);
        id.setServiceId(request.getServiceId());

        MedicalRecordServiceDetail detail = medicalRecordServiceDetailRepository.findById(id)
                .orElseGet(MedicalRecordServiceDetail::new);

        detail.setId(id);
        detail.setMedicalRecord(medicalRecord);
        detail.setService(medicalService);
        detail.setQuantity(request.getQuantity());
        detail.setActualPrice(request.getActualPrice());
        detail.setResultNote(normalizeOptionalResultNote(request.getResultNote()));

        MedicalRecordServiceDetail savedDetail = medicalRecordServiceDetailRepository.save(detail);
        var invoice = invoiceService.aggregateInvoiceAmount(medicalRecordId);
        // If the invoice now has a remaining amount, update the appointment's
        // paymentStatus so receptionist shows the correct outstanding state.
        Appointment appt = medicalRecord.getAppointment();
        if (appt != null && invoice != null) {
            var remaining = invoice.getRemainingAmount() == null ? java.math.BigDecimal.ZERO : invoice.getRemainingAmount();
            if (remaining.compareTo(java.math.BigDecimal.ZERO) > 0) {
                appt.setPaymentStatus(com.example.demo.constants.PaymentStatus.PARTIALLY_PAID);
            } else {
                appt.setPaymentStatus(com.example.demo.constants.PaymentStatus.FULLY_PAID);
            }
            appointmentRepository.save(appt);
        }

        return savedDetail;
    }

    // Chức năng: xử lý get by id.
    public MedicalRecord getById(Long id) {
        return medicalRecordRepository.findById(id)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy bệnh án"));
    }

    // Chức năng: xử lý get by appointment id.
    public MedicalRecord getByAppointmentId(Long appointmentId) {
        return medicalRecordRepository.findAllByAppointment_IdOrderByCreatedAtDescIdDesc(appointmentId)
                .stream()
                .findFirst()
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND,
                        "Không tìm thấy bệnh án theo lịch hẹn"));
    }

    // Chức năng: xử lý create.
    public MedicalRecord create(Long appointmentId, MedicalRecord request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn"));

        if (medicalRecordRepository.existsByAppointment_Id(appointmentId)) {
            throw AppException.of(HttpStatus.CONFLICT, "Lịch hẹn đã có bệnh án");
        }

        MedicalRecord record = new MedicalRecord();
        record.setAppointment(appointment);
        record.setDiagnosis(request.getDiagnosis());
        record.setDoctorAdvice(request.getDoctorAdvice());
        record.setCreatedAt(LocalDateTime.now());

        return medicalRecordRepository.save(record);
    }

    // Chức năng: xử lý create by doctor.
    public MedicalRecord createByDoctor(String username, CreateMedicalRecordRequest request) {
        User doctor = userRepository.findByUsernameAndRole(username, Role.DOCTOR)
                .orElseThrow(
                        () -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản bác sĩ"));

        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn"));

        if (appointment.getDoctor() == null || !doctor.getId().equals(appointment.getDoctor().getId())) {
            throw AppException.of(HttpStatus.FORBIDDEN, "Bạn chưa được phân công cho lịch hẹn này");
        }

        if (!STATUS_WAITING.equalsIgnoreCase(appointment.getStatus())
            && !"IN_PROGRESS".equalsIgnoreCase(appointment.getStatus())
            && !"IN_ROOM".equalsIgnoreCase(appointment.getStatus())) {
            throw AppException.of(HttpStatus.CONFLICT,
                "Chỉ lịch hẹn WAITING, IN_ROOM hoặc IN_PROGRESS mới có thể tạo bệnh án");
        }

        if (medicalRecordRepository.existsByAppointment_Id(request.getAppointmentId())) {
            throw AppException.of(HttpStatus.CONFLICT, "Lịch hẹn đã có bệnh án");
        }

        MedicalRecord record = new MedicalRecord();
        record.setAppointment(appointment);
        record.setDiagnosis(request.getDiagnosis().trim());
        record.setDoctorAdvice(request.getDoctorAdvice().trim());
        record.setCreatedAt(LocalDateTime.now());

        appointment.setStatus("IN_PROGRESS");
        appointmentRepository.save(appointment);

        return medicalRecordRepository.save(record);
    }

    // Chức năng: xử lý update.
    public MedicalRecord update(Long id, MedicalRecord request) {
        MedicalRecord existing = getById(id);

        existing.setDiagnosis(request.getDiagnosis());
        existing.setDoctorAdvice(request.getDoctorAdvice());

        return medicalRecordRepository.save(existing);
    }

    // Chức năng: xử lý add medicine to current medical record.
    public PrescriptionDetail addMedicineToCurrentMedicalRecord(
            String username,
            Long medicalRecordId,
            AddPrescriptionDetailRequest request) {
        MedicalRecord medicalRecord = getAuthorizedMedicalRecord(username, medicalRecordId);
        ensurePrescriptionEditable(medicalRecord);

        Medicine medicine = medicineRepository.findById(request.getMedicineId())
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy thuốc"));

        if (Boolean.FALSE.equals(medicine.getIsActive())) {
            throw AppException.of(HttpStatus.CONFLICT, "Thuốc đang ngừng hoạt động");
        }

        Integer stockQuantity = medicine.getStockQuantity();
        if (stockQuantity == null || stockQuantity < request.getQuantity()) {
            throw AppException.of(HttpStatus.CONFLICT, "Không đủ tồn kho cho thuốc này");
        }

        PrescriptionDetailId id = new PrescriptionDetailId();
        id.setMedicalRecordId(medicalRecord.getId());
        id.setMedicineId(medicine.getId());

        PrescriptionDetail detail = prescriptionDetailRepository.findById(id).orElseGet(PrescriptionDetail::new);
        detail.setMedicalRecord(medicalRecord);
        detail.setMedicine(medicine);

        int currentQuantity = java.util.Objects.requireNonNullElse(detail.getQuantity(), 0);
        int updatedQuantity = currentQuantity + request.getQuantity();
        if (stockQuantity < updatedQuantity) {
            throw AppException.of(HttpStatus.CONFLICT, "Không đủ tồn kho cho thuốc này");
        }

        detail.setQuantity(updatedQuantity);
        String usageInstructions = request.getUsageInstructions();
        detail.setUsageInstructions(usageInstructions == null || usageInstructions.isBlank()
                ? null
                : usageInstructions.trim());

        return prescriptionDetailRepository.save(detail);
    }

    // Chức năng: xử lý get prescription details by medical record.
    public List<PrescriptionDetail> getPrescriptionDetailsByMedicalRecord(String username, Long medicalRecordId) {
        MedicalRecord medicalRecord = getAuthorizedMedicalRecord(username, medicalRecordId);
        return prescriptionDetailRepository.findByMedicalRecord_Id(medicalRecord.getId());
    }

    // Chức năng: xử lý get prescription workspace.
    public PrescriptionWorkspaceResponse getPrescriptionWorkspace(String username, Long medicalRecordId) {
        MedicalRecord medicalRecord = getAuthorizedMedicalRecord(username, medicalRecordId);

        List<PrescriptionLineResponse> prescribedMedicines = prescriptionDetailRepository
                .findByMedicalRecord_Id(medicalRecordId)
                .stream()
                .map(this::toPrescriptionLineResponse)
                .toList();

        List<PrescriptionCatalogMedicineResponse> medicineCatalog = mapToMedicineCards(
                medicineRepository.findByIsActiveTrueOrderByMedicineNameAsc());

        BigDecimal totalAmount = prescribedMedicines.stream()
                .map(PrescriptionLineResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PrescriptionWorkspaceResponse(
                medicalRecord.getId(),
                prescribedMedicines,
                medicineCatalog,
                totalAmount);
    }

    // Chức năng: xử lý get medicine catalog for prescription by group.
    public PrescriptionMedicineCatalogResponse getMedicineCatalogForPrescription(
            String username,
            Long medicalRecordId,
            String group) {
        MedicalRecord medicalRecord = getAuthorizedMedicalRecord(username, medicalRecordId);
        ensurePrescriptionEditable(medicalRecord);

        List<PrescriptionCatalogMedicineResponse> allCards = mapToMedicineCards(
                medicineRepository.findByIsActiveTrueOrderByMedicineNameAsc());

        List<String> availableGroups = allCards.stream()
                .map(PrescriptionCatalogMedicineResponse::getPharmacologyGroup)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        String normalizedSelectedGroup = normalizeSelectedGroup(group, availableGroups);

        List<PrescriptionCatalogMedicineResponse> filteredCards = allCards;
        if (!GROUP_ALL.equalsIgnoreCase(normalizedSelectedGroup)) {
            filteredCards = allCards.stream()
                    .filter(card -> normalizedSelectedGroup.equalsIgnoreCase(card.getPharmacologyGroup()))
                    .toList();
        }

        List<String> groupsForResponse = new ArrayList<>(availableGroups);
        groupsForResponse.add(0, GROUP_ALL);

        return new PrescriptionMedicineCatalogResponse(
                normalizedSelectedGroup,
                groupsForResponse,
                filteredCards);
    }

    // Chức năng: xử lý quick add medicine to prescription.
    public PrescriptionWorkspaceResponse quickAddMedicineToPrescription(
            String username,
            Long medicalRecordId,
            Long medicineId) {
        MedicalRecord medicalRecord = getAuthorizedMedicalRecord(username, medicalRecordId);
        ensurePrescriptionEditable(medicalRecord);

        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy thuốc"));

        if (Boolean.FALSE.equals(medicine.getIsActive())) {
            throw AppException.of(HttpStatus.CONFLICT, "Thuốc đang ngừng hoạt động");
        }

        Integer stockQuantity = medicine.getStockQuantity();
        if (stockQuantity == null || stockQuantity <= 0) {
            throw AppException.of(HttpStatus.CONFLICT, "Thuốc đã hết trong kho");
        }

        PrescriptionDetailId id = new PrescriptionDetailId();
        id.setMedicalRecordId(medicalRecordId);
        id.setMedicineId(medicineId);

        PrescriptionDetail existingDetail = prescriptionDetailRepository.findById(id).orElse(null);
        if (existingDetail == null) {
            PrescriptionDetail detail = new PrescriptionDetail();
            detail.setId(id);
            detail.setMedicalRecord(medicalRecord);
            detail.setMedicine(medicine);
            detail.setQuantity(1);
            detail.setUsageInstructions(DEFAULT_USAGE_PLACEHOLDER);
            prescriptionDetailRepository.save(detail);
        }

        return getPrescriptionWorkspace(username, medicalRecordId);
    }

    // Chức năng: xử lý update prescription detail.
    public PrescriptionDetail updatePrescriptionDetail(
            String username,
            Long medicalRecordId,
            Long medicineId,
            UpdatePrescriptionDetailRequest request) {
        MedicalRecord medicalRecord = getAuthorizedMedicalRecord(username, medicalRecordId);
        ensurePrescriptionEditable(medicalRecord);

        PrescriptionDetailId id = new PrescriptionDetailId();
        id.setMedicalRecordId(medicalRecordId);
        id.setMedicineId(medicineId);

        PrescriptionDetail detail = prescriptionDetailRepository.findById(id)
                .orElseThrow(
                        () -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy chi tiết đơn thuốc"));

        Medicine medicine = detail.getMedicine();
        if (medicine == null) {
            throw AppException.of(HttpStatus.CONFLICT, "Thiếu dữ liệu thuốc trong chi tiết đơn thuốc");
        }

        int newQuantity = request.getQuantity();

        Integer currentStock = java.util.Objects.requireNonNullElse(medicine.getStockQuantity(), 0);
        if (newQuantity > currentStock) {
            throw AppException.of(
                    HttpStatus.CONFLICT,
                    "Số lượng vượt quá tồn kho hiện tại. Tồn còn lại: " + currentStock);
        }

        detail.setQuantity(newQuantity);
        String usageInstructions = request.getUsageInstructions();
        detail.setUsageInstructions(usageInstructions == null || usageInstructions.isBlank()
                ? null
                : usageInstructions.trim());

        return prescriptionDetailRepository.save(detail);
    }

    // Chức năng: xử lý autosave prescription detail.
    public PrescriptionAutosaveResponse autosavePrescriptionDetail(
            String username,
            Long medicalRecordId,
            Long medicineId,
            UpdatePrescriptionDetailRequest request) {
        PrescriptionDetail savedDetail = updatePrescriptionDetail(username, medicalRecordId, medicineId, request);

        PrescriptionLineResponse line = toPrescriptionLineResponse(savedDetail);
        BigDecimal totalAmount = calculatePrescriptionTotal(medicalRecordId);
        Integer remainingStock = null;
        if (savedDetail.getMedicine() != null) {
            Integer currentStock = java.util.Objects.requireNonNullElse(savedDetail.getMedicine().getStockQuantity(),
                    0);
            Integer prescribedQuantity = java.util.Objects.requireNonNullElse(savedDetail.getQuantity(), 0);
            remainingStock = Math.max(0, currentStock - prescribedQuantity);
        }

        return new PrescriptionAutosaveResponse(
                medicalRecordId,
                medicineId,
                savedDetail.getQuantity(),
                savedDetail.getUsageInstructions(),
                line.getLineTotal(),
                totalAmount,
                remainingStock);
    }

    // Chức năng: xử lý remove prescription detail.
    public PrescriptionWorkspaceResponse removePrescriptionDetail(
            String username,
            Long medicalRecordId,
            Long medicineId) {
        MedicalRecord medicalRecord = getAuthorizedMedicalRecord(username, medicalRecordId);
        ensurePrescriptionEditable(medicalRecord);

        PrescriptionDetailId id = new PrescriptionDetailId();
        id.setMedicalRecordId(medicalRecordId);
        id.setMedicineId(medicineId);

        PrescriptionDetail detail = prescriptionDetailRepository.findById(id)
                .orElseThrow(
                        () -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy chi tiết đơn thuốc"));

        prescriptionDetailRepository.delete(detail);
        return getPrescriptionWorkspace(username, medicalRecordId);
    }

    // Chức năng: xử lý lưu đơn thuốc và đồng bộ hóa với thu ngân.
    public PrescriptionWorkspaceResponse savePrescription(String username, Long medicalRecordId) {
        MedicalRecord medicalRecord = getAuthorizedMedicalRecord(username, medicalRecordId);
        ensurePrescriptionEditable(medicalRecord);

        List<PrescriptionDetail> details = prescriptionDetailRepository.findByMedicalRecord_Id(medicalRecordId);
        if (details.isEmpty()) {
            throw AppException.of(HttpStatus.CONFLICT, "Đơn thuốc đang trống");
        }

        try {
            invoiceService.aggregateInvoiceAmount(medicalRecordId);
        } catch (AppException ex) {
            if (ex.getStatus() != HttpStatus.CONFLICT) {
                throw ex;
            }
            LOGGER.warn("[SavePrescription] invoice already paid - skipping aggregate: {}", ex.getMessage());
        }
        return getPrescriptionWorkspace(username, medicalRecordId);
    }

    // Chức năng: xử lý hoàn thành bệnh án.
    public MedicalRecord completeMedicalRecord(String username, Long medicalRecordId) {
        MedicalRecord medicalRecord = getAuthorizedMedicalRecord(username, medicalRecordId);

        if (medicalRecord.getCompletedAt() == null) {
            medicalRecord.setCompletedAt(LocalDateTime.now());
            medicalRecordRepository.save(medicalRecord);
        }

        // Tính hóa đơn để biết còn phần dịch vụ nào phải chuyển qua thu ngân.
        var invoice = invoiceService.aggregateInvoiceAmount(medicalRecordId);
        Appointment appointment = medicalRecord.getAppointment();

        if (appointment != null) {
            BigDecimal remainingAmount = invoice == null || invoice.getRemainingAmount() == null
                    ? BigDecimal.ZERO
                    : invoice.getRemainingAmount();
            if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
                appointment.setStatus(AppointmentStatus.WAITING_CASHIER);
                appointment.setPaymentStatus(com.example.demo.constants.PaymentStatus.PARTIALLY_PAID);
            } else {
                appointment.setStatus(AppointmentStatus.COMPLETED);
                appointment.setPaymentStatus(com.example.demo.constants.PaymentStatus.FULLY_PAID);
            }
            appointmentRepository.save(appointment);
        }

        return medicalRecord;
    }

    // Chức năng: xử lý delete.
    public void delete(Long id) {
        MedicalRecord existing = getById(id);
        medicalRecordRepository.delete(existing);
    }

    // Chức năng: xử lý lấy các bệnh án của tôi.
    public List<MedicalRecord> getMyMedicalRecords(String username) {
        Patient patient = patientService.getPatientFromUsername(username);
        return medicalRecordRepository.findByAppointment_Patient_Id(patient.getId());
    }

    // Chức năng: xử lý lấy tóm tắt lịch sử bệnh án cho bác sĩ.
    public DoctorPatientHistoryResponse getPatientHistorySummaryForDoctor(String username, Long appointmentId) {
        Appointment currentAppointment = getAuthorizedDoctorAppointment(username, appointmentId);
        Patient patient = currentAppointment.getPatient();

        List<MedicalRecord> previousRecords = medicalRecordRepository.findByAppointment_Patient_Id(patient.getId())
                .stream()
                .filter(record -> record.getAppointment() != null
                        && !record.getAppointment().getId().equals(currentAppointment.getId()))
                .sorted(Comparator.comparing(
                        (MedicalRecord record) -> record.getAppointment().getAppointmentTime(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        if (previousRecords.isEmpty()) {
            return new DoctorPatientHistoryResponse(
                    patient.getId(),
                    patient.getFullName(),
                    "Chua co lich su kham benh",
                    List.of());
        }

        List<DoctorPatientHistoryRowResponse> histories = previousRecords.stream()
                .map(this::toHistoryRowResponse)
                .toList();

        return new DoctorPatientHistoryResponse(
                patient.getId(),
                patient.getFullName(),
                "OK",
                histories);
    }

    // Chức năng: xử lý lấy chi tiết lịch sử bệnh án cho bác sĩ.
    public DoctorPatientHistoryDetailResponse getPatientHistoryDetailForDoctor(
            String username,
            Long appointmentId,
            Long medicalRecordId) {
        Appointment currentAppointment = getAuthorizedDoctorAppointment(username, appointmentId);

        MedicalRecord medicalRecord = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy bệnh án"));

        if (medicalRecord.getAppointment() == null
                || medicalRecord.getAppointment().getPatient() == null
                || currentAppointment.getPatient() == null
                || !medicalRecord.getAppointment().getPatient().getId()
                        .equals(currentAppointment.getPatient().getId())) {
            throw AppException.of(HttpStatus.FORBIDDEN, "Bệnh án không thuộc về bệnh nhân này");
        }

        List<DoctorPatientPrescriptionItemResponse> prescriptions = prescriptionDetailRepository
                .findByMedicalRecord_Id(medicalRecordId)
                .stream()
                .map(detail -> new DoctorPatientPrescriptionItemResponse(
                        detail.getMedicine() == null ? null : detail.getMedicine().getId(),
                        detail.getMedicine() == null ? null : detail.getMedicine().getMedicineName(),
                        detail.getQuantity(),
                        detail.getUsageInstructions()))
                .toList();

        List<DoctorPatientServiceItemResponse> services = medicalRecordServiceDetailRepository
                .findByMedicalRecord_Id(medicalRecordId)
                .stream()
                .map(this::toServiceItemResponse)
                .toList();

        Appointment oldAppointment = medicalRecord.getAppointment();
        return new DoctorPatientHistoryDetailResponse(
                medicalRecord.getId(),
                oldAppointment.getId(),
                oldAppointment.getAppointmentTime(),
                oldAppointment.getDoctor() == null ? null : oldAppointment.getDoctor().getUsername(),
                medicalRecord.getDiagnosis(),
                medicalRecord.getDoctorAdvice(),
                oldAppointment.getSymptoms(),
                prescriptions,
                services);
    }

    // Chức năng: xử lý lấy cuộc hẹn bác sĩ được ủy quyền.
    private Appointment getAuthorizedDoctorAppointment(String username, Long appointmentId) {
        User doctor = userRepository.findByUsernameAndRole(username, Role.DOCTOR)
                .orElseThrow(
                        () -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản bác sĩ"));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn"));

        if (appointment.getPatient() == null) {
            throw AppException.of(HttpStatus.CONFLICT, "Lịch hẹn không gắn với bệnh nhân");
        }

        if (appointment.getDoctor() == null || !doctor.getId().equals(appointment.getDoctor().getId())) {
            throw AppException.of(HttpStatus.FORBIDDEN, "Bạn chưa được phân công cho lịch hẹn này");
        }

        return appointment;
    }

    // Chức năng: xử lý ánh xạ tới phản hồi lịch sử.
    private DoctorPatientHistoryRowResponse toHistoryRowResponse(MedicalRecord medicalRecord) {
        Appointment appointment = medicalRecord.getAppointment();
        List<String> usedMedicines = prescriptionDetailRepository.findByMedicalRecord_Id(medicalRecord.getId())
                .stream()
                .map(detail -> detail.getMedicine() == null ? null : detail.getMedicine().getMedicineName())
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();

        return new DoctorPatientHistoryRowResponse(
                medicalRecord.getId(),
                appointment.getId(),
                appointment.getAppointmentTime(),
                appointment.getDoctor() == null ? null : appointment.getDoctor().getUsername(),
                medicalRecord.getDiagnosis(),
                usedMedicines);
    }

    // Chức năng: xử lý ánh xạ tới phản hồi mục dịch vụ.
    private DoctorPatientServiceItemResponse toServiceItemResponse(MedicalRecordServiceDetail detail) {
        return new DoctorPatientServiceItemResponse(
                detail.getService() == null ? null : detail.getService().getId(),
                detail.getService() == null ? null : detail.getService().getServiceName(),
                detail.getQuantity(),
                detail.getActualPrice(),
                detail.getResultNote());
    }

    // Chức năng: xử lý thông tin chi tiết về đơn thuốc tương ứng với phản hồi theo
    // dòng.
    private PrescriptionLineResponse toPrescriptionLineResponse(PrescriptionDetail detail) {
        BigDecimal price = BigDecimal.ZERO;
        if (detail.getMedicine() != null && detail.getMedicine().getSellingPrice() != null) {
            price = detail.getMedicine().getSellingPrice();
        }

        Integer quantity = java.util.Objects.requireNonNullElse(detail.getQuantity(), 0);
        BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity.longValue()));

        return new PrescriptionLineResponse(
                detail.getMedicine() == null ? null : detail.getMedicine().getId(),
                detail.getMedicine() == null ? null : detail.getMedicine().getMedicineName(),
                detail.getMedicine() == null ? null : detail.getMedicine().getUnit(),
                detail.getQuantity(),
                detail.getUsageInstructions(),
                price,
                lineTotal);
    }

    // Chức năng: xử lý đảm bảo bác sĩ có thể cập nhật kết quả dịch vụ từ phòng gốc
    // hoặc phòng hỗ trợ được chỉ định.
    private void ensureDoctorCanUpdateCrossRoomServiceResult(User doctor, MedicalRecord medicalRecord) {
        Appointment appointment = medicalRecord.getAppointment();
        if (appointment == null) {
            throw AppException.of(HttpStatus.CONFLICT, "Bệnh án không gắn với lịch hẹn");
        }

        boolean isOriginalDoctor = appointment.getDoctor() != null
                && doctor.getId().equals(appointment.getDoctor().getId());
        boolean isDoctorAssignedToAnyRoom = !roomRepository.findByCurrentDoctor_Id(doctor.getId()).isEmpty();

        if (!isOriginalDoctor && !isDoctorAssignedToAnyRoom) {
            throw AppException.of(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền cập nhật kết quả dịch vụ liên phòng");
        }
    }

    // Chức năng: xử lý chuẩn hóa ghi chú kết quả tùy chọn.
    private String normalizeOptionalResultNote(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Chức năng: xử lý chuẩn hóa danh sách thuốc thành các thẻ đơn thuốc.
    private List<PrescriptionCatalogMedicineResponse> mapToMedicineCards(List<Medicine> medicines) {
        return medicines.stream()
                .map(medicine -> {
                    Integer stock = medicine.getStockQuantity();
                    boolean inStock = stock != null && stock > 0;
                    return new PrescriptionCatalogMedicineResponse(
                            medicine.getId(),
                            medicine.getMedicineName(),
                            classifyPharmacologyGroup(medicine.getMedicineName()),
                            extractConcentration(medicine.getMedicineName()),
                            medicine.getUnit(),
                            medicine.getSellingPrice(),
                            stock,
                            inStock,
                            inStock ? null : "Thuốc đã hết trong kho");
                })
                .toList();
    }

    // Chức năng: xử lý chuẩn hóa nhóm đã chọn.
    private String normalizeSelectedGroup(String group, List<String> availableGroups) {
        if (group == null || group.isBlank()) {
            return GROUP_ALL;
        }

        String selected = group.trim();
        if (GROUP_ALL.equalsIgnoreCase(selected)) {
            return GROUP_ALL;
        }

        return availableGroups.stream()
                .filter(item -> item.equalsIgnoreCase(selected))
                .findFirst()
                .orElse(GROUP_ALL);
    }

    // Chức năng: xử lý phân loại nhóm dược lý.
    private String classifyPharmacologyGroup(String medicineName) {
        if (medicineName == null || medicineName.isBlank()) {
            return GROUP_OTHER;
        }

        String lowerName = medicineName.toLowerCase(Locale.ROOT);

        if (containsAny(lowerName, "cillin", "cef", "mycin", "floxacin", "doxy", "amox", "khang sinh")) {
            return GROUP_ANTIBIOTIC;
        }
        if (containsAny(lowerName, "paracetamol", "ibuprofen", "diclofenac", "acetaminophen", "ha sot", "giam dau")) {
            return GROUP_PAIN_FEVER;
        }
        if (containsAny(lowerName, "ambroxol", "acetylcysteine", "dextromethorphan", "bromhexine", "thuoc ho")) {
            return GROUP_COUGH;
        }
        if (containsAny(lowerName, "loratadin", "cetirizin", "fexofenadin", "khang histamin", "di ung")) {
            return GROUP_ALLERGY;
        }
        if (containsAny(lowerName, "omeprazole", "pantoprazole", "smecta", "men tieu hoa", "tieu hoa")) {
            return GROUP_DIGESTIVE;
        }

        return GROUP_OTHER;
    }

    // Chức năng: xử lý trích xuất nồng độ từ tên thuốc.
    private String extractConcentration(String medicineName) {
        if (medicineName == null || medicineName.isBlank()) {
            return "Không có";
        }

        Matcher matcher = CONCENTRATION_PATTERN.matcher(medicineName);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "Không có";
    }

    // Chức năng: xử lý kiểm tra xem văn bản có chứa bất kỳ từ khóa nào không.
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    // Chức năng: xử lý tính tổng đơn thuốc.
    private BigDecimal calculatePrescriptionTotal(Long medicalRecordId) {
        return prescriptionDetailRepository.findByMedicalRecord_Id(medicalRecordId)
                .stream()
                .map(this::toPrescriptionLineResponse)
                .map(PrescriptionLineResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Chức năng: xử lý lấy bệnh án được ủy quyền.
    private MedicalRecord getAuthorizedMedicalRecord(String username, Long medicalRecordId) {
        User doctor = userRepository.findByUsernameAndRole(username, Role.DOCTOR)
                .orElseThrow(
                        () -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản bác sĩ"));

        MedicalRecord medicalRecord = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy bệnh án"));

        if (medicalRecord.getAppointment() == null
                || medicalRecord.getAppointment().getDoctor() == null
                || !doctor.getId().equals(medicalRecord.getAppointment().getDoctor().getId())) {
            throw AppException.of(HttpStatus.FORBIDDEN, "Bạn chưa được phân công cho bệnh án này");
        }

        return medicalRecord;
    }

    // Chức năng: xử lý đảm bảo đơn thuốc có thể chỉnh sửa được.
    private void ensurePrescriptionEditable(MedicalRecord medicalRecord) {
        if (STATUS_COMPLETED.equalsIgnoreCase(medicalRecord.getAppointment().getStatus())) {
            throw AppException.of(HttpStatus.CONFLICT,
                    "Không thể chỉnh sửa đơn thuốc khi lịch hẹn đã hoàn tất");
        }
    }

    // Chức năng: tự động thêm các thuốc gợi ý dựa trên DiagnosisTemplate và khoảng
    // tuổi
    @Transactional
    public PrescriptionWorkspaceResponse autoPopulatePrescriptionsFromDiagnosis(
            String username,
            Long medicalRecordId,
            Long diagnosisTemplateId) {
        MedicalRecord medicalRecord = getAuthorizedMedicalRecord(username, medicalRecordId);
        ensurePrescriptionEditable(medicalRecord);

        Appointment appointment = medicalRecord.getAppointment();
        if (appointment == null || appointment.getPatient() == null) {
            throw AppException.of(HttpStatus.CONFLICT, "Bệnh án không gắn với bệnh nhân");
        }

        Integer age = 0;
        if (appointment.getPatient().getDateOfBirth() != null) {
            age = Period.between(appointment.getPatient().getDateOfBirth(), LocalDate.now()).getYears();
        }

        LOGGER.debug("[AutoPopulate] appointmentId={}, patientId={}, age={}, diagnosisTemplateId={}",
                appointment.getId(), appointment.getPatient().getId(), age, diagnosisTemplateId);
        LOGGER.info("[AutoPopulate] start medicalRecordId={}, diagnosisTemplateId={}, age={}",
                medicalRecordId, diagnosisTemplateId, age);

        List<com.example.demo.entity.DiagnosisMedicineRule> rules = diagnosisMedicineRuleRepository
                .findByDiagnosisTemplate_IdAndMinAgeLessThanEqualAndMaxAgeGreaterThanEqualOrderByMedicine_MedicineNameAsc(
                        diagnosisTemplateId,
                        age,
                        age);

        LOGGER.debug("[AutoPopulate] found {} rules for diagnosisId={}", rules.size(), diagnosisTemplateId);
        LOGGER.info("[AutoPopulate] rulesFound={} for diagnosisTemplateId={}", rules.size(), diagnosisTemplateId);

        if (rules.isEmpty()) {
            List<Medicine> fallbackMedicines = medicineRepository.findByIsActiveTrueOrderByMedicineNameAsc();
            if (fallbackMedicines.isEmpty()) {
                fallbackMedicines = medicineRepository.findAll();
            }
            List<Medicine> candidateList = resolveMedicinesForDiagnosis(diagnosisTemplateId, fallbackMedicines);
            if (candidateList.isEmpty()) {
                LOGGER.debug("[AutoPopulate] no fallback medicines found for medicalRecordId={}", medicalRecordId);
                LOGGER.info("[AutoPopulate] no fallback medicines for medicalRecordId={}", medicalRecordId);
                return getPrescriptionWorkspace(username, medicalRecordId);
            }
            prescriptionDetailRepository.deleteByMedicalRecord_Id(medicalRecordId);
            int count = Math.min(3, candidateList.size());
            int created = 0;
            for (int i = 0; i < count; i++) {
                Medicine med = candidateList.get(i);
                PrescriptionDetailId id = new PrescriptionDetailId();
                id.setMedicalRecordId(medicalRecordId);
                id.setMedicineId(med.getId());

                PrescriptionDetail detail = new PrescriptionDetail();
                detail.setId(id);
                detail.setMedicalRecord(medicalRecord);
                detail.setMedicine(med);
                detail.setQuantity(1);
                detail.setUsageInstructions(null);
                prescriptionDetailRepository.save(detail);
                created++;
            }

            LOGGER.info("[AutoPopulate] skip invoice aggregate for fallback suggestions");
            LOGGER.info("[AutoPopulate] fallback prescriptions created={} for medicalRecordId={}", created,
                    medicalRecordId);
            return getPrescriptionWorkspace(username, medicalRecordId);
        }

        prescriptionDetailRepository.deleteByMedicalRecord_Id(medicalRecordId);
        int created = 0;

        for (com.example.demo.entity.DiagnosisMedicineRule rule : rules) {
            if (rule.getMedicine() == null)
                continue;

            Long medicineId = rule.getMedicine().getId();
            LOGGER.debug("[AutoPopulate] processing rule for medicineId={}, defaultQty={}", medicineId,
                    rule.getDefaultQuantity());
            PrescriptionDetailId id = new PrescriptionDetailId();
            id.setMedicalRecordId(medicalRecordId);
            id.setMedicineId(medicineId);

            PrescriptionDetail detail = prescriptionDetailRepository.findById(id).orElseGet(PrescriptionDetail::new);
            detail.setMedicalRecord(medicalRecord);
            detail.setMedicine(rule.getMedicine());

            Integer existingQty = java.util.Objects.requireNonNullElse(detail.getQuantity(), 0);
            int qtyToSet = java.util.Objects.requireNonNullElse(rule.getDefaultQuantity(), 1);
            if (existingQty == 0) {
                detail.setQuantity(qtyToSet);
            }

            String usage = rule.getDefaultUsage();
            detail.setUsageInstructions(usage == null || usage.isBlank() ? null : usage.trim());

            prescriptionDetailRepository.save(detail);
            LOGGER.debug("[AutoPopulate] saved prescription detail for medicalRecordId={}, medicineId={}, qty={}",
                    medicalRecordId, medicineId, detail.getQuantity());
            created++;
        }

        LOGGER.info("[AutoPopulate] skip invoice aggregate for rule suggestions");
        LOGGER.info("[AutoPopulate] rules prescriptions created={} for medicalRecordId={}", created, medicalRecordId);
        LOGGER.debug("[AutoPopulate] returning workspace for medicalRecordId={}", medicalRecordId);
        return getPrescriptionWorkspace(username, medicalRecordId);
    }

    private List<String> resolvePreferredGroups(Long diagnosisTemplateId) {
        String categoryName = diagnosisTemplateRepository.findById(diagnosisTemplateId)
                .map(template -> template.getCategory() == null ? null : template.getCategory().getName())
                .orElse(null);

        String normalized = normalizeCategory(categoryName);
        List<String> groups = new ArrayList<>();

        if (normalized.contains("ho") || normalized.contains("ho hap") || normalized.contains("phoi")) {
            groups.add(GROUP_ANTIBIOTIC);
            groups.add(GROUP_PAIN_FEVER);
            groups.add(GROUP_COUGH);
        } else if (normalized.contains("tieu") || normalized.contains("da day")) {
            groups.add(GROUP_DIGESTIVE);
            groups.add(GROUP_PAIN_FEVER);
        } else if (normalized.contains("da") || normalized.contains("da lieu")) {
            groups.add(GROUP_ALLERGY);
            groups.add(GROUP_OTHER);
        } else if (normalized.contains("than kinh")) {
            groups.add(GROUP_PAIN_FEVER);
            groups.add(GROUP_OTHER);
        } else {
            groups.add(GROUP_OTHER);
        }

        return groups;
    }

    private String normalizeCategory(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(trimmed, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return withoutAccents.replace('-', ' ');
    }

    private List<Medicine> resolveMedicinesForDiagnosis(Long diagnosisTemplateId, List<Medicine> fallbackMedicines) {
        String diagnosisName = diagnosisTemplateRepository.findById(diagnosisTemplateId)
                .map(template -> template.getDiagnosisName())
                .orElse(null);

        Map<String, List<String>> diagnosisMedicineMap = buildDiagnosisMedicineMap();
        List<String> preferredByDiagnosis = diagnosisMedicineMap.get(normalizeCategory(diagnosisName));
        LinkedHashSet<Medicine> candidates = new LinkedHashSet<>();

        if (preferredByDiagnosis != null) {
            for (String medName : preferredByDiagnosis) {
                String key = normalizeCategory(medName);
                for (Medicine med : fallbackMedicines) {
                    if (normalizeCategory(med.getMedicineName()).equals(key)) {
                        candidates.add(med);
                        break;
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            List<String> preferredGroups = resolvePreferredGroups(diagnosisTemplateId);
            for (String group : preferredGroups) {
                for (Medicine med : fallbackMedicines) {
                    if (group.equalsIgnoreCase(classifyPharmacologyGroup(med.getMedicineName()))) {
                        candidates.add(med);
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            candidates.addAll(fallbackMedicines);
        }

        return new ArrayList<>(candidates);
    }

    private Map<String, List<String>> buildDiagnosisMedicineMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();

        map.put(normalizeCategory("Viêm họng cấp"), List.of(
                "Amoxicillin 500mg",
                "Dextromethorphan 15mg",
                "Paracetamol 500mg"));
        map.put(normalizeCategory("Viêm mũi dị ứng"), List.of(
                "Cetirizine 10mg",
                "Loratadine 10mg",
                "Paracetamol 500mg"));
        map.put(normalizeCategory("Viêm phế quản"), List.of(
                "Azithromycin 500mg",
                "Ambroxol 30mg",
                "Paracetamol 500mg"));
        map.put(normalizeCategory("Hen phế quản"), List.of(
                "Ambroxol 30mg",
                "Dextromethorphan 15mg",
                "Paracetamol 500mg"));
        map.put(normalizeCategory("Viêm phổi nhẹ"), List.of(
                "Cefuroxime 500mg",
                "Acetylcysteine 200mg",
                "Paracetamol 500mg"));

        map.put(normalizeCategory("Viêm dạ dày"), List.of(
                "Omeprazole 20mg",
                "Pantoprazole 40mg",
                "Men tieu hoa"));
        map.put(normalizeCategory("Trào ngược dạ dày thực quản"), List.of(
                "Pantoprazole 40mg",
                "Omeprazole 20mg",
                "Men tieu hoa"));
        map.put(normalizeCategory("Viêm đại tràng"), List.of(
                "Smecta 3g",
                "Men tieu hoa",
                "Paracetamol 500mg"));
        map.put(normalizeCategory("Rối loạn tiêu hóa"), List.of(
                "Men tieu hoa",
                "Smecta 3g",
                "Paracetamol 500mg"));
        map.put(normalizeCategory("Nhiễm khuẩn đường ruột"), List.of(
                "Amoxicillin 500mg",
                "Smecta 3g",
                "Men tieu hoa"));

        map.put(normalizeCategory("Đau đầu căng thẳng"), List.of(
                "Paracetamol 500mg",
                "Ibuprofen 400mg",
                "Vitamin B Complex"));
        map.put(normalizeCategory("Migraine"), List.of(
                "Ibuprofen 400mg",
                "Diclofenac 50mg",
                "Vitamin B Complex"));
        map.put(normalizeCategory("Rối loạn giấc ngủ"), List.of(
                "Vitamin B Complex",
                "Vitamin C 500mg"));
        map.put(normalizeCategory("Chóng mặt tiền đình"), List.of(
                "Vitamin B Complex",
                "Paracetamol 500mg"));
        map.put(normalizeCategory("Đau dây thần kinh tọa"), List.of(
                "Diclofenac 50mg",
                "Ibuprofen 400mg",
                "Vitamin B Complex"));

        map.put(normalizeCategory("Sốt siêu vi"), List.of(
                "Paracetamol 500mg",
                "Ibuprofen 400mg",
                "Vitamin C 500mg"));
        map.put(normalizeCategory("Suy nhược cơ thể"), List.of(
                "Vitamin C 500mg",
                "Vitamin B Complex",
                "Paracetamol 500mg"));
        map.put(normalizeCategory("Mệt mỏi do stress"), List.of(
                "Vitamin B Complex",
                "Vitamin C 500mg"));
        map.put(normalizeCategory("Thiếu máu nhẹ"), List.of(
                "Vitamin B Complex",
                "Vitamin C 500mg"));
        map.put(normalizeCategory("Nhiễm trùng chưa rõ ổ"), List.of(
                "Amoxicillin 500mg",
                "Paracetamol 500mg",
                "Vitamin C 500mg"));

        map.put(normalizeCategory("Viêm da dị ứng"), List.of(
                "Loratadine 10mg",
                "Cetirizine 10mg",
                "Hydrocortisone 1%"));
        map.put(normalizeCategory("Mề đay"), List.of(
                "Cetirizine 10mg",
                "Loratadine 10mg",
                "Fexofenadine 180mg"));
        map.put(normalizeCategory("Nấm da"), List.of(
                "Clotrimazole 1%",
                "Hydrocortisone 1%",
                "Vitamin C 500mg"));
        map.put(normalizeCategory("Viêm nang lông"), List.of(
                "Amoxicillin 500mg",
                "Hydrocortisone 1%",
                "Paracetamol 500mg"));
        map.put(normalizeCategory("Chàm"), List.of(
                "Hydrocortisone 1%",
                "Cetirizine 10mg",
                "Vitamin C 500mg"));

        map.put(normalizeCategory("Chẩn đoán chưa xác định"), List.of(
                "Paracetamol 500mg",
                "Ibuprofen 400mg",
                "Vitamin C 500mg"));
        map.put(normalizeCategory("Rối loạn chức năng"), List.of(
                "Vitamin B Complex",
                "Vitamin C 500mg"));
        map.put(normalizeCategory("Khám tổng quát"), List.of(
                "Vitamin C 500mg",
                "Vitamin B Complex"));

        return map;
    }
}
