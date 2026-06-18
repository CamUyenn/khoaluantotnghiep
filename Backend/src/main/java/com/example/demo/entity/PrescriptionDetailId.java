package com.example.demo.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class PrescriptionDetailId implements Serializable {

    @Column(name = "medical_record_id")
    private Long medicalRecordId;

    @Column(name = "medicine_id")
    private Long medicineId;
}
