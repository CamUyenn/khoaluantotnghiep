import { api } from "./api";

export interface CashierWaitingPaymentItemResponse {
  invoiceId: number;
  appointmentId: number;
  patientId: number;
  patientName: string;
  phoneNumber: string;
  appointmentTime: string;
  grandTotal: number;
  advanceAmount: number;
  remainingAmount: number;
  invoiceStatus: string;
}

export interface CashierServiceLineItemResponse {
  serviceId: number;
  serviceName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface CashierMedicineLineItemResponse {
  medicineId: number;
  medicineName: string;
  quantity: number;
  usageInstructions: string;
  unitPrice: number;
  lineTotal: number;
}

export interface CashierPaymentRecordDetailResponse {
  invoiceId: number;
  appointmentId: number;
  patientId: number;
  patientName: string;
  phoneNumber: string;
  appointmentTime: string;
  invoiceStatus: string;
  paymentMethod?: string | null;
  paidAt?: string | null;
  totalServiceFee: number;
  grandTotal: number;
  advanceAmount: number;
  remainingAmount: number;
  consultationFee?: number;
  consultationCoveredAmount?: number;
  consultationOutstandingAmount?: number;
  additionalServiceFee?: number;
  additionalCoveredAmount?: number;
  additionalOutstandingAmount?: number;
  services: CashierServiceLineItemResponse[];
  serviceBreakdown?: Array<CashierServiceLineItemResponse & {
    coveredAmount: number;
    unpaidAmount: number;
  }>;
  medicines: CashierMedicineLineItemResponse[];
}

export interface CashierTransactionHistoryItemResponse {
  invoiceId: number;
  appointmentId: number;
  patientId: number;
  patientName: string;
  paymentMethod: string;
  paidAt: string;
  grandTotal: number;
}

export interface CashierTransactionHistoryResponse {
  startTime: string;
  endTime: string;
  paymentMethodFilter: string;
  totalTransactions: number;
  totalAmount: number;
  totalCash: number;
  totalBankTransfer: number;
  totalPos: number;
  transactions: CashierTransactionHistoryItemResponse[];
}

export type CashierPaymentMethod = "TIEN_MAT" | "CHUYEN_KHOAN" | "POS";

export interface CashierProcessPaymentRequest {
  paymentMethod: CashierPaymentMethod;
  paymentSuccessful: boolean;
  exportInvoice: boolean;
  applyHealthInsurance: boolean;
}

export interface CashierProcessPaymentResponse {
  invoiceId: number;
  appointmentId: number;
  paymentMethod: string;
  totalServiceFee: number;
  grandTotal: number;
  insuranceDiscountAmount: number;
  remainingAmount: number;
  insuranceApplied: boolean;
  paidAt: string;
  transactionStatus: string;
  invoiceExported: boolean;
  invoiceCode: string;
  message: string;
}

export interface PaymentReferenceStatusResponse {
  paymentReference: string;
  invoiceId: number | null;
  appointmentId: number | null;
  paymentStatus: string;
  transactionStatus: string | null;
  message: string;
}

export const cashierService = {
  async getWaitingPaymentQueue(keyword?: string) {
    const response = await api.get<CashierWaitingPaymentItemResponse[]>("/api/cashier/payment-queue", {
      params: keyword?.trim() ? { keyword: keyword.trim() } : undefined,
    });
    return response.data;
  },

  async searchPaymentRecord(keyword: string) {
    const response = await api.get<CashierPaymentRecordDetailResponse>("/api/cashier/payment-records/search", {
      params: { keyword },
    });
    return response.data;
  },

  async getPaidInvoiceDetail(invoiceId: number) {
    const response = await api.get<CashierPaymentRecordDetailResponse>(`/api/cashier/invoices/${invoiceId}/paid-detail`);
    return response.data;
  },

  async getTransactionHistory(params?: { startTime?: string; endTime?: string; paymentMethod?: string }) {
    const response = await api.get<CashierTransactionHistoryResponse>("/api/cashier/transaction-history", {
      params,
    });
    return response.data;
  },

  async processPayment(invoiceId: number, payload: CashierProcessPaymentRequest) {
    const response = await api.post<CashierProcessPaymentResponse>(`/api/cashier/invoices/${invoiceId}/process-payment`, payload);
    return response.data;
  },

  async getPaymentReferenceStatus(paymentReference: string) {
    const response = await api.get<PaymentReferenceStatusResponse>(
      `/api/payments/reference/${encodeURIComponent(paymentReference)}/status`,
    );
    return response.data;
  },

  async exportInvoicePdf(invoiceId: number) {
    const response = await api.get<Blob>(`/api/cashier/invoices/${invoiceId}/export-pdf`, {
      responseType: "blob",
    });
    return response.data;
  },
};
