package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminRevenueReportResponse {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String groupBy;
    private String serviceFilter;
    private String medicineFilter;
    private Integer totalInvoices;
    private BigDecimal totalRevenue;
    private BigDecimal totalServiceRevenue;
    private List<AdminRevenueReportItemResponse> items;
    private List<AdminRevenueChartPointResponse> chart;
    private String message;
}
