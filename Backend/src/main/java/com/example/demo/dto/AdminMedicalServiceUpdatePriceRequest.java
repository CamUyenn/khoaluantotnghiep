package com.example.demo.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminMedicalServiceUpdatePriceRequest {

    @NotNull(message = "Giá hiện tại là bắt buộc")
    @DecimalMin(value = "0", message = "Giá hiện tại phải lớn hơn hoặc bằng 0")
    private BigDecimal currentPrice;
}