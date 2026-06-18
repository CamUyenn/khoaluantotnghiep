package com.example.demo.config.seeder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Patient;
import com.example.demo.entity.User;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PatientSeeder {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatientSeeder.class);

    @Value("${app.seed.patients.enabled:true}")
    private boolean defaultPatientsEnabled;

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    @Transactional
    public void seed() {
        if (!defaultPatientsEnabled) {
            return;
        }

        User patientUser = userRepository.findByUsernameIgnoreCase("patient").orElse(null);
        if (patientUser == null) {
            LOGGER.warn("[PatientSeeder] Skip seeding patient because user 'patient' does not exist");
            return;
        }

        if (patientRepository.findByUserId(patientUser.getId()).isPresent()) {
            LOGGER.info("[PatientSeeder] Default patient already exists (username=patient)");
            return;
        }

        Patient patient = new Patient();
        patient.setUser(patientUser);
        patient.setFullName("Patient Demo");
        patient.setGender("Nam");
        patient.setNationalId("012345678901");
        patient.setHealthInsuranceNumber("BHYT-0001");
        patient.setPhoneNumber("0900000005");
        patient.setGmail("patient@clinic.local");
        patientRepository.save(patient);
        LOGGER.info("[PatientSeeder] Seeded default patient record");
    }
}
