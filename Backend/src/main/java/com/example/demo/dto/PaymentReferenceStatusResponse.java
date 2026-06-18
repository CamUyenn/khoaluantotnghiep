package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentReferenceStatusResponse {
    private String paymentReference;
    private Long invoiceId;
    private Long appointmentId;
    private String paymentStatus;
    private String transactionStatus;
    private String message;
}