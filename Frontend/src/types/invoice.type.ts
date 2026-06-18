import type { MedicalRecord } from "./record.type";

export interface Invoice {
	id: number;
	medical_record_id: number;
	total_service_fee: number;
	total_amount: number;
	is_paid: boolean;
	paid_at: string | null;
	medicalRecord?: MedicalRecord;
}

