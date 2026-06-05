package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AppointmentFeeEstimateResponse {
    private BigDecimal estimatedTotalFee;
    private List<CashierServiceLineItemResponse> services;
}