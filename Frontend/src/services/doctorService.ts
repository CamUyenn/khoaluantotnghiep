import { api } from "./api";

export interface DoctorPatient {
  id: number;
  fullName: string;
  gender?: string | null;
  nationalId?: string | null;
  healthInsuranceNumber?: string | null;
  phoneNumber?: string | null;
  gmail?: string | null;
  dateOfBirth?: string | null;
  hometown?: string | null;
}

export interface DoctorUser {
  id: number;
  username: string;
}

export interface DoctorAppointment {
  id: number;
  appointmentTime: string;
  symptoms?: string | null;
  status: string;
  patient: DoctorPatient;
  doctor?: DoctorUser | null;
  category?: { id?: number | null; name?: string | null } | null;
}

export interface DoctorMedicine {
  id: number;
  medicineName: string;
  medicineType: string;
  unit?: string | null;
  sellingPrice: number;
  stockQuantity: number;
  isActive: boolean;
}

export interface DoctorMedicalService {
  id: number;
  serviceName: string;
  currentPrice: number;
  isActive: boolean;
}

export interface MedicalRecordResponse {
  id: number;
  diagnosis: string;
  doctorAdvice: string;
  createdAt?: string;
}

export interface DiagnosisTemplateResponse {
  id: number;
  categoryId?: number | null;
  categoryName?: string | null;
  diagnosisName: string;
  defaultAdvice?: string | null;
}

export interface PrescriptionCatalogMedicine {
  medicineId: number;
  medicineName: string;
  pharmacologyGroup: string;
  concentration: string;
  unit: string | null;
  sellingPrice: number;
  stockQuantity: number;
  canQuickAdd: boolean;
  outOfStockMessage?: string | null;
}

export interface PrescriptionLine {
  medicineId: number;
  medicineName: string;
  unit: string | null;
  quantity: number;
  usageInstructions: string;
  sellingPrice: number;
  lineTotal: number;
}

export interface PrescriptionWorkspaceResponse {
  medicalRecordId: number;
  prescribedMedicines: PrescriptionLine[];
  medicineCatalog: PrescriptionCatalogMedicine[];
  totalAmount: number;
}

export interface DoctorHistoryRow {
  medicalRecordId: number;
  appointmentId: number;
  appointmentTime: string;
  doctorName?: string | null;
  oldDiagnosis?: string | null;
  usedMedicines: string[];
}

export interface DoctorHistorySummary {
  patientId: number;
  patientName: string;
  message: string;
  histories: DoctorHistoryRow[];
}

export interface DoctorHistoryPrescription {
  medicineId: number;
  medicineName: string;
  quantity: number;
  usageInstructions: string;
}

export interface DoctorHistoryServiceItem {
  serviceId: number;
  serviceName: string;
  quantity: number;
  actualPrice: number;
  resultNote?: string | null;
}

export interface DoctorHistoryDetail {
  medicalRecordId: number;
  appointmentId: number;
  appointmentTime: string;
  doctorName?: string | null;
  diagnosis?: string | null;
  doctorAdvice?: string | null;
  previousSymptoms?: string | null;
  prescriptions: DoctorHistoryPrescription[];
  services: DoctorHistoryServiceItem[];
}

export const doctorService = {
  async getWaitingPatients(date?: string) {
    const response = await api.get<DoctorAppointment[]>("/api/doctors/me/waiting-patients", {
      params: date ? { date } : undefined,
    });
    return response.data;
  },

  async getCompletedPatients(date?: string) {
    const response = await api.get<DoctorAppointment[]>("/api/doctors/me/completed-patients", {
      params: date ? { date } : undefined,
    });
    return response.data;
  },

  async getAvailableMedicines() {
    const response = await api.get<DoctorMedicine[]>("/api/doctors/me/medicines");
    return response.data;
  },

  async createMedicalRecord(payload: { appointmentId: number; diagnosis: string; doctorAdvice: string }) {
    const response = await api.post<MedicalRecordResponse>("/api/medical-records/doctor", payload);
    return response.data;
  },

  async updateMedicalRecord(medicalRecordId: number, payload: { diagnosis: string; doctorAdvice: string }) {
    const response = await api.put<MedicalRecordResponse>(`/api/medical-records/${medicalRecordId}`, payload);
    return response.data;
  },

  async autoPopulatePrescriptionsFromDiagnosis(medicalRecordId: number, diagnosisId: number) {
    const response = await api.post<PrescriptionWorkspaceResponse>(
      `/api/doctors/medical-records/${medicalRecordId}/prescriptions/autopopulate`,
      null,
      { params: { diagnosisId } },
    );
    return response.data;
  },

  async getMedicalRecordByAppointment(appointmentId: number) {
    const response = await api.get<MedicalRecordResponse>(`/api/medical-records/appointment/${appointmentId}`);
    return response.data;
  },

  async getPrescriptionWorkspace(medicalRecordId: number) {
    const response = await api.get<PrescriptionWorkspaceResponse>(
      `/api/medical-records/${medicalRecordId}/prescription-workspace`,
    );
    return response.data;
  },

  async quickAddMedicine(medicalRecordId: number, medicineId: number) {
    const response = await api.post<PrescriptionWorkspaceResponse>(
      `/api/medical-records/${medicalRecordId}/prescription-details/quick-add`,
      { medicineId },
    );
    return response.data;
  },

  async addPrescriptionDetail(
    medicalRecordId: number,
    payload: { medicineId: number; quantity: number; usageInstructions: string },
  ) {
    await api.post(`/api/medical-records/${medicalRecordId}/prescription-details`, payload);
  },

  async updatePrescriptionDetail(
    medicalRecordId: number,
    medicineId: number,
    payload: { quantity: number; usageInstructions: string },
  ) {
    await api.put(`/api/medical-records/${medicalRecordId}/prescription-details/${medicineId}`, payload);
  },

  async removePrescriptionDetail(medicalRecordId: number, medicineId: number) {
    await api.delete(`/api/medical-records/${medicalRecordId}/prescription-details/${medicineId}`);
  },

  async savePrescription(medicalRecordId: number) {
    await api.post(`/api/medical-records/${medicalRecordId}/prescription-save`);
  },

  async completeMedicalRecord(medicalRecordId: number) {
    await api.put(`/api/medical-records/${medicalRecordId}/complete`);
  },

  async getPatientHistorySummary(appointmentId: number) {
    const response = await api.get<DoctorHistorySummary>(`/api/doctors/appointments/${appointmentId}/patient-history`);
    return response.data;
  },

  async getPatientHistoryDetail(appointmentId: number, medicalRecordId: number) {
    const response = await api.get<DoctorHistoryDetail>(
      `/api/doctors/appointments/${appointmentId}/patient-history/${medicalRecordId}`,
    );
    return response.data;
  },

  async getAvailableServices() {
    const response = await api.get<DoctorMedicalService[]>('/api/doctors/me/services');
    return response.data;
  },

  async upsertMedicalRecordServiceResult(
    medicalRecordId: number,
    payload: { serviceId: number; quantity: number; actualPrice: number; resultNote?: string },
  ) {
    await api.post(`/api/medical-records/${medicalRecordId}/service-results`, payload);
  },
};
