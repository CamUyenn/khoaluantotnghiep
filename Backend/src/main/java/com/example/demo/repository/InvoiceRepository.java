package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

        Optional<Invoice> findByMedicalRecord_Id(Long medicalRecordId);

        Optional<Invoice> findByPaymentReference(String paymentReference);

        List<Invoice> findByIsPaidOrderByIdDesc(Boolean isPaid);

        List<Invoice> findAllByOrderByIdDesc();

        List<Invoice> findByIsPaidAndPaidAtBetweenOrderByPaidAtDesc(
                        Boolean isPaid,
                        LocalDateTime startTime,
                        LocalDateTime endTime);

        List<Invoice> findByIsPaidAndPaymentMethodAndPaidAtBetweenOrderByPaidAtDesc(
                        Boolean isPaid,
                        String paymentMethod,
                        LocalDateTime startTime,
                        LocalDateTime endTime);

        List<Invoice> findByIsPaidFalseAndPaymentMethodIgnoreCaseAndPaymentReferenceIsNotNull(String paymentMethod);
}
