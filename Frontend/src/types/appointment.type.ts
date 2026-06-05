import type { Patient, User } from "./user.type";

export interface Appointment {
	id: number;
	patient_id: number;
	doctor_id: number;
	appointment_time: string;
	symptoms: string;
	status: "pending" | "confirmed" | "completed" | "cancelled";
	patient?: Patient;
	doctor?: User;
}

