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
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "APPOINTMENTS")
@Data
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY appointments → 1 patient
    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    // MANY appointments → 1 user(role=DOCTOR)
    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private User doctor;

    // MANY appointments → 1 medical category
    @ManyToOne
    @JoinColumn(name = "category_id")
    private MedicalCategory category;

    // MANY appointments → 1 room (assigned by receptionist)
    @ManyToOne
    @JoinColumn(name = "assigned_room_id")
    private Room assignedRoom;

    @Column(name = "appointment_time")
    private LocalDateTime appointmentTime;

    // PENDING, APPROVED, WAITING_CASHIER, IN_ROOM, IN_PROGRESS, COMPLETED
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "symptoms", length = 500)
    private String symptoms;

    @Column(name = "symptoms_text", length = 500)
    private String symptomsText;

    @Column(name = "estimated_total_fee", precision = 18, scale = 0)
    private BigDecimal estimatedTotalFee;

    @Column(name = "advance_payment", precision = 18, scale = 0)
    private BigDecimal advancePayment;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "payment_status", length = 30)
    private String paymentStatus;

    @Column(name = "payment_reference", length = 200)
    private String paymentReference;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;
}
