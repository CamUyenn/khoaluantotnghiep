package com.example.demo.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminMedicalServiceCreateRequest {

    @NotBlank(message = "Tên dịch vụ là bắt buộc")
    @Size(max = 100, message = "Tên dịch vụ tối đa 100 ký tự")
    private String serviceName;

    @NotNull(message = "Giá hiện tại là bắt buộc")
    @DecimalMin(value = "0", message = "Giá hiện tại phải lớn hơn hoặc bằng 0")
    private BigDecimal currentPrice;

    private Boolean isActive;
}