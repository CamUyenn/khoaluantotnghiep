package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MedicalCategoryRequest {
    @NotBlank(message = "Tên nhóm bệnh là bắt buộc")
    @Size(max = 150, message = "Tên nhóm bệnh tối đa 150 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    private Boolean isActive;
}
