export interface User {
	id: number;
	username: string;
	password: string;
	role: "admin" | "doctor" | "receptionist" | "cashier";
	is_active: boolean;
}

export interface Patient {
	id: number;
	user_id: number;
	full_name: string;
	gender: "male" | "female" | "other";
	national_id: string;
	phone_number: string;
}

export interface Room {
	id: number;
	room_name: string;
	current_doctor_id: number | null;
	doctor?: User;
}

