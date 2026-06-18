import { api } from "./api";

export interface MedicalCategoryResponse {
  id: number;
  name: string;
  description?: string | null;
  isActive?: boolean;
}

export interface SymptomTemplateResponse {
  id: number;
  categoryId?: number | null;
  categoryName?: string | null;
  symptomName: string;
}

export interface DiagnosisTemplateResponse {
  id: number;
  categoryId?: number | null;
  categoryName?: string | null;
  diagnosisName: string;
  defaultAdvice?: string | null;
}

export const masterDataService = {
  async getCategories() {
    const res = await api.get<MedicalCategoryResponse[]>('/api/catalog/categories');
    return res.data;
  },

  async getSymptomsByCategory(categoryId: number) {
    const res = await api.get<SymptomTemplateResponse[]>(`/api/catalog/categories/${categoryId}/symptoms`);
    return res.data;
  },

  async getDiagnosesByCategory(categoryId: number) {
    const res = await api.get<DiagnosisTemplateResponse[]>(`/api/catalog/categories/${categoryId}/diagnoses`);
    return res.data;
  },
};
