package com.example.demo.config.seeder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DoctorSeeder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DoctorSeeder.class);

    @Value("${app.seed.doctors.enabled:true}")
    private boolean seedEnabled;

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void seed() {
        if (!seedEnabled) {
            return;
        }

        final String username = "doctor202";
        if (userRepository.existsByUsername(username)) {
            LOGGER.info("[DoctorSeeder] user '{}' already exists, skipping", username);
            return;
        }

        User user = new User();
        user.setUsername(username);
        // default password; please change after first login
        user.setPasswordHash(passwordEncoder.encode("ChangeMe123!"));
        user.setFullName("Bác sĩ phòng 202");
        // Provide default contact info to satisfy NOT NULL DB constraints
        user.setPhoneNumber("0900000002");
        user.setEmail("doctor202@clinic.local");
        user.setRole(Role.DOCTOR);
        user.setIsActive(Boolean.TRUE);

        User saved = userRepository.save(user);
        LOGGER.info("[DoctorSeeder] Created doctor user id={} username={}", saved.getId(), saved.getUsername());

        // attempt to find room named exactly as seeded in RoomSeeder
        String targetRoomName = "Phòng 202";
        roomRepository.findByRoomNameIgnoreCase(targetRoomName).ifPresent(room -> {
            room.setCurrentDoctor(saved);
            roomRepository.save(room);
            LOGGER.info("[DoctorSeeder] Assigned doctor {} to room {}", saved.getUsername(), room.getRoomName());
        });
    }
}
