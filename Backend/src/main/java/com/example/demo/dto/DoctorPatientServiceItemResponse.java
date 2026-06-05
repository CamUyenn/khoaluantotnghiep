package com.example.demo.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DoctorPatientServiceItemResponse {

    private Long serviceId;
    private String serviceName;
    private Integer quantity;
    private BigDecimal actualPrice;
    private String resultNote;
}