import type { Appointment } from "./appointment.type";
import type { Medicine, Service } from "./medicine.type";

export interface PrescriptionDetail {
	id: number;
	medical_record_id: number;
	medicine_id: number;
	quantity: number;
	usage_instructions: string;
	medicine?: Medicine;
}

export interface MedicalRecordService {
	id: number;
	medical_record_id: number;
	service_id: number;
	quantity: number;
	actual_price: number;
	service?: Service;
}

export interface MedicalRecord {
	id: number;
	appointment_id: number;
	diagnosis: string;
	doctor_advice: string;
	created_at: string;
	appointment?: Appointment;
	prescriptionDetails?: PrescriptionDetail[];
	services?: MedicalRecordService[];
}

