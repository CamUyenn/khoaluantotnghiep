package com.example.demo.dto;

import lombok.Data;

@Data
public class CashierProcessPaymentRequest {

    private String paymentMethod;
    private Boolean paymentSuccessful;
    private Boolean exportInvoice;
    private Boolean applyHealthInsurance;
}
