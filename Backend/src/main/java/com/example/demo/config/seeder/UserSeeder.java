package com.example.demo.config.seeder;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserSeeder {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSeeder.class);

    @Value("${app.seed.default-admin.enabled:true}")
    private boolean defaultAdminEnabled;

    @Value("${app.seed.default-admin.username:admin}")
    private String defaultAdminUsername;

    @Value("${app.seed.default-admin.password:123456}")
    private String defaultAdminPassword;

    @Value("${app.seed.default-admin.full-name:Administrator}")
    private String defaultAdminFullName;

    @Value("${app.seed.default-admin.email:admin@clinic.local}")
    private String defaultAdminEmail;

    @Value("${app.seed.users.enabled:true}")
    private boolean defaultUsersEnabled;

    @Value("${app.seed.users.default-password:123456}")
    private String defaultUsersPassword;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void seed() {
        if (defaultAdminEnabled) {
            seedDefaultAdmin();
        }

        if (defaultUsersEnabled) {
            seedDefaultUsers();
        }
    }

    private void seedDefaultAdmin() {
        String normalizedUsername = SeedUtils.trimToNull(defaultAdminUsername);
        String normalizedPassword = SeedUtils.trimToNull(defaultAdminPassword);

        if (normalizedUsername == null || normalizedPassword == null) {
            LOGGER.warn("[UserSeeder] Skip seeding default admin because username/password is empty");
            return;
        }

        if (userRepository.findByUsernameIgnoreCase(normalizedUsername).isPresent()) {
            LOGGER.info("[UserSeeder] Default admin already exists (username={})", normalizedUsername);
            return;
        }

        User admin = new User();
        admin.setUsername(normalizedUsername);
        admin.setPasswordHash(passwordEncoder.encode(normalizedPassword));
        admin.setRole(Role.ADMIN);
        admin.setIsActive(true);
        admin.setFullName(SeedUtils.trimToNull(defaultAdminFullName));
        admin.setEmail(SeedUtils.trimToNull(defaultAdminEmail));

        userRepository.save(admin);
        LOGGER.warn(
                "[UserSeeder] Created default admin account (username={}). Please change password after first login.",
                normalizedUsername);
    }

    private void seedDefaultUsers() {
        String normalizedPassword = SeedUtils.trimToNull(defaultUsersPassword);
        if (normalizedPassword == null) {
            LOGGER.warn("[UserSeeder] Skip seeding sample users because default password is empty");
            return;
        }

        List<UserSeedItem> users = List.of(
                new UserSeedItem("admin", "Admin Default", "admin@clinic.local", "0900000001", Role.ADMIN),
                new UserSeedItem("doctor", "Doctor Demo", "doctor@clinic.local", "0900000002", Role.DOCTOR),
                new UserSeedItem("reception", "Reception Demo", "reception@clinic.local", "0900000003",
                        Role.RECEPTIONIST),
                new UserSeedItem("cashier", "Cashier Demo", "cashier@clinic.local", "0900000004", Role.CASHIER),
                new UserSeedItem("patient", "Patient Demo", "patient@clinic.local", "0900000005", Role.PATIENT));

        int inserted = 0;
        for (UserSeedItem item : users) {
            if (userRepository.findByUsernameIgnoreCase(item.username()).isPresent()) {
                continue;
            }

            User user = new User();
            user.setUsername(item.username());
            user.setPasswordHash(passwordEncoder.encode(normalizedPassword));
            user.setRole(item.role());
            user.setIsActive(true);
            user.setFullName(item.fullName());
            user.setEmail(item.email());
            user.setPhoneNumber(item.phoneNumber());
            userRepository.save(user);
            inserted++;
        }

        LOGGER.info("[UserSeeder] Seeded users: {} newly inserted", inserted);
    }

    private record UserSeedItem(String username, String fullName, String email, String phoneNumber, Role role) {
    }
}
