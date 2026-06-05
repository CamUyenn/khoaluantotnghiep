package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CashierTransactionHistoryItemResponse {

    private Long invoiceId;
    private Long appointmentId;
    private Long patientId;
    private String patientName;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private BigDecimal grandTotal;
}
