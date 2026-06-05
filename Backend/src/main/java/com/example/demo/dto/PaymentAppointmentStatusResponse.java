package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentAppointmentStatusResponse {
    private Long appointmentId;
    private Long invoiceId;
    private String paymentMethod;
    private String paymentStatus;
    private String paymentReference;
    private String transactionStatus;
    private String appointmentStatus;
    private String message;
}