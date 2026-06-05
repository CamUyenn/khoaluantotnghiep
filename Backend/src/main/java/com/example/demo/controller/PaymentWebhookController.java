package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.dto.ClinicBankConfigResponse;
import com.example.demo.dto.PaymentAppointmentStatusResponse;
import com.example.demo.dto.PaymentReferenceStatusResponse;
import com.example.demo.entity.Appointment;
import com.example.demo.entity.Invoice;
import com.example.demo.service.InvoiceService;
import com.example.demo.constants.PaymentStatus;

import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "https://webhook.site")
public class PaymentWebhookController {

    private final InvoiceService invoiceService;
    private final com.example.demo.repository.PaymentTransactionRepository paymentTransactionRepository;
    private final com.example.demo.repository.InvoiceRepository invoiceRepository;
    private final com.example.demo.repository.AppointmentRepository appointmentRepository;
    private final com.example.demo.service.AppointmentService appointmentService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Value("${payments.webhook.secret:}")
    private String webhookSecret;

    @Value("${sepay.webhook.token:}")
    private String validSepayToken;

    @Value("${clinic.bank.bin:}")
    private String clinicBankBin;

    @Value("${clinic.bank.account:}")
    private String clinicBankAccount;

    @Value("${clinic.bank.account-name:}")
    private String clinicBankAccountName;

    @Value("${clinic.bank.name:}")
    private String clinicBankName;

    @PostMapping(value = "/webhook")
    @Operation(summary = "Webhook callback for external payments", description = "Process payment notifications from VNPAY/Momo.")
    public ResponseEntity<?> handleWebhook(
            HttpServletRequest request,
            @RequestParam(name = "signature", required = false) String signature,
            @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String apiToken) {
        if (apiToken != null && !apiToken.isBlank()) {
            String incomingToken;
            if (apiToken.startsWith("Apikey ")) {
                incomingToken = apiToken.replace("Apikey ", "").trim();
            } else if (apiToken.startsWith("Bearer ")) {
                incomingToken = apiToken.replace("Bearer ", "").trim();
            } else {
                log.warn("SePay webhook rejected: missing or invalid Authorization header");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Tu choi truy cap: Thieu hoac sai dinh dang Token");
            }
            if (validSepayToken == null || validSepayToken.isBlank()) {
                log.warn("SePay webhook rejected: server token not configured");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Tu choi truy cap: Token backend chua duoc cau hinh");
            }

            if (!validSepayToken.equals(incomingToken)) {
                log.warn("SePay webhook rejected: token mismatch");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Tu choi truy cap: Sai API Key");
            }
        }
        WebhookPayload payload = readWebhookPayload(request);
        if (payload == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing webhook payload");
        }

        log.info(
                "Webhook received: invoiceId={}, medicalRecordId={}, externalTransactionId={}, transactionId={}, paymentReference={}, status={}, amount={}, provider={}",
                payload.getInvoiceId(),
                payload.getMedicalRecordId(),
                payload.getExternalTransactionId(),
                payload.getTransactionId(),
                payload.getPaymentReference(),
                payload.getStatus(),
                payload.getAmount(),
                payload.getProvider());

        // Xác minh chữ ký nếu có mã bí mật được cung cấp.
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            boolean ok = verifySignature(signature, payload);
            if (!ok) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid webhook signature");
            }
        }

        String resolvedTransactionId = resolveTransactionId(payload);
        String resolvedPaymentReference = resolvePaymentReference(payload);
        boolean failureStatus = isFailureStatus(payload.getStatus());
        boolean successStatus = isSuccessStatus(payload.getStatus());

        // Tính bất biến: kiểm tra giao dịch hiện có
        java.util.Optional<com.example.demo.entity.PaymentTransaction> existing = paymentTransactionRepository
                .findByExternalTransactionId(resolvedTransactionId);
        if (existing.isPresent() && "SUCCESS".equalsIgnoreCase(existing.get().getStatus())) {
            com.example.demo.entity.PaymentTransaction existingTx = existing.get();
            Long payloadInvoiceId = payload == null ? null : payload.getInvoiceId();
            Long payloadMedicalId = payload == null ? null : payload.getMedicalRecordId();
            Long invoiceIdFromReference = extractInvoiceIdFromReference(resolvedPaymentReference);
            com.example.demo.entity.Invoice invoice = null;
            if (payloadInvoiceId != null) {
                invoice = invoiceRepository.findById(payloadInvoiceId).orElse(null);
                if (invoice == null && payloadMedicalId != null) {
                    invoice = invoiceRepository.findByMedicalRecord_Id(payloadMedicalId).orElse(null);
                }
                if (invoice == null && resolvedPaymentReference != null) {
                    invoice = invoiceRepository.findByPaymentReference(resolvedPaymentReference).orElse(null);
                }
                if (invoice == null && invoiceIdFromReference != null) {
                    invoice = invoiceRepository.findById(invoiceIdFromReference).orElse(null);
                }
            } else if (payloadMedicalId != null) {
                invoice = invoiceRepository.findByMedicalRecord_Id(payloadMedicalId).orElse(null);
            } else {
                if (resolvedPaymentReference != null) {
                    invoice = invoiceRepository.findByPaymentReference(resolvedPaymentReference).orElse(null);
                }
                if (invoice == null && invoiceIdFromReference != null) {
                    invoice = invoiceRepository.findById(invoiceIdFromReference).orElse(null);
                }
            }

            if (invoice != null) {
                existingTx.setInvoice(invoice);
                if (invoice.getAppointment() != null) {
                    existingTx.setAppointment(invoice.getAppointment());
                }
                if (existingTx.getPaymentReference() == null || existingTx.getPaymentReference().isBlank()) {
                    existingTx.setPaymentReference(resolvedPaymentReference);
                }
                paymentTransactionRepository.save(existingTx);
                if (!Boolean.TRUE.equals(invoice.getIsPaid())) {
                    invoiceService.markTransferInvoiceAsPaid(invoice.getId(),
                            payload.getPaymentMethod() == null ? "CHUYEN_KHOAN" : payload.getPaymentMethod());
                }
                notifyCashierRefresh(invoice.getId());
            }

            log.info("Webhook duplicate success ignored: transactionId={}", resolvedTransactionId);
            return ResponseEntity.ok("Already processed");
        }

        // Tạo hồ sơ giao dịch
        com.example.demo.entity.PaymentTransaction tx = new com.example.demo.entity.PaymentTransaction();
        tx.setExternalTransactionId(resolvedTransactionId);
        tx.setPaymentReference(resolvedPaymentReference);
        tx.setProvider(payload.getProvider());
        tx.setPaymentMethod(payload.getPaymentMethod() == null ? "CHUYEN_KHOAN" : payload.getPaymentMethod());
        tx.setAmount(payload.getAmount());
        tx.setReceivedAt(java.time.LocalDateTime.now());
        tx.setRawPayload(payload.getRawPayload());

        // Hãy thử giải quyết hóa đơn theo ID nếu có.
        com.example.demo.entity.Invoice invoice = null;
        Long payloadInvoiceId = payload == null ? null : payload.getInvoiceId();
        Long payloadMedicalId = payload == null ? null : payload.getMedicalRecordId();
        Long invoiceIdFromReference = extractInvoiceIdFromReference(resolvedPaymentReference);
        if (payloadInvoiceId != null) {
            invoice = invoiceRepository.findById(payloadInvoiceId).orElse(null);
            if (invoice == null && payloadMedicalId != null) {
                invoice = invoiceRepository.findByMedicalRecord_Id(payloadMedicalId).orElse(null);
            }
            if (invoice == null && resolvedPaymentReference != null) {
                invoice = invoiceRepository.findByPaymentReference(resolvedPaymentReference).orElse(null);
            }
            if (invoice == null && invoiceIdFromReference != null) {
                invoice = invoiceRepository.findById(invoiceIdFromReference).orElse(null);
            }
        } else if (payloadMedicalId != null) {
            invoice = invoiceRepository.findByMedicalRecord_Id(payloadMedicalId).orElse(null);
        } else {
            if (resolvedPaymentReference != null) {
                invoice = invoiceRepository.findByPaymentReference(resolvedPaymentReference).orElse(null);
            }
            if (invoice == null && invoiceIdFromReference != null) {
                invoice = invoiceRepository.findById(invoiceIdFromReference).orElse(null);
            }
        }

        if (invoice != null && invoice.getAppointment() != null) {
            tx.setAppointment(invoice.getAppointment());
        }

        String initialStatus = failureStatus ? "FAILED" : (successStatus ? "SUCCESS" : "PENDING");
        tx.setStatus(initialStatus);

        // Giao dịch vẫn tiếp diễn (chưa có liên kết hóa đơn)
        paymentTransactionRepository.save(tx);

        if (successStatus && invoice == null) {
            String paymentCode = resolvedPaymentReference != null && !resolvedPaymentReference.isBlank()
                    ? resolvedPaymentReference
                    : resolvedTransactionId;
            if (paymentCode != null && !paymentCode.isBlank()) {
                messagingTemplate.convertAndSend("/topic/payments/" + paymentCode, "PAYMENT_SUCCESS");
                log.info("WebSocket payment event sent: destination=/topic/payments/{}, code={}",
                        paymentCode,
                        paymentCode);
            }
        }

        // If webhook indicates success, process payment on invoice
        if (!failureStatus && invoice != null && successStatus) {
            try {
                Long invoiceIdNullable = invoice.getId();
                if (invoiceIdNullable == null) {
                    tx.setStatus("FAILED");
                    paymentTransactionRepository.save(tx);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invoice id is null");
                }
                Long invoiceId = java.util.Objects.requireNonNull(invoiceIdNullable);

                invoiceService.markTransferInvoiceAsPaid(invoiceId,
                        payload.getPaymentMethod() == null ? "CHUYEN_KHOAN" : payload.getPaymentMethod());
                // Đính kèm hóa đơn vào giao dịch
                tx.setInvoice(invoice);
                if (invoice.getAppointment() != null) {
                    tx.setAppointment(invoice.getAppointment());
                }
                tx.setStatus("SUCCESS");
                paymentTransactionRepository.save(tx);

                // Lưu trữ tham chiếu bên ngoài trên hóa đơn
                try {
                    invoice.setPaymentReference(resolvedPaymentReference);
                    invoiceRepository.save(invoice);
                } catch (Exception ex) {
                    // ignore persistence failures here
                }

                String paymentCode = resolvedPaymentReference != null && !resolvedPaymentReference.isBlank()
                        ? resolvedPaymentReference
                        : resolvedTransactionId;
                if (paymentCode != null && !paymentCode.isBlank()) {
                    messagingTemplate.convertAndSend("/topic/payments/" + paymentCode, "PAYMENT_SUCCESS");
                    log.info("WebSocket payment event sent: destination=/topic/payments/{}, code={}",
                            paymentCode,
                            paymentCode);
                }

                notifyCashierRefresh(invoice.getId());

                log.info("Webhook processed successfully: transactionId={}, invoiceId={}, appointmentId={}",
                        resolvedTransactionId,
                        invoiceId,
                        invoice.getAppointment() == null ? null : invoice.getAppointment().getId());

                return ResponseEntity.ok("Processed");
            } catch (Exception ex) {
                tx.setStatus("FAILED");
                paymentTransactionRepository.save(tx);
                log.error("Webhook processing failed: transactionId={}, reason={}", resolvedTransactionId,
                        ex.getMessage(), ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Processing failed: " + ex.getMessage());
            }
        }

        if (invoice != null && failureStatus) {
            try {
                Appointment appointment = invoice.getAppointment();
                if (appointment != null) {
                    tx.setAppointment(appointment);
                    appointmentService.cancelAppointmentBySystem(
                            appointment.getId(),
                            "Thanh toán chuyển khoản thất bại");
                }
                log.warn("Webhook failure processed: transactionId={}, invoiceId={}, appointmentId={}",
                        resolvedTransactionId,
                        invoice.getId(),
                        appointment == null ? null : appointment.getId());
            } catch (Exception ignored) {
                // keep webhook idempotent even if cancellation fails
            }
        }

        if (invoice != null) {
            tx.setInvoice(invoice);
            if (invoice.getAppointment() != null) {
                tx.setAppointment(invoice.getAppointment());
            }
        }
        paymentTransactionRepository.save(tx);
        log.info("Webhook stored transaction: transactionId={}, invoiceFound={}, status={}",
                resolvedTransactionId,
                invoice != null,
                tx.getStatus());
        return ResponseEntity.ok("Received");
    }

    @PostMapping(value = "/webhook/debug")
    @Operation(summary = "Debug webhook payload", description = "Nhan payload SePay thu cong de kiem tra backend co bat duoc hay khong.")
    public ResponseEntity<?> debugWebhook(HttpServletRequest request) {
        WebhookPayload payload = readWebhookPayload(request);
        if (payload == null) {
            return ResponseEntity.badRequest().body("Missing webhook payload");
        }

        log.info(
                "Webhook debug received: invoiceId={}, medicalRecordId={}, externalTransactionId={}, transactionId={}, paymentReference={}, status={}, amount={}, provider={}, rawPayload={}",
                payload.getInvoiceId(),
                payload.getMedicalRecordId(),
                payload.getExternalTransactionId(),
                payload.getTransactionId(),
                payload.getPaymentReference(),
                payload.getStatus(),
                payload.getAmount(),
                payload.getProvider(),
                payload.getRawPayload());

        return ResponseEntity.ok(java.util.Map.of(
                "received", true,
                "invoiceId", payload.getInvoiceId(),
                "medicalRecordId", payload.getMedicalRecordId(),
                "externalTransactionId", payload.getExternalTransactionId(),
                "transactionId", payload.getTransactionId(),
                "paymentReference", payload.getPaymentReference(),
                "status", payload.getStatus(),
                "amount", payload.getAmount(),
                "provider", payload.getProvider()));
    }

    @GetMapping(value = "/bank-config")
    @Operation(summary = "Cấu hình ngân hàng cho QR", description = "Trả về thông tin ngân hàng hardcode để frontend tạo QR đồng nhất.")
    public ClinicBankConfigResponse getClinicBankConfig() {
        return new ClinicBankConfigResponse(
                clinicBankBin,
                clinicBankAccount,
                clinicBankAccountName,
                clinicBankName);
    }

    @GetMapping(value = "/appointments/{appointmentId}/status")
    @Operation(summary = "Trạng thái thanh toán của lịch hẹn", description = "Tra cứu trạng thái thanh toán theo lịch hẹn để frontend polling.")
    public PaymentAppointmentStatusResponse getAppointmentPaymentStatus(@PathVariable Long appointmentId) {
        java.util.Objects.requireNonNull(appointmentId, "appointmentId is required");
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy lịch hẹn"));

        Invoice invoice = null;
        if (appointment.getPaymentReference() != null && !appointment.getPaymentReference().isBlank()) {
            invoice = invoiceRepository.findByPaymentReference(appointment.getPaymentReference()).orElse(null);
        }
        String transactionStatus = null;
        if (invoice != null && invoice.getId() != null) {
            var tx = paymentTransactionRepository.findTopByInvoice_IdOrderByIdDesc(invoice.getId()).orElse(null);
            if (tx != null) {
                transactionStatus = tx.getStatus();
            }
        }

        String paymentStatus = appointment.getPaymentStatus();
        String message;
        if (PaymentStatus.FULLY_PAID.equalsIgnoreCase(paymentStatus)) {
            message = "Đã thanh toán thành công";
        } else if (PaymentStatus.PENDING_TRANSFER.equalsIgnoreCase(paymentStatus)) {
            message = "Đang chờ xác nhận thanh toán chuyển khoản";
        } else if (PaymentStatus.UNPAID.equalsIgnoreCase(paymentStatus)) {
            message = "Chưa thanh toán";
        } else {
            message = "Đang chờ xử lý thanh toán";
        }

        return new PaymentAppointmentStatusResponse(
                appointment.getId(),
                invoice == null ? null : invoice.getId(),
                appointment.getPaymentMethod(),
                paymentStatus,
                appointment.getPaymentReference(),
                transactionStatus,
                appointment.getStatus(),
                message);
    }

    @GetMapping(value = "/reference/{paymentReference}/status")
    @Operation(summary = "Trạng thái thanh toán theo mã tham chiếu", description = "Tra cứu giao dịch SePay/VietQR trước khi lịch hẹn được tạo.")
    public PaymentReferenceStatusResponse getPaymentReferenceStatus(@PathVariable String paymentReference) {
        com.example.demo.entity.PaymentTransaction tx = paymentTransactionRepository
                .findTopByPaymentReferenceOrderByIdDesc(paymentReference)
                .orElseGet(
                        () -> paymentTransactionRepository.findByExternalTransactionId(paymentReference).orElse(null));

        log.info("Payment reference status lookup: reference={}, foundTransaction={}, txStatus={}",
                paymentReference,
                tx != null,
                tx == null ? null : tx.getStatus());

        if (tx == null) {
            return new PaymentReferenceStatusResponse(
                    paymentReference,
                    null,
                    null,
                    PaymentStatus.UNPAID,
                    "PENDING",
                    "Đang chờ ngân hàng xác nhận giao dịch");
        }

        Invoice invoice = invoiceRepository.findByPaymentReference(paymentReference).orElse(null);
        Long invoiceId = invoice == null ? null : invoice.getId();
        Long appointmentId = invoice == null || invoice.getAppointment() == null
                ? null
                : invoice.getAppointment().getId();

        String transactionStatus = tx.getStatus();
        String normalized = transactionStatus == null ? "" : transactionStatus.trim().toUpperCase();
        String paymentStatus;
        String message;
        if ("SUCCESS".equals(normalized)) {
            paymentStatus = PaymentStatus.FULLY_PAID;
            message = "Đã nộp tiền thành công";
        } else if ("FAILED".equals(normalized)) {
            paymentStatus = PaymentStatus.UNPAID;
            message = "Thanh toán thất bại";
        } else {
            paymentStatus = PaymentStatus.PENDING_TRANSFER;
            message = "Đang chờ ngân hàng xác nhận giao dịch";
        }

        log.info(
                "Payment reference status resolved: reference={}, invoiceId={}, appointmentId={}, paymentStatus={}, transactionStatus={}",
                paymentReference,
                invoiceId,
                appointmentId,
                paymentStatus,
                transactionStatus);

        return new PaymentReferenceStatusResponse(
                paymentReference,
                invoiceId,
                appointmentId,
                paymentStatus,
                transactionStatus,
                message);
    }

    private boolean verifySignature(String signature, WebhookPayload payload) {
        if (signature == null || signature.isBlank())
            return false;
        try {
            String payloadStr = payload.getRawPayload() == null ? payload.toString() : payload.getRawPayload();
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                    webhookSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmac = mac.doFinal(payloadStr.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String computed = java.util.Base64.getEncoder().encodeToString(hmac);
            return computed.equals(signature);
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isFailureStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toUpperCase();
        return "FAILED".equals(normalized) || "FAIL".equals(normalized) || "CANCELLED".equals(normalized)
                || "CANCELED".equals(normalized);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookPayload {
        private Long invoiceId;
        private Long medicalRecordId;
        private String externalTransactionId;
        @JsonAlias({ "transactionId", "transaction_id", "bankTransactionId", "bank_transaction_id", "id" })
        private String transactionId;
        @JsonAlias({ "paymentReference", "payment_reference", "reference", "content", "description", "note", "memo",
                "remark", "comment" })
        private String paymentReference;
        private String provider;
        private java.math.BigDecimal amount;
        private String rawPayload;
        private String status;
        private String paymentMethod;
        @JsonAlias({ "transferAmount", "transfer_amount", "amount" })
        private java.math.BigDecimal transferAmount;
        @JsonAlias({ "transferType", "transfer_type", "direction" })
        private String transferType;
        @JsonAlias({ "content", "note", "memo", "remark", "comment" })
        private String content;
        @JsonAlias({ "description" })
        private String description;
        @JsonAlias({ "referenceCode", "reference_code", "ref" })
        private String referenceCode;
        @JsonAlias({ "gateway", "bank", "bankName" })
        private String gateway;
        @JsonAlias({ "id" })
        private Long id;

        public Long getInvoiceId() {
            return invoiceId;
        }

        public void setInvoiceId(Long invoiceId) {
            this.invoiceId = invoiceId;
        }

        public Long getMedicalRecordId() {
            return medicalRecordId;
        }

        public void setMedicalRecordId(Long medicalRecordId) {
            this.medicalRecordId = medicalRecordId;
        }

        public String getExternalTransactionId() {
            return externalTransactionId;
        }

        public void setExternalTransactionId(String externalTransactionId) {
            this.externalTransactionId = externalTransactionId;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public String getPaymentReference() {
            return paymentReference;
        }

        public void setPaymentReference(String paymentReference) {
            this.paymentReference = paymentReference;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public java.math.BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(java.math.BigDecimal amount) {
            this.amount = amount;
        }

        public String getRawPayload() {
            return rawPayload;
        }

        public void setRawPayload(String rawPayload) {
            this.rawPayload = rawPayload;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public java.math.BigDecimal getTransferAmount() {
            return transferAmount;
        }

        public void setTransferAmount(java.math.BigDecimal transferAmount) {
            this.transferAmount = transferAmount;
        }

        public String getTransferType() {
            return transferType;
        }

        public void setTransferType(String transferType) {
            this.transferType = transferType;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getReferenceCode() {
            return referenceCode;
        }

        public void setReferenceCode(String referenceCode) {
            this.referenceCode = referenceCode;
        }

        public String getGateway() {
            return gateway;
        }

        public void setGateway(String gateway) {
            this.gateway = gateway;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    private String resolveTransactionId(WebhookPayload payload) {
        if (payload.getExternalTransactionId() != null && !payload.getExternalTransactionId().isBlank()) {
            return payload.getExternalTransactionId().trim();
        }
        if (payload.getReferenceCode() != null && !payload.getReferenceCode().isBlank()) {
            return payload.getReferenceCode().trim();
        }
        if (payload.getId() != null) {
            return String.valueOf(payload.getId());
        }
        if (payload.getTransactionId() != null && !payload.getTransactionId().isBlank()) {
            return payload.getTransactionId().trim();
        }
        if (payload.getPaymentReference() != null && !payload.getPaymentReference().isBlank()) {
            return payload.getPaymentReference().trim();
        }
        return java.util.UUID.randomUUID().toString();
    }

    private String resolvePaymentReference(WebhookPayload payload) {
        if (payload.getPaymentReference() != null && !payload.getPaymentReference().isBlank()) {
            return normalizePaymentReference(payload.getPaymentReference());
        }
        String fromText = extractPaymentReferenceFromText(payload.getContent());
        if (fromText == null) {
            fromText = extractPaymentReferenceFromText(payload.getDescription());
        }
        if (fromText != null) {
            return normalizePaymentReference(fromText);
        }
        if (payload.getExternalTransactionId() != null && !payload.getExternalTransactionId().isBlank()) {
            return payload.getExternalTransactionId().trim();
        }
        if (payload.getReferenceCode() != null && !payload.getReferenceCode().isBlank()) {
            return payload.getReferenceCode().trim();
        }
        if (payload.getId() != null) {
            return String.valueOf(payload.getId());
        }
        if (payload.getTransactionId() != null && !payload.getTransactionId().isBlank()) {
            return payload.getTransactionId().trim();
        }
        return null;
    }

    private boolean isSuccessStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toUpperCase();
        return "SUCCESS".equals(normalized) || "SUCCESSFUL".equals(normalized) || "PAID".equals(normalized)
                || "COMPLETED".equals(normalized) || "DONE".equals(normalized);
    }

    @SuppressWarnings("null")
    private void notifyCashierRefresh(Long invoiceId) {
        if (invoiceId == null) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/cashier/refresh", String.valueOf(invoiceId));
    }

    private WebhookPayload readWebhookPayload(HttpServletRequest request) {
        try {
            StringBuilder rawBuilder = new StringBuilder();
            try (java.io.BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    rawBuilder.append(line);
                }
            }

            String rawBody = rawBuilder.toString().trim();
            WebhookPayload payload = null;
            if (!rawBody.isBlank()) {
                try {
                    payload = objectMapper.readValue(rawBody, WebhookPayload.class);
                } catch (Exception jsonEx) {
                    payload = new WebhookPayload();
                    payload.setRawPayload(rawBody);
                    applyRequestParameters(request, payload);
                }
            } else {
                payload = new WebhookPayload();
                applyRequestParameters(request, payload);
            }

            if (payload != null && (payload.getRawPayload() == null || payload.getRawPayload().isBlank())) {
                payload.setRawPayload(rawBody);
            }

            if (payload != null) {
                if (payload.getAmount() == null && payload.getTransferAmount() != null) {
                    payload.setAmount(payload.getTransferAmount());
                }
                if ((payload.getProvider() == null || payload.getProvider().isBlank())
                        && payload.getGateway() != null) {
                    payload.setProvider(payload.getGateway());
                }
                if ((payload.getStatus() == null || payload.getStatus().isBlank())
                        && payload.getTransferType() != null) {
                    String normalized = payload.getTransferType().trim().toLowerCase();
                    if ("in".equals(normalized)) {
                        payload.setStatus("SUCCESS");
                    } else if ("out".equals(normalized)) {
                        payload.setStatus("FAILED");
                    }
                }
            }

            return payload;
        } catch (Exception ex) {
            log.error("Failed to read webhook payload", ex);
            return null;
        }
    }

    private void applyRequestParameters(HttpServletRequest request, WebhookPayload payload) {
        java.util.Map<String, String[]> params = request.getParameterMap();
        if (params == null || params.isEmpty()) {
            return;
        }

        payload.setInvoiceId(parseLongParam(params, "invoiceId", payload.getInvoiceId()));
        payload.setMedicalRecordId(parseLongParam(params, "medicalRecordId", payload.getMedicalRecordId()));
        payload.setExternalTransactionId(firstNonBlank(params, payload.getExternalTransactionId(),
                "externalTransactionId", "transactionId", "id"));
        payload.setTransactionId(
                firstNonBlank(params, payload.getTransactionId(), "transactionId", "transaction_id", "id"));
        payload.setPaymentReference(firstNonBlank(params, payload.getPaymentReference(), "paymentReference",
                "payment_reference", "reference", "content", "description", "note", "memo", "remark", "comment"));
        payload.setProvider(firstNonBlank(params, payload.getProvider(), "provider", "bank", "bankName"));
        payload.setStatus(
                firstNonBlank(params, payload.getStatus(), "status", "transactionStatus", "transaction_status"));
        payload.setPaymentMethod(firstNonBlank(params, payload.getPaymentMethod(), "paymentMethod", "payment_method"));
        payload.setReferenceCode(firstNonBlank(params, payload.getReferenceCode(), "referenceCode", "reference_code"));
        payload.setTransferType(firstNonBlank(params, payload.getTransferType(), "transferType", "transfer_type"));
        payload.setGateway(firstNonBlank(params, payload.getGateway(), "gateway"));
        payload.setContent(firstNonBlank(params, payload.getContent(), "content", "note", "memo", "remark", "comment"));
        payload.setDescription(firstNonBlank(params, payload.getDescription(), "description"));
        payload.setRawPayload(payload.getRawPayload() == null ? request.getQueryString() : payload.getRawPayload());
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

    private Long extractInvoiceIdFromReference(String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("BK-INV-(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(reference);
        if (!matcher.find()) {
            matcher = java.util.regex.Pattern
                    .compile("BKINV(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(reference);
            if (!matcher.find()) {
                return null;
            }
        }
        try {
            return Long.valueOf(matcher.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizePaymentReference(String rawReference) {
        if (rawReference == null) {
            return null;
        }
        String trimmed = rawReference.trim();
        if (trimmed.isBlank()) {
            return trimmed;
        }
        String normalized = trimmed.toUpperCase();
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
        return trimmed;
    }

    private Long parseLongParam(java.util.Map<String, String[]> params, String key, Long currentValue) {
        if (currentValue != null) {
            return currentValue;
        }
        String value = firstValue(params, key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstNonBlank(java.util.Map<String, String[]> params, String currentValue, String... keys) {
        if (currentValue != null && !currentValue.isBlank()) {
            return currentValue;
        }
        for (String key : keys) {
            String value = firstValue(params, key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return currentValue;
    }

    private String firstValue(java.util.Map<String, String[]> params, String key) {
        String[] values = params.get(key);
        if (values == null || values.length == 0) {
            return null;
        }
        return values[0];
    }
}
