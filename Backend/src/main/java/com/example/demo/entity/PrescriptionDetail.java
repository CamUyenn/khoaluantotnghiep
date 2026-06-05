package com.example.demo.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "PRESCRIPTION_DETAILS")
@Data
public class PrescriptionDetail {
    @EmbeddedId
    private PrescriptionDetailId id = new PrescriptionDetailId();

    // MANY to 1 medical record
    @ManyToOne
    @MapsId("medicalRecordId")
    @JoinColumn(name = "medical_record_id")
    private MedicalRecord medicalRecord;

    // MANY to 1 medicine
    @ManyToOne
    @MapsId("medicineId")
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    private Integer quantity;

    @jakarta.persistence.Column(name = "usage_instructions", length = 255)
    private String usageInstructions;
}
