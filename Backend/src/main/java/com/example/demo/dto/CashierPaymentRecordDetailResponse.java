package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CashierPaymentRecordDetailResponse {

    private Long invoiceId;
    private Long appointmentId;
    private Long patientId;
    private String patientName;
    private String phoneNumber;
    private LocalDateTime appointmentTime;
    private String invoiceStatus;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private BigDecimal totalServiceFee;
    private BigDecimal grandTotal;
    private BigDecimal advanceAmount;
    private BigDecimal remainingAmount;
    private BigDecimal consultationFee;
    private BigDecimal consultationCoveredAmount;
    private BigDecimal consultationOutstandingAmount;
    private BigDecimal additionalServiceFee;
    private BigDecimal additionalCoveredAmount;
    private BigDecimal additionalOutstandingAmount;
    private List<CashierServiceLineItemResponse> services;
    private List<CashierServicePaymentBreakdownLineResponse> serviceBreakdown;
    private List<CashierMedicineLineItemResponse> medicines;
}