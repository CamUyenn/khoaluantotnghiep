package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SymptomTemplateRequest {
    @NotNull(message = "categoryId là bắt buộc")
    private Long categoryId;

    @NotBlank(message = "Tên triệu chứng là bắt buộc")
    @Size(max = 200, message = "Tên triệu chứng tối đa 200 ký tự")
    private String symptomName;
}
