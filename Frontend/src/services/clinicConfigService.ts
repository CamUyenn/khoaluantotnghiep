import { api } from "./api";

export interface ClinicBankConfigResponse {
  bankBin: string;
  bankAccount: string;
  accountName: string;
  bankName: string;
}

export const clinicConfigService = {
  async getBankConfig() {
    const response = await api.get<ClinicBankConfigResponse>("/api/payments/bank-config");
    return response.data;
  },
};