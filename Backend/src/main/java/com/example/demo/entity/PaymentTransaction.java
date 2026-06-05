package com.example.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "PAYMENT_TRANSACTIONS")
@Data
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_transaction_id", nullable = false, unique = true, length = 100)
    private String externalTransactionId;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;
}
