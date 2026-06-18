package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.PaymentTransaction;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByExternalTransactionId(String externalTransactionId);

    Optional<PaymentTransaction> findTopByPaymentReferenceOrderByIdDesc(String paymentReference);

    Optional<PaymentTransaction> findTopByAppointment_IdOrderByIdDesc(Long appointmentId);

    Optional<PaymentTransaction> findTopByInvoice_IdOrderByIdDesc(Long invoiceId);

    List<PaymentTransaction> findByInvoice_IdOrderByIdAsc(Long invoiceId);

    List<PaymentTransaction> findByPaymentReferenceAndInvoiceIsNullOrderByIdAsc(String paymentReference);

}
