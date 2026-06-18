package com.example.demo.config.seeder;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.MedicalService;
import com.example.demo.repository.MedicalServiceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MedicalServiceSeeder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MedicalServiceSeeder.class);

    private static final List<MedicalServiceSeedItem> DEFAULT_MEDICAL_SERVICES = List.of(
            new MedicalServiceSeedItem("Khám tổng quát", new BigDecimal("150000")),
            new MedicalServiceSeedItem("Khám nội tổng quát", new BigDecimal("200000")),
            new MedicalServiceSeedItem("Khám nhi", new BigDecimal("180000")),
            new MedicalServiceSeedItem("Tư vấn dinh dưỡng", new BigDecimal("120000")),
            new MedicalServiceSeedItem("Đo huyết áp", new BigDecimal("30000")),
            new MedicalServiceSeedItem("Khám tiêu hóa", new BigDecimal("150000")),
            new MedicalServiceSeedItem("Nội soi", new BigDecimal("400000")),
            new MedicalServiceSeedItem("Chụp phim", new BigDecimal("100000")),
            new MedicalServiceSeedItem("Chụp X-quang", new BigDecimal("180000")),
            new MedicalServiceSeedItem("Siêu âm", new BigDecimal("220000")),
            new MedicalServiceSeedItem("Chụp MRI", new BigDecimal("1500000")),
            new MedicalServiceSeedItem("Khám da liễu", new BigDecimal("150000")),
            new MedicalServiceSeedItem("Khám hô hấp", new BigDecimal("150000")),
            new MedicalServiceSeedItem("Khám thần kinh", new BigDecimal("150000")));

    @Value("${app.seed.medical-services.enabled:true}")
    private boolean defaultMedicalServicesEnabled;

    private final MedicalServiceRepository medicalServiceRepository;

    @Transactional
    public void seed() {
        if (!defaultMedicalServicesEnabled) {
            return;
        }

        int inserted = 0;
        for (MedicalServiceSeedItem item : DEFAULT_MEDICAL_SERVICES) {
            if (medicalServiceRepository.existsByServiceNameIgnoreCase(item.serviceName())) {
                continue;
            }

            MedicalService service = new MedicalService();
            service.setServiceName(item.serviceName());
            service.setCurrentPrice(item.currentPrice());
            service.setIsActive(true);
            medicalServiceRepository.save(service);
            inserted++;
        }

        LOGGER.info("[MedicalServiceSeeder] Seeded medical services: {} newly inserted", inserted);
    }

    private record MedicalServiceSeedItem(String serviceName, BigDecimal currentPrice) {
    }
}
