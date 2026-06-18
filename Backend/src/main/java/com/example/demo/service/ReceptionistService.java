package com.example.demo.service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.exception.AppException;

import com.example.demo.dto.ReceptionistDoctorOptionResponse;
import com.example.demo.dto.ReceptionistApproveResponse;
import com.example.demo.dto.ReceptionistRoomSuggestionResponse;
import com.example.demo.dto.ReceptionistRoomScheduleOptionResponse;
import com.example.demo.config.SpecialtyRoomMappingConfig;
import com.example.demo.entity.Appointment;
import com.example.demo.entity.Role;
import com.example.demo.entity.Room;
import com.example.demo.entity.RoomSchedule;
import com.example.demo.entity.User;
import com.example.demo.constants.PaymentStatus;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.RoomScheduleRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ReceptionistService {

    public static final String STATUS_PENDING_CONFIRMATION = "PENDING";
    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_WAITING_CASHIER = "WAITING_CASHIER";
    public static final String STATUS_IN_ROOM = "IN_ROOM";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_CANCELLED_BY_CLINIC = "CANCELLED_BY_CLINIC";

    private static final Set<String> RECEPTIONIST_CANCELLABLE_STATUSES = Set.of(
            STATUS_PENDING_CONFIRMATION,
            STATUS_WAITING);
    private static final Set<String> RELEASED_SLOT_STATUSES = Set.of(
            STATUS_CANCELLED,
            STATUS_CANCELLED_BY_CLINIC);

    private static final Set<String> WAITING_STATUSES = Set.of(
            STATUS_PENDING_CONFIRMATION,
            STATUS_WAITING,
            STATUS_WAITING_CASHIER,
            STATUS_IN_ROOM,
            STATUS_IN_PROGRESS,
            STATUS_COMPLETED);

    private static final Set<String> RECEPTIONIST_QUERY_STATUSES = Set.of(
            STATUS_PENDING_CONFIRMATION,
            STATUS_WAITING,
            STATUS_WAITING_CASHIER,
            STATUS_IN_ROOM,
            STATUS_IN_PROGRESS,
            STATUS_COMPLETED,
            STATUS_CANCELLED,
            STATUS_CANCELLED_BY_CLINIC);

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomScheduleRepository roomScheduleRepository;
    private final InvoiceService invoiceService;
    private final NotificationService notificationService;
    private final SpecialtyRoomMappingConfig specialtyRoomMappingConfig;

    // Chức năng: xử lý lấy danh sách cuộc hẹn hôm nay.
    public List<Appointment> getTodayAppointments() {
        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = from.plusDays(1);
        return appointmentRepository.findByAppointmentTimeBetweenOrderByAppointmentTimeAsc(from, to);
    }

    // Chức năng: xử lý lấy danh sách cuộc hẹn từ danh sách đặt lịch.
    public List<Appointment> getAppointmentsFromBookingList() {
        return appointmentRepository.findByStatusInOrderByAppointmentTimeAsc(
                List.of(STATUS_PENDING_CONFIRMATION, "DRAFT", "PENDING", "PENDING_CONFIRMATION"));
    }

    // Chức năng: xử lý lấy danh sách bác sĩ theo chuyên khoa.
    public List<ReceptionistDoctorOptionResponse> getDoctorsBySpecialty(String specialty) {
        String normalizedSpecialty = normalizeOptionalText(specialty);

        Map<Long, ReceptionistDoctorOptionResponse> result = new LinkedHashMap<>();
        List<Room> rooms = roomRepository.findAllByOrderByRoomNameAsc();
        Long mappedRoomId = specialtyRoomMappingConfig.getRoomIdForSpecialty(normalizedSpecialty);
        boolean hasMappedRoom = mappedRoomId != null && roomRepository.existsById(mappedRoomId);
        for (Room room : rooms) {
            User doctor = room.getCurrentDoctor();
            if (doctor == null || doctor.getRole() != Role.DOCTOR) {
                continue;
            }
            if (hasMappedRoom && !Objects.equals(room.getId(), mappedRoomId)) {
                continue;
            }

            result.putIfAbsent(
                    doctor.getId(),
                    new ReceptionistDoctorOptionResponse(
                            doctor.getId(),
                            doctor.getUsername(),
                            room.getId(),
                            normalizedSpecialty,
                            room.getRoomName()));
        }

        if (normalizedSpecialty == null) {
            userRepository.findByRole(Role.DOCTOR).forEach(doctor -> result.putIfAbsent(
                    doctor.getId(),
                    new ReceptionistDoctorOptionResponse(doctor.getId(), doctor.getUsername(), null, null, null)));
        }

        return result.values().stream().toList();
    }

    // Chức năng: lấy danh sách phòng + bác sĩ theo lịch phân công trong ngày.
    @Transactional(readOnly = true)
    public List<ReceptionistRoomScheduleOptionResponse> getRoomSchedules(LocalDate scheduleDate) {
        LocalDate date = scheduleDate == null ? LocalDate.now() : scheduleDate;
        return roomScheduleRepository.findByScheduleDateOrderByStartTimeAsc(date)
                .stream()
                .map(schedule -> new ReceptionistRoomScheduleOptionResponse(
                        schedule.getRoom() == null ? null : schedule.getRoom().getId(),
                        schedule.getRoom() == null ? null : schedule.getRoom().getRoomName(),
                        schedule.getDoctor() == null ? null : schedule.getDoctor().getId(),
                        schedule.getDoctor() == null ? null : schedule.getDoctor().getUsername(),
                        schedule.getStartTime() == null ? null : schedule.getStartTime().toString(),
                        schedule.getEndTime() == null ? null : schedule.getEndTime().toString()))
                .toList();
    }

    // Chức năng: gợi ý phòng khám theo lịch hẹn.
    @Transactional(readOnly = true)
    public ReceptionistRoomSuggestionResponse getSuggestedRoom(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn"));

        Room room = resolveSuggestedRoom(appointment, null);
        if (room == null) {
            return new ReceptionistRoomSuggestionResponse(null, null, inferSpecialtyFromAppointment(appointment));
        }

        return new ReceptionistRoomSuggestionResponse(
                room.getId(),
                room.getRoomName(),
                inferSpecialtyFromAppointment(appointment));
    }

    // Chức năng: xử lý lấy hàng đợi chờ.
    public List<Appointment> getWaitingQueue(String status) {
        if (status == null || status.isBlank()) {
            return appointmentRepository.findByStatusInOrderByAppointmentTimeAsc(
                    List.of(STATUS_PENDING_CONFIRMATION, STATUS_WAITING_CASHIER, STATUS_IN_ROOM, STATUS_IN_PROGRESS));
        }

        String normalizedStatus = normalizeStatus(status);
        if (!RECEPTIONIST_QUERY_STATUSES.contains(normalizedStatus)) {
            throw AppException.of(
                    HttpStatus.BAD_REQUEST,
                    "Trạng thái không hợp lệ. Cho phép: PENDING, WAITING_CASHIER, IN_ROOM, IN_PROGRESS, COMPLETED, CANCELLED");
        }

        if (STATUS_CANCELLED.equals(normalizedStatus)) {
            return appointmentRepository.findByStatusInOrderByAppointmentTimeAsc(
                    List.of(STATUS_CANCELLED, STATUS_CANCELLED_BY_CLINIC));
        }

        return appointmentRepository.findByStatusOrderByAppointmentTimeAsc(normalizedStatus);
    }

    // Chức năng: xử lý duyệt cuộc hẹn.
    public ReceptionistApproveResponse approveAppointment(Long appointmentId, Long doctorId, Long assignedRoomId,
            String specialty) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn"));

        String currentStatus = normalizeStatus(appointment.getStatus());
        if (!STATUS_PENDING_CONFIRMATION.equals(currentStatus)) {
            throw AppException.of(HttpStatus.CONFLICT, "Chỉ lịch hẹn ở trạng thái PENDING mới có thể được duyệt");
        }

        Room assignedRoom = resolveAssignedRoom(appointment, assignedRoomId, specialty);
        if (assignedRoom == null) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Vui lòng chọn phòng khám phù hợp");
        }
        appointment.setAssignedRoom(assignedRoom);

        User doctor = resolveDoctorForAssignment(doctorId, specialty, appointment, assignedRoom);
        if (doctor != null) {
            ensureDoctorIsAvailable(doctor.getId(), appointment.getAppointmentTime(), appointment.getId());
            appointment.setDoctor(doctor);
        }

        String paymentStatus = normalizeOptionalStatus(appointment.getPaymentStatus());
        String approvalMessage;
        if (PaymentStatus.FULLY_PAID.equals(paymentStatus)) {
            appointment.setStatus(STATUS_IN_ROOM);
            approvalMessage = "Bệnh nhân đã thanh toán đầy đủ, mời vào phòng khám.";
        } else {
            appointment.setStatus(STATUS_WAITING_CASHIER);
            invoiceService.createInvoiceForAppointment(appointment);
            approvalMessage = "Mời anh chị qua quầy thu ngân đóng tiền trước khi vào phòng khám.";
        }

        Appointment saved = appointmentRepository.save(appointment);
        notificationService.notifyAppointmentApproved(saved);
        return new ReceptionistApproveResponse(saved, approvalMessage);
    }

    // Chức năng: xử lý chỉ định bác sĩ và chuyển đến phòng chờ.
    public ReceptionistApproveResponse assignDoctorAndMoveToWaiting(Long appointmentId, Long doctorId,
            String specialty) {
        return approveAppointment(appointmentId, doctorId, null, specialty);
    }

    // Chức năng: xử lý cập nhật trạng thái chờ.
    public Appointment updateWaitingStatus(Long appointmentId, String status) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn"));

        String normalizedStatus = normalizeStatus(status);
        if (!WAITING_STATUSES.contains(normalizedStatus)) {
            throw AppException.of(
                    HttpStatus.BAD_REQUEST,
                    "Trạng thái không hợp lệ. Cho phép: PENDING, WAITING_CASHIER, IN_ROOM, IN_PROGRESS, COMPLETED");
        }

        validateTransition(appointment.getStatus(), normalizedStatus);

        if ((STATUS_WAITING.equals(normalizedStatus)
                || STATUS_IN_ROOM.equals(normalizedStatus)
                || STATUS_IN_PROGRESS.equals(normalizedStatus))
                && appointment.getDoctor() == null) {
            throw AppException.of(HttpStatus.CONFLICT, "Lịch hẹn phải được phân công bác sĩ trước");
        }

        appointment.setStatus(normalizedStatus);
        return appointmentRepository.save(appointment);
    }

    // Chức năng: xử lý từ chối cuộc hẹn.
    public Appointment cancelAppointmentByReceptionist(Long appointmentId, String cancellationReason,
            Boolean requireReason) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn"));

        String normalizedStatus = normalizeStatus(appointment.getStatus());
        if (!RECEPTIONIST_CANCELLABLE_STATUSES.contains(normalizedStatus)) {
            throw AppException.of(
                    HttpStatus.CONFLICT,
                    "Chỉ lịch hẹn ở trạng thái PENDING hoặc WAITING mới có thể bị lễ tân hủy");
        }

        boolean isReasonRequired = requireReason == null || requireReason;
        String normalizedReason = cancellationReason == null ? null : cancellationReason.trim();
        if (isReasonRequired && (normalizedReason == null || normalizedReason.isBlank())) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Lý do hủy là bắt buộc");
        }

        appointment.setStatus(STATUS_CANCELLED_BY_CLINIC);
        appointment.setDoctor(null);
        appointment.setCancellationReason(normalizedReason);
        Appointment saved = appointmentRepository.save(appointment);

        notificationService.notifyClinicCancelledAppointment(saved, normalizedReason);
        return saved;
    }

    // Chức năng: xử lý đảm bảo bác sĩ có sẵn.
    private void ensureDoctorIsAvailable(Long doctorId, LocalDateTime appointmentTime, Long appointmentId) {
        boolean occupied = appointmentRepository.existsByDoctor_IdAndAppointmentTimeAndIdNotAndStatusNotIn(
                doctorId,
                appointmentTime,
                appointmentId,
                RELEASED_SLOT_STATUSES);
        if (occupied) {
            throw AppException.of(HttpStatus.CONFLICT, "Bác sĩ đã có lịch hẹn vào thời điểm này");
        }
    }

    // Chức năng: xử lý chọn bác sĩ từ doctorId hoặc chuyên khoa.
    private User resolveDoctorForAssignment(Long doctorId, String specialty, Appointment appointment,
            Room assignedRoom) {
        if (doctorId != null) {
            return userRepository.findByIdAndRole(doctorId, Role.DOCTOR)
                    .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy bác sĩ"));
        }

        String normalizedSpecialty = normalizeOptionalText(specialty);
        if (assignedRoom != null && appointment.getAppointmentTime() != null) {
            User scheduledDoctor = resolveDoctorFromSchedule(assignedRoom.getId(), appointment.getAppointmentTime());
            if (scheduledDoctor != null) {
                return scheduledDoctor;
            }
        }

        if (normalizedSpecialty == null) {
            return null;
        }

        return getDoctorsBySpecialty(normalizedSpecialty).stream()
                .map(option -> userRepository.findByIdAndRole(option.getDoctorId(), Role.DOCTOR).orElse(null))
                .filter(Objects::nonNull)
                .filter(doctor -> isDoctorAvailable(doctor.getId(), appointment.getAppointmentTime(),
                        appointment.getId()))
                .findFirst()
                .orElse(null);
    }

    // Chức năng: xử lý kiểm tra bác sĩ có rảnh ở khung giờ không.
    private boolean isDoctorAvailable(Long doctorId, LocalDateTime appointmentTime, Long appointmentId) {
        return !appointmentRepository.existsByDoctor_IdAndAppointmentTimeAndIdNotAndStatusNotIn(
                doctorId,
                appointmentTime,
                appointmentId,
                RELEASED_SLOT_STATUSES);
    }

    // Chức năng: xử lý xác thực quá trình chuyển đổi trạng thái.
    private void validateTransition(String currentStatus, String nextStatus) {
        String normalizedCurrent = normalizeStatus(currentStatus);
        if (normalizedCurrent.equals(nextStatus)) {
            return;
        }

        boolean valid = switch (normalizedCurrent) {
            case STATUS_PENDING_CONFIRMATION -> STATUS_WAITING_CASHIER.equals(nextStatus)
                    || STATUS_IN_ROOM.equals(nextStatus);
            case STATUS_WAITING_CASHIER -> STATUS_IN_ROOM.equals(nextStatus);
            case STATUS_IN_ROOM -> STATUS_IN_PROGRESS.equals(nextStatus);
            case STATUS_IN_PROGRESS -> STATUS_COMPLETED.equals(nextStatus);
            case STATUS_COMPLETED -> false;
            default -> false;
        };

        if (!valid) {
            throw AppException.of(
                    HttpStatus.CONFLICT,
                    "Chuyển trạng thái không hợp lệ: " + normalizedCurrent + " -> " + nextStatus);
        }
    }

    // Chức năng: xử lý chuẩn hóa trạng thái.
    private String normalizeStatus(String status) {
        if (status == null) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Trạng thái là bắt buộc");
        }

        String value = normalizeComparableStatus(status);
        return switch (value) {
            case "CHO_XAC_NHAN", "PENDING", "PENDING_CONFIRMATION", "DRAFT" -> STATUS_PENDING_CONFIRMATION;
            case "CHO_THU_NGAN", "WAITING_CASHIER" -> STATUS_WAITING_CASHIER;
            case "VAO_PHONG", "IN_ROOM" -> STATUS_IN_ROOM;
            case "DANG_CHO", "WAITING" -> STATUS_WAITING;
            case "DANG_KHAM", "IN_PROGRESS" -> STATUS_IN_PROGRESS;
            case "DA_KHAM", "COMPLETED" -> STATUS_COMPLETED;
            default -> value;
        };
    }

    private Room resolveAssignedRoom(Appointment appointment, Long assignedRoomId, String specialty) {
        if (assignedRoomId != null) {
            return roomRepository.findById(assignedRoomId)
                    .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy phòng"));
        }

        return resolveSuggestedRoom(appointment, specialty);
    }

    private Room resolveSuggestedRoom(Appointment appointment, String specialty) {
        List<Room> rooms = getRoomsAvailableAt(appointment.getAppointmentTime());
        if (rooms.isEmpty()) {
            return null;
        }

        String hint = normalizeOptionalText(specialty);
        if (hint == null) {
            hint = inferSpecialtyFromAppointment(appointment);
        }

        if (hint != null) {
            Long mapped = specialtyRoomMappingConfig.getRoomIdForSpecialty(hint);
            if (mapped != null) {
                Room mappedRoom = roomRepository.findById(mapped).orElse(null);
                if (mappedRoom != null) {
                    return mappedRoom;
                }
            }
        }

        String normalizedCategory = appointment.getCategory() == null ? null
                : normalizeOptionalText(appointment.getCategory().getName());
        if (normalizedCategory != null) {
            Long mapped = specialtyRoomMappingConfig.getRoomIdForSpecialty(normalizedCategory);
            if (mapped != null) {
                Room mappedRoom = roomRepository.findById(mapped).orElse(null);
                if (mappedRoom != null) {
                    return mappedRoom;
                }
            }
        }

        // Để có phương pháp phán đoán tốt hơn, hãy thử so khớp văn bản tên phòng với
        // chuyên khoa hoặc hạng mục (đã chuẩn hóa) được suy luận trước khi quay lại
        // phòng đầu tiên.
        String normalizedHint = hint == null ? null : normalizeTextForMatching(hint);
        String normalizedCat = normalizedCategory == null ? null : normalizeTextForMatching(normalizedCategory);

        for (Room r : rooms) {
            String rn = normalizeTextForMatching(r.getRoomName());
            if (rn == null)
                continue;
            if (normalizedHint != null && rn.contains(normalizedHint)) {
                return r;
            }
            if (normalizedCat != null && rn.contains(normalizedCat)) {
                return r;
            }
        }

        return rooms.get(0);
    }

    private List<Room> getRoomsAvailableAt(LocalDateTime appointmentTime) {
        List<Room> rooms = roomRepository.findAllByOrderByRoomNameAsc();
        if (appointmentTime == null) {
            return rooms;
        }

        LocalDate date = appointmentTime.toLocalDate();
        LocalTime time = appointmentTime.toLocalTime();
        List<Room> scheduled = roomScheduleRepository.findByScheduleDateOrderByStartTimeAsc(date)
                .stream()
                .filter(schedule -> schedule.getStartTime() != null && schedule.getEndTime() != null)
                .filter(schedule -> !time.isBefore(schedule.getStartTime()) && time.isBefore(schedule.getEndTime()))
                .map(RoomSchedule::getRoom)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return scheduled.isEmpty() ? rooms : scheduled;
    }

    private String inferSpecialtyFromAppointment(Appointment appointment) {
        String categoryName = appointment.getCategory() == null ? null : appointment.getCategory().getName();
        String normalizedCategory = normalizeTextForMatching(categoryName);
        if (normalizedCategory != null) {
            return normalizedCategory;
        }

        String symptomText = normalizeTextForMatching(appointment.getSymptomsText());
        if (symptomText == null) {
            symptomText = normalizeTextForMatching(appointment.getSymptoms());
        }
        if (symptomText == null) {
            return null;
        }

        if (containsAny(symptomText, List.of("đau bụng", "buồn nôn", "ói mửa", "tiêu chảy", "táo bón", "thượng vị"))) {
            return "Tiêu hóa";
        }
        if (containsAny(symptomText, List.of("ho", "đau họng", "khó thở", "chảy mũi", "khàn tiếng", "viêm mũi"))) {
            return "Hô hấp";
        }
        if (containsAny(symptomText, List.of("đau đầu", "chóng mặt", "mất ngủ", "tê tay", "tê chân", "co giật"))) {
            return "Thần kinh";
        }
        if (containsAny(symptomText, List.of("ngứa", "phát ban", "mẩn đỏ", "nóng rát", "dị ứng", "mụn"))) {
            return "Da liễu";
        }
        if (containsAny(symptomText, List.of("sốt", "mỏi mệt", "sụt cân", "sưng", "uể oải", "đau nhức"))) {
            return "Toàn thân";
        }

        return null;
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(normalizeTextForMatching(keyword))) {
                return true;
            }
        }
        return false;
    }

    private User resolveDoctorFromSchedule(Long roomId, LocalDateTime appointmentTime) {
        LocalDate date = appointmentTime.toLocalDate();
        LocalTime time = appointmentTime.toLocalTime();
        List<RoomSchedule> schedules = roomScheduleRepository.findByRoom_IdAndScheduleDate(roomId, date);
        return schedules.stream()
                .filter(schedule -> schedule.getStartTime() != null && schedule.getEndTime() != null)
                .filter(schedule -> !time.isBefore(schedule.getStartTime()) && time.isBefore(schedule.getEndTime()))
                .map(RoomSchedule::getDoctor)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String normalizeOptionalStatus(String status) {
        if (status == null) {
            return null;
        }
        String trimmed = status.trim().toUpperCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Chức năng: xử lý chuẩn hóa trạng thái có thể so sánh.
    private String normalizeComparableStatus(String status) {
        String compact = status.trim().replace('-', '_').replace(' ', '_').toUpperCase();
        String withoutAccents = Normalizer.normalize(compact, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return withoutAccents;
    }

    // Chức năng: xử lý chuẩn hóa text tùy chọn.
    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    private String normalizeTextForMatching(String value) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            return null;
        }

        String compact = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return compact.isEmpty() ? null : compact;
    }
}
