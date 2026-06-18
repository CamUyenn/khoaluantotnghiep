package com.example.demo.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "SYMPTOM_SERVICE_MAPPINGS")
@Data
public class SymptomServiceMapping {
    @EmbeddedId
    private SymptomServiceMappingId id = new SymptomServiceMappingId();

    @ManyToOne
    @MapsId("symptomId")
    @JoinColumn(name = "symptom_id")
    private SymptomTemplate symptom;

    @ManyToOne
    @MapsId("serviceId")
    @JoinColumn(name = "service_id")
    private MedicalService service;
}
