package com.example.demo.config;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.MedicalCategory;
import com.example.demo.entity.SymptomTemplate;
import com.example.demo.repository.MedicalCategoryRepository;
import com.example.demo.repository.SymptomTemplateRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SymptomDataSeeder {

    private final MedicalCategoryRepository categoryRepository;
    private final SymptomTemplateRepository symptomRepository;

    @Transactional
    public void seed() {
        seedSymptoms();
    }

    private void seedSymptoms() {
        // Định nghĩa nhóm và triệu chứng mẫu
        var data = List.of(
                new CategoryWithSymptoms("Hô hấp", List.of("Ho", "Đau họng", "Khó thở", "Chảy mũi", "Khàn tiếng")),
                new CategoryWithSymptoms("Tiêu hóa",
                        List.of("Đau bụng", "Buồn nôn", "Ói mửa", "Tiêu chảy", "Táo bón", "Đau rát thượng vị")),
                new CategoryWithSymptoms("Thần kinh", List.of("Đau đầu", "Chóng mặt", "Mất ngủ", "Tê tay/chân")),
                new CategoryWithSymptoms("Toàn thân", List.of("Sốt", "Mỏi mệt", "Sụt cân", "Sưng")),
                new CategoryWithSymptoms("Da liễu", List.of("Ngứa", "Phát ban", "Mẩn đỏ", "Nóng rát")));

        for (CategoryWithSymptoms cs : data) {
            MedicalCategory category = categoryRepository.findAllByOrderByNameAsc().stream()
                    .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(cs.name))
                    .findFirst()
                    .orElse(null);

            if (category == null) {
                category = new MedicalCategory();
                category.setName(cs.name);
                category.setDescription(null);
                category.setIsActive(true);
                category = categoryRepository.save(category);
                System.out.println("[Seeder] Created category: " + cs.name);
            }

            for (String s : cs.symptoms) {
                boolean exists = symptomRepository.findByCategory_IdOrderBySymptomNameAsc(category.getId()).stream()
                        .anyMatch(x -> x.getSymptomName() != null && x.getSymptomName().equalsIgnoreCase(s));
                if (!exists) {
                    SymptomTemplate st = new SymptomTemplate();
                    st.setCategory(category);
                    st.setSymptomName(s);
                    symptomRepository.save(st);
                    System.out.println("[Seeder] Added symptom '" + s + "' to category '" + cs.name + "'");
                }
            }
        }
    }

    private static class CategoryWithSymptoms {
        final String name;
        final List<String> symptoms;

        CategoryWithSymptoms(String name, List<String> symptoms) {
            this.name = name;
            this.symptoms = symptoms;
        }
    }
}
