package com.example.demo.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.example.demo.exception.AppException;

import com.example.demo.constants.AppointmentStatus;
import com.example.demo.constants.InvoiceStatus;
import com.example.demo.constants.PaymentStatus;
import com.example.demo.dto.CashierMedicineLineItemResponse;
import com.example.demo.dto.CashierPaymentRecordDetailResponse;
import com.example.demo.dto.CashierPrintReceiptResponse;
import com.example.demo.dto.CashierProcessPaymentRequest;
import com.example.demo.dto.CashierProcessPaymentResponse;
import com.example.demo.dto.CashierReceiptResponse;
import com.example.demo.dto.CashierServiceLineItemResponse;
import com.example.demo.dto.CashierServicePaymentBreakdownLineResponse;
import com.example.demo.dto.CashierTransactionHistoryItemResponse;
import com.example.demo.dto.CashierTransactionHistoryResponse;
import com.example.demo.dto.CashierWaitingPaymentItemResponse;
import com.example.demo.entity.Appointment;
import com.example.demo.entity.Invoice;
import com.example.demo.entity.MedicalRecord;
import com.example.demo.entity.MedicalRecordServiceDetail;
import com.example.demo.entity.MedicalRecordServiceId;
import com.example.demo.entity.MedicalService;
import com.example.demo.entity.Medicine;
import com.example.demo.entity.Patient;
import com.example.demo.entity.PrescriptionDetail;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.InvoiceRepository;
import com.example.demo.repository.MedicalRecordRepository;
import com.example.demo.repository.MedicalRecordServiceDetailRepository;
import com.example.demo.repository.MedicalServiceRepository;
import com.example.demo.repository.MedicineRepository;
import com.example.demo.repository.PrescriptionDetailRepository;
import com.example.demo.repository.RoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
@Slf4j
public class InvoiceService {

    private static final String STATUS_WAITING_PAYMENT = InvoiceStatus.UNPAID;
    private static final String STATUS_PAID = InvoiceStatus.PAID;
    private static final String TRANSACTION_SUCCESS = "THANH_TOAN_THANH_CONG";
    private static final String CLINIC_NAME = "Phong Kham Tong Hop";
    private static final String CLINIC_LOGO_TEXT = "[LOGO PHONG KHAM]";
    private static final DateTimeFormatter RECEIPT_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Set<String> SUPPORTED_PAYMENT_METHODS = Set.of("TIEN_MAT", "CHUYEN_KHOAN", "POS");
    private static final BigDecimal HEALTH_INSURANCE_DISCOUNT_RATE = new BigDecimal("0.70");
    private static final List<String> CONSULTATION_KEYWORDS = List.of("kham", "consultation", "examination", "tu van");

    private final InvoiceRepository invoiceRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordServiceDetailRepository medicalRecordServiceDetailRepository;
    private final PrescriptionDetailRepository prescriptionDetailRepository;
    private final MedicineRepository medicineRepository;
    private final MedicalServiceRepository medicalServiceRepository;
    private final RoomRepository roomRepository;
    private final com.example.demo.repository.PaymentTransactionRepository paymentTransactionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Chức năng: xử lý lấy thông tin theo mã số hồ sơ y tế.
    public Invoice getByMedicalRecordId(Long medicalRecordId) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy bệnh án"));
        return invoiceRepository.findByMedicalRecord_Id(medicalRecord.getId())
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND,
                        "Không tìm thấy hóa đơn cho bệnh án này"));
    }

    // Chức năng: xử lý lấy hàng đợi chờ thanh toán.
    public List<CashierWaitingPaymentItemResponse> getWaitingPaymentQueue(String keyword) {
        // Một số cuộc hẹn có thể sử dụng giá trị trạng thái được bản địa hóa
        // (CHO_THU_NGAN)
        // tùy thuộc vào cách chúng được tạo hoặc chuyển đổi. Truy vấn cả hai biến thể
        // để nhân viên thu ngân thấy tất cả các bản ghi đang chờ thanh toán.
        // Truy xuất các cuộc hẹn cho cả giá trị trạng thái chuẩn và được bản địa hóa và
        // hợp nhất để đảm bảo không bỏ sót bản ghi nào đang chờ do các giá trị biến
        // thể.
        List<com.example.demo.entity.Appointment> apptsCanonical = appointmentRepository
                .findByStatusOrderByAppointmentTimeAsc(AppointmentStatus.WAITING_CASHIER);
        List<com.example.demo.entity.Appointment> apptsLocalized = appointmentRepository
                .findByStatusOrderByAppointmentTimeAsc("CHO_THU_NGAN");

        List<com.example.demo.entity.Appointment> combined = new ArrayList<>();
        combined.addAll(apptsCanonical);
        for (com.example.demo.entity.Appointment a : apptsLocalized) {
            if (a != null && a.getId() != null && combined.stream().noneMatch(x -> x.getId().equals(a.getId()))) {
                combined.add(a);
            }
        }

        List<Invoice> unpaidInvoices = combined.stream()
                .map(this::resolveUnpaidInvoiceForAppointment)
                .filter(Objects::nonNull)
                .toList();

        if (keyword == null || keyword.isBlank()) {
            return unpaidInvoices.stream()
                    .map(this::toWaitingPaymentItemResponse)
                    .toList();
        }

        String normalizedKeyword = normalizeKeyword(keyword);
        List<CashierWaitingPaymentItemResponse> matched = unpaidInvoices.stream()
                .filter(invoice -> matchesKeyword(invoice, normalizedKeyword))
                .map(this::toWaitingPaymentItemResponse)
                .toList();

        if (matched.isEmpty()) {
            throw AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ");
        }

        return matched;
    }

    private Invoice resolveUnpaidInvoiceForAppointment(Appointment appointment) {
        if (appointment == null || appointment.getId() == null) {
            return null;
        }

        List<MedicalRecord> records = medicalRecordRepository
                .findAllByAppointment_IdOrderByCreatedAtDescIdDesc(appointment.getId());
        MedicalRecord medicalRecord = records.isEmpty() ? null : records.get(0);

        Invoice invoice = medicalRecord == null ? null
                : invoiceRepository.findByMedicalRecord_Id(medicalRecord.getId()).orElse(null);
        if (invoice == null) {
            invoice = createInvoiceForAppointment(appointment);
        }

        if (invoice == null || Boolean.TRUE.equals(invoice.getIsPaid())) {
            return null;
        }

        return invoice;
    }

    // Chức năng: xử lý tìm kiếm hồ sơ thanh toán.
    public CashierPaymentRecordDetailResponse searchPaymentRecord(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Từ khóa là bắt buộc");
        }

        String normalizedKeyword = normalizeKeyword(keyword);
        Optional<Invoice> matchedInvoice = invoiceRepository.findAllByOrderByIdDesc().stream()
                .filter(invoice -> matchesKeyword(invoice, normalizedKeyword))
                .findFirst();

        Invoice invoice = matchedInvoice
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ"));

        if (Boolean.TRUE.equals(invoice.getIsPaid())) {
            throw AppException.of(HttpStatus.CONFLICT, "Hồ sơ đã thanh toán");
        }

        return toPaymentRecordDetailResponse(invoice);
    }

    // Chức năng: xử lý lấy chi tiết hóa đơn đã thanh toán cho lịch sử của lễ tân.
    public CashierPaymentRecordDetailResponse getPaidInvoiceDetail(Long invoiceId) {
        Invoice invoice = getPaidInvoiceOrThrow(invoiceId);
        return toPaymentRecordDetailResponse(invoice);
    }

    // Chức năng: xử lý lấy lịch sử giao dịch đã thanh toán cho ngày hiện tại hoặc
    // khoảng ca làm việc.
    public CashierTransactionHistoryResponse getTransactionHistory(
            LocalDateTime startTime,
            LocalDateTime endTime,
            String paymentMethod) {
        LocalDateTime resolvedStartTime = startTime;
        LocalDateTime resolvedEndTime = endTime;

        if (resolvedStartTime == null || resolvedEndTime == null) {
            LocalDate today = LocalDate.now();
            if (resolvedStartTime == null) {
                resolvedStartTime = today.atStartOfDay();
            }
            if (resolvedEndTime == null) {
                resolvedEndTime = LocalDateTime.now();
            }
        }

        if (resolvedStartTime.isAfter(resolvedEndTime)) {
            throw AppException.of(HttpStatus.BAD_REQUEST,
                    "Thời gian bắt đầu phải trước thời gian kết thúc");
        }

        String normalizedFilter = normalizePaymentMethodFilter(paymentMethod);
        List<Invoice> paidInvoices;
        if (normalizedFilter == null) {
            paidInvoices = invoiceRepository.findByIsPaidAndPaidAtBetweenOrderByPaidAtDesc(
                    Boolean.TRUE,
                    resolvedStartTime,
                    resolvedEndTime);
        } else {
            paidInvoices = invoiceRepository.findByIsPaidAndPaymentMethodAndPaidAtBetweenOrderByPaidAtDesc(
                    Boolean.TRUE,
                    normalizedFilter,
                    resolvedStartTime,
                    resolvedEndTime);
        }

        List<CashierTransactionHistoryItemResponse> items = paidInvoices.stream()
                .map(this::toTransactionHistoryItem)
                .toList();

        BigDecimal totalAmount = paidInvoices.stream()
                .map(Invoice::getGrandTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCash = sumAmountByPaymentMethod(paidInvoices, "TIEN_MAT");
        BigDecimal totalBankTransfer = sumAmountByPaymentMethod(paidInvoices, "CHUYEN_KHOAN");
        BigDecimal totalPos = sumAmountByPaymentMethod(paidInvoices, "POS");

        return new CashierTransactionHistoryResponse(
                resolvedStartTime,
                resolvedEndTime,
                normalizedFilter == null ? "ALL" : normalizedFilter,
                items.size(),
                totalAmount,
                totalCash,
                totalBankTransfer,
                totalPos,
                items);
    }

    // Chức năng: xử lý tổng hợp số tiền hóa đơn.
    public Invoice aggregateInvoiceAmount(Long medicalRecordId) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy bệnh án"));

        Appointment appointment = medicalRecord.getAppointment();
        if (appointment == null) {
            throw AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn");
        }

        Invoice invoice = invoiceRepository.findByMedicalRecord_Id(medicalRecordId)
                .orElseGet(Invoice::new);

        // Recompute totals even if invoice was previously marked paid. If new
        // services have been added after a prior payment, we must update the
        // remaining amount and unmark the invoice as paid when appropriate so
        // reception/cashier sees the outstanding balance.

        List<MedicalRecordServiceDetail> serviceDetails = medicalRecordServiceDetailRepository
                .findByMedicalRecord_Id(medicalRecordId);

        serviceDetails = ensureConsultationService(
                medicalRecordId,
                medicalRecord,
                serviceDetails);

        BigDecimal totalServiceFee = serviceDetails.stream()
                .map(this::serviceLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMedicineFee = BigDecimal.ZERO;

        BigDecimal totalAmount = totalServiceFee;

        invoice.setMedicalRecord(medicalRecord);
        invoice.setTotalServiceFee(totalServiceFee);
        invoice.setTotalMedicineFee(totalMedicineFee);
        invoice.setGrandTotal(totalAmount);

        BigDecimal advanceAmount = defaultAmount(appointment.getAdvancePayment());

        invoice.setAdvanceAmount(advanceAmount);
        invoice.setRemainingAmount(
                defaultAmount(invoice.getGrandTotal()).subtract(advanceAmount));

        invoice.setIsPaid(Boolean.FALSE);
        invoice.setPaidAt(null);

        return saveInvoiceWithPaymentReference(invoice);
    }

    // Chức năng: tạo hóa đơn cho lịch hẹn khi chuyển qua thu ngân.
    public Invoice createInvoiceForAppointment(Appointment appointment) {
        if (appointment == null || appointment.getId() == null) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Lịch hẹn là bắt buộc");
        }

        List<MedicalRecord> records = medicalRecordRepository
                .findAllByAppointment_IdOrderByCreatedAtDescIdDesc(appointment.getId());
        MedicalRecord medicalRecord;
        if (records.isEmpty()) {
            // Create a placeholder medical record so invoicing can proceed before the
            // doctor creates one.
            medicalRecord = new MedicalRecord();
            medicalRecord.setAppointment(appointment);
            medicalRecord.setCreatedAt(LocalDateTime.now());
            medicalRecord = medicalRecordRepository.save(medicalRecord);
        } else {
            medicalRecord = records.get(0);
        }

        Invoice invoice = invoiceRepository.findByMedicalRecord_Id(medicalRecord.getId()).orElse(new Invoice());
        if (Boolean.TRUE.equals(invoice.getIsPaid())) {
            return invoice;
        }

        // Calculate totals from persisted medical record service details to ensure
        // consistency
        List<MedicalRecordServiceDetail> serviceDetails = medicalRecordServiceDetailRepository
                .findByMedicalRecord_Id(medicalRecord.getId());
        serviceDetails = ensureConsultationService(medicalRecord.getId(), medicalRecord, serviceDetails);
        BigDecimal totalServiceFee = serviceDetails.stream()
                .map(this::serviceLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMedicineFee = BigDecimal.ZERO;

        BigDecimal grandTotal = totalServiceFee.add(totalMedicineFee);
        BigDecimal advanceAmount = defaultAmount(appointment.getAdvancePayment());
        BigDecimal remainingAmount = grandTotal.subtract(advanceAmount);
        if (remainingAmount.signum() < 0) {
            remainingAmount = BigDecimal.ZERO;
        }

        invoice.setMedicalRecord(medicalRecord);
        invoice.setIsPaid(Boolean.FALSE);
        invoice.setPaymentMethod(appointment.getPaymentMethod());
        invoice.setTotalServiceFee(totalServiceFee);
        invoice.setTotalMedicineFee(totalMedicineFee);
        invoice.setGrandTotal(grandTotal);
        invoice.setAdvanceAmount(advanceAmount);
        invoice.setRemainingAmount(remainingAmount);
        invoice.setPaidAt(null);

        return saveInvoiceWithPaymentReference(invoice);
    }

    private Invoice saveInvoiceWithPaymentReference(Invoice invoice) {
        Invoice saved = invoiceRepository.save(invoice);
        if (saved.getPaymentReference() == null || saved.getPaymentReference().isBlank()) {
            saved.setPaymentReference(buildPaymentReference(saved));
            saved = invoiceRepository.save(saved);
        }
        return saved;
    }

    private String buildPaymentReference(Invoice invoice) {
        return "INV-" + invoice.getId();
    }

    // Chức năng: lưu hóa đơn sau khi gán thêm thông tin như payment reference.
    public Invoice saveInvoice(Invoice invoice) {
        if (invoice == null) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Hóa đơn là bắt buộc");
        }
        return invoiceRepository.save(invoice);
    }

    private List<MedicalRecordServiceDetail> ensureConsultationService(
            Long medicalRecordId,
            MedicalRecord medicalRecord,
            List<MedicalRecordServiceDetail> serviceDetails) {
        MedicalService consultationService = resolveConsultationService(medicalRecord);
        if (consultationService == null) {
            return serviceDetails;
        }

        // Nếu đã có các dòng dịch vụ được lưu trữ cho hồ sơ y tế này,
        // thì không cần thêm bất kỳ khoản phí tư vấn nào nữa — nhân viên thu ngân sẽ
        // thấy
        // chính xác những gì đã được đề xuất khi đặt lịch hẹn.
        if (serviceDetails != null && !serviceDetails.isEmpty()) {
            return serviceDetails;
        }

        MedicalRecordServiceId id = new MedicalRecordServiceId();
        id.setMedicalRecordId(medicalRecordId);
        id.setServiceId(consultationService.getId());

        MedicalRecordServiceDetail detail = new MedicalRecordServiceDetail();
        detail.setId(id);
        detail.setMedicalRecord(medicalRecord);
        detail.setService(consultationService);
        detail.setQuantity(1);
        detail.setActualPrice(defaultAmount(consultationService.getCurrentPrice()));
        detail.setResultNote("Phí khám ban đầu");

        MedicalRecordServiceDetail saved = medicalRecordServiceDetailRepository.save(detail);
        List<MedicalRecordServiceDetail> updated = new ArrayList<>(serviceDetails);
        updated.add(saved);
        return updated;
    }

    private MedicalService resolveConsultationService(MedicalRecord medicalRecord) {
        MedicalService byRoom = resolveConsultationServiceFromRoom(medicalRecord);
        if (byRoom != null) {
            return byRoom;
        }

        List<MedicalService> activeServices = medicalServiceRepository.findByIsActiveTrueOrderByServiceNameAsc();
        List<MedicalService> candidates = activeServices.stream()
                .filter(this::isConsultationService)
                .toList();
        if (candidates.isEmpty()) {
            return null;
        }

        // Prefer a short/generic consultation service name (e.g. "Khám") over
        // specialty-specific services (e.g. "Khám da liễu"). Choose the candidate
        // with the shortest normalized name as a heuristic for the generic service.
        return candidates.stream()
                .min(Comparator.comparingInt(s -> normalizeText(s.getServiceName()).length()))
                .orElse(null);
    }

    private MedicalService resolveConsultationServiceFromRoom(MedicalRecord medicalRecord) {
        if (medicalRecord == null || medicalRecord.getAppointment() == null) {
            return null;
        }

        Appointment appointment = medicalRecord.getAppointment();
        if (appointment.getDoctor() == null || appointment.getDoctor().getId() == null) {
            return null;
        }

        List<com.example.demo.entity.Room> rooms = roomRepository
                .findByCurrentDoctor_Id(appointment.getDoctor().getId());
        if (rooms.isEmpty()) {
            return null;
        }

        String roomName = rooms.get(0).getRoomName();
        if (roomName == null || roomName.isBlank()) {
            return null;
        }

        String normalizedRoom = normalizeText(roomName);
        if (normalizedRoom.isEmpty()) {
            return null;
        }

        List<MedicalService> activeServices = medicalServiceRepository.findByIsActiveTrueOrderByServiceNameAsc();
        for (MedicalService service : activeServices) {
            if (service == null || service.getServiceName() == null) {
                continue;
            }
            String normalizedService = normalizeText(service.getServiceName());
            if (normalizedRoom.equals(normalizedService)) {
                return service;
            }
        }
        return null;
    }

    private boolean isConsultationService(MedicalService service) {
        if (service == null || service.getServiceName() == null) {
            return false;
        }

        String normalized = normalizeText(service.getServiceName());
        for (String keyword : CONSULTATION_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        return normalized;
    }

    @Transactional
    // Chức năng: xử lý xác nhận thanh toán.
    public Invoice confirmPayment(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn"));

        if (Boolean.TRUE.equals(invoice.getIsPaid())) {
            return invoice;
        }

        MedicalRecord medicalRecord = resolveMedicalRecord(invoice);
        if (medicalRecord == null || medicalRecord.getId() == null) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Bệnh án là bắt buộc để thanh toán");
        }

        Invoice recalculatedInvoice = aggregateInvoiceAmount(medicalRecord.getId());
        deductMedicineStockOnPayment(medicalRecord.getId());
        return markInvoiceAsPaidAndCloseSession(recalculatedInvoice, "TIEN_MAT");
    }

    @Transactional
    // Chức năng: Xử lý thanh toán với phương thức thanh toán và tùy chọn xuất hóa
    // đơn.
    public CashierProcessPaymentResponse processPayment(Long invoiceId, CashierProcessPaymentRequest request) {
        if (request == null) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Dữ liệu yêu cầu là bắt buộc");
        }

        String paymentMethod = normalizePaymentMethod(request.getPaymentMethod());
        if (!SUPPORTED_PAYMENT_METHODS.contains(paymentMethod)) {
            throw AppException.of(
                    HttpStatus.BAD_REQUEST,
                    "Phương thức thanh toán không hợp lệ. Hỗ trợ: TIEN_MAT, CHUYEN_KHOAN, POS");
        }
        ensureElectronicPaymentSucceeded(paymentMethod, request.getPaymentSuccessful());

        Invoice existingInvoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn"));

        if (Boolean.TRUE.equals(existingInvoice.getIsPaid())) {
            throw AppException.of(HttpStatus.CONFLICT, "Hồ sơ đã thanh toán");
        }

        MedicalRecord medicalRecord = resolveMedicalRecord(existingInvoice);

        Invoice invoice = existingInvoice;
        if (medicalRecord != null && medicalRecord.getId() != null) {
            invoice = aggregateInvoiceAmount(medicalRecord.getId());
        }

        BigDecimal grossTotalAmount = defaultAmount(invoice.getGrandTotal());

        Patient patient = invoice.getPatient();
        boolean applyHealthInsurance = Boolean.TRUE.equals(request.getApplyHealthInsurance());
        boolean eligibleHealthInsurance = hasHealthInsurance(patient);
        if (applyHealthInsurance && !eligibleHealthInsurance) {
            throw AppException.of(
                    HttpStatus.BAD_REQUEST,
                    "Bệnh nhân không có thông tin bảo hiểm y tế");
        }

        BigDecimal insuranceDiscountAmount = applyHealthInsurance
                ? calculateHealthInsuranceDiscount(grossTotalAmount)
                : BigDecimal.ZERO;
        BigDecimal payableTotalAmount = grossTotalAmount.subtract(insuranceDiscountAmount);

        // Khoản tạm ứng hiện có (ví dụ: chuyển khoản ngân hàng đã được ghi nhận trước
        // đó)
        BigDecimal existingAdvance = defaultAmount(invoice.getAdvanceAmount());

        // Số tiền thu ngân cần nhận ngay
        BigDecimal amountToCollect = payableTotalAmount.subtract(existingAdvance);
        if (amountToCollect.signum() < 0) {
            amountToCollect = BigDecimal.ZERO;
        }

        // Nếu thanh toán thành công (tiền mặt/máy POS hoặc chuyển khoản đã xác nhận),
        // hãy ghi lại khoản thanh toán
        // giao dịch
        if (Boolean.TRUE.equals(request.getPaymentSuccessful()) && amountToCollect.signum() > 0) {
            com.example.demo.entity.PaymentTransaction tx = new com.example.demo.entity.PaymentTransaction();
            tx.setExternalTransactionId(
                    (paymentMethod == null ? "CASH" : paymentMethod) + "-" + System.currentTimeMillis());
            tx.setPaymentReference(invoice.getPaymentReference());
            tx.setProvider(paymentMethod == null ? "CASH" : paymentMethod);
            tx.setPaymentMethod(paymentMethod);
            tx.setAmount(amountToCollect);
            tx.setReceivedAt(java.time.LocalDateTime.now());
            tx.setStatus("SUCCESS");
            tx.setRawPayload("{\"source\":\"cashier\"}");
            tx.setInvoice(invoice);
            if (invoice.getAppointment() != null) {
                tx.setAppointment(invoice.getAppointment());
            }
            paymentTransactionRepository.save(tx);

            // Lưu ngay phương thức thanh toán hỗn hợp để dòng hóa đơn tự phản ánh cả giao
            // dịch chuyển khoản ban đầu và khoản thu của nhân viên thu ngân.
            invoice.setPaymentMethod(mergePaymentMethod(invoice.getPaymentMethod(), paymentMethod));
            invoiceRepository.save(invoice);

            // Cập nhật hóa đơn tạm ứng và số dư còn lại
            invoice.setAdvanceAmount(existingAdvance.add(amountToCollect));
            invoice.setGrandTotal(payableTotalAmount);
            BigDecimal remaining = payableTotalAmount.subtract(defaultAmount(invoice.getAdvanceAmount()));
            invoice.setRemainingAmount(remaining.signum() < 0 ? BigDecimal.ZERO : remaining);
        } else {
            // Không có khoản thanh toán mới nào được thu ngân ghi nhận; cập nhật tổng số
            // tiền/giảm giá nhưng giữ nguyên
            // ứng trước
            invoice.setGrandTotal(payableTotalAmount);
            invoice.setAdvanceAmount(existingAdvance);
            BigDecimal remaining = payableTotalAmount.subtract(existingAdvance);
            invoice.setRemainingAmount(remaining.signum() < 0 ? BigDecimal.ZERO : remaining);
        }

        if (medicalRecord != null && medicalRecord.getId() != null) {
            deductMedicineStockOnPayment(medicalRecord.getId());
        }

        Invoice savedInvoice;
        // Nếu đã thanh toán đầy đủ sau khi ghi hình, hãy đánh dấu đã thanh toán và đóng
        // phiên.
        if (defaultAmount(invoice.getRemainingAmount()).compareTo(BigDecimal.ZERO) <= 0) {
            savedInvoice = markInvoiceAsPaidAndCloseSession(invoice, paymentMethod);
        } else {
            savedInvoice = invoiceRepository.save(invoice);
        }
        notifyCashierDashboardRefresh(savedInvoice);
        LocalDateTime paidAt = savedInvoice.getPaidAt();
        boolean exportInvoice = Boolean.TRUE.equals(request.getExportInvoice());

        return new CashierProcessPaymentResponse(
                savedInvoice.getId(),
                savedInvoice.getAppointment() == null ? null : savedInvoice.getAppointment().getId(),
                paymentMethod,
                savedInvoice.getTotalServiceFee(),
                grossTotalAmount,
                insuranceDiscountAmount,
                savedInvoice.getRemainingAmount(),
                applyHealthInsurance,
                paidAt,
                TRANSACTION_SUCCESS,
                exportInvoice,
                buildInvoiceCode(savedInvoice.getId(), paidAt),
                "Thanh toan thanh cong");
    }

    // Chức năng: xử lý Xem trước nội dung biên lai sau khi thanh toán thành công.
    public CashierReceiptResponse previewReceipt(Long invoiceId) {
        Invoice invoice = getPaidInvoiceOrThrow(invoiceId);
        CashierPaymentRecordDetailResponse detail = toPaymentRecordDetailResponse(invoice);
        String formattedText = buildReceiptText(invoice, detail);

        return new CashierReceiptResponse(
                CLINIC_NAME,
                CLINIC_LOGO_TEXT,
                invoice.getId(),
                detail.getAppointmentId(),
                detail.getPatientName(),
                detail.getPhoneNumber(),
                invoice.getPaidAt(),
                detail.getServices(),
                detail.getMedicines(),
                detail.getTotalServiceFee(),
                detail.getGrandTotal(),
                formattedText);
    }

    // Chức năng: xử lý In hóa đơn ra máy in nhiệt/laser được kết nối.
    public CashierPrintReceiptResponse printReceipt(Long invoiceId, String printerName) {
        Invoice invoice = getPaidInvoiceOrThrow(invoiceId);
        CashierPaymentRecordDetailResponse detail = toPaymentRecordDetailResponse(invoice);
        String formattedText = buildReceiptText(invoice, detail);

        PrintService targetPrinter = resolvePrinter(printerName);
        try {
            DocPrintJob printJob = targetPrinter.createPrintJob();
            Doc doc = new SimpleDoc(formattedText, DocFlavor.STRING.TEXT_PLAIN, null);
            printJob.print(doc, null);
        } catch (PrintException ex) {
            throw AppException.of(HttpStatus.SERVICE_UNAVAILABLE, "Không thể gửi lệnh in", ex);
        }

        return new CashierPrintReceiptResponse(
                invoiceId,
                targetPrinter.getName(),
                "PRINT_JOB_SENT",
                LocalDateTime.now(),
                "Da gui lenh in bien lai");
    }

    // Chức năng: xử lý xuất hóa đơn điện tử sang tệp PDF (bytes).
    public byte[] exportInvoicePdf(Long invoiceId) {
        Invoice invoice = getPaidInvoiceOrThrow(invoiceId);
        CashierPaymentRecordDetailResponse detail = toPaymentRecordDetailResponse(invoice);
        List<String> lines = buildReceiptLines(invoice, detail);

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float marginLeft = 48f;
                float y = page.getMediaBox().getHeight() - 52f;

                content.setLeading(16f);
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 14);
                content.newLineAtOffset(marginLeft, y);
                content.showText(CLINIC_LOGO_TEXT + " " + CLINIC_NAME);
                content.newLine();
                content.setFont(PDType1Font.HELVETICA, 11);
                for (String line : lines) {
                    content.showText(line);
                    content.newLine();
                }
                content.endText();
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw AppException.of(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo file PDF", ex);
        }
    }

    // Chức năng: xử lý tổng số dịch vụ.
    private BigDecimal serviceLineTotal(MedicalRecordServiceDetail detail) {
        BigDecimal price = detail.getActualPrice() == null ? BigDecimal.ZERO : detail.getActualPrice();
        BigDecimal quantity = BigDecimal.ZERO;
        if (detail.getQuantity() != null) {
            quantity = BigDecimal.valueOf(detail.getQuantity().longValue());
        }
        return price.multiply(quantity);
    }

    private boolean isConsultationServiceName(String serviceName) {
        if (serviceName == null) {
            return false;
        }

        String normalized = normalizeServiceName(serviceName);
        for (String keyword : CONSULTATION_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeServiceName(String value) {
        return value == null ? ""
                : Normalizer.normalize(value, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}+", "")
                        .toLowerCase(Locale.ROOT)
                        .trim();
    }

    private ServicePaymentBreakdown calculateServicePaymentBreakdown(
            List<CashierServiceLineItemResponse> services,
            BigDecimal advanceAmount) {
        List<CashierServiceLineItemResponse> consultationServices = services.stream()
                .filter(service -> isConsultationServiceName(service.getServiceName()))
                .toList();
        List<CashierServiceLineItemResponse> additionalServices = services.stream()
                .filter(service -> !isConsultationServiceName(service.getServiceName()))
                .toList();

        BigDecimal consultationFee = sumServiceLines(consultationServices);
        BigDecimal additionalServiceFee = sumServiceLines(additionalServices);

        BigDecimal remainingAdvance = defaultAmount(advanceAmount);
        BigDecimal consultationCoveredAmount = minAmount(consultationFee, remainingAdvance);
        remainingAdvance = remainingAdvance.subtract(consultationCoveredAmount);
        if (remainingAdvance.signum() < 0) {
            remainingAdvance = BigDecimal.ZERO;
        }

        BigDecimal additionalCoveredAmount = minAmount(additionalServiceFee, remainingAdvance);
        BigDecimal consultationOutstandingAmount = consultationFee.subtract(consultationCoveredAmount);
        BigDecimal additionalOutstandingAmount = additionalServiceFee.subtract(additionalCoveredAmount);

        if (consultationOutstandingAmount.signum() < 0) {
            consultationOutstandingAmount = BigDecimal.ZERO;
        }
        if (additionalOutstandingAmount.signum() < 0) {
            additionalOutstandingAmount = BigDecimal.ZERO;
        }

        List<CashierServicePaymentBreakdownLineResponse> breakdownLines = new ArrayList<>();
        BigDecimal remainingConsultationCoverage = consultationCoveredAmount;
        for (CashierServiceLineItemResponse service : consultationServices) {
            BigDecimal lineTotal = defaultAmount(service.getLineTotal());
            BigDecimal coveredAmount = minAmount(lineTotal, remainingConsultationCoverage);
            remainingConsultationCoverage = remainingConsultationCoverage.subtract(coveredAmount);
            if (remainingConsultationCoverage.signum() < 0) {
                remainingConsultationCoverage = BigDecimal.ZERO;
            }
            breakdownLines.add(new CashierServicePaymentBreakdownLineResponse(
                    service.getServiceId(),
                    service.getServiceName(),
                    service.getQuantity(),
                    service.getUnitPrice(),
                    lineTotal,
                    coveredAmount,
                    lineTotal.subtract(coveredAmount)));
        }

        BigDecimal remainingAdditionalCoverage = additionalCoveredAmount;
        for (CashierServiceLineItemResponse service : additionalServices) {
            BigDecimal lineTotal = defaultAmount(service.getLineTotal());
            BigDecimal coveredAmount = minAmount(lineTotal, remainingAdditionalCoverage);
            remainingAdditionalCoverage = remainingAdditionalCoverage.subtract(coveredAmount);
            if (remainingAdditionalCoverage.signum() < 0) {
                remainingAdditionalCoverage = BigDecimal.ZERO;
            }
            breakdownLines.add(new CashierServicePaymentBreakdownLineResponse(
                    service.getServiceId(),
                    service.getServiceName(),
                    service.getQuantity(),
                    service.getUnitPrice(),
                    lineTotal,
                    coveredAmount,
                    lineTotal.subtract(coveredAmount)));
        }

        return new ServicePaymentBreakdown(
                consultationFee,
                consultationCoveredAmount,
                consultationOutstandingAmount,
                additionalServiceFee,
                additionalCoveredAmount,
                additionalOutstandingAmount,
                breakdownLines);
    }

    private BigDecimal sumServiceLines(List<CashierServiceLineItemResponse> services) {
        return services.stream()
                .map(service -> defaultAmount(service.getLineTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal minAmount(BigDecimal left, BigDecimal right) {
        return defaultAmount(left).min(defaultAmount(right));
    }

    private record ServicePaymentBreakdown(
            BigDecimal consultationFee,
            BigDecimal consultationCoveredAmount,
            BigDecimal consultationOutstandingAmount,
            BigDecimal additionalServiceFee,
            BigDecimal additionalCoveredAmount,
            BigDecimal additionalOutstandingAmount,
            List<CashierServicePaymentBreakdownLineResponse> lines) {
    }

    // Chức năng: xử lý tổng số thuốc.
    @SuppressWarnings("unused")
    private BigDecimal medicineLineTotal(PrescriptionDetail detail) {
        BigDecimal price = BigDecimal.ZERO;
        if (detail.getMedicine() != null && detail.getMedicine().getSellingPrice() != null) {
            price = detail.getMedicine().getSellingPrice();
        }
        BigDecimal quantity = BigDecimal.ZERO;
        if (detail.getQuantity() != null) {
            quantity = BigDecimal.valueOf(detail.getQuantity().longValue());
        }
        return price.multiply(quantity);
    }

    // Chức năng: xử lý ánh xạ tới phản hồi mục thanh toán đang chờ xử lý.
    private CashierWaitingPaymentItemResponse toWaitingPaymentItemResponse(Invoice invoice) {
        Appointment appointment = invoice.getAppointment();
        Patient patient = appointment == null ? null : appointment.getPatient();
        String invoiceStatus = Boolean.TRUE.equals(invoice.getIsPaid()) ? STATUS_PAID : STATUS_WAITING_PAYMENT;
        BigDecimal advanceAmount = appointment == null
                ? BigDecimal.ZERO
                : defaultAmount(appointment.getAdvancePayment());
        BigDecimal remainingAmount = defaultAmount(invoice.getGrandTotal()).subtract(advanceAmount);
        if (remainingAmount.signum() < 0) {
            remainingAmount = BigDecimal.ZERO;
        }

        return new CashierWaitingPaymentItemResponse(
                invoice.getId(),
                appointment == null ? null : appointment.getId(),
                patient == null ? null : patient.getId(),
                patient == null ? null : patient.getFullName(),
                patient == null ? null : patient.getPhoneNumber(),
                appointment == null ? null : appointment.getAppointmentTime(),
                invoice.getGrandTotal(),
                advanceAmount,
                remainingAmount,
                invoiceStatus);
    }

    // Chức năng: xử lý ánh xạ tới phản hồi chi tiết hồ sơ thanh toán.
    private CashierPaymentRecordDetailResponse toPaymentRecordDetailResponse(Invoice invoice) {
        Appointment appointment = invoice.getAppointment();
        Patient patient = appointment == null ? null : appointment.getPatient();
        MedicalRecord medicalRecord = resolveMedicalRecord(invoice);
        Long medicalRecordId = medicalRecord == null ? null : medicalRecord.getId();
        String invoiceStatus = Boolean.TRUE.equals(invoice.getIsPaid()) ? STATUS_PAID : STATUS_WAITING_PAYMENT;
        BigDecimal advanceAmount = appointment == null
                ? BigDecimal.ZERO
                : defaultAmount(appointment.getAdvancePayment());
        BigDecimal remainingAmount = defaultAmount(invoice.getGrandTotal()).subtract(advanceAmount);
        if (remainingAmount.signum() < 0) {
            remainingAmount = BigDecimal.ZERO;
        }

        List<CashierServiceLineItemResponse> services = medicalRecordId == null
                ? List.of()
                : medicalRecordServiceDetailRepository.findByMedicalRecord_Id(medicalRecordId).stream()
                        .map(detail -> {
                            BigDecimal unitPrice = detail.getActualPrice() == null ? BigDecimal.ZERO
                                    : detail.getActualPrice();
                            BigDecimal quantity = BigDecimal.valueOf(detail.getQuantity());
                            return new CashierServiceLineItemResponse(
                                    detail.getService() == null ? null : detail.getService().getId(),
                                    detail.getService() == null ? null : detail.getService().getServiceName(),
                                    detail.getQuantity(),
                                    unitPrice,
                                    unitPrice.multiply(quantity));
                        })
                        .toList();

        List<CashierMedicineLineItemResponse> medicines = medicalRecordId == null
                ? List.of()
                : prescriptionDetailRepository.findByMedicalRecord_Id(medicalRecordId).stream()
                        .map(detail -> new CashierMedicineLineItemResponse(
                                detail.getMedicine() == null ? null : detail.getMedicine().getId(),
                                detail.getMedicine() == null ? null : detail.getMedicine().getMedicineName(),
                                detail.getQuantity(),
                                detail.getUsageInstructions(),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO))
                        .toList();

        ServicePaymentBreakdown breakdown = calculateServicePaymentBreakdown(services, advanceAmount);

        return new CashierPaymentRecordDetailResponse(
                invoice.getId(),
                appointment == null ? null : appointment.getId(),
                patient == null ? null : patient.getId(),
                patient == null ? null : patient.getFullName(),
                patient == null ? null : patient.getPhoneNumber(),
                appointment == null ? null : appointment.getAppointmentTime(),
                invoiceStatus,
                invoice.getPaymentMethod(),
                invoice.getPaidAt(),
                invoice.getTotalServiceFee(),
                invoice.getGrandTotal(),
                advanceAmount,
                remainingAmount,
                breakdown.consultationFee(),
                breakdown.consultationCoveredAmount(),
                breakdown.consultationOutstandingAmount(),
                breakdown.additionalServiceFee(),
                breakdown.additionalCoveredAmount(),
                breakdown.additionalOutstandingAmount(),
                services,
                breakdown.lines(),
                medicines);
    }

    // Chức năng: xử lý Ánh xạ hóa đơn với mục lịch sử giao dịch.
    private CashierTransactionHistoryItemResponse toTransactionHistoryItem(Invoice invoice) {
        Appointment appointment = invoice.getAppointment();
        Patient patient = appointment == null ? null : appointment.getPatient();

        return new CashierTransactionHistoryItemResponse(
                invoice.getId(),
                appointment == null ? null : appointment.getId(),
                patient == null ? null : patient.getId(),
                patient == null ? null : patient.getFullName(),
                invoice.getPaymentMethod(),
                invoice.getPaidAt(),
                invoice.getGrandTotal());
    }

    // Chức năng: xử lý chuẩn hóa keyword.
    private String normalizeKeyword(String keyword) {
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    // Chức năng: xử lý so sánh từ khóa cho mã số hóa đơn/bệnh nhân.
    private boolean matchesKeyword(Invoice invoice, String keyword) {
        Appointment appointment = invoice.getAppointment();
        Patient patient = appointment == null ? null : appointment.getPatient();

        return containsNumber(invoice.getId(), keyword)
                || containsNumber(appointment == null ? null : appointment.getId(), keyword)
                || containsNumber(patient == null ? null : patient.getId(), keyword)
                || containsText(patient == null ? null : patient.getPhoneNumber(), keyword)
                || containsText(patient == null ? null : patient.getNationalId(), keyword)
                || containsText(patient == null ? null : patient.getHealthInsuranceNumber(), keyword);
    }

    private MedicalRecord resolveMedicalRecord(Invoice invoice) {
        return invoice.getMedicalRecord();
    }

    // Chức năng: xử lý nội dung chứa giá trị số.
    private boolean containsNumber(Long value, String keyword) {
        return value != null && String.valueOf(value).contains(keyword);
    }

    // Chức năng: xử lý nội dung chứa giá trị văn bản.
    private boolean containsText(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    // Chức năng: xử lý kiểm tra bệnh nhân có BHYT hay không.
    private boolean hasHealthInsurance(Patient patient) {
        return patient != null
                && patient.getHealthInsuranceNumber() != null
                && !patient.getHealthInsuranceNumber().trim().isEmpty();
    }

    // Chức năng: xử lý tính mức giảm trừ BHYT 70%.
    private BigDecimal calculateHealthInsuranceDiscount(BigDecimal grossAmount) {
        return defaultAmount(grossAmount)
                .multiply(HEALTH_INSURANCE_DISCOUNT_RATE)
                .setScale(0, RoundingMode.HALF_UP);
    }

    // Chức năng: xử lý giá trị số tiền mặc định.
    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    // Chức năng: xử lý chuẩn hóa đầu vào phương thức thanh toán.
    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Phương thức thanh toán là bắt buộc");
        }
        return paymentMethod.trim().toUpperCase(Locale.ROOT);
    }

    // Chức năng: xử lý xác nhận trạng thái thanh toán điện tử.
    private void ensureElectronicPaymentSucceeded(String paymentMethod, Boolean paymentSuccessful) {
        boolean isElectronic = "CHUYEN_KHOAN".equals(paymentMethod) || "POS".equals(paymentMethod);
        if (isElectronic && !Boolean.TRUE.equals(paymentSuccessful)) {
            throw AppException.of(HttpStatus.CONFLICT, "Giao dịch chưa thành công");
        }
    }

    // Chức năng: xử lý Trừ đi lượng thuốc tồn kho khi xác nhận thanh toán.
    private void deductMedicineStockOnPayment(Long medicalRecordId) {
        List<PrescriptionDetail> prescriptionDetails = prescriptionDetailRepository
                .findByMedicalRecord_Id(medicalRecordId);

        for (PrescriptionDetail detail : prescriptionDetails) {
            Medicine medicine = detail.getMedicine();
            if (medicine == null) {
                throw AppException.of(HttpStatus.CONFLICT, "Không tìm thấy thông tin thuốc trong đơn");
            }

            int quantity = Objects.requireNonNullElse(detail.getQuantity(), 0);
            if (quantity <= 0) {
                continue;
            }

            int currentStock = Objects.requireNonNullElse(medicine.getStockQuantity(), 0);
            if (currentStock < quantity) {
                throw AppException.of(
                        HttpStatus.CONFLICT,
                        "Không đủ tồn kho để thanh toán thuốc: " + medicine.getMedicineName());
            }

            medicine.setStockQuantity(currentStock - quantity);
            medicineRepository.save(medicine);
        }
    }

    // Chức năng: xử lý Đánh dấu hóa đơn đã thanh toán bằng tiền mặt và kết thúc
    // phiên thanh toán của bệnh nhân.
    @SuppressWarnings("unused")
    private Invoice markInvoiceAsPaidAndCloseSession(Invoice invoice) {
        return markInvoiceAsPaidAndCloseSession(invoice, "TIEN_MAT");
    }

    // Chức năng: xử lý Đánh dấu hóa đơn đã thanh toán và kết thúc phiên thanh toán
    // của bệnh nhân.
    private Invoice markInvoiceAsPaidAndCloseSession(Invoice invoice, String paymentMethod) {
        LocalDateTime paidAt = LocalDateTime.now();
        invoice.setPaidAt(paidAt);
        invoice.setPaymentMethod(
                mergePaymentMethod(resolveInvoicePaymentMethod(invoice, paymentMethod), paymentMethod));
        invoice.setIsPaid(Boolean.TRUE);
        invoice.setRemainingAmount(BigDecimal.ZERO);

        Appointment appointment = invoice.getAppointment();
        if (appointment != null) {
            appointment.setPaymentStatus(PaymentStatus.FULLY_PAID);
            appointment.setAdvancePayment(invoice.getGrandTotal());
            MedicalRecord medicalRecord = resolveMedicalRecord(invoice);
            boolean hasCompletedExam = medicalRecord != null && medicalRecord.getCompletedAt() != null;

            if (AppointmentStatus.WAITING_CASHIER.equals(appointment.getStatus())) {
                appointment.setStatus(hasCompletedExam
                        ? AppointmentStatus.COMPLETED
                        : AppointmentStatus.IN_ROOM);
            } else {
                appointment.setStatus(AppointmentStatus.IN_ROOM);
            }
            appointmentRepository.save(appointment);
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);
        backfillUnlinkedTransactionsByReference(savedInvoice);
        return savedInvoice;
    }

    // Chức năng: đánh dấu hóa đơn chuyển khoản đã thanh toán nhưng chưa đổi trạng
    // thái khám.
    public Invoice markTransferInvoiceAsPaid(Long invoiceId, String paymentMethod) {
        if (invoiceId == null) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "invoiceId là bắt buộc");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn"));

        if (Boolean.TRUE.equals(invoice.getIsPaid())) {
            backfillUnlinkedTransactionsByReference(invoice);
            return invoice;
        }

        LocalDateTime paidAt = LocalDateTime.now();
        invoice.setPaidAt(paidAt);
        invoice.setPaymentMethod(
                mergePaymentMethod(resolveInvoicePaymentMethod(invoice, paymentMethod), paymentMethod));

        // Determine total amount actually received for this invoice (including dangling txns by reference).
        BigDecimal totalReceived = BigDecimal.ZERO;
        try {
            // Sum transactions already linked to this invoice
            List<com.example.demo.entity.PaymentTransaction> linked = paymentTransactionRepository.findByInvoice_IdOrderByIdAsc(invoice.getId());
            if (linked != null && !linked.isEmpty()) {
                for (com.example.demo.entity.PaymentTransaction t : linked) {
                    if (t == null) continue;
                    if (!"SUCCESS".equalsIgnoreCase(t.getStatus())) continue;
                    totalReceived = totalReceived.add(defaultAmount(t.getAmount()));
                }
            }

            // Also include any dangling transactions that match the paymentReference but are not yet linked
            String paymentReference = invoice.getPaymentReference();
            if (paymentReference != null && !paymentReference.isBlank()) {
                List<com.example.demo.entity.PaymentTransaction> dangling = paymentTransactionRepository
                        .findByPaymentReferenceAndInvoiceIsNullOrderByIdAsc(paymentReference);
                if (dangling != null && !dangling.isEmpty()) {
                    for (com.example.demo.entity.PaymentTransaction t : dangling) {
                        if (t == null) continue;
                        if (!"SUCCESS".equalsIgnoreCase(t.getStatus())) continue;
                        totalReceived = totalReceived.add(defaultAmount(t.getAmount()));
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Could not compute totalReceived for invoice {}: {}", invoice.getId(), ex.getMessage());
        }

        // Save advanceAmount as actual received (capped between 0 and grandTotal)
        BigDecimal grandTotal = defaultAmount(invoice.getGrandTotal());
        BigDecimal advanceAmount = totalReceived.max(BigDecimal.ZERO).min(grandTotal);
        invoice.setAdvanceAmount(advanceAmount);
        BigDecimal remaining = grandTotal.subtract(advanceAmount);
        invoice.setRemainingAmount(remaining);
        invoice.setIsPaid(remaining.compareTo(BigDecimal.ZERO) <= 0);

        Appointment appointment = invoice.getAppointment();
        if (appointment != null) {
            appointment.setAdvancePayment(defaultAmount(invoice.getAdvanceAmount()));
            appointment.setPaymentStatus(Boolean.TRUE.equals(invoice.getIsPaid()) ? PaymentStatus.FULLY_PAID : PaymentStatus.PENDING_TRANSFER);
            MedicalRecord medicalRecord = resolveMedicalRecord(invoice);
            boolean hasCompletedExam = medicalRecord != null && medicalRecord.getCompletedAt() != null;
                appointment.setStatus(AppointmentStatus.WAITING_CASHIER.equals(appointment.getStatus())
                    ? (hasCompletedExam ? AppointmentStatus.COMPLETED : AppointmentStatus.IN_ROOM)
                    : appointment.getStatus());
            appointmentRepository.save(appointment);
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);
        backfillUnlinkedTransactionsByReference(savedInvoice);
        notifyCashierDashboardRefresh(savedInvoice);
        return savedInvoice;
    }

    private void backfillUnlinkedTransactionsByReference(Invoice invoice) {
        if (invoice == null || invoice.getId() == null) {
            return;
        }

        String paymentReference = invoice.getPaymentReference();
        if (paymentReference == null || paymentReference.isBlank()) {
            return;
        }

        List<com.example.demo.entity.PaymentTransaction> dangling = paymentTransactionRepository
                .findByPaymentReferenceAndInvoiceIsNullOrderByIdAsc(paymentReference);
        if (dangling == null || dangling.isEmpty()) {
            return;
        }

        Appointment appointment = invoice.getAppointment();
        for (com.example.demo.entity.PaymentTransaction tx : dangling) {
            if (tx == null) {
                continue;
            }
            tx.setInvoice(invoice);
            if (appointment != null) {
                tx.setAppointment(appointment);
            }
            paymentTransactionRepository.save(tx);
        }
    }

    private void notifyCashierDashboardRefresh(Invoice invoice) {
        if (invoice == null || invoice.getId() == null) {
            return;
        }
        try {
            messagingTemplate.convertAndSend("/topic/cashier/refresh", String.valueOf(invoice.getId()));
        } catch (Exception ex) {
            log.warn("Không thể phát tín hiệu refresh cho trang thu ngân: invoiceId={}, reason={}", invoice.getId(),
                    ex.getMessage());
        }
    }

    private String resolveInvoicePaymentMethod(Invoice invoice, String... additionalMethods) {
        java.util.LinkedHashSet<String> methods = new java.util.LinkedHashSet<>();

        if (invoice != null) {
            addPaymentMethods(methods, invoice.getPaymentMethod());
            Long invoiceId = invoice.getId();
            if (invoiceId != null) {
                List<com.example.demo.entity.PaymentTransaction> transactions = paymentTransactionRepository
                        .findByInvoice_IdOrderByIdAsc(invoiceId);
                for (com.example.demo.entity.PaymentTransaction transaction : transactions) {
                    if (transaction == null) {
                        continue;
                    }
                    if (!"SUCCESS".equalsIgnoreCase(transaction.getStatus())) {
                        continue;
                    }
                    addPaymentMethods(methods, transaction.getPaymentMethod());
                }
            }
        }

        if (additionalMethods != null) {
            for (String method : additionalMethods) {
                addPaymentMethods(methods, method);
            }
        }

        return methods.isEmpty() ? null : String.join("|", methods);
    }

    private String mergePaymentMethod(String currentValue, String newValue) {
        java.util.LinkedHashSet<String> methods = new java.util.LinkedHashSet<>();
        addPaymentMethods(methods, currentValue);
        addPaymentMethods(methods, newValue);
        return methods.isEmpty() ? null : String.join("|", methods);
    }

    private void addPaymentMethods(java.util.LinkedHashSet<String> methods, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        String[] parts = value.split("\\|");
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                methods.add(part.trim());
            }
        }
    }

    // Chức năng: xử lý chuẩn hóa bộ lọc phương thức thanh toán tùy chọn.
    private String normalizePaymentMethodFilter(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return null;
        }
        String normalized = paymentMethod.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(normalized)) {
            return null;
        }
        if (!SUPPORTED_PAYMENT_METHODS.contains(normalized)) {
            throw AppException.of(
                    HttpStatus.BAD_REQUEST,
                    "Phương thức thanh toán không hợp lệ. Hỗ trợ: TIEN_MAT, CHUYEN_KHOAN, POS");
        }
        return normalized;
    }

    // Chức năng: xử lý Tổng số tiền theo phương thức thanh toán.
    private BigDecimal sumAmountByPaymentMethod(List<Invoice> invoices, String paymentMethod) {
        return invoices.stream()
                .filter(invoice -> paymentMethod.equalsIgnoreCase(nullSafe(invoice.getPaymentMethod())))
                .map(Invoice::getGrandTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Chức năng: xử lý Xây dựng mã hóa đơn cho quy trình xuất báo cáo.
    private String buildInvoiceCode(Long invoiceId, LocalDateTime paidAt) {
        return "INV-" + paidAt.getYear() + "-" + invoiceId;
    }

    // Chức năng: xử lý Đảm bảo hóa đơn đã được xác nhận thanh toán trước khi
    // in/xuất.
    private Invoice getPaidInvoiceOrThrow(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn"));

        if (!Boolean.TRUE.equals(invoice.getIsPaid())) {
            throw AppException.of(HttpStatus.CONFLICT, "Hóa đơn chưa được xác nhận thanh toán");
        }
        return invoice;
    }

    // Chức năng: xử lý tạo văn bản biên lai được định dạng.
    private String buildReceiptText(Invoice invoice, CashierPaymentRecordDetailResponse detail) {
        return String.join(System.lineSeparator(), buildReceiptLines(invoice, detail));
    }

    // Chức năng: xử lý xây dựng các dòng biên lai cho đầu ra in/PDF.
    private List<String> buildReceiptLines(Invoice invoice, CashierPaymentRecordDetailResponse detail) {
        List<String> lines = new ArrayList<>();
        lines.add("================ BIÊN LAI THU TIỀN ================");
        lines.add("Mã hóa đơn: " + invoice.getId());
        lines.add("Mã lịch hẹn: " + detail.getAppointmentId());
        lines.add("Bệnh nhân: " + nullSafe(detail.getPatientName()));
        lines.add("Số điện thoại: " + nullSafe(detail.getPhoneNumber()));
        lines.add("Ngày giờ thanh toán: " + formatDateTime(invoice.getPaidAt()));
        lines.add("----------------------------------------------------");
        lines.add("Chi tiết dịch vụ:");
        if (detail.getServices() == null || detail.getServices().isEmpty()) {
            lines.add("  - Không có");
        } else {
            for (CashierServiceLineItemResponse service : detail.getServices()) {
                lines.add("  - " + nullSafe(service.getServiceName())
                        + " | SL: " + valueOrZero(service.getQuantity())
                        + " | Đơn giá: " + formatAmount(service.getUnitPrice())
                        + " | Thành tiền: " + formatAmount(service.getLineTotal()));
            }
        }

        lines.add("----------------------------------------------------");
        lines.add("Chi tiết thuốc:");
        if (detail.getMedicines() == null || detail.getMedicines().isEmpty()) {
            lines.add("  - Không có");
        } else {
            for (CashierMedicineLineItemResponse medicine : detail.getMedicines()) {
                lines.add("  - " + nullSafe(medicine.getMedicineName())
                        + " | SL: " + valueOrZero(medicine.getQuantity())
                        + " | Đơn giá: " + formatAmount(medicine.getUnitPrice())
                        + " | Thành tiền: " + formatAmount(medicine.getLineTotal()));
            }
        }

        lines.add("----------------------------------------------------");
        lines.add("Tổng tiền dịch vụ: " + formatAmount(detail.getTotalServiceFee()));
        lines.add("TỔNG THANH TOÁN: " + formatAmount(detail.getGrandTotal()));
        lines.add("====================================================");
        lines.add("Cảm ơn quý khách đã sử dụng dịch vụ!");
        return lines;
    }

    // Chức năng: xử lý kết nối máy in.
    private PrintService resolvePrinter(String printerName) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        if (services == null || services.length == 0) {
            throw AppException.of(HttpStatus.SERVICE_UNAVAILABLE, "Không tìm thấy máy in được kết nối");
        }

        if (printerName == null || printerName.isBlank()) {
            PrintService defaultPrinter = PrintServiceLookup.lookupDefaultPrintService();
            if (defaultPrinter != null) {
                return defaultPrinter;
            }
            return services[0];
        }

        String normalized = printerName.trim().toLowerCase(Locale.ROOT);
        for (PrintService service : services) {
            if (service.getName() != null && service.getName().toLowerCase(Locale.ROOT).contains(normalized)) {
                return service;
            }
        }

        throw AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy máy in: " + printerName);
    }

    // Chức năng: xử lý văn bản không an toàn.
    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    // Chức năng: xử lý định dạng thời gian.
    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.format(RECEIPT_TIME_FORMAT);
    }

    // Chức năng: xử lý định dạng số tiền.
    private String formatAmount(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return safeAmount.toPlainString() + " VND";
    }

    // Chức năng: xử lý giá trị mặc định.
    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
