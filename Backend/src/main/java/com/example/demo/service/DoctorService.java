package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.example.demo.dto.DoctorResponse;
import com.example.demo.dto.DoctorMedicineResponse;
import com.example.demo.entity.Appointment;
import com.example.demo.entity.Medicine;
import com.example.demo.entity.Role;
import com.example.demo.entity.Room;
import com.example.demo.entity.User;
import com.example.demo.exception.AppException;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.MedicineRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DoctorService {

    private static final String STATUS_IN_ROOM = "IN_ROOM";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final RoomRepository roomRepository;
    private final MedicineRepository medicineRepository;

    // Chức năng: lấy danh sách tất cả bác sĩ.
    public List<DoctorResponse> getAllDoctors() {
        return userRepository.findByRole(Role.DOCTOR).stream()
                .map(this::toDoctorResponse)
                .toList();
    }

    // Chức năng: cập nhật phòng khám cho bác sĩ.
    public DoctorResponse updateClinicRoom(Long doctorId, String clinicRoom) {
        if (clinicRoom == null || clinicRoom.isBlank()) {
            throw AppException.badRequest("Phòng khám là bắt buộc");
        }

        User doctor = userRepository.findByIdAndRole(doctorId, Role.DOCTOR)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy bác sĩ"));

        String normalizedRoomName = clinicRoom.trim();

        Room targetRoom = roomRepository.findByRoomName(normalizedRoomName).orElseGet(Room::new);
        targetRoom.setRoomName(normalizedRoomName);
        targetRoom.setCurrentDoctor(doctor);
        targetRoom = roomRepository.save(targetRoom);

        List<Room> previouslyAssignedRooms = roomRepository.findByCurrentDoctor_Id(doctorId);
        for (Room room : previouslyAssignedRooms) {
            if (!Objects.equals(room.getId(), targetRoom.getId())) {
                room.setCurrentDoctor(null);
            }
        }
        roomRepository.saveAll(previouslyAssignedRooms);

        return toDoctorResponse(doctor);
    }

    // Chức năng: lấy danh sách bệnh nhân đang chờ khám.
    public List<Appointment> getMyWaitingPatients(String username, LocalDate date) {
        User doctor = userRepository.findByUsernameAndRole(username, Role.DOCTOR)
                .orElseThrow(
                        () -> AppException.notFound("Không tìm thấy tài khoản bác sĩ"));

        if (date != null) {
            LocalDateTime from = date.atStartOfDay();
            LocalDateTime to = from.plusDays(1);
            return appointmentRepository
                    .findVisibleForDoctorAndStatusAndAppointmentTimeBetweenOrderByAppointmentTimeAsc(
                            doctor.getId(),
                            STATUS_IN_ROOM,
                            from,
                            to);
        }

        return appointmentRepository.findVisibleForDoctorAndStatusOrderByAppointmentTimeAsc(
                doctor.getId(),
                STATUS_IN_ROOM);
    }

    // Chức năng: lấy danh sách bệnh nhân đã khám.
    public List<Appointment> getMyCompletedPatients(String username, LocalDate date) {
        User doctor = userRepository.findByUsernameAndRole(username, Role.DOCTOR)
                .orElseThrow(
                        () -> AppException.notFound("Không tìm thấy tài khoản bác sĩ"));

        if (date != null) {
            LocalDateTime from = date.atStartOfDay();
            LocalDateTime to = from.plusDays(1);
            return appointmentRepository
                    .findVisibleForDoctorAndStatusAndAppointmentTimeBetweenOrderByAppointmentTimeAsc(
                            doctor.getId(),
                            STATUS_COMPLETED,
                            from,
                            to);
        }

        return appointmentRepository.findVisibleForDoctorAndStatusOrderByAppointmentTimeAsc(
                doctor.getId(),
                STATUS_COMPLETED);
    }

    // Chức năng: lấy danh sách thuốc active cho bác sĩ kê đơn.
    public List<DoctorMedicineResponse> getMyMedicines() {
        return medicineRepository.findByIsActiveTrueOrderByMedicineNameAsc().stream()
                .map(this::toDoctorMedicineResponse)
                .toList();

    }

    // Chức năng: lời khuyên của bác sĩ.
    private DoctorResponse toDoctorResponse(User doctor) {
        return new DoctorResponse(doctor.getId(), doctor.getUsername(), doctor.getIsActive());
    }

    private DoctorMedicineResponse toDoctorMedicineResponse(Medicine medicine) {
        return new DoctorMedicineResponse(
                medicine.getId(),
                medicine.getMedicineName(),
                medicine.getMedicineType() == null || medicine.getMedicineType().isBlank()
                        ? "Khác"
                        : medicine.getMedicineType(),
                medicine.getUnit(),
                medicine.getSellingPrice(),
                medicine.getStockQuantity(),
                medicine.getIsActive());
    }
}
