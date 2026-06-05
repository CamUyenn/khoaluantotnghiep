'use client';

import { useEffect, useMemo, useState } from 'react';
import { useForm, type Resolver } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { getApiErrorMessage } from '@/services/api';
import {
  adminService,
  type AdminDiagnosisMedicineRule,
  type AdminDiagnosisTemplate,
  type AdminMedicine,
} from '@/services/adminService';
import { useDiagnosisTemplateDashboard } from '@/hooks/useDiagnosisTemplateDashboard';
import styles from '../diagnosis-template-management.module.css';

type AgeGroupKey = 'child' | 'adult' | 'elderly';

type AgeGroupDosage = {
  age_group: AgeGroupKey;
  age_range: string;
  quantity: number;
  usage_instructions: string;
};

type MedicineTemplate = {
  medicine_id: number;
  medicine_name: string;
  age_groups: AgeGroupDosage[];
  default_usage_instructions: string;
};


const AGE_RANGES: Record<AgeGroupKey, { minAge: number; maxAge: number; label: string; range: string }> = {
  child: { minAge: 0, maxAge: 17, label: '👶 Trẻ em', range: '<18' },
  adult: { minAge: 18, maxAge: 60, label: '👤 Người lớn', range: '18-60' },
  elderly: { minAge: 61, maxAge: 120, label: '👴 Người cao tuổi', range: '>60' },
};

const getAgeGroupKey = (rule: AdminDiagnosisMedicineRule): AgeGroupKey => {
  if (rule.maxAge <= 17) return 'child';
  if (rule.minAge >= 61) return 'elderly';
  return 'adult';
};

const diagnosisTemplateSchema = z.object({
  disease_category_id: z.coerce.number().int().min(1, 'Vui lòng chọn loại bệnh'),
  diagnosis_name: z.string().trim().min(1, 'Vui lòng nhập tên chẩn đoán'),
  doctor_advice: z.string().trim().min(1, 'Vui lòng nhập lời khuyên điều trị'),
});

type DiagnosisTemplateFormValues = z.infer<typeof diagnosisTemplateSchema>;

const medicineTemplateSchema = z.object({
  medicine_id: z.number(),
  medicine_name: z.string().trim().min(1, 'Vui lòng chọn thuốc hợp lệ'),
  default_usage_instructions: z.string().trim().min(1, 'Vui lòng nhập hướng dẫn sử dụng cho tất cả thuốc'),
  age_groups: z.array(
    z.object({
      age_group: z.enum(['child', 'adult', 'elderly']),
      age_range: z.string(),
      quantity: z.number().min(1, 'Số lượng thuốc phải >= 1'),
      usage_instructions: z.string().trim().min(1, 'Vui lòng nhập ghi chú theo độ tuổi cho tất cả thuốc'),
    }),
  ),
});

const medicineTemplateListSchema = z
  .array(medicineTemplateSchema)
  .min(1, 'Vui lòng thêm ít nhất một loại thuốc');

export function DiagnosisTemplateManagement() {
  const {
    templates,
    setTemplates,
    categories,
    medicines,
    rulesMap,
    setRulesMap,
    loading,
    loadData,
  } = useDiagnosisTemplateDashboard();
  const [isAdding, setIsAdding] = useState(false);
  const [editing, setEditing] = useState<AdminDiagnosisTemplate | null>(null);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);

  const [formMedicines, setFormMedicines] = useState<MedicineTemplate[]>([]);

  const {
    register,
    reset,
    getValues,
    trigger,
    formState: { errors },
  } = useForm<DiagnosisTemplateFormValues>({
    resolver: zodResolver(diagnosisTemplateSchema) as Resolver<DiagnosisTemplateFormValues>,
    defaultValues: {
      disease_category_id: 0,
      diagnosis_name: '',
      doctor_advice: '',
    },
    mode: 'onChange',
  });

  const [showMedicineModal, setShowMedicineModal] = useState(false);
  const [modalMedicines, setModalMedicines] = useState<MedicineTemplate[]>([]);
  const [medicineSearch, setMedicineSearch] = useState('');
  const [activeMedicineCategory, setActiveMedicineCategory] = useState('');

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const medicineCategories = useMemo(() => {
    return Array.from(
      new Set(medicines.map((medicine) => medicine.medicineType || 'Khác')),
    );
  }, [medicines]);

  const filteredMedicines = useMemo(() => {
    const searchTerm = medicineSearch.trim().toLowerCase();
    return medicines.filter((medicine) => {
      const matchesCategory = activeMedicineCategory
        ? (medicine.medicineType || 'Khác') === activeMedicineCategory
        : true;
      const matchesSearch = searchTerm
        ? medicine.medicineName.toLowerCase().includes(searchTerm)
        : true;
      return matchesCategory && matchesSearch;
    });
  }, [activeMedicineCategory, medicineSearch, medicines]);

  useEffect(() => {
    if (!medicineCategories.length) {
      setActiveMedicineCategory('');
      return;
    }
    if (!medicineCategories.includes(activeMedicineCategory)) {
      setActiveMedicineCategory(medicineCategories[0]);
    }
  }, [activeMedicineCategory, medicineCategories]);

  const handleAdd = () => {
    setIsAdding(true);
    reset({
      disease_category_id: 0,
      diagnosis_name: '',
      doctor_advice: '',
    });
    setFormMedicines([]);
  };

  const handleEdit = (template: AdminDiagnosisTemplate) => {
    setEditing(template);
    const rules = rulesMap[template.id] ?? [];
    const grouped = rules.reduce<Record<number, AdminDiagnosisMedicineRule[]>>((acc, rule) => {
      if (!acc[rule.medicineId]) {
        acc[rule.medicineId] = [];
      }
      acc[rule.medicineId].push(rule);
      return acc;
    }, {});

    const medicinesFromRules: MedicineTemplate[] = Object.values(grouped).map((group) => {
      const sample = group[0];
      const ageGroups: AgeGroupDosage[] = group.map((rule) => {
        const key = getAgeGroupKey(rule);
        return {
          age_group: key,
          age_range: AGE_RANGES[key].range,
          quantity: rule.defaultQuantity,
          usage_instructions: rule.defaultUsage ?? '',
        };
      });

      return {
        medicine_id: sample.medicineId,
        medicine_name: sample.medicineName ?? '',
        age_groups: ageGroups,
        default_usage_instructions: group[0]?.defaultUsage ?? '',
      };
    });

    reset({
      disease_category_id: template.categoryId,
      diagnosis_name: template.diagnosisName,
      doctor_advice: template.defaultAdvice ?? '',
    });
    setFormMedicines(medicinesFromRules);
  };

  const handleSave = async () => {
    const isFormValid = await trigger();
    if (!isFormValid) {
      toast.error('Vui lòng điền đầy đủ thông tin');
      return;
    }

    const medicinesResult = medicineTemplateListSchema.safeParse(formMedicines);
    if (!medicinesResult.success) {
      toast.error(medicinesResult.error.issues[0]?.message ?? 'Vui lòng kiểm tra danh sách thuốc');
      return;
    }

    const formValues = diagnosisTemplateSchema.parse(getValues());

    try {
      let savedDiagnosis: AdminDiagnosisTemplate;

      if (editing) {
        savedDiagnosis = await adminService.updateDiagnosis(editing.id, {
          categoryId: formValues.disease_category_id,
          diagnosisName: formValues.diagnosis_name,
          defaultAdvice: formValues.doctor_advice,
        });
      } else {
        savedDiagnosis = await adminService.createDiagnosis({
          categoryId: formValues.disease_category_id,
          diagnosisName: formValues.diagnosis_name,
          defaultAdvice: formValues.doctor_advice,
        });
      }

      const existingRules = rulesMap[savedDiagnosis.id] ?? [];
      await Promise.all(existingRules.map((rule) => adminService.deleteDiagnosisMedicineRule(rule.id)));

      const rulePayloads = formMedicines.flatMap((medicine) =>
        medicine.age_groups.map((group) => {
          const range = AGE_RANGES[group.age_group];
          return {
            diagnosisId: savedDiagnosis.id,
            medicineId: medicine.medicine_id,
            minAge: range.minAge,
            maxAge: range.maxAge,
            defaultQuantity: group.quantity,
            defaultUsage: group.usage_instructions,
          };
        }),
      );

      await Promise.all(rulePayloads.map((payload) => adminService.createDiagnosisMedicineRule(payload)));

      const updatedRules = await adminService.getDiagnosisMedicineRules(savedDiagnosis.id);
      setRulesMap({ ...rulesMap, [savedDiagnosis.id]: updatedRules });

      if (editing) {
        setTemplates(templates.map((t) => (t.id === savedDiagnosis.id ? savedDiagnosis : t)));
        setEditing(null);
      } else {
        setTemplates([...templates, savedDiagnosis]);
        setIsAdding(false);
      }

      reset({
        disease_category_id: 0,
        diagnosis_name: '',
        doctor_advice: '',
      });
      setFormMedicines([]);
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Không thể lưu chẩn đoán'));
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Bạn có chắc muốn xóa chẩn đoán này?')) {
      return;
    }
    try {
      await adminService.deleteDiagnosis(id);
      setTemplates(templates.filter((t) => t.id !== id));
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Không thể xóa chẩn đoán'));
    }
  };

  const handleCancel = () => {
    setIsAdding(false);
    setEditing(null);
    reset({
      disease_category_id: 0,
      diagnosis_name: '',
      doctor_advice: '',
    });
    setFormMedicines([]);
  };

  const cloneMedicineTemplate = (medicine: MedicineTemplate): MedicineTemplate => ({
    ...medicine,
    age_groups: medicine.age_groups.map((group) => ({ ...group })),
  });

  const createMedicineTemplate = (medicine: AdminMedicine): MedicineTemplate => ({
    medicine_id: medicine.id,
    medicine_name: medicine.medicineName,
    age_groups: [
      { age_group: 'child', age_range: AGE_RANGES.child.range, quantity: 1, usage_instructions: '' },
      { age_group: 'adult', age_range: AGE_RANGES.adult.range, quantity: 1, usage_instructions: '' },
      { age_group: 'elderly', age_range: AGE_RANGES.elderly.range, quantity: 1, usage_instructions: '' },
    ],
    default_usage_instructions: '',
  });

  const handleAddMedicine = () => {
    setShowMedicineModal(true);
    setMedicineSearch('');
    setModalMedicines(formMedicines.map(cloneMedicineTemplate));
  };

  const handleEditMedicine = () => {
    setShowMedicineModal(true);
    setMedicineSearch('');
    setModalMedicines(formMedicines.map(cloneMedicineTemplate));
  };

  const handleSaveMedicine = () => {
    const medicinesResult = medicineTemplateListSchema.safeParse(modalMedicines);
    if (!medicinesResult.success) {
      toast.error(medicinesResult.error.issues[0]?.message ?? 'Vui lòng kiểm tra danh sách thuốc');
      return;
    }

    setFormMedicines(modalMedicines.map(cloneMedicineTemplate));
    setShowMedicineModal(false);
  };

  const handleRemoveMedicine = (index: number) => {
    if (confirm('Bạn có chắc muốn xóa thuốc này khỏi chẩn đoán?')) {
      setFormMedicines(formMedicines.filter((_, i) => i !== index));
    }
  };

  const handleSelectCatalogMedicine = (medicine: AdminMedicine) => {
    setModalMedicines((prev) => {
      if (prev.some((item) => item.medicine_id === medicine.id)) {
        return prev;
      }
      return [...prev, createMedicineTemplate(medicine)];
    });
  };

  const updateModalMedicineQuantity = (medicineId: number, quantity: number) => {
    setModalMedicines((prev) =>
      prev.map((medicine) =>
        medicine.medicine_id === medicineId
          ? {
              ...medicine,
              age_groups: medicine.age_groups.map((group) => ({
                ...group,
                quantity,
              })),
            }
          : medicine,
      ),
    );
  };

  const updateModalMedicineUsage = (medicineId: number, usage: string) => {
    setModalMedicines((prev) =>
      prev.map((medicine) =>
        medicine.medicine_id === medicineId
          ? {
              ...medicine,
              default_usage_instructions: usage,
            }
          : medicine,
      ),
    );
  };

  const updateModalAgeGroupUsage = (medicineId: number, ageGroup: AgeGroupKey, usage: string) => {
    setModalMedicines((prev) =>
      prev.map((medicine) =>
        medicine.medicine_id === medicineId
          ? {
              ...medicine,
              age_groups: medicine.age_groups.map((group) =>
                group.age_group === ageGroup ? { ...group, usage_instructions: usage } : group,
              ),
            }
          : medicine,
      ),
    );
  };

  const handleRemoveModalMedicine = (medicineId: number) => {
    setModalMedicines((prev) => prev.filter((medicine) => medicine.medicine_id !== medicineId));
  };

  const getFilteredTemplates = () => {
    if (!selectedCategory) return templates;
    return templates.filter((t) => String(t.categoryId) === selectedCategory);
  };

  const medicineUnits = useMemo(() => {
    return new Map(medicines.map((medicine) => [medicine.id, medicine.unit ?? 'đơn vị']));
  }, [medicines]);

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1>Quản lý Chẩn đoán và Thuốc</h1>
        <p>Tạo và quản lý chẩn đoán kèm theo thuốc điều trị (phân theo độ tuổi)</p>
      </div>

      <div className={styles.card}>
        <label className={styles.label}>Lọc theo loại bệnh:</label>
        <div className={styles.filterRow}>
          <button
            type="button"
            className={`${styles.filterPill} ${selectedCategory === null ? styles.filterActive : ''}`}
            onClick={() => setSelectedCategory(null)}
          >
            Tất cả ({templates.length})
          </button>
          {categories.map((cat) => {
            const count = templates.filter((t) => t.categoryId === cat.id).length;
            return (
              <button
                key={cat.id}
                type="button"
                className={`${styles.filterPill} ${selectedCategory === String(cat.id) ? styles.filterActive : ''}`}
                onClick={() => setSelectedCategory(String(cat.id))}
              >
                {cat.name} ({count})
              </button>
            );
          })}
        </div>
      </div>

      <div className={styles.card}>
        <div className={styles.sectionHeader}>
          <h2>Danh sách chẩn đoán ({getFilteredTemplates().length})</h2>
          <button className={styles.primaryButton} type="button" onClick={handleAdd}>
            + Thêm chẩn đoán
          </button>
        </div>

        {(isAdding || editing) && (
          <div className={styles.formCard}>
            <h3>{editing ? 'Chỉnh sửa' : 'Thêm mới'} chẩn đoán</h3>

            <div className={styles.formGroup}>
              <label className={styles.label}>Loại bệnh *</label>
              <select
                className={styles.input}
                {...register('disease_category_id')}
              >
                <option value="0">-- Chọn loại bệnh --</option>
                {categories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.name}
                  </option>
                ))}
              </select>
              {errors.disease_category_id?.message && (
                <p className={styles.errorText}>{errors.disease_category_id.message}</p>
              )}
            </div>

            <div className={styles.grid2}>
              <div className={styles.formGroup}>
                <label className={styles.label}>Tên chẩn đoán *</label>
                <input
                  className={styles.input}
                  {...register('diagnosis_name')}
                  placeholder="VD: Viêm họng cấp"
                />
                {errors.diagnosis_name?.message && (
                  <p className={styles.errorText}>{errors.diagnosis_name.message}</p>
                )}
              </div>
            </div>

            <div className={styles.formGroup}>
              <label className={styles.label}>Lời khuyên điều trị *</label>
              <textarea
                className={styles.textarea}
                rows={3}
                {...register('doctor_advice')}
                placeholder="VD: Nghỉ ngơi đầy đủ, uống nhiều nước ấm, tái khám sau 5-7 ngày..."
              />
              {errors.doctor_advice?.message && (
                <p className={styles.errorText}>{errors.doctor_advice.message}</p>
              )}
            </div>

            <div className={styles.formGroup}>
              <div className={styles.sectionHeader}>
                <label className={styles.label}>Danh sách thuốc *</label>
                <button type="button" className={styles.primaryButton} onClick={handleAddMedicine}>
                  + Thêm thuốc
                </button>
              </div>

              {formMedicines.length > 0 ? (
                <div className={styles.medicineList}>
                  {formMedicines.map((medicine, index) => (
                    <div key={index} className={styles.medicineCard}>
                      <div className={styles.sectionHeader}>
                        <h4>{medicine.medicine_name}</h4>
                        <div className={styles.actionRow}>
                          <button
                            type="button"
                            className={styles.outlineButton}
                            onClick={handleEditMedicine}
                          >
                            Sửa
                          </button>
                          <button
                            type="button"
                            className={`${styles.outlineButton} ${styles.danger}`}
                            onClick={() => handleRemoveMedicine(index)}
                          >
                            Xóa
                          </button>
                        </div>
                      </div>
                      <div className={styles.ageGroupGrid}>
                        {medicine.age_groups.map((ag) => (
                          <div key={ag.age_group} className={styles.ageGroupCard}>
                            <div className={styles.ageGroupTitle}>
                              {AGE_RANGES[ag.age_group].label} ({ag.age_range})
                            </div>
                            <div>SL: {ag.quantity}</div>
                            <div>{ag.usage_instructions}</div>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className={styles.warningBox}>Chưa có thuốc nào. Vui lòng thêm ít nhất một loại thuốc.</div>
              )}
            </div>

            <div className={styles.formActions}>
              <button className={styles.primaryButton} type="button" onClick={handleSave}>
                Lưu chẩn đoán
              </button>
              <button className={styles.outlineButton} type="button" onClick={handleCancel}>
                Hủy
              </button>
            </div>
          </div>
        )}

        <div>
          {loading ? (
            <div className={styles.emptyState}>Đang tải dữ liệu...</div>
          ) : (
            getFilteredTemplates().map((template) => {
              const category = categories.find((c) => c.id === template.categoryId);
              const rules = rulesMap[template.id] ?? [];
              const groupedRules = Array.from(
                rules.reduce<Map<number, AdminDiagnosisMedicineRule[]>>((acc, rule) => {
                  const list = acc.get(rule.medicineId) ?? [];
                  list.push(rule);
                  acc.set(rule.medicineId, list);
                  return acc;
                }, new Map()),
              );

              return (
                <div key={template.id} className={styles.templateCard}>
                  <div className={styles.templateHeader}>
                    <div>
                      <div className={styles.templateTitle}>
                        <h3>{template.diagnosisName}</h3>
                        <span className={styles.categoryTag}>{category?.name}</span>
                      </div>
                      <p className={styles.templateAdvice}>
                        <strong>Lời khuyên:</strong> {template.defaultAdvice || 'Chưa có'}
                      </p>
                    </div>
                    <div className={styles.actionRow}>
                      <button className={styles.outlineButton} type="button" onClick={() => handleEdit(template)}>
                        Sửa
                      </button>
                      <button
                        className={`${styles.outlineButton} ${styles.danger}`}
                        type="button"
                        onClick={() => handleDelete(template.id)}
                      >
                        Xóa
                      </button>
                    </div>
                  </div>

                  <div className={styles.medicineSection}>
                    <h4>💊 Thuốc điều trị ({groupedRules.length} loại):</h4>
                    <div className={styles.medicineDetails}>
                      {groupedRules.map(([medicineId, group], index) => (
                        <div key={medicineId} className={styles.medicineDetail}>
                          <div className={styles.medicineName}>
                            {index + 1}. {group[0]?.medicineName}
                          </div>
                          <div className={styles.ageGroupMiniGrid}>
                            {group.map((rule) => {
                              const key = getAgeGroupKey(rule);
                              return (
                                <div key={rule.id} className={styles.ageGroupMiniCard}>
                                  <div className={styles.ageGroupMiniTitle}>{AGE_RANGES[key].label}</div>
                                  <div className={styles.ageGroupMiniText}>
                                    {rule.defaultQuantity} {medicineUnits.get(medicineId)}: {rule.defaultUsage}
                                  </div>
                                </div>
                              );
                            })}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              );
            })
          )}

          {!loading && getFilteredTemplates().length === 0 && (
            <div className={styles.emptyState}>
              {selectedCategory ? 'Không có chẩn đoán nào cho loại bệnh này' : 'Chưa có chẩn đoán nào'}
            </div>
          )}
        </div>
      </div>

      {showMedicineModal && (
        <div className={styles.modalBackdrop} onClick={() => setShowMedicineModal(false)}>
          <div className={styles.modalCard} onClick={(event) => event.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h2>Thêm thuốc vào chẩn đoán</h2>
            </div>
            <div className={styles.catalogPanel}>
              <div className={styles.searchRow}>
                <input
                  className={styles.searchInput}
                  value={medicineSearch}
                  onChange={(event) => setMedicineSearch(event.target.value)}
                  placeholder="Danh dịch"
                />
              </div>
              <div className={styles.categoryTabs}>
                {medicineCategories.map((category) => (
                  <button
                    key={category}
                    type="button"
                    className={`${styles.categoryTab} ${
                      activeMedicineCategory === category ? styles.categoryTabActive : ''
                    }`}
                    onClick={() => setActiveMedicineCategory(category)}
                  >
                    {category}
                  </button>
                ))}
              </div>
              <div className={styles.medicineGrid}>
                {filteredMedicines.map((medicine) => (
                  <button
                    key={medicine.id}
                    type="button"
                    className={`${styles.medicineItem} ${
                      modalMedicines.some((item) => item.medicine_id === medicine.id)
                        ? styles.medicineItemActive
                        : ''
                    }`}
                    onClick={() => handleSelectCatalogMedicine(medicine)}
                  >
                    <span className={styles.medicineName}>{medicine.medicineName}</span>
                    <span className={styles.medicineMeta}>
                      {medicine.unit || 'đv'} - {medicine.sellingPrice.toLocaleString('vi-VN')}đ
                    </span>
                  </button>
                ))}
                {!filteredMedicines.length && (
                  <div className={styles.emptyState}>Không có thuốc phù hợp.</div>
                )}
              </div>
            </div>

            <div className={styles.sectionCard}>
              <h3 className={styles.sectionTitle}>Danh sách thuốc chung</h3>
              {modalMedicines.length === 0 ? (
                <div className={styles.emptyState}>Chọn thuốc từ danh mục để bắt đầu.</div>
              ) : (
                <div className={styles.commonTable}>
                  {modalMedicines.map((medicine) => (
                    <div key={medicine.medicine_id} className={styles.commonRow}>
                      <div className={styles.commonRowTop}>
                        <span className={styles.commonName}>{medicine.medicine_name}</span>
                        <button
                          type="button"
                          className={styles.removeIconButton}
                          onClick={() => handleRemoveModalMedicine(medicine.medicine_id)}
                        >
                          Xóa
                        </button>
                      </div>
                      <div className={styles.commonRowBottom}>
                        <div className={styles.commonField}>
                          <label className={styles.commonLabel}>Số lượng</label>
                          <input
                            type="number"
                            className={styles.commonInput}
                            min="1"
                            value={medicine.age_groups[0]?.quantity ?? 1}
                            onChange={(event) =>
                              updateModalMedicineQuantity(medicine.medicine_id, parseInt(event.target.value) || 1)
                            }
                          />
                        </div>
                        <div className={styles.commonField}>
                          <label className={styles.commonLabel}>Hướng dẫn sử dụng *</label>
                          <input
                            className={styles.commonInput}
                            value={medicine.default_usage_instructions}
                            onChange={(event) =>
                              updateModalMedicineUsage(medicine.medicine_id, event.target.value)
                            }
                            placeholder="VD: Uống 2 viên 1 ngày"
                          />
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className={styles.sectionCard}>
              <h3 className={styles.sectionTitle}>Ghi chú liều lượng cụ thể</h3>
              <div className={styles.ageGroupColumns}>
                {(['child', 'adult', 'elderly'] as AgeGroupKey[]).map((ageKey) => (
                  <div key={ageKey} className={styles.ageColumn}>
                    <div className={styles.ageColumnHeader}>
                      <div className={styles.ageTitle}>{AGE_RANGES[ageKey].label}</div>
                      <div className={styles.ageSubtitle}>({AGE_RANGES[ageKey].range})</div>
                    </div>
                    <div className={styles.ageRows}>
                      {modalMedicines.map((medicine) => {
                        const group = medicine.age_groups.find((item) => item.age_group === ageKey);
                        return (
                          <div key={medicine.medicine_id} className={styles.ageRow}>
                            <span className={styles.ageRowLabel}>{medicine.medicine_name}</span>
                            <input
                              className={styles.commonInput}
                              value={group?.usage_instructions ?? ''}
                              onChange={(event) =>
                                updateModalAgeGroupUsage(
                                  medicine.medicine_id,
                                  ageKey,
                                  event.target.value,
                                )
                              }
                              placeholder="Ghi"
                            />
                          </div>
                        );
                      })}
                      {modalMedicines.length === 0 && (
                        <div className={styles.emptyState}>Chưa có thuốc nào.</div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className={styles.formActions}>
              <button className={styles.primaryButton} type="button" onClick={handleSaveMedicine}>
                Lưu thuốc
              </button>
              <button className={styles.outlineButton} type="button" onClick={() => setShowMedicineModal(false)}>
                Hủy
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
