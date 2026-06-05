package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CashierTransactionHistoryResponse {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String paymentMethodFilter;
    private Integer totalTransactions;
    private BigDecimal totalAmount;
    private BigDecimal totalCash;
    private BigDecimal totalBankTransfer;
    private BigDecimal totalPos;
    private List<CashierTransactionHistoryItemResponse> transactions;
}
