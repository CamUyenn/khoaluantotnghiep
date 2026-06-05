package com.example.demo.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "MEDICAL_RECORD_SERVICES")
@Data
public class MedicalRecordServiceDetail {

    @EmbeddedId
    private MedicalRecordServiceId id = new MedicalRecordServiceId();

    @ManyToOne
    @MapsId("medicalRecordId")
    @JoinColumn(name = "medical_record_id")
    private MedicalRecord medicalRecord;

    @ManyToOne
    @MapsId("serviceId")
    @JoinColumn(name = "service_id")
    private MedicalService service;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "actual_price", precision = 18, scale = 0, nullable = false)
    private BigDecimal actualPrice;

    @Column(name = "result_note", columnDefinition = "TEXT")
    private String resultNote;
}
