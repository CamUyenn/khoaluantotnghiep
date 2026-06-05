'use client';

import { useCallback, useState } from 'react';
import { toast } from 'sonner';

import { getApiErrorMessage } from '@/services/api';
import {
  adminService,
  type AdminDiagnosisMedicineRule,
  type AdminDiagnosisTemplate,
  type AdminMedicalCategory,
  type AdminMedicine,
} from '@/services/adminService';

export function useDiagnosisTemplateDashboard() {
  const [templates, setTemplates] = useState<AdminDiagnosisTemplate[]>([]);
  const [categories, setCategories] = useState<AdminMedicalCategory[]>([]);
  const [medicines, setMedicines] = useState<AdminMedicine[]>([]);
  const [rulesMap, setRulesMap] = useState<Record<number, AdminDiagnosisMedicineRule[]>>({});
  const [loading, setLoading] = useState(true);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [categoriesData, medicinesData] = await Promise.all([
        adminService.getMedicalCategories(),
        adminService.getMedicines(),
      ]);

      const diagnosisLists = await Promise.all(
        categoriesData.map((category) => adminService.getDiagnoses(category.id)),
      );
      const diagnoses = diagnosisLists.flat();

      const ruleEntries = await Promise.all(
        diagnoses.map(async (diagnosis) => {
          const rules = await adminService.getDiagnosisMedicineRules(diagnosis.id);
          return [diagnosis.id, rules] as const;
        }),
      );

      const nextRulesMap: Record<number, AdminDiagnosisMedicineRule[]> = {};
      ruleEntries.forEach(([diagnosisId, rules]) => {
        nextRulesMap[diagnosisId] = rules;
      });

      setCategories(categoriesData);
      setMedicines(medicinesData);
      setTemplates(diagnoses);
      setRulesMap(nextRulesMap);
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Khong the tai du lieu chan doan'));
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    templates,
    setTemplates,
    categories,
    medicines,
    rulesMap,
    setRulesMap,
    loading,
    loadData,
  };
}
