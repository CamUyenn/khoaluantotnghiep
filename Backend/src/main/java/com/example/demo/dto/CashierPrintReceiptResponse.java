package com.example.demo.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CashierPrintReceiptResponse {

    private Long invoiceId;
    private String printerName;
    private String status;
    private LocalDateTime printedAt;
    private String message;
}
