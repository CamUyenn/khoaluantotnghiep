package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.demo.constants.PaymentStatus;
import com.example.demo.entity.Invoice;
import com.example.demo.entity.PaymentTransaction;
import com.example.demo.repository.InvoiceRepository;
import com.example.demo.repository.PaymentTransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationJob {

    private static final String TRANSFER_METHOD = "CHUYEN_KHOAN";
    private final InvoiceRepository invoiceRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final InvoiceService invoiceService;
    private final SepayClient sepayClient;

    @Value("${payments.reconciliation.enabled:true}")
    private boolean enabled;

    @Value("${payments.reconciliation.lookback-minutes:1440}")
    private long lookbackMinutes;

    @Value("${sepay.api.enabled:true}")
    private boolean sepayEnabled;

    @Value("${sepay.api.require-amount-match:true}")
    private boolean requireAmountMatch;

    @Value("${sepay.api.amount-tolerance:0}")
    private BigDecimal amountTolerance;

    @Scheduled(fixedDelayString = "${payments.reconciliation.fixed-delay-ms:180000}", initialDelayString = "${payments.reconciliation.initial-delay-ms:30000}")
    public void reconcilePendingTransfers() {
        if (!enabled) {
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(lookbackMinutes);
        if (!sepayEnabled) {
            return;
        }

        List<Invoice> pendingInvoices = invoiceRepository
                .findByIsPaidFalseAndPaymentMethodIgnoreCaseAndPaymentReferenceIsNotNull(TRANSFER_METHOD);

        Map<String, SepayClient.SepayTransaction> sepayMatches = indexSepayTransactionsByReference(
                sepayClient.fetchTransactions(),
                cutoff);

        int markedPaid = 0;
        for (Invoice invoice : pendingInvoices) {
            if (invoice == null || Boolean.TRUE.equals(invoice.getIsPaid())) {
                continue;
            }

            String paymentReference = invoice.getPaymentReference();
            if (paymentReference == null || paymentReference.isBlank()) {
                continue;
            }

            SepayClient.SepayTransaction sepayTx = sepayMatches.get(normalizeReference(paymentReference));
            if (sepayTx != null && isSepayPaymentValid(invoice, sepayTx)) {
                PaymentTransaction saved = ensurePaymentTransaction(invoice, sepayTx);
                linkTransaction(invoice, saved);
                invoiceService.markTransferInvoiceAsPaid(invoice.getId(), TRANSFER_METHOD);
                markedPaid++;
                continue;
            }

            PaymentTransaction existing = findExistingSuccessTransaction(paymentReference, cutoff);
            if (existing != null) {
                linkTransaction(invoice, existing);
                invoiceService.markTransferInvoiceAsPaid(invoice.getId(), TRANSFER_METHOD);
                markedPaid++;
            }
        }

        for (Map.Entry<String, SepayClient.SepayTransaction> entry : sepayMatches.entrySet()) {
            String paymentReference = entry.getKey();
            if (paymentReference == null || paymentReference.isBlank()) {
                continue;
            }

            Invoice invoice = invoiceRepository.findByPaymentReference(paymentReference).orElse(null);
            if (invoice == null || Boolean.TRUE.equals(invoice.getIsPaid())) {
                continue;
            }

            SepayClient.SepayTransaction sepayTx = entry.getValue();
            if (sepayTx == null || !isSepayPaymentValid(invoice, sepayTx)) {
                continue;
            }

            PaymentTransaction saved = ensurePaymentTransaction(invoice, sepayTx);
            linkTransaction(invoice, saved);
            invoiceService.markTransferInvoiceAsPaid(invoice.getId(), TRANSFER_METHOD);
            markedPaid++;
        }

        if (markedPaid > 0) {
            log.info("Payment reconciliation done: markedPaid={}", markedPaid);
        }
    }

    private Map<String, SepayClient.SepayTransaction> indexSepayTransactionsByReference(
            List<SepayClient.SepayTransaction> transactions,
            LocalDateTime cutoff) {
        Map<String, SepayClient.SepayTransaction> mapped = new HashMap<>();
        if (transactions == null || transactions.isEmpty()) {
            return mapped;
        }

        for (SepayClient.SepayTransaction tx : transactions) {
            if (tx == null) {
                continue;
            }
            LocalDateTime receivedAt = sepayClient.parseTransactionTime(tx.transaction_date);
            if (receivedAt != null && receivedAt.isBefore(cutoff)) {
                continue;
            }
            String reference = resolveSepayReference(tx);
            if (reference == null) {
                continue;
            }
            mapped.putIfAbsent(reference, tx);
        }
        return mapped;
    }

    private String resolveSepayReference(SepayClient.SepayTransaction tx) {
        String reference = normalizePaymentReference(tx.code);
        if (reference != null) {
            return reference;
        }
        reference = normalizePaymentReference(tx.reference_number);
        if (reference != null) {
            return reference;
        }
        String content = tx.transaction_content;
        if (content == null || content.isBlank()) {
            return null;
        }
        String extracted = extractPaymentReferenceFromText(content);
        return normalizePaymentReference(extracted);
    }

    private String normalizeReference(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private String extractPaymentReferenceFromText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("REF\\s*[:=]?\\s*(BK[0-9A-Z-]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(value);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        matcher = java.util.regex.Pattern
                .compile("BK-INV-\\d+", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(value);
        if (matcher.find()) {
            return matcher.group().trim();
        }

        matcher = java.util.regex.Pattern
                .compile("BKINV\\d+", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(value);
        if (matcher.find()) {
            return matcher.group().trim();
        }

        matcher = java.util.regex.Pattern
                .compile("BK\\d{8}[A-Z0-9]{6}", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(value);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return null;
    }

    private String normalizePaymentReference(String rawReference) {
        if (rawReference == null) {
            return null;
        }
        String trimmed = rawReference.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        String normalized = trimmed.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("BK-INV-") || normalized.startsWith("BKINV")) {
            return normalized.startsWith("BK-INV-")
                    ? normalized
                    : normalized.replaceFirst("BKINV", "BK-INV-");
        }

        String compact = normalized.replaceAll("[^A-Z0-9]", "");
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^BK(\\d{8})([A-Z0-9]{6})$")
                .matcher(compact);
        if (matcher.find()) {
            return "BK-" + matcher.group(1) + "-" + matcher.group(2);
        }
        return normalized;
    }

    private boolean isSepayPaymentValid(Invoice invoice, SepayClient.SepayTransaction sepayTx) {
        if (invoice == null || sepayTx == null) {
            return false;
        }
        BigDecimal amountIn = sepayTx.getAmountIn();
        if (amountIn == null || amountIn.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (!requireAmountMatch) {
            return true;
        }

        BigDecimal expected = invoice.getGrandTotal();
        if (expected == null) {
            return true;
        }
        BigDecimal tolerance = amountTolerance == null ? BigDecimal.ZERO : amountTolerance;
        BigDecimal min = expected.subtract(tolerance);
        return amountIn.compareTo(min) >= 0;
    }

    private PaymentTransaction ensurePaymentTransaction(Invoice invoice, SepayClient.SepayTransaction sepayTx) {
        String externalId = sepayTx.id == null || sepayTx.id.isBlank()
                ? String.valueOf(System.currentTimeMillis())
                : sepayTx.id.trim();

        PaymentTransaction existing = paymentTransactionRepository.findByExternalTransactionId(externalId)
                .orElse(null);
        if (existing != null) {
            existing.setStatus("SUCCESS");
            if (existing.getAmount() == null) {
                existing.setAmount(sepayTx.getAmountIn());
            }
            if (existing.getPaymentReference() == null || existing.getPaymentReference().isBlank()) {
                existing.setPaymentReference(invoice.getPaymentReference());
            }
            return paymentTransactionRepository.save(existing);
        }

        PaymentTransaction tx = new PaymentTransaction();
        tx.setExternalTransactionId(externalId);
        tx.setPaymentReference(invoice.getPaymentReference());
        tx.setProvider("SePay");
        tx.setPaymentMethod(TRANSFER_METHOD);
        tx.setAmount(sepayTx.getAmountIn());
        tx.setReceivedAt(sepayClient.parseTransactionTime(sepayTx.transaction_date));
        tx.setStatus("SUCCESS");
        tx.setRawPayload(sepayClient.toRawPayload(sepayTx));
        tx.setInvoice(invoice);
        if (invoice.getAppointment() != null) {
            tx.setAppointment(invoice.getAppointment());
        }
        return paymentTransactionRepository.save(tx);
    }

    private PaymentTransaction findExistingSuccessTransaction(String paymentReference, LocalDateTime cutoff) {
        if (paymentReference == null || paymentReference.isBlank()) {
            return null;
        }
        PaymentTransaction tx = paymentTransactionRepository
                .findTopByPaymentReferenceOrderByIdDesc(paymentReference)
                .orElse(null);
        if (tx == null || !"SUCCESS".equalsIgnoreCase(tx.getStatus())) {
            return null;
        }
        LocalDateTime receivedAt = tx.getReceivedAt();
        if (receivedAt != null && receivedAt.isBefore(cutoff)) {
            return null;
        }
        return tx;
    }

    private void linkTransaction(Invoice invoice, PaymentTransaction tx) {
        if (invoice == null || tx == null) {
            return;
        }
        tx.setInvoice(invoice);
        if (invoice.getAppointment() != null) {
            tx.setAppointment(invoice.getAppointment());
            tx.setPaymentReference(invoice.getPaymentReference());
        }
        if (tx.getPaymentMethod() == null || tx.getPaymentMethod().isBlank()) {
            tx.setPaymentMethod(TRANSFER_METHOD);
        }
        paymentTransactionRepository.save(tx);

        if (invoice.getAppointment() != null
                && !PaymentStatus.FULLY_PAID.equalsIgnoreCase(invoice.getAppointment().getPaymentStatus())) {
            invoice.getAppointment().setPaymentStatus(PaymentStatus.FULLY_PAID);
        }
    }
}