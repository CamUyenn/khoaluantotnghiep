export interface SelectedServiceItem {
  service_id: number;
  service_name: string;
  quantity: number;
  actual_price: number;
  result_note?: string;
}
export type Gender = "male" | "female";

export type AppointmentStatus = "pending" | "confirmed" | "completed" | "cancelled";

export interface Patient {
  id: number;
  full_name: string;
  date_of_birth?: string;
  phone_number?: string;
  hometown?: string;
  national_id?: string;
  insurance_number?: string;
  gender?: Gender;
}

export interface Appointment {
  id: number;
  patient: Patient;
  doctor_name: string;
  symptoms: string;
  category?: { id?: number | null; name?: string | null } | null;
  scheduled_date?: string;
  scheduled_time: string;
  queue_number: number;
  appointment_time: string;
  status: AppointmentStatus;
  diagnosis?: string;
  doctor_advice?: string;
  prescription_items?: PrescriptionItem[];
  service_items?: SelectedServiceItem[];
  total_medicine_cost?: number;
  total_service_cost?: number;
  total_exam_cost?: number;
}

export interface Medicine {
  id: number;
  medicine_name: string;
  dosage?: string;
  category: string;
  unit?: string;
  stock_quantity: number;
  selling_price: number;
}

export interface PrescriptionItem {
  medicine_id: number;
  quantity: number;
  usage_instructions: string;
  medicine: Medicine;
}

export interface MedicalRecordInput {
  diagnosis: string;
  doctor_advice: string;
}

export interface PatientHistoryRow {
  medicalRecordId: number;
  appointmentId: number;
  appointmentTime: string;
  doctorName?: string | null;
  oldDiagnosis?: string | null;
  usedMedicines: string[];
}

export interface PatientHistoryDetail {
  medicalRecordId: number;
  appointmentId: number;
  appointmentTime: string;
  doctorName?: string | null;
  diagnosis?: string | null;
  doctorAdvice?: string | null;
  previousSymptoms?: string | null;
  prescriptions: Array<{
    medicineId: number;
    medicineName: string;
    quantity: number;
    usageInstructions: string;
  }>;
  services: Array<{
    serviceId: number;
    serviceName: string;
    quantity: number;
    actualPrice: number;
    resultNote?: string | null;
  }>;
}
