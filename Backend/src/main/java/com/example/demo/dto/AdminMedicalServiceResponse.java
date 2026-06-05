package com.example.demo.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminMedicalServiceResponse {

    private Long id;
    private String serviceName;
    private BigDecimal currentPrice;
    private Boolean isActive;
}