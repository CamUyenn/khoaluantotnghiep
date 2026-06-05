package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CashierReceiptResponse {

    private String clinicName;
    private String clinicLogoText;
    private Long invoiceId;
    private Long appointmentId;
    private String patientName;
    private String phoneNumber;
    private LocalDateTime paidAt;
    private List<CashierServiceLineItemResponse> services;
    private List<CashierMedicineLineItemResponse> medicines;
    private BigDecimal totalServiceFee;
    private BigDecimal grandTotal;
    private String formattedReceiptText;
}
