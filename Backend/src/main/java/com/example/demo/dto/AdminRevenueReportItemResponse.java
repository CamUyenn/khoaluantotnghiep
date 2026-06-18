package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminRevenueReportItemResponse {

    private Long invoiceId;
    private Long appointmentId;
    private String patientName;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private BigDecimal totalServiceFee;
    private BigDecimal grandTotal;
    private List<String> services;
    private List<String> medicines;
}
