package com.example.demo.config;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.DiagnosisMedicineRule;
import com.example.demo.entity.DiagnosisTemplate;
import com.example.demo.entity.Medicine;
import com.example.demo.repository.DiagnosisMedicineRuleRepository;
import com.example.demo.repository.DiagnosisTemplateRepository;
import com.example.demo.repository.MedicineRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DiagnosisMedicineRuleSeeder {

    private final DiagnosisTemplateRepository diagnosisRepository;
    private final MedicineRepository medicineRepository;
    private final DiagnosisMedicineRuleRepository ruleRepository;

    @Transactional
    public void seed() {
        var diagnoses = diagnosisRepository.findAll();
        var medicines = medicineRepository.findByIsActiveTrueOrderByMedicineNameAsc();
        if (medicines.isEmpty() || diagnoses.isEmpty())
            return;

        Map<String, List<String>> diagnosisMedicineMap = buildDiagnosisMedicineMap();

        for (DiagnosisTemplate diag : diagnoses) {
            var existing = ruleRepository.findByDiagnosisTemplate_IdOrderByMedicine_MedicineNameAsc(diag.getId());
            if (!existing.isEmpty()) {
                ruleRepository.deleteByDiagnosisTemplate_Id(diag.getId());
            }

            String categoryName = diag.getCategory() != null ? diag.getCategory().getName() : "";
            String cat = normalizeText(categoryName);

            // preferred medicines by diagnosis (explicit mapping)
            Set<Medicine> candidates = new LinkedHashSet<>();
            List<String> preferredByDiagnosis = diagnosisMedicineMap
                    .get(normalizeText(diag.getDiagnosisName()));
            if (preferredByDiagnosis != null && !preferredByDiagnosis.isEmpty()) {
                for (String medName : preferredByDiagnosis) {
                    String key = normalizeText(medName);
                    for (Medicine m : medicines) {
                        if (normalizeText(m.getMedicineName()).equals(key)) {
                            candidates.add(m);
                            break;
                        }
                    }
                }
            }

            // preferred medicine types by category (simple heuristic)
            java.util.List<String> preferredTypes = new java.util.ArrayList<>();
            if (cat.contains("ho") || cat.contains("ho hap") || cat.contains("phoi")) {
                preferredTypes.add("khang sinh");
                preferredTypes.add("giam dau");
                preferredTypes.add("ha sot");
                preferredTypes.add("vitamin");
            } else if (cat.contains("tieu") || cat.contains("da day")) {
                preferredTypes.add("khac");
                preferredTypes.add("giam dau");
                preferredTypes.add("ha sot");
            } else if (cat.contains("da") || cat.contains("da lieu")) {
                preferredTypes.add("khac");
            } else if (cat.contains("than kinh")) {
                preferredTypes.add("giam dau");
                preferredTypes.add("ha sot");
                preferredTypes.add("vitamin");
            } else {
                preferredTypes.add("khac");
                preferredTypes.add("vitamin");
            }

            // collect candidate medicines matching preferred types
            if (candidates.isEmpty()) {
                for (String t : preferredTypes) {
                    for (Medicine m : medicines) {
                        String mt = normalizeText(m.getMedicineType());
                        if (mt.isEmpty()) {
                            if ("khac".equals(t)) {
                                candidates.add(m);
                            }
                            continue;
                        }

                        if (mt.contains(t)) {
                            candidates.add(m);
                        }
                    }
                }
            }

            // if not enough candidates, fallback to first active medicines
            if (candidates.isEmpty()) {
                candidates.addAll(medicines.subList(0, Math.min(3, medicines.size())));
            }

            java.util.List<Medicine> candidateList = new java.util.ArrayList<>(candidates);
            int count = Math.min(3, candidateList.size());
            for (int i = 0; i < count; i++) {
                Medicine med = candidateList.get(i);
                DiagnosisMedicineRule rule = new DiagnosisMedicineRule();
                rule.setDiagnosisTemplate(diag);
                rule.setMedicine(med);
                rule.setMinAge(0);
                rule.setMaxAge(200);

                // sensible defaults based on medicine type
                int defaultQty = 1;
                String mt = normalizeText(med.getMedicineType());
                if (mt.contains("kháng")) {
                    // antibiotics: 2 viên/ngày x 7 ngày = 14 viên
                    defaultQty = 14;
                } else if (mt.contains("giam dau") || mt.contains("ha sot")) {
                    // analgesics: 2 viên/ngày, short course
                    defaultQty = 5;
                } else if (mt.contains("vitamin")) {
                    defaultQty = 14;
                } else {
                    // default: 2 viên/ngày
                    defaultQty = 7;
                }

                rule.setDefaultQuantity(defaultQty);
                rule.setDefaultUsage(null);
                ruleRepository.save(rule);
                System.out.println("[Seeder] Added rule for diagnosis=" + diag.getDiagnosisName() + " medicine="
                        + med.getMedicineName() + " defaultQty=" + defaultQty);
            }
        }
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(trimmed, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return withoutAccents.replace('-', ' ');
    }

    private Map<String, List<String>> buildDiagnosisMedicineMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();

        map.put(normalizeText("Viêm họng cấp"), List.of(
                "Amoxicillin 500mg",
                "Dextromethorphan 15mg",
                "Paracetamol 500mg"));
        map.put(normalizeText("Viêm mũi dị ứng"), List.of(
                "Cetirizine 10mg",
                "Loratadine 10mg",
                "Paracetamol 500mg"));
        map.put(normalizeText("Viêm phế quản"), List.of(
                "Azithromycin 500mg",
                "Ambroxol 30mg",
                "Paracetamol 500mg"));
        map.put(normalizeText("Hen phế quản"), List.of(
                "Ambroxol 30mg",
                "Dextromethorphan 15mg",
                "Paracetamol 500mg"));
        map.put(normalizeText("Viêm phổi nhẹ"), List.of(
                "Cefuroxime 500mg",
                "Acetylcysteine 200mg",
                "Paracetamol 500mg"));

        map.put(normalizeText("Viêm dạ dày"), List.of(
                "Omeprazole 20mg",
                "Pantoprazole 40mg",
                "Men tieu hoa"));
        map.put(normalizeText("Trào ngược dạ dày thực quản"), List.of(
                "Pantoprazole 40mg",
                "Omeprazole 20mg",
                "Men tieu hoa"));
        map.put(normalizeText("Viêm đại tràng"), List.of(
                "Smecta 3g",
                "Men tieu hoa",
                "Paracetamol 500mg"));
        map.put(normalizeText("Rối loạn tiêu hóa"), List.of(
                "Men tieu hoa",
                "Smecta 3g",
                "Paracetamol 500mg"));
        map.put(normalizeText("Nhiễm khuẩn đường ruột"), List.of(
                "Amoxicillin 500mg",
                "Smecta 3g",
                "Men tieu hoa"));

        map.put(normalizeText("Đau đầu căng thẳng"), List.of(
                "Paracetamol 500mg",
                "Ibuprofen 400mg",
                "Vitamin B Complex"));
        map.put(normalizeText("Migraine"), List.of(
                "Ibuprofen 400mg",
                "Diclofenac 50mg",
                "Vitamin B Complex"));
        map.put(normalizeText("Rối loạn giấc ngủ"), List.of(
                "Vitamin B Complex",
                "Vitamin C 500mg"));
        map.put(normalizeText("Chóng mặt tiền đình"), List.of(
                "Vitamin B Complex",
                "Paracetamol 500mg"));
        map.put(normalizeText("Đau dây thần kinh tọa"), List.of(
                "Diclofenac 50mg",
                "Ibuprofen 400mg",
                "Vitamin B Complex"));

        map.put(normalizeText("Sốt siêu vi"), List.of(
                "Paracetamol 500mg",
                "Ibuprofen 400mg",
                "Vitamin C 500mg"));
        map.put(normalizeText("Suy nhược cơ thể"), List.of(
                "Vitamin C 500mg",
                "Vitamin B Complex",
                "Paracetamol 500mg"));
        map.put(normalizeText("Mệt mỏi do stress"), List.of(
                "Vitamin B Complex",
                "Vitamin C 500mg"));
        map.put(normalizeText("Thiếu máu nhẹ"), List.of(
                "Vitamin B Complex",
                "Vitamin C 500mg"));
        map.put(normalizeText("Nhiễm trùng chưa rõ ổ"), List.of(
                "Amoxicillin 500mg",
                "Paracetamol 500mg",
                "Vitamin C 500mg"));

        map.put(normalizeText("Viêm da dị ứng"), List.of(
                "Loratadine 10mg",
                "Cetirizine 10mg",
                "Hydrocortisone 1%"));
        map.put(normalizeText("Mề đay"), List.of(
                "Cetirizine 10mg",
                "Loratadine 10mg",
                "Fexofenadine 180mg"));
        map.put(normalizeText("Nấm da"), List.of(
                "Clotrimazole 1%",
                "Hydrocortisone 1%",
                "Vitamin C 500mg"));
        map.put(normalizeText("Viêm nang lông"), List.of(
                "Amoxicillin 500mg",
                "Hydrocortisone 1%",
                "Paracetamol 500mg"));
        map.put(normalizeText("Chàm"), List.of(
                "Hydrocortisone 1%",
                "Cetirizine 10mg",
                "Vitamin C 500mg"));

        map.put(normalizeText("Chẩn đoán chưa xác định"), List.of(
                "Paracetamol 500mg",
                "Ibuprofen 400mg",
                "Vitamin C 500mg"));
        map.put(normalizeText("Rối loạn chức năng"), List.of(
                "Vitamin B Complex",
                "Vitamin C 500mg"));
        map.put(normalizeText("Khám tổng quát"), List.of(
                "Vitamin C 500mg",
                "Vitamin B Complex"));

        return map;
    }
}
