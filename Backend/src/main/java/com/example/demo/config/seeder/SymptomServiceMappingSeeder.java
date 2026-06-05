package com.example.demo.config.seeder;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.MedicalService;
import com.example.demo.entity.SymptomServiceMapping;
import com.example.demo.entity.SymptomServiceMappingId;
import com.example.demo.entity.SymptomTemplate;
import com.example.demo.repository.MedicalServiceRepository;
import com.example.demo.repository.SymptomServiceMappingRepository;
import com.example.demo.repository.SymptomTemplateRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SymptomServiceMappingSeeder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SymptomServiceMappingSeeder.class);

    @Value("${app.seed.symptom-service-mappings.enabled:true}")
    private boolean seedEnabled;

    private final SymptomTemplateRepository symptomTemplateRepository;
    private final MedicalServiceRepository medicalServiceRepository;
    private final SymptomServiceMappingRepository symptomServiceMappingRepository;

    @Transactional
    public void seed() {
        if (!seedEnabled) {
            return;
        }

        int inserted = 0;

        inserted += seedCategoryMappings("Hô hấp", List.of("Khám hô hấp"));
        inserted += seedCategoryMappings("Da liễu", List.of("Khám da liễu"));
        inserted += seedCategoryMappings("Thần kinh", List.of("Khám thần kinh", "Chụp MRI"));
        inserted += seedCategoryMappings("Toàn thân", List.of("Khám nội tổng quát", "Siêu âm"));
        inserted += seedCategoryMappings("Hô hấp", List.of("Chụp phim", "Chụp X-quang"));

        inserted += seedDigestiveMappings();

        LOGGER.info("[SymptomServiceMappingSeeder] Seeded symptom-service mappings: {} newly inserted", inserted);
    }

    private int seedCategoryMappings(String categoryName, List<String> serviceNames) {
        int inserted = 0;
        List<SymptomTemplate> symptoms = symptomTemplateRepository.findAll().stream()
                .filter(symptom -> symptom.getCategory() != null
                        && symptom.getCategory().getName() != null
                        && symptom.getCategory().getName().equalsIgnoreCase(categoryName))
                .toList();

        for (SymptomTemplate symptom : symptoms) {
            for (String serviceName : serviceNames) {
                inserted += createMappingIfMissing(symptom, serviceName);
            }
        }
        return inserted;
    }

    private int seedDigestiveMappings() {
        int inserted = 0;
        SymptomTemplate specificSymptom = symptomTemplateRepository.findAll().stream()
                .filter(symptom -> symptom.getSymptomName() != null
                        && symptom.getSymptomName().equalsIgnoreCase("Đau rát thượng vị"))
                .findFirst()
                .orElse(null);

        if (specificSymptom != null) {
            inserted += createMappingIfMissing(specificSymptom, "Khám tiêu hóa");
            inserted += createMappingIfMissing(specificSymptom, "Nội soi");
            inserted += createMappingIfMissing(specificSymptom, "Siêu âm");
        }

        List<SymptomTemplate> digestiveSymptoms = symptomTemplateRepository.findAll().stream()
                .filter(symptom -> symptom.getCategory() != null
                        && symptom.getCategory().getName() != null
                        && symptom.getCategory().getName().equalsIgnoreCase("Tiêu hóa"))
                .toList();

        for (SymptomTemplate symptom : digestiveSymptoms) {
            inserted += createMappingIfMissing(symptom, "Khám tiêu hóa");
            inserted += createMappingIfMissing(symptom, "Siêu âm");
        }

        return inserted;
    }

    private int createMappingIfMissing(SymptomTemplate symptom, String serviceName) {
        MedicalService service = medicalServiceRepository.findAll().stream()
                .filter(item -> item.getServiceName() != null && item.getServiceName().equalsIgnoreCase(serviceName))
                .findFirst()
                .orElse(null);

        if (symptom == null || service == null) {
            return 0;
        }

        SymptomServiceMappingId id = new SymptomServiceMappingId();
        id.setSymptomId(symptom.getId());
        id.setServiceId(service.getId());

        if (symptomServiceMappingRepository.existsById(id)) {
            return 0;
        }

        SymptomServiceMapping mapping = new SymptomServiceMapping();
        mapping.setId(id);
        mapping.setSymptom(symptom);
        mapping.setService(service);
        symptomServiceMappingRepository.save(mapping);
        return 1;
    }
}