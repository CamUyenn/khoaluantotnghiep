package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SymptomServiceMappingResponse {
    private Long symptomId;
    private String symptomName;
    private Long serviceId;
    private String serviceName;
    private String categoryName;
}
