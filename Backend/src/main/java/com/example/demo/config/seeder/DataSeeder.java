package com.example.demo.config.seeder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSeeder.class);

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    private final UserSeeder userSeeder;
    private final PatientSeeder patientSeeder;
    private final MedicineSeeder medicineSeeder;
    private final MedicalServiceSeeder medicalServiceSeeder;
    private final com.example.demo.config.SymptomDataSeeder symptomDataSeeder;
    private final com.example.demo.config.DiagnosisDataSeeder diagnosisDataSeeder;
    private final com.example.demo.config.DiagnosisMedicineRuleSeeder diagnosisMedicineRuleSeeder;
    private final SymptomServiceMappingSeeder symptomServiceMappingSeeder;
    private final RoomSeeder roomSeeder;
    private final com.example.demo.config.seeder.DoctorSeeder doctorSeeder;
    private final SystemSettingSeeder systemSettingSeeder;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            LOGGER.info("[DataSeeder] Seed startup data is disabled");
            return;
        }

        userSeeder.seed();
        patientSeeder.seed();
        medicineSeeder.seed();
        medicalServiceSeeder.seed();
        symptomDataSeeder.seed();
        diagnosisDataSeeder.seed();
        diagnosisMedicineRuleSeeder.seed();
        symptomServiceMappingSeeder.seed();
        roomSeeder.seed();
        doctorSeeder.seed();
        systemSettingSeeder.seed();
    }
}
