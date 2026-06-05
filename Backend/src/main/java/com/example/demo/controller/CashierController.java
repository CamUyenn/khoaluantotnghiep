package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CashierPaymentRecordDetailResponse;
import com.example.demo.dto.CashierPrintReceiptResponse;
import com.example.demo.dto.CashierProcessPaymentRequest;
import com.example.demo.dto.CashierProcessPaymentResponse;
import com.example.demo.dto.CashierReceiptResponse;
import com.example.demo.dto.CashierTransactionHistoryResponse;
import com.example.demo.dto.CashierWaitingPaymentItemResponse;
import com.example.demo.entity.Invoice;
import com.example.demo.service.InvoiceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cashier")
@RequiredArgsConstructor
@SuppressWarnings("null")
@Tag(name = "Cashier", description = "Nghiệp vụ thu ngan: hang doi thanh toan, xu ly giao dich, in bien lai va xuat hoa don PDF.")
public class CashierController {

    private final InvoiceService invoiceService;

    @GetMapping("/payment-queue")
    @Operation(summary = "Hàng đợi chờ thanh toán", description = "Lấy danh sách hồ sơ đang chờ thu ngân xử lý.")
    // Chức năng: xử lý lấy tất cả hóa đơn đang chờ thanh toán.
    public List<CashierWaitingPaymentItemResponse> getWaitingPaymentQueue(
            @RequestParam(name = "keyword", required = false) String keyword) {
        return invoiceService.getWaitingPaymentQueue(keyword);
    }

    @GetMapping("/payment-records/search")
    @Operation(summary = "Tìm giao dịch", description = "Tìm thông tin thanh toán theo mã hồ sơ, tên bệnh nhân hoặc từ khóa.")
    // Chức năng: xử lý tìm kiếm hồ sơ thanh toán.
    public CashierPaymentRecordDetailResponse searchPaymentRecord(
            @RequestParam(name = "keyword") String keyword) {
        return invoiceService.searchPaymentRecord(keyword);
    }

    @GetMapping("/invoices/{invoiceId}/paid-detail")
    @Operation(summary = "Chi tiết hóa đơn đã thanh toán", description = "Lấy thông tin chi tiết giao dịch đã thanh toán để đối soát.")
    // Chức năng: xử lý lấy thông tin chi tiết hóa đơn đã thanh toán.
    public CashierPaymentRecordDetailResponse getPaidInvoiceDetail(
            @PathVariable("invoiceId") Long invoiceId) {
        return invoiceService.getPaidInvoiceDetail(invoiceId);
    }

    @GetMapping("/transaction-history")
    @Operation(summary = "Lịch sử giao dịch", description = "Thống kê giao dịch theo khoảng thời gian và hình thức thanh toán.")
    public CashierTransactionHistoryResponse getTransactionHistory(
            @RequestParam(name = "startTime", required = false) java.time.LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false) java.time.LocalDateTime endTime,
            @RequestParam(name = "paymentMethod", required = false) String paymentMethod) {
        return invoiceService.getTransactionHistory(startTime, endTime, paymentMethod);
    }

    @GetMapping("/invoices/by-medical-record")
    @Operation(summary = "Tìm hóa đơn theo bệnh án", description = "Lấy hóa đơn dựa trên medicalRecordId.")
    public Invoice getInvoiceByMedicalRecordId(
            @RequestParam(name = "medicalRecordId") Long medicalRecordId) {
        return invoiceService.getByMedicalRecordId(medicalRecordId);
    }

    @PostMapping("/invoices/aggregate")
    @Operation(summary = "Tổng hợp hóa đơn", description = "Tổng hợp tiền dịch vụ và thuốc từ bệnh án để tạo hóa đơn thanh toán.")
    public Invoice aggregateInvoiceAmount(
            @RequestParam(name = "medicalRecordId") Long medicalRecordId) {
        return invoiceService.aggregateInvoiceAmount(medicalRecordId);
    }

    @PutMapping("/invoices/{invoiceId}/confirm-payment")
    @Operation(summary = "Xác nhận đã thanh toán", description = "Đánh dấu hóa đơn đã thanh toán thành công.")
    public Invoice confirmPayment(@PathVariable Long invoiceId) {
        return invoiceService.confirmPayment(invoiceId);
    }

    @PostMapping("/invoices/{invoiceId}/process-payment")
    @Operation(summary = "Xử lý thanh toán", description = "Xử lý thanh toán voi phuong thuc thanh toan va tuy chon xuat hoa don.")
    public CashierProcessPaymentResponse processPayment(
            @PathVariable("invoiceId") Long invoiceId,
            @RequestBody CashierProcessPaymentRequest request) {
        return invoiceService.processPayment(invoiceId, request);
    }

    @GetMapping("/invoices/{invoiceId}/receipt")
    @Operation(summary = "Xem trước biên lai", description = "Xem nội dung biên lai trước khi in hoặc xuất file.")
    public CashierReceiptResponse previewReceipt(@PathVariable Long invoiceId) {
        return invoiceService.previewReceipt(invoiceId);
    }

    @PostMapping("/invoices/{invoiceId}/print-receipt")
    @Operation(summary = "In biên lai", description = "Gửi lệnh in biên lai tới máy in được chỉ định.")
    public CashierPrintReceiptResponse printReceipt(
            @PathVariable Long invoiceId,
            @RequestParam(name = "printerName", required = false) String printerName) {
        return invoiceService.printReceipt(invoiceId, printerName);
    }

    @GetMapping("/invoices/{invoiceId}/export-pdf")
    @Operation(summary = "Xuất hóa đơn PDF", description = "Xuất hóa đơn điện tử dạng PDF để tải về.")
    public ResponseEntity<byte[]> exportInvoicePdf(@PathVariable Long invoiceId) {
        byte[] pdf = invoiceService.exportInvoicePdf(invoiceId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + invoiceId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}
