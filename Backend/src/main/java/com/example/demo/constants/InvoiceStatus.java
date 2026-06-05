package com.example.demo.constants;

public final class InvoiceStatus {

    public static final String UNPAID = "UNPAID";
    public static final String PAID = "PAID";

    private InvoiceStatus() {
        throw new IllegalStateException("Utility class");
    }
}
