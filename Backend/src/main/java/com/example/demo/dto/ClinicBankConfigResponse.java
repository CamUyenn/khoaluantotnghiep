package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClinicBankConfigResponse {
    private String bankBin;
    private String bankAccount;
    private String accountName;
    private String bankName;
}