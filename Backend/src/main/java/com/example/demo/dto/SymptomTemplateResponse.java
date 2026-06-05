package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SymptomTemplateResponse {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String symptomName;
}
