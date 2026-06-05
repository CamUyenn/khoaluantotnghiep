'use client';

import { useEffect, useState } from 'react';
import { Edit, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { getApiErrorMessage } from '@/services/api';
import {
  adminService,
  type AdminMedicalCategory,
  type AdminDiagnosisTemplate,
} from '@/services/adminService';
import styles from '../disease-category-management.module.css';

export function DiseaseCategoryManagement() {
  const [categories, setCategories] = useState<AdminMedicalCategory[]>([]);
  const [diagnoses, setDiagnoses] = useState<AdminDiagnosisTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [isAdding, setIsAdding] = useState(false);
  const [editing, setEditing] = useState<AdminMedicalCategory | null>(null);
  const [form, setForm] = useState({
    id: 0,
    name: '',
    description: '',
  });

  const loadData = async () => {
    try {
      setLoading(true);
      const categoriesData = await adminService.getMedicalCategories();
      const diagnosisLists = await Promise.all(
        categoriesData.map((category) => adminService.getDiagnoses(category.id)),
      );
      setCategories(categoriesData);
      setDiagnoses(diagnosisLists.flat());
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Không thể tải danh mục bệnh'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, []);

  const handleAdd = () => {
    setIsAdding(true);
    setForm({ id: 0, name: '', description: '' });
  };

  const handleEdit = (category: AdminMedicalCategory) => {
    setEditing(category);
    setForm({ id: category.id, name: category.name, description: category.description ?? '' });
  };

  const handleSave = async () => {
    if (!form.name.trim()) {
      toast.error('Vui lòng điền đầy đủ thông tin');
      return;
    }

    try {
      if (editing) {
        const updated = await adminService.updateMedicalCategory(editing.id, {
          name: form.name.trim(),
          description: form.description.trim() || null,
        });
        setCategories(categories.map((c) => (c.id === editing.id ? updated : c)));
        setEditing(null);
      } else {
        const created = await adminService.createMedicalCategory({
          name: form.name.trim(),
          description: form.description.trim() || null,
        });
        setCategories([...categories, created]);
        setIsAdding(false);
      }

      setForm({ id: 0, name: '', description: '' });
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Không thể lưu loại bệnh'));
    }
  };

  const handleDelete = async (id: number) => {
    const diagnosisCount = diagnoses.filter((d) => d.categoryId === id).length;
    if (diagnosisCount > 0) {
      toast.error(`Không thể xóa! Loại bệnh này có ${diagnosisCount} chẩn đoán liên kết.`);
      return;
    }

    if (!confirm('Bạn có chắc muốn xóa loại bệnh này?')) {
      return;
    }

    try {
      await adminService.deleteMedicalCategory(id);
      setCategories(categories.filter((c) => c.id !== id));
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Không thể xóa loại bệnh'));
    }
  };

  const handleCancel = () => {
    setIsAdding(false);
    setEditing(null);
    setForm({ id: 0, name: '', description: '' });
  };

  const getDiagnosisCount = (categoryId: string) => {
    return diagnoses.filter((d) => d.categoryId === Number(categoryId)).length;
  };

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1>Quản lý Loại bệnh</h1>
        <p>Tạo và quản lý các loại bệnh cho chẩn đoán của bác sĩ</p>
      </div>

      <div className={styles.card}>
        <div className={styles.sectionHeader}>
          <h2>Danh sách loại bệnh ({categories.length})</h2>
          <button className={styles.primaryButton} type="button" onClick={handleAdd}>
            + Thêm loại bệnh
          </button>
        </div>

        {(isAdding || editing) && (
          <div className={styles.formCard}>
            <h3>{editing ? 'Chỉnh sửa' : 'Thêm mới'} loại bệnh</h3>
            <div className={styles.grid2}>
              <div className={styles.formGroup}>
                <label className={styles.label}>Tên loại bệnh *</label>
                <input
                  className={styles.input}
                  value={form.name}
                  onChange={(event) => setForm({ ...form, name: event.target.value })}
                  placeholder="VD: Tim mạch, Gan mật, Dạ dày"
                />
              </div>
              <div className={styles.formGroup}>
                <label className={styles.label}>Mô tả</label>
                <input
                  className={styles.input}
                  value={form.description}
                  onChange={(event) => setForm({ ...form, description: event.target.value })}
                  placeholder="VD: Tim mạch, tiêu hóa"
                />
              </div>
            </div>
            <div className={styles.formActions}>
              <button className={styles.primaryButton} type="button" onClick={handleSave}>
                Lưu
              </button>
              <button className={styles.outlineButton} type="button" onClick={handleCancel}>
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
                <h3>{category.name}</h3>
                <div className={styles.countBadge}>
                  {getDiagnosisCount(String(category.id))} chẩn đoán
                </div>
                <div className={styles.actionRow}>
                  <button
                    className={`${styles.pillButton} ${styles.pillPrimary}`}
                    type="button"
                    onClick={() => handleEdit(category)}
                  >
                    <Edit size={14} />
                    Sửa
                  </button>
                  <button
                    className={`${styles.pillButton} ${styles.pillDanger}`}
                    type="button"
                    onClick={() => handleDelete(category.id)}
                  >
                    <Trash2 size={14} />
                    Xóa
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        {!loading && categories.length === 0 && <div className={styles.emptyState}>Chưa có loại bệnh nào</div>}
      </div>

      <div className={styles.infoCard}>
        <h3>💡 Hướng dẫn sử dụng</h3>
        <ul>
          <li>Loại bệnh được sử dụng để phân loại các chẩn đoán trong popup khám của bác sĩ</li>
          <li>Mỗi loại bệnh có thể chứa nhiều chẩn đoán khác nhau</li>
          <li>Hạn chế xóa loại bệnh nếu đã có chẩn đoán liên kết</li>
        </ul>
      </div>
    </div>
  );
}
