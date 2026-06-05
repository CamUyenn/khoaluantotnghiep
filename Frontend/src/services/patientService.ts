import { api } from "./api";

export interface PatientProfileResponse {
  patientId: number;
  fullName: string;
  gender: string;
  dateOfBirth?: string | null;
  hometown?: string | null;
  nationalId: string;
  phoneNumber: string;
  healthInsuranceNumber: string;
  gmail: string;
}

export interface PatientAppointmentRequestPayload {
  appointmentTime: string;
  categoryId: number;
  symptomIds: number[];
  paymentMethod: string;
  symptoms: string;
  paymentReference?: string;
}

export interface AppointmentPaymentStatusResponse {
  appointmentId: number;
  invoiceId: number | null;
  paymentMethod: string | null;
  paymentStatus: string | null;
  paymentReference: string | null;
  transactionStatus: string | null;
  appointmentStatus: string | null;
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

export interface AppointmentFeeEstimateResponse {
  estimatedTotalFee: number;
  services: {
    serviceId: number;
    serviceName: string;
    quantity: number;
    unitPrice: number;
    lineTotal: number;
  }[];
}

export interface PatientAppointmentResponse {
  id: number;
  appointmentTime: string;
  symptoms: string;
  status: string;
  paymentStatus?: string | null;
  paymentMethod?: string | null;
  paymentReference?: string | null;
  cancellationReason?: string | null;
  doctor?: {
    id: number;
    username?: string;
    fullName?: string;
  } | null;
}

export interface PatientMedicalRecordHistoryItemResponse {
  medicalRecordId: number;
  appointmentId: number | null;
  appointmentTime: string | null;
  appointmentStatus: string | null;
  doctorUsername: string | null;
  diagnosis: string | null;
  doctorAdvice: string | null;
  createdAt: string | null;
  prescriptionItemCount: number;
  invoiceId: number | null;
  totalServiceFee: number;
  totalAmount: number;
  paid: boolean;
  paidAt: string | null;
  paymentMethod: string | null;
  paymentReference: string | null;
}

export interface PatientPrescriptionHistoryItemResponse {
  medicineId: number | null;
  medicineName: string | null;
  unit: string | null;
  quantity: number | null;
  usageInstructions: string | null;
  unitPrice: number;
  totalPrice: number;
}

export interface PatientMedicalRecordDetailResponse {
  medicalRecordId: number;
  appointmentId: number | null;
  appointmentTime: string | null;
  appointmentStatus: string | null;
  doctorUsername: string | null;
  diagnosis: string | null;
  doctorAdvice: string | null;
  createdAt: string | null;
  invoiceId: number | null;
  totalServiceFee: number;
  totalAmount: number;
  paid: boolean;
  paidAt: string | null;
  paymentMethod: string | null;
  paymentReference: string | null;
  services: {
    serviceId: number | null;
    serviceName: string | null;
    quantity: number | null;
    actualPrice: number;
    resultNote: string | null;
  }[];
  prescriptionItems: PatientPrescriptionHistoryItemResponse[];
}

export const patientService = {
  async getProfile() {
    const response = await api.get<PatientProfileResponse>("/api/patient/profile");
    return response.data;
  },

  async createAppointment(payload: PatientAppointmentRequestPayload) {
    const response = await api.post<PatientAppointmentResponse>("/api/patient/appointments", payload);
    return response.data;
  },

  async estimateAppointmentFee(symptomIds: number[]) {
    const response = await api.get<AppointmentFeeEstimateResponse>("/api/patient/appointments/estimate-fee", {
      params: { symptomIds },
    });
    return response.data;
  },

  async getMyAppointments() {
    const response = await api.get<PatientAppointmentResponse[]>("/api/patient/appointments");
    return response.data;
  },

  async cancelMyAppointment(appointmentId: number) {
    const response = await api.put<PatientAppointmentResponse>(`/api/patient/appointments/${appointmentId}/cancel`);
    return response.data;
  },

  async getMedicalRecordHistory() {
    const response = await api.get<PatientMedicalRecordHistoryItemResponse[]>("/api/patient/medical-records");
    return response.data;
  },

  async getMedicalRecordDetail(medicalRecordId: number) {
    const response = await api.get<PatientMedicalRecordDetailResponse>(`/api/patient/medical-records/${medicalRecordId}`);
    return response.data;
  },

  async getAppointmentPaymentStatus(appointmentId: number) {
    const response = await api.get<AppointmentPaymentStatusResponse>(`/api/payments/appointments/${appointmentId}/status`);
    return response.data;
  },

  async getPaymentReferenceStatus(paymentReference: string) {
    const response = await api.get<PaymentReferenceStatusResponse>(`/api/payments/reference/${encodeURIComponent(paymentReference)}/status`);
    return response.data;
  },
};
