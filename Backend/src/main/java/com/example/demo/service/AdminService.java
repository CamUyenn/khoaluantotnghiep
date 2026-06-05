package com.example.demo.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.demo.exception.AppException;

import com.example.demo.dto.AdminCreateUserRequest;
import com.example.demo.dto.AdminMedicalServiceCreateRequest;
import com.example.demo.dto.AdminMedicalServiceResponse;
import com.example.demo.dto.AdminMedicineCreateRequest;
import com.example.demo.dto.AdminMedicineResponse;
import com.example.demo.dto.AdminMedicineUpdateRequest;
import com.example.demo.dto.AdminRevenueChartPointResponse;
import com.example.demo.dto.AdminRevenueReportItemResponse;
import com.example.demo.dto.AdminRevenueReportResponse;
import com.example.demo.dto.AdminRoomResponse;
import com.example.demo.dto.AdminRoomScheduleRequest;
import com.example.demo.dto.AdminRoomScheduleResponse;
import com.example.demo.dto.AdminUpdateUserRequest;
import com.example.demo.dto.AdminUserResponse;
import com.example.demo.dto.DashboardResponse;
import com.example.demo.dto.TopDoctorDTO;
import com.example.demo.entity.Appointment;
import com.example.demo.entity.Invoice;
import com.example.demo.entity.MedicalRecord;
import com.example.demo.entity.MedicalService;
import com.example.demo.entity.Medicine;
import com.example.demo.entity.Patient;
import com.example.demo.entity.PrescriptionDetail;
import com.example.demo.entity.Role;
import com.example.demo.entity.Room;
import com.example.demo.entity.RoomSchedule;
import com.example.demo.entity.User;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.InvoiceRepository;
import com.example.demo.repository.MedicalRecordRepository;
import com.example.demo.repository.MedicalRecordServiceDetailRepository;
import com.example.demo.repository.MedicalServiceRepository;
import com.example.demo.repository.MedicineRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.PrescriptionDetailRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.RoomScheduleRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AdminService {

    private static final String GROUP_DAY = "DAY";
    private static final String GROUP_MONTH = "MONTH";
    private static final String GROUP_YEAR = "YEAR";

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter YEAR_FORMAT = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoomRepository roomRepository;
    private final RoomScheduleRepository roomScheduleRepository;
    private final MedicineRepository medicineRepository;
    private final MedicalServiceRepository medicalServiceRepository;
    private final InvoiceRepository invoiceRepository;
    private final MedicalRecordServiceDetailRepository medicalRecordServiceDetailRepository;
    private final PrescriptionDetailRepository prescriptionDetailRepository;

    // Chức năng: xử lý get dashboard.
    public DashboardResponse getDashboard() {
        DashboardResponse res = new DashboardResponse();

        // Tong quan
        res.setTotalPatients(patientRepository.count());
        res.setTotalDoctors(userRepository.countByRole(Role.DOCTOR));
        res.setTotalAppointments(appointmentRepository.count());
        res.setTotalMedicalRecords(medicalRecordRepository.count());

        // Hom nay
        res.setTodayAppointments(
                appointmentRepository.countByAppointmentTimeBetween(
                        LocalDate.now().atStartOfDay(),
                        LocalDate.now().plusDays(1).atStartOfDay()));

        // Theo trang thai
        res.setPendingAppointments(appointmentRepository.countByStatus("PENDING"));
        res.setConfirmedAppointments(appointmentRepository.countByStatus("COMPLETED"));
        res.setCancelledAppointments(appointmentRepository.countByStatus("CANCELLED"));

        // Thong ke theo thang
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();
        res.setThisMonthAppointments(appointmentRepository.countByMonth(month, year));

        // Top bac si co nhieu lich nhat
        List<Object[]> result = appointmentRepository.findTopDoctors();
        List<TopDoctorDTO> topDoctors = result.stream()
                .map(obj -> new TopDoctorDTO((Long) obj[0], (Long) obj[1]))
                .toList();

        res.setTopDoctors(topDoctors);
        return res;
    }

    // Chức năng: xử lý Nhận báo cáo tài chính quản trị với các bộ lọc và dữ liệu
    // biểu đồ.
    public AdminRevenueReportResponse getRevenueReport(
            LocalDateTime startTime,
            LocalDateTime endTime,
            String serviceFilter,
            String medicineFilter,
            String groupBy) {
        LocalDateTime resolvedStart = startTime;
        LocalDateTime resolvedEnd = endTime;
        if (resolvedStart == null || resolvedEnd == null) {
            if (resolvedStart == null) {
                resolvedStart = LocalDate.now().atStartOfDay();
            }
            if (resolvedEnd == null) {
                resolvedEnd = LocalDateTime.now();
            }
        }

        if (resolvedStart.isAfter(resolvedEnd)) {
            throw AppException.of(HttpStatus.BAD_REQUEST,
                    "Thời gian bắt đầu phải trước thời gian kết thúc");
        }

        String normalizedGroupBy = normalizeGroupBy(groupBy);
        String normalizedServiceFilter = normalizeFilter(serviceFilter);
        String normalizedMedicineFilter = normalizeFilter(medicineFilter);

        List<Invoice> paidInvoices = invoiceRepository
                .findByIsPaidAndPaidAtBetweenOrderByPaidAtDesc(
                        Boolean.TRUE,
                        resolvedStart,
                        resolvedEnd);

        List<AdminRevenueReportItemResponse> items = paidInvoices.stream()
                .map(this::toRevenueReportItem)
                .filter(item -> matchesRevenueFilters(item, normalizedServiceFilter, normalizedMedicineFilter))
                .toList();

        if (items.isEmpty()) {
            return new AdminRevenueReportResponse(
                    resolvedStart,
                    resolvedEnd,
                    normalizedGroupBy,
                    serviceFilter,
                    medicineFilter,
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    List.of(),
                    List.of(),
                    "Không có du lieu trong khoang thoi gian nay");
        }

        BigDecimal totalRevenue = items.stream()
                .map(AdminRevenueReportItemResponse::getGrandTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalServiceRevenue = items.stream()
                .map(AdminRevenueReportItemResponse::getTotalServiceFee)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AdminRevenueChartPointResponse> chart = buildRevenueChart(items, normalizedGroupBy);

        return new AdminRevenueReportResponse(
                resolvedStart,
                resolvedEnd,
                normalizedGroupBy,
                serviceFilter,
                medicineFilter,
                items.size(),
                totalRevenue,
                totalServiceRevenue,
                items,
                chart,
                "OK");
    }

    // Chức năng: xử lý xuất báo cáo doanh thu quản trị sang CSV/PDF.
    public byte[] exportRevenueReport(
            LocalDateTime startTime,
            LocalDateTime endTime,
            String serviceFilter,
            String medicineFilter,
            String groupBy,
            String format) {
        AdminRevenueReportResponse report = getRevenueReport(startTime, endTime, serviceFilter, medicineFilter,
                groupBy);
        String normalizedFormat = format == null ? "CSV" : format.trim().toUpperCase(Locale.ROOT);

        if ("PDF".equals(normalizedFormat)) {
            return exportRevenueReportPdf(report);
        }
        if (!"CSV".equals(normalizedFormat)) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Định dạng phải là CSV hoặc PDF");
        }
        return exportRevenueReportCsv(report);
    }

    // Chức năng: xử lý lấy danh sách tất cả user.
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toAdminUserResponse)
                .toList();
    }

    // Chức năng: xử lý tạo user.
    public AdminUserResponse createUser(AdminCreateUserRequest request) {
        String username = request.getUsername().trim();
        String phoneNumber = normalizeOptionalText(request.getPhoneNumber());
        String email = normalizeOptionalEmail(request.getEmail());

        if (userRepository.existsByUsername(username)) {
            throw AppException.of(HttpStatus.CONFLICT, "Tên đăng nhập đã tồn tại");
        }

        if (phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber)) {
            throw AppException.of(HttpStatus.CONFLICT, "Số điện thoại đã tồn tại");
        }

        if (email != null && userRepository.existsByEmailIgnoreCase(email)) {
            throw AppException.of(HttpStatus.CONFLICT, "Email đã tồn tại");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(normalizeOptionalText(request.getFullName()));
        user.setPhoneNumber(phoneNumber);
        user.setEmail(email);
        user.setRole(request.getRole());
        Boolean active = request.getIsActive();
        user.setIsActive(active == null ? Boolean.TRUE : active);

        User saved = userRepository.save(user);
        return toAdminUserResponse(saved);
    }

    // Chức năng: xử lý cập nhật user.
    public AdminUserResponse updateUser(Long userId, AdminUpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String username = request.getUsername().trim();
            if (!username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
                throw AppException.of(HttpStatus.CONFLICT, "Tên đăng nhập đã tồn tại");
            }
            user.setUsername(username);
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getFullName() != null) {
            user.setFullName(normalizeOptionalText(request.getFullName()));
        }

        if (request.getPhoneNumber() != null) {
            String phoneNumber = normalizeOptionalText(request.getPhoneNumber());
            if (phoneNumber != null && !phoneNumber.equals(user.getPhoneNumber())
                    && userRepository.existsByPhoneNumber(phoneNumber)) {
                throw AppException.of(HttpStatus.CONFLICT, "Số điện thoại đã tồn tại");
            }
            user.setPhoneNumber(phoneNumber);
        }

        if (request.getEmail() != null) {
            String email = normalizeOptionalEmail(request.getEmail());
            boolean emailChanged = (user.getEmail() == null && email != null)
                    || (user.getEmail() != null && (email == null || !user.getEmail().equalsIgnoreCase(email)));
            if (email != null && emailChanged && userRepository.existsByEmailIgnoreCase(email)) {
                throw AppException.of(HttpStatus.CONFLICT, "Email đã tồn tại");
            }
            user.setEmail(email);
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        User saved = userRepository.save(user);
        return toAdminUserResponse(saved);
    }

    // Chức năng: xử lý xóa user.
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        user.setIsActive(false);
        userRepository.save(user);
    }

    // Chức năng: xử lý lấy danh sách tất cả phòng khám.
    public List<AdminRoomResponse> getAllRooms() {
        return roomRepository.findAllByOrderByRoomNameAsc().stream()
                .map(this::toAdminRoomResponse)
                .toList();
    }

    // Chức năng: xử lý tạo phòng khám.
    public AdminRoomResponse createRoom(String roomName) {
        String normalizedRoomName = normalizeRequiredText(roomName, "Tên phòng là bắt buộc");

        if (roomRepository.existsByRoomNameIgnoreCase(normalizedRoomName)) {
            throw AppException.of(HttpStatus.CONFLICT, "Tên phòng đã tồn tại");
        }

        Room room = new Room();
        room.setRoomName(normalizedRoomName);
        Room saved = roomRepository.save(room);
        return toAdminRoomResponse(saved);
    }

    // Chức năng: xử lý cập nhật phòng khám.
    public AdminRoomResponse updateRoom(Long roomId, String roomName) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy phòng"));

        String normalizedRoomName = normalizeRequiredText(roomName, "Tên phòng là bắt buộc");

        roomRepository.findByRoomNameIgnoreCase(normalizedRoomName)
                .filter(existing -> !Objects.equals(existing.getId(), room.getId()))
                .ifPresent(existing -> {
                    throw AppException.of(HttpStatus.CONFLICT, "Tên phòng đã tồn tại");
                });

        room.setRoomName(normalizedRoomName);
        Room saved = roomRepository.save(room);
        return toAdminRoomResponse(saved);
    }

    // Chức năng: xử lý chỉ định bác sĩ vào phòng.
    public AdminRoomResponse assignDoctorToRoom(Long roomId, Long doctorId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy phòng"));

        User doctor = userRepository.findByIdAndRole(doctorId, Role.DOCTOR)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy bác sĩ"));

        room.setCurrentDoctor(doctor);
        Room saved = roomRepository.save(room);

        List<Room> doctorRooms = roomRepository.findByCurrentDoctor_Id(doctorId);
        for (Room assignedRoom : doctorRooms) {
            if (!Objects.equals(assignedRoom.getId(), roomId)) {
                assignedRoom.setCurrentDoctor(null);
            }
        }
        roomRepository.saveAll(doctorRooms);

        return toAdminRoomResponse(saved);
    }

    // Chức năng: xử lý lấy lịch phân công phòng theo tháng.
    public List<AdminRoomScheduleResponse> getRoomSchedules(String month) {
        YearMonth yearMonth = resolveYearMonth(month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        return roomScheduleRepository.findByScheduleDateBetweenOrderByScheduleDateAscStartTimeAsc(startDate, endDate)
                .stream()
                .map(this::toAdminRoomScheduleResponse)
                .toList();
    }

    // Chức năng: xử lý tạo lịch phân công phòng.
    public AdminRoomScheduleResponse createRoomSchedule(AdminRoomScheduleRequest request) {
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy phòng"));

        User doctor = userRepository.findByIdAndRole(request.getDoctorId(), Role.DOCTOR)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy bác sĩ"));

        LocalDate scheduleDate = request.getScheduleDate();
        TimeSlot timeSlot = parseTimeSlot(request.getTimeSlot());

        ensureScheduleAvailable(room.getId(), doctor.getId(), scheduleDate, timeSlot, null);

        RoomSchedule schedule = new RoomSchedule();
        schedule.setRoom(room);
        schedule.setDoctor(doctor);
        schedule.setScheduleDate(scheduleDate);
        schedule.setStartTime(timeSlot.start);
        schedule.setEndTime(timeSlot.end);

        return toAdminRoomScheduleResponse(roomScheduleRepository.save(schedule));
    }

    // Chức năng: xử lý cập nhật lịch phân công phòng.
    public AdminRoomScheduleResponse updateRoomSchedule(Long scheduleId, AdminRoomScheduleRequest request) {
        RoomSchedule schedule = roomScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy lịch phân công"));

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy phòng"));

        User doctor = userRepository.findByIdAndRole(request.getDoctorId(), Role.DOCTOR)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy bác sĩ"));

        LocalDate scheduleDate = request.getScheduleDate();
        TimeSlot timeSlot = parseTimeSlot(request.getTimeSlot());

        ensureScheduleAvailable(room.getId(), doctor.getId(), scheduleDate, timeSlot, scheduleId);

        schedule.setRoom(room);
        schedule.setDoctor(doctor);
        schedule.setScheduleDate(scheduleDate);
        schedule.setStartTime(timeSlot.start);
        schedule.setEndTime(timeSlot.end);

        return toAdminRoomScheduleResponse(roomScheduleRepository.save(schedule));
    }

    // Chức năng: xử lý xóa lịch phân công phòng.
    public void deleteRoomSchedule(Long scheduleId) {
        RoomSchedule schedule = roomScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy lịch phân công"));
        roomScheduleRepository.delete(schedule);
    }

    // Chức năng: xử lý lấy danh sách tất cả thuốc.
    public List<AdminMedicineResponse> getAllMedicines() {
        return medicineRepository.findAll().stream()
                .map(this::toAdminMedicineResponse)
                .toList();
    }

    // Chức năng: xử lý tạo thuốc mới.
    public AdminMedicineResponse createMedicine(AdminMedicineCreateRequest request) {
        String normalizedMedicineName = normalizeRequiredText(request.getMedicineName(), "Tên thuốc là bắt buộc");
        String normalizedMedicineType = normalizeRequiredText(request.getMedicineType(), "Loại thuốc là bắt buộc");

        if (medicineRepository.existsByMedicineNameIgnoreCase(normalizedMedicineName)) {
            throw AppException.of(HttpStatus.CONFLICT, "Tên thuốc đã tồn tại");
        }

        Medicine medicine = new Medicine();
        medicine.setMedicineName(normalizedMedicineName);
        medicine.setMedicineType(normalizedMedicineType);
        medicine.setUnit(normalizeOptionalText(request.getUnit()));
        medicine.setSellingPrice(request.getSellingPrice());
        Integer stockQuantity = request.getStockQuantity();
        medicine.setStockQuantity(stockQuantity == null ? 0 : stockQuantity);
        medicine.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());

        Medicine saved = medicineRepository.save(medicine);
        return toAdminMedicineResponse(saved);
    }

    // Chức năng: xử lý cập nhật thông tin thuốc.
    public AdminMedicineResponse updateMedicine(Long medicineId, AdminMedicineUpdateRequest request) {
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy thuốc"));

        if (request.getMedicineName() != null && !request.getMedicineName().isBlank()) {
            String normalizedMedicineName = request.getMedicineName().trim();
            boolean sameName = medicine.getMedicineName() != null
                    && medicine.getMedicineName().equalsIgnoreCase(normalizedMedicineName);
            if (!sameName && medicineRepository.existsByMedicineNameIgnoreCase(normalizedMedicineName)) {
                throw AppException.of(HttpStatus.CONFLICT, "Tên thuốc đã tồn tại");
            }
            medicine.setMedicineName(normalizedMedicineName);
        }

        if (request.getUnit() != null) {
            medicine.setUnit(normalizeOptionalText(request.getUnit()));
        }
        if (request.getMedicineType() != null) {
            String normalizedMedicineType = normalizeOptionalText(request.getMedicineType());
            medicine.setMedicineType(normalizedMedicineType == null ? "Khác" : normalizedMedicineType);
        }
        if (request.getSellingPrice() != null) {
            medicine.setSellingPrice(request.getSellingPrice());
        }
        if (request.getStockQuantity() != null) {
            medicine.setStockQuantity(request.getStockQuantity());
        }
        if (request.getIsActive() != null) {
            medicine.setIsActive(request.getIsActive());
        }

        Medicine saved = medicineRepository.save(medicine);
        return toAdminMedicineResponse(saved);
    }

    // Chức năng: xử lý vô hiệu hóa thuốc.
    public void deactivateMedicine(Long medicineId) {
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy thuốc"));

        medicine.setIsActive(false);
        medicineRepository.save(medicine);
    }

    // Chức năng: xử lý lấy danh sách tất cả dịch vụ.
    public List<AdminMedicalServiceResponse> getAllMedicalServices() {
        return medicalServiceRepository.findAll().stream()
                .map(this::toAdminMedicalServiceResponse)
                .toList();
    }

    // Chức năng: xử lý tạo dịch vụ mới.
    public AdminMedicalServiceResponse createMedicalService(AdminMedicalServiceCreateRequest request) {
        String normalizedServiceName = normalizeRequiredText(request.getServiceName(), "Tên dịch vụ là bắt buộc");

        if (medicalServiceRepository.existsByServiceNameIgnoreCase(normalizedServiceName)) {
            throw AppException.of(HttpStatus.CONFLICT, "Tên dịch vụ đã tồn tại");
        }

        MedicalService medicalService = new MedicalService();
        medicalService.setServiceName(normalizedServiceName);
        medicalService.setCurrentPrice(request.getCurrentPrice());
        medicalService.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());

        MedicalService saved = medicalServiceRepository.save(medicalService);
        return toAdminMedicalServiceResponse(saved);
    }

    // Chức năng: xử lý cập nhật giá dịch vụ.
    public AdminMedicalServiceResponse updateMedicalServicePrice(Long serviceId, BigDecimal currentPrice) {
        MedicalService medicalService = medicalServiceRepository.findById(serviceId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy dịch vụ"));

        medicalService.setCurrentPrice(currentPrice);
        MedicalService saved = medicalServiceRepository.save(medicalService);
        return toAdminMedicalServiceResponse(saved);
    }

    // Chức năng: xử lý vô hiệu hóa dịch vụ.
    public void deactivateMedicalService(Long serviceId) {
        MedicalService medicalService = medicalServiceRepository.findById(serviceId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy dịch vụ"));

        medicalService.setIsActive(false);
        medicalServiceRepository.save(medicalService);
    }

    // Chức năng: xử lý chuẩn hóa văn bản cần thiết.
    private String normalizeRequiredText(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw AppException.of(HttpStatus.BAD_REQUEST, errorMessage);
        }
        return value.trim();
    }

    // Chức năng: xử lý chuẩn hóa văn bản tùy chọn.
    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Chức năng: xử lý chuẩn hóa email tùy chọn.
    private String normalizeOptionalEmail(String value) {
        String normalized = normalizeOptionalText(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    // Chức năng: xử lý chuyển đổi response phòng khám.
    private AdminRoomResponse toAdminRoomResponse(Room room) {
        User doctor = room.getCurrentDoctor();
        return new AdminRoomResponse(
                room.getId(),
                room.getRoomName(),
                doctor == null ? null : doctor.getId(),
                doctor == null ? null : doctor.getUsername());
    }

    // Chức năng: xử lý chuyển đổi response lịch phân công phòng.
    private AdminRoomScheduleResponse toAdminRoomScheduleResponse(RoomSchedule schedule) {
        Room room = schedule.getRoom();
        User doctor = schedule.getDoctor();
        LocalTime startTime = schedule.getStartTime();
        LocalTime endTime = schedule.getEndTime();
        String timeSlot = startTime.format(TIME_FORMAT) + "-" + endTime.format(TIME_FORMAT);

        return new AdminRoomScheduleResponse(
                schedule.getId(),
                room == null ? null : room.getId(),
                room == null ? null : room.getRoomName(),
                doctor == null ? null : doctor.getId(),
                doctor == null ? null : doctor.getUsername(),
                schedule.getScheduleDate(),
                startTime,
                endTime,
                timeSlot);
    }

    // Chức năng: xử lý đảm bảo lịch phân công không trùng.
    private void ensureScheduleAvailable(
            Long roomId,
            Long doctorId,
            LocalDate scheduleDate,
            TimeSlot timeSlot,
            Long ignoredScheduleId) {
        List<RoomSchedule> doctorSchedules = roomScheduleRepository.findByDoctor_IdAndScheduleDate(doctorId,
                scheduleDate);
        for (RoomSchedule existing : doctorSchedules) {
            if (ignoredScheduleId != null && ignoredScheduleId.equals(existing.getId())) {
                continue;
            }
            if (overlaps(timeSlot.start, timeSlot.end, existing.getStartTime(), existing.getEndTime())) {
                throw AppException.of(HttpStatus.CONFLICT, "Bác sĩ đã có lịch vào khung giờ này");
            }
        }

        List<RoomSchedule> roomSchedules = roomScheduleRepository.findByRoom_IdAndScheduleDate(roomId, scheduleDate);
        for (RoomSchedule existing : roomSchedules) {
            if (ignoredScheduleId != null && ignoredScheduleId.equals(existing.getId())) {
                continue;
            }
            if (overlaps(timeSlot.start, timeSlot.end, existing.getStartTime(), existing.getEndTime())) {
                throw AppException.of(HttpStatus.CONFLICT, "Phòng đã có lịch vào khung giờ này");
            }
        }
    }

    // Chức năng: xử lý kiểm tra giao nhau thời gian.
    private boolean overlaps(LocalTime start, LocalTime end, LocalTime otherStart, LocalTime otherEnd) {
        return start.isBefore(otherEnd) && end.isAfter(otherStart);
    }

    // Chức năng: xử lý parse timeSlot.
    private TimeSlot parseTimeSlot(String timeSlot) {
        if (timeSlot == null || timeSlot.isBlank()) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "timeSlot là bắt buộc");
        }

        String[] parts = timeSlot.trim().split("-");
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
            return new TimeSlot(start, end);
        } catch (DateTimeParseException ex) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Giá trị timeSlot không hợp lệ", ex);
        }
    }

    // Chức năng: xử lý parse năm-tháng.
    private YearMonth resolveYearMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }

        try {
            return YearMonth.parse(month.trim(), DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (DateTimeParseException ex) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "month phải đúng định dạng yyyy-MM", ex);
        }
    }

    private static class TimeSlot {
        private final LocalTime start;
        private final LocalTime end;

        private TimeSlot(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }
    }

    // Chức năng: xử lý chuyển đổi response thuốc.
    private AdminMedicineResponse toAdminMedicineResponse(Medicine medicine) {
        return new AdminMedicineResponse(
                medicine.getId(),
                medicine.getMedicineName(),
                medicine.getMedicineType(),
                medicine.getUnit(),
                medicine.getSellingPrice(),
                medicine.getStockQuantity(),
                medicine.getIsActive());
    }

    // Chức năng: xử lý chuyển đổi response dịch vụ.
    private AdminMedicalServiceResponse toAdminMedicalServiceResponse(MedicalService medicalService) {
        return new AdminMedicalServiceResponse(
                medicalService.getId(),
                medicalService.getServiceName(),
                medicalService.getCurrentPrice(),
                medicalService.getIsActive());
    }

    // Chức năng: xử lý chuyển đổi response người dùng.
    private AdminUserResponse toAdminUserResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getRole(),
                user.getIsActive());
    }

    // Chức năng: xử lý tùy chọn nhóm theo báo cáo chuẩn hóa.
    private String normalizeGroupBy(String groupBy) {
        if (groupBy == null || groupBy.isBlank()) {
            return GROUP_DAY;
        }
        String normalized = groupBy.trim().toUpperCase(Locale.ROOT);
        if (!GROUP_DAY.equals(normalized) && !GROUP_MONTH.equals(normalized) && !GROUP_YEAR.equals(normalized)) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "groupBy phải là DAY, MONTH hoặc YEAR");
        }
        return normalized;
    }

    // Chức năng: xử lý chuẩn hóa bộ lọc văn bản tùy chọn.
    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    // Chức năng: xử lý ánh xạ hóa đơn vào dòng báo cáo.
    private AdminRevenueReportItemResponse toRevenueReportItem(Invoice invoice) {
        Appointment appointment = invoice.getAppointment();
        Patient patient = appointment == null ? null : appointment.getPatient();
        Long appointmentId = appointment == null ? null : appointment.getId();
        Long medicalRecordId = resolveMedicalRecordId(appointmentId);

        List<String> services = medicalRecordId == null
                ? List.of()
                : medicalRecordServiceDetailRepository.findByMedicalRecord_Id(medicalRecordId).stream()
                        .map(detail -> detail.getService() == null ? null : detail.getService().getServiceName())
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        List<String> medicines = medicalRecordId == null
                ? List.of()
                : prescriptionDetailRepository.findByMedicalRecord_Id(medicalRecordId).stream()
                        .map(PrescriptionDetail::getMedicine)
                        .filter(Objects::nonNull)
                        .map(Medicine::getMedicineName)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        return new AdminRevenueReportItemResponse(
                invoice.getId(),
                appointmentId,
                patient == null ? null : patient.getFullName(),
                invoice.getPaymentMethod(),
                invoice.getPaidAt(),
                invoice.getTotalServiceFee(),
                invoice.getGrandTotal(),
                services,
                medicines);
    }

    private Long resolveMedicalRecordId(Long appointmentId) {
        if (appointmentId == null) {
            return null;
        }
        List<MedicalRecord> records = medicalRecordRepository
                .findAllByAppointment_IdOrderByCreatedAtDescIdDesc(appointmentId);
        if (records.isEmpty()) {
            return null;
        }
        return records.get(0).getId();
    }

    // Chức năng: xử lý áp dụng bộ lọc dịch vụ/thuốc.
    private boolean matchesRevenueFilters(
            AdminRevenueReportItemResponse item,
            String serviceFilter,
            String medicineFilter) {
        boolean serviceMatches = true;
        if (serviceFilter != null) {
            serviceMatches = item.getServices().stream()
                    .filter(Objects::nonNull)
                    .map(name -> name.toLowerCase(Locale.ROOT))
                    .anyMatch(name -> name.contains(serviceFilter));
        }

        boolean medicineMatches = true;
        if (medicineFilter != null) {
            medicineMatches = item.getMedicines().stream()
                    .filter(Objects::nonNull)
                    .map(name -> name.toLowerCase(Locale.ROOT))
                    .anyMatch(name -> name.contains(medicineFilter));
        }

        return serviceMatches && medicineMatches;
    }

    // Chức năng: xử lý tổng hợp điểm biểu đồ.
    private List<AdminRevenueChartPointResponse> buildRevenueChart(
            List<AdminRevenueReportItemResponse> items,
            String groupBy) {
        String normalizedGroupBy = groupBy == null ? GROUP_DAY : groupBy;

        return switch (normalizedGroupBy) {
            case GROUP_MONTH -> {
                TreeMap<YearMonth, BigDecimal> bucket = new TreeMap<>();
                for (AdminRevenueReportItemResponse item : items) {
                    if (item.getPaidAt() == null) {
                        continue;
                    }
                    YearMonth key = YearMonth.from(item.getPaidAt());
                    BigDecimal amount = item.getGrandTotal() == null ? BigDecimal.ZERO : item.getGrandTotal();
                    bucket.put(key, bucket.getOrDefault(key, BigDecimal.ZERO).add(amount));
                }

                List<AdminRevenueChartPointResponse> points = new ArrayList<>();
                for (var entry : bucket.entrySet()) {
                    points.add(
                            new AdminRevenueChartPointResponse(entry.getKey().format(MONTH_FORMAT), entry.getValue()));
                }
                yield points;
            }
            case GROUP_YEAR -> {
                TreeMap<Year, BigDecimal> bucket = new TreeMap<>();
                for (AdminRevenueReportItemResponse item : items) {
                    if (item.getPaidAt() == null) {
                        continue;
                    }
                    Year key = Year.of(item.getPaidAt().getYear());
                    BigDecimal amount = item.getGrandTotal() == null ? BigDecimal.ZERO : item.getGrandTotal();
                    bucket.put(key, bucket.getOrDefault(key, BigDecimal.ZERO).add(amount));
                }

                List<AdminRevenueChartPointResponse> points = new ArrayList<>();
                for (var entry : bucket.entrySet()) {
                    points.add(
                            new AdminRevenueChartPointResponse(entry.getKey().format(YEAR_FORMAT), entry.getValue()));
                }
                yield points;
            }
            default -> {
                TreeMap<LocalDate, BigDecimal> bucket = new TreeMap<>();
                for (AdminRevenueReportItemResponse item : items) {
                    if (item.getPaidAt() == null) {
                        continue;
                    }
                    LocalDate key = item.getPaidAt().toLocalDate();
                    BigDecimal amount = item.getGrandTotal() == null ? BigDecimal.ZERO : item.getGrandTotal();
                    bucket.put(key, bucket.getOrDefault(key, BigDecimal.ZERO).add(amount));
                }

                List<AdminRevenueChartPointResponse> points = new ArrayList<>();
                for (var entry : bucket.entrySet()) {
                    points.add(new AdminRevenueChartPointResponse(entry.getKey().format(DAY_FORMAT), entry.getValue()));
                }
                yield points;
            }
        };
    }

    // Chức năng: xử lý xuất báo cáo sang định dạng CSV byte.
    private byte[] exportRevenueReportCsv(AdminRevenueReportResponse report) {
        StringBuilder builder = new StringBuilder();
        builder.append(
                "InvoiceId,AppointmentId,PatientName,PaymentMethod,PaidAt,ServiceRevenue,TotalRevenue,Services,Medicines\n");

        for (AdminRevenueReportItemResponse item : report.getItems()) {
            builder.append(nullSafe(item.getInvoiceId())).append(',')
                    .append(nullSafe(item.getAppointmentId())).append(',')
                    .append(csvEscape(item.getPatientName())).append(',')
                    .append(csvEscape(item.getPaymentMethod())).append(',')
                    .append(csvEscape(item.getPaidAt() == null ? null : item.getPaidAt().format(DATETIME_FORMAT)))
                    .append(',')
                    .append(nullSafe(item.getTotalServiceFee())).append(',')
                    .append(nullSafe(item.getGrandTotal())).append(',')
                    .append(csvEscape(String.join(" | ", item.getServices()))).append(',')
                    .append(csvEscape(String.join(" | ", item.getMedicines())))
                    .append('\n');
        }

        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    // Chức năng: xử lý xuất báo cáo sang định dạng PDF byte.
    private byte[] exportRevenueReportPdf(AdminRevenueReportResponse report) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float x = 40f;
                float y = page.getMediaBox().getHeight() - 50f;
                content.beginText();
                content.setLeading(15f);
                content.newLineAtOffset(x, y);
                content.setFont(PDType1Font.HELVETICA_BOLD, 13);
                content.showText("Báo cáo doanh thu phong kham");
                content.newLine();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.showText("Tu: " + report.getStartTime().format(DATETIME_FORMAT)
                        + "  Den: " + report.getEndTime().format(DATETIME_FORMAT));
                content.newLine();
                content.showText("Tong giao dich: " + report.getTotalInvoices()
                        + " | Tong doanh thu: " + nullSafe(report.getTotalRevenue()));
                content.newLine();
                content.newLine();

                if (report.getItems().isEmpty()) {
                    content.showText("Không có du lieu trong khoang thoi gian nay");
                } else {
                    int limit = Math.min(report.getItems().size(), 30);
                    for (int i = 0; i < limit; i++) {
                        AdminRevenueReportItemResponse item = report.getItems().get(i);
                        String line = "HD#" + item.getInvoiceId()
                                + " | BN: " + nullSafe(item.getPatientName())
                                + " | Tong: " + nullSafe(item.getGrandTotal())
                                + " | " + (item.getPaidAt() == null ? "" : item.getPaidAt().format(DATETIME_FORMAT));
                        content.showText(line);
                        content.newLine();
                    }
                    if (report.getItems().size() > limit) {
                        content.showText("... con " + (report.getItems().size() - limit)
                                + " dong, vui long dung file CSV de xem day du.");
                    }
                }

                content.endText();
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw AppException.of(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xuất file PDF", ex);
        }
    }

    // Chức năng: xử lý Văn bản an toàn với CSV.
    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }

    // Chức năng: xử lý chuyển đổi giá trị null thành chuỗi.
    private String nullSafe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

}
