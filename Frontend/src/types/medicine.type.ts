export interface Medicine {
	id: number;
	medicine_name: string;
	unit: string;
	selling_price: number;
	stock_quantity: number;
	is_active: boolean;
}

export interface Service {
	id: number;
	service_name: string;
	current_price: number;
	is_active: boolean;
}

