'use client';

import { useEffect, useMemo, useState } from 'react';
import { Edit, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { getApiErrorMessage } from '@/services/api';
import {
  adminService,
  type AdminMedicalCategory,
  type AdminMedicalService,
  type AdminSymptomServiceMapping,
  type AdminSymptomTemplate,
} from '@/services/adminService';
import styles from '../symptom-management.module.css';

export function SymptomManagement() {
  const [categories, setCategories] = useState<AdminMedicalCategory[]>([]);
  const [suggestions, setSuggestions] = useState<AdminSymptomTemplate[]>([]);
  const [services, setServices] = useState<AdminMedicalService[]>([]);
  const [mappings, setMappings] = useState<Record<number, AdminSymptomServiceMapping[]>>({});
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'categories' | 'suggestions'>('categories');

  const [isAddingCategory, setIsAddingCategory] = useState(false);
  const [editingCategory, setEditingCategory] = useState<AdminMedicalCategory | null>(null);
  const [categoryForm, setCategoryForm] = useState({
    id: 0,
    name: '',
    description: '',
  });

  const [isAddingSuggestion, setIsAddingSuggestion] = useState(false);
  const [editingSuggestion, setEditingSuggestion] = useState<AdminSymptomTemplate | null>(null);
  const [suggestionForm, setSuggestionForm] = useState({
    id: 0,
    symptom_category_id: 0,
    symptom_name: '',
    recommended_services: [] as number[],
  });

  const loadData = async () => {
    try {
      setLoading(true);
      const [categoriesData, servicesData] = await Promise.all([
        adminService.getMedicalCategories(),
        adminService.getMedicalServices(),
      ]);

      const symptomLists = await Promise.all(
        categoriesData.map((category) => adminService.getSymptoms(category.id)),
      );

      const symptoms = symptomLists.flat();

      const mappingEntries = await Promise.all(
        symptoms.map(async (symptom) => {
          const data = await adminService.getSymptomServiceMappings(symptom.id);
          return [symptom.id, data] as const;
        }),
      );

      const mappingMap: Record<number, AdminSymptomServiceMapping[]> = {};
      mappingEntries.forEach(([symptomId, data]) => {
        mappingMap[symptomId] = data;
      });

      setCategories(categoriesData);
      setServices(servicesData);
      setSuggestions(symptoms);
      setMappings(mappingMap);
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Không thể tải dữ liệu triệu chứng'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, []);

  const handleAddCategory = () => {
    setIsAddingCategory(true);
    setCategoryForm({ id: 0, name: '', description: '' });
  };

  const handleEditCategory = (category: AdminMedicalCategory) => {
    setEditingCategory(category);
    setCategoryForm({ id: category.id, name: category.name, description: category.description ?? '' });
  };

  const handleSaveCategory = async () => {
    if (!categoryForm.name.trim()) {
      toast.error('Vui lòng điền đầy đủ thông tin');
      return;
    }

    try {
      if (editingCategory) {
        const updated = await adminService.updateMedicalCategory(editingCategory.id, {
          name: categoryForm.name.trim(),
          description: categoryForm.description.trim() || null,
        });
        setCategories(categories.map((c) => (c.id === editingCategory.id ? updated : c)));
        setEditingCategory(null);
      } else {
        const created = await adminService.createMedicalCategory({
          name: categoryForm.name.trim(),
          description: categoryForm.description.trim() || null,
        });
        setCategories([...categories, created]);
        setIsAddingCategory(false);
      }
      setCategoryForm({ id: 0, name: '', description: '' });
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Không thể lưu loại bệnh'));
    }
  };

  const handleDeleteCategory = async (id: number) => {
    if (!confirm('Bạn có chắc muốn xóa loại bệnh này?')) {
      return;
    }
    try {
      await adminService.deleteMedicalCategory(id);
      setCategories(categories.filter((c) => c.id !== id));
      setSuggestions(suggestions.filter((s) => s.categoryId !== id));
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Không thể xóa loại bệnh'));
    }
  };

  const handleCancelCategory = () => {
    setIsAddingCategory(false);
    setEditingCategory(null);
    setCategoryForm({ id: 0, name: '', description: '' });
  };

  const handleAddSuggestion = () => {
    setIsAddingSuggestion(true);
    setSuggestionForm({
      id: 0,
      symptom_category_id: 0,
      symptom_name: '',
      recommended_services: [],
    });
  };

  const handleEditSuggestion = (suggestion: AdminSymptomTemplate) => {
    setEditingSuggestion(suggestion);
    const currentMappings = mappings[suggestion.id] ?? [];
    setSuggestionForm({
      id: suggestion.id,
      symptom_category_id: suggestion.categoryId,
      symptom_name: suggestion.symptomName,
      recommended_services: currentMappings.map((mapping) => mapping.serviceId),
    });
  };

  const handleSaveSuggestion = async () => {
    if (!suggestionForm.symptom_name.trim() || !suggestionForm.symptom_category_id) {
      toast.error('Vui lòng điền đầy đủ thông tin');
      return;
    }

    try {
      if (editingSuggestion) {
        const updated = await adminService.updateSymptom(editingSuggestion.id, {
          categoryId: suggestionForm.symptom_category_id,
          symptomName: suggestionForm.symptom_name.trim(),
        });

        const existing = mappings[editingSuggestion.id] ?? [];
        const existingIds = new Set(existing.map((m) => m.serviceId));
        const nextIds = new Set(suggestionForm.recommended_services);

        await Promise.all(
          [...existingIds]
            .filter((id) => !nextIds.has(id))
            .map((serviceId) => adminService.deleteSymptomServiceMapping(editingSuggestion.id, serviceId)),
        );

        await Promise.all(
          suggestionForm.recommended_services
            .filter((serviceId) => !existingIds.has(serviceId))
            .map((serviceId) =>
              adminService.createSymptomServiceMapping({ symptomId: editingSuggestion.id, serviceId }),
            ),
        );

        const updatedMappings = await adminService.getSymptomServiceMappings(editingSuggestion.id);
        setMappings({ ...mappings, [editingSuggestion.id]: updatedMappings });

        setSuggestions(suggestions.map((s) => (s.id === editingSuggestion.id ? updated : s)));
        setEditingSuggestion(null);
      } else {
        const created = await adminService.createSymptom({
          categoryId: suggestionForm.symptom_category_id,
          symptomName: suggestionForm.symptom_name.trim(),
        });

        await Promise.all(
          suggestionForm.recommended_services.map((serviceId) =>
            adminService.createSymptomServiceMapping({ symptomId: created.id, serviceId }),
          ),
        );

        const createdMappings = await adminService.getSymptomServiceMappings(created.id);
        setMappings({ ...mappings, [created.id]: createdMappings });
        setSuggestions([...suggestions, created]);
        setIsAddingSuggestion(false);
      }

      setSuggestionForm({
        id: 0,
        symptom_category_id: 0,
        symptom_name: '',
        recommended_services: [],
      });
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Không thể lưu triệu chứng'));
    }
  };

  const handleDeleteSuggestion = async (id: number) => {
    if (!confirm('Bạn có chắc muốn xóa triệu chứng này?')) {
      return;
    }
    try {
      await adminService.deleteSymptom(id);
      setSuggestions(suggestions.filter((s) => s.id !== id));
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Không thể xóa triệu chứng'));
    }
  };

  const handleCancelSuggestion = () => {
    setIsAddingSuggestion(false);
    setEditingSuggestion(null);
    setSuggestionForm({
      id: 0,
      symptom_category_id: 0,
      symptom_name: '',
      recommended_services: [],
    });
  };

  const toggleService = (serviceId: number) => {
    if (suggestionForm.recommended_services.includes(serviceId)) {
      setSuggestionForm({
        ...suggestionForm,
        recommended_services: suggestionForm.recommended_services.filter((id) => id !== serviceId),
      });
    } else {
      setSuggestionForm({
        ...suggestionForm,
        recommended_services: [...suggestionForm.recommended_services, serviceId],
      });
    }
  };

  const servicesById = useMemo(() => {
    return new Map(services.map((service) => [service.id, service]));
  }, [services]);

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1>Quản lý Triệu chứng</h1>
        <p>Tạo và quản lý loại triệu chứng và gợi ý triệu chứng cho bệnh nhân</p>
      </div>

      <div className={styles.tabs}>
        <button
          type="button"
          className={`${styles.tabButton} ${activeTab === 'categories' ? styles.tabActive : ''}`}
          onClick={() => setActiveTab('categories')}
        >
          Loại triệu chứng ({categories.length})
        </button>
        <button
          type="button"
          className={`${styles.tabButton} ${activeTab === 'suggestions' ? styles.tabActive : ''}`}
          onClick={() => setActiveTab('suggestions')}
        >
          Triệu chứng ({suggestions.length})
        </button>
      </div>

      {activeTab === 'categories' && (
        <div className={styles.card}>
          <div className={styles.sectionHeader}>
            <h2>Danh sách loại triệu chứng</h2>
            <button className={styles.primaryButton} type="button" onClick={handleAddCategory}>
              + Thêm loại triệu chứng
            </button>
          </div>

          {(isAddingCategory || editingCategory) && (
            <div className={styles.formCard}>
              <h3>{editingCategory ? 'Chỉnh sửa' : 'Thêm mới'} loại triệu chứng</h3>
              <div className={styles.grid2}>
                <div className={styles.formGroup}>
                  <label className={styles.label}>Tên loại *</label>
                  <input
                    className={styles.input}
                    value={categoryForm.name}
                    onChange={(event) => setCategoryForm({ ...categoryForm, name: event.target.value })}
                    placeholder="VD: Tim mạch"
                  />
                </div>
                <div className={styles.formGroup}>
                  <label className={styles.label}>Mô tả</label>
                  <input
                    className={styles.input}
                    value={categoryForm.description}
                    onChange={(event) => setCategoryForm({ ...categoryForm, description: event.target.value })}
                    placeholder="VD: Tim, mạch máu"
                  />
                </div>
              </div>
              <div className={styles.formActions}>
                <button className={styles.primaryButton} type="button" onClick={handleSaveCategory}>
                  Lưu
                </button>
                <button className={styles.outlineButton} type="button" onClick={handleCancelCategory}>
                  Hủy
                </button>
              </div>
            </div>
          )}

          {loading ? (
            <div className={styles.emptyState}>Đang tải dữ liệu...</div>
          ) : (
            <div className={styles.categoryGrid}>
              {categories.map((category) => (
                <div key={category.id} className={styles.categoryCard}>
                  <div className={styles.categoryHeader}>
                    <h3>{category.name}</h3>
                    <p>{category.description || 'Chưa có mô tả'}</p>
                  </div>
                  <div className={styles.categoryBadge}>
                    {suggestions.filter((s) => s.categoryId === category.id).length} triệu chứng
                  </div>
                  <div className={styles.actionRow}>
                    <button
                      className={`${styles.pillButton} ${styles.pillPrimary}`}
                      type="button"
                      onClick={() => handleEditCategory(category)}
                    >
                      <Edit size={14} />
                      Sửa
                    </button>
                    <button
                      className={`${styles.pillButton} ${styles.pillDanger}`}
                      type="button"
                      onClick={() => handleDeleteCategory(category.id)}
                    >
                      <Trash2 size={14} />
                      Xóa
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {activeTab === 'suggestions' && (
        <div className={styles.card}>
          <div className={styles.sectionHeader}>
            <h2>Danh sách triệu chứng gợi ý</h2>
            <button className={styles.primaryButton} type="button" onClick={handleAddSuggestion}>
              + Thêm triệu chứng
            </button>
          </div>

          {(isAddingSuggestion || editingSuggestion) && (
            <div className={styles.formCard}>
              <h3>{editingSuggestion ? 'Chỉnh sửa' : 'Thêm mới'} triệu chứng gợi ý</h3>

              <div className={styles.grid2}>
                <div className={styles.formGroup}>
                  <label className={styles.label}>Loại triệu chứng *</label>
                  <select
                    className={styles.input}
                    value={suggestionForm.symptom_category_id}
                    onChange={(event) =>
                      setSuggestionForm({ ...suggestionForm, symptom_category_id: Number(event.target.value) })
                    }
                  >
                    <option value={0}>-- Chọn loại --</option>
                    {categories.map((cat) => (
                      <option key={cat.id} value={cat.id}>
                        {cat.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className={styles.formGroup}>
                <label className={styles.label}>Tên triệu chứng *</label>
                <input
                  className={styles.input}
                  value={suggestionForm.symptom_name}
                  onChange={(event) =>
                    setSuggestionForm({ ...suggestionForm, symptom_name: event.target.value })
                  }
                  placeholder="VD: Đau đầu dữ dội"
                />
              </div>

              <div className={styles.formGroup}>
                <label className={styles.label}>Dịch vụ gợi ý</label>
                <div className={styles.serviceGrid}>
                  {services.map((service) => {
                    const isSelected = suggestionForm.recommended_services.includes(service.id);
                    return (
                      <label
                        key={service.id}
                        className={`${styles.serviceTile} ${isSelected ? styles.serviceTileSelected : ''}`}
                      >
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => toggleService(service.id)}
                        />
                        <div>
                          <div className={styles.serviceName}>{service.serviceName}</div>
                          <div className={styles.servicePrice}>{service.currentPrice.toLocaleString('vi-VN')}đ</div>
                        </div>
                      </label>
                    );
                  })}
                </div>
                <p className={styles.estimateText}>
                  Chi phí ước tính sẽ được tính tự động:{' '}
                  <strong>
                    {suggestionForm.recommended_services
                      .reduce((sum, id) => {
                        const service = servicesById.get(id);
                        return sum + (service?.currentPrice || 0);
                      }, 0)
                      .toLocaleString('vi-VN')}
                    đ
                  </strong>
                </p>
              </div>

              <div className={styles.formActions}>
                <button className={styles.primaryButton} type="button" onClick={handleSaveSuggestion}>
                  Lưu
                </button>
                <button className={styles.outlineButton} type="button" onClick={handleCancelSuggestion}>
                  Hủy
                </button>
              </div>
            </div>
          )}

          <div>
            {suggestions.map((suggestion) => {
              const category = categories.find((c) => c.id === suggestion.categoryId);
              const currentMappings = mappings[suggestion.id] ?? [];
              const totalCost = currentMappings.reduce((sum, mapping) => {
                const service = servicesById.get(mapping.serviceId);
                return sum + (service?.currentPrice || 0);
              }, 0);
              return (
                <div key={suggestion.id} className={styles.suggestionCard}>
                  <div className={styles.suggestionMain}>
                    <div>
                      <div className={styles.suggestionTitle}>
                        <h3>{suggestion.symptomName}</h3>
                      </div>
                      <p className={styles.categoryMeta}>{category?.name}</p>
                      <div className={styles.serviceMeta}>
                        <strong>Dịch vụ:</strong>{' '}
                        {currentMappings.map((mapping) => mapping.serviceName).filter(Boolean).join(', ') || 'Chưa có'}
                      </div>
                    </div>
                    <div className={styles.suggestionActions}>
                      <div className={styles.costText}>{totalCost.toLocaleString('vi-VN')}đ</div>
                      <div className={styles.actionRow}>
                        <button
                          className={styles.outlineButton}
                          type="button"
                          onClick={() => handleEditSuggestion(suggestion)}
                        >
                          Sửa
                        </button>
                        <button
                          className={`${styles.outlineButton} ${styles.danger}`}
                          type="button"
                          onClick={() => handleDeleteSuggestion(suggestion.id)}
                        >
                          Xóa
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}

            {suggestions.length === 0 && (
              <div className={styles.emptyState}>Chưa có triệu chứng nào</div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
