package com.example.demo.config;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.DiagnosisTemplate;
import com.example.demo.entity.MedicalCategory;
import com.example.demo.repository.DiagnosisTemplateRepository;
import com.example.demo.repository.MedicalCategoryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DiagnosisDataSeeder {

    private final MedicalCategoryRepository categoryRepository;
    private final DiagnosisTemplateRepository diagnosisRepository;

    @Transactional
    public void seed() {
        seedDiagnoses();
    }

    private void seedDiagnoses() {
        var data = List.of(
                new CategoryWithDiagnoses(
                        "Hô hấp",
                        List.of(
                                new DiagnosisSeed("Viêm họng cấp", "Uống nhiều nước, giữ ấm, súc họng nước muối."),
                                new DiagnosisSeed("Viêm mũi dị ứng", "Tránh dị nguyên, giữ vệ sinh mũi."),
                                new DiagnosisSeed("Viêm phế quản", "Nghỉ ngơi, theo dõi ho và sốt."),
                                new DiagnosisSeed("Hen phế quản", "Tránh kích thích, dùng thuốc theo hướng dẫn."),
                                new DiagnosisSeed("Viêm phổi nhẹ", "Theo dõi triệu chứng, tái khám nếu nặng hơn."))),
                new CategoryWithDiagnoses(
                        "Tiêu hóa",
                        List.of(
                                new DiagnosisSeed("Viêm dạ dày", "Ăn uống điều độ, tránh cay nóng và rượu bia."),
                                new DiagnosisSeed("Trào ngược dạ dày thực quản", "Ăn nhẹ, tránh nằm ngay sau ăn."),
                                new DiagnosisSeed("Viêm đại tràng", "Hạn chế đồ ăn kích ứng, uống đủ nước."),
                                new DiagnosisSeed("Rối loạn tiêu hóa", "Ăn uống dễ tiêu, theo dõi phân."),
                                new DiagnosisSeed("Nhiễm khuẩn đường ruột", "Bù nước điện giải, theo dõi sốt."))),
                new CategoryWithDiagnoses(
                        "Thần kinh",
                        List.of(
                                new DiagnosisSeed("Đau đầu căng thẳng", "Nghỉ ngơi, giảm stress, ngủ đủ giấc."),
                                new DiagnosisSeed("Migraine", "Tránh kích thích, theo dõi cơn đau."),
                                new DiagnosisSeed("Rối loạn giấc ngủ", "Giữ giờ ngủ đều, hạn chế cafe."),
                                new DiagnosisSeed("Chóng mặt tiền đình", "Tránh đổi tư thế đột ngột."),
                                new DiagnosisSeed("Đau dây thần kinh tọa", "Hạn chế vận động nặng, theo dõi đau."))),
                new CategoryWithDiagnoses(
                        "Toàn thân",
                        List.of(
                                new DiagnosisSeed("Sốt siêu vi", "Uống đủ nước, theo dõi nhiệt độ."),
                                new DiagnosisSeed("Suy nhược cơ thể", "Nghỉ ngơi, bổ sung dinh dưỡng."),
                                new DiagnosisSeed("Mệt mỏi do stress", "Giảm áp lực, vận động nhẹ."),
                                new DiagnosisSeed("Thiếu máu nhẹ", "Bổ sung sắt và dinh dưỡng."),
                                new DiagnosisSeed("Nhiễm trùng chưa rõ ổ", "Theo dõi triệu chứng, tái khám."))),
                new CategoryWithDiagnoses(
                        "Da liễu",
                        List.of(
                                new DiagnosisSeed("Viêm da dị ứng", "Tránh dị nguyên, dưỡng ẩm da."),
                                new DiagnosisSeed("Mề đay", "Tránh tác nhân kích ứng, theo dõi ngứa."),
                                new DiagnosisSeed("Nấm da", "Giữ khô da, dùng thuốc theo hướng dẫn."),
                                new DiagnosisSeed("Viêm nang lông", "Vệ sinh sạch, tránh cào gãi."),
                                new DiagnosisSeed("Chàm", "Dưỡng ẩm, tránh xà phòng mạnh."))));

        var fallback = List.of(
                new DiagnosisSeed("Chẩn đoán chưa xác định", "Theo dõi triệu chứng và tái khám."),
                new DiagnosisSeed("Rối loạn chức năng", "Ăn uống lành mạnh, nghỉ ngơi."),
                new DiagnosisSeed("Khám tổng quát", "Tư vấn và theo dõi theo chỉ định."));

        for (MedicalCategory category : categoryRepository.findAllByOrderByNameAsc()) {
            List<DiagnosisSeed> diagnoses = data.stream()
                    .filter(item -> item.name.equalsIgnoreCase(category.getName()))
                    .findFirst()
                    .map(item -> item.diagnoses)
                    .orElse(fallback);

            var existing = diagnosisRepository.findByCategory_IdOrderByDiagnosisNameAsc(category.getId());
            for (DiagnosisSeed seed : diagnoses) {
                boolean exists = existing.stream()
                        .anyMatch(item -> item.getDiagnosisName() != null
                                && item.getDiagnosisName().equalsIgnoreCase(seed.name));
                if (exists) {
                    continue;
                }

                DiagnosisTemplate diagnosis = new DiagnosisTemplate();
                diagnosis.setCategory(category);
                diagnosis.setDiagnosisName(seed.name);
                diagnosis.setDefaultAdvice(seed.defaultAdvice);
                diagnosisRepository.save(diagnosis);
                System.out.println(
                        "[Seeder] Added diagnosis '" + seed.name + "' to category '" + category.getName() + "'");
            }
        }
    }

    private static class CategoryWithDiagnoses {
        final String name;
        final List<DiagnosisSeed> diagnoses;

        CategoryWithDiagnoses(String name, List<DiagnosisSeed> diagnoses) {
            this.name = name;
            this.diagnoses = diagnoses;
        }
    }

    private static class DiagnosisSeed {
        final String name;
        final String defaultAdvice;

        DiagnosisSeed(String name, String defaultAdvice) {
            this.name = name;
            this.defaultAdvice = defaultAdvice;
        }
    }
}
