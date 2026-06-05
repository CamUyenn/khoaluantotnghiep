package com.example.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import com.example.demo.constants.InvoiceStatus;

@Entity
@Table(name = "INVOICES")
@Data
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "medical_record_id")
    private MedicalRecord medicalRecord;

    @Column(name = "total_service_fee", precision = 18, scale = 0, nullable = false)
    private BigDecimal totalServiceFee;

    @Column(name = "total_medicine_fee", precision = 18, scale = 0, nullable = false)
    private BigDecimal totalMedicineFee;

    @Column(name = "total_amount", precision = 18, scale = 0)
    private BigDecimal grandTotal;

    @Column(name = "is_paid")
    private Boolean isPaid;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "payment_reference", length = 200)
    private String paymentReference;

    @Column(name = "advance_amount", precision = 18, scale = 0)
    private BigDecimal advanceAmount;

    @Column(name = "remaining_amount", precision = 18, scale = 0)
    private BigDecimal remainingAmount;

    @Column(name = "invoice_status", length = 30)
    private String invoiceStatus;

    public Appointment getAppointment() {
        return medicalRecord == null ? null : medicalRecord.getAppointment();
    }

    public void setAppointment(Appointment appointment) {
        // Use setMedicalRecord(...) for persistence.
    }

    public Patient getPatient() {
        Appointment appointment = getAppointment();
        return appointment == null ? null : appointment.getPatient();
    }

    public void setPatient(Patient patient) {
        // Derived from appointment; stored for API compatibility only.
    }

    @PrePersist
    @PreUpdate
    private void syncInvoiceStatus() {
        this.invoiceStatus = Boolean.TRUE.equals(this.isPaid)
                ? InvoiceStatus.PAID
                : InvoiceStatus.UNPAID;
    }
}
