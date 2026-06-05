"use client";

import { useEffect, useMemo, useState } from "react";
import { Edit, Package, Plus, Search, Trash2 } from "lucide-react";
import { toast } from "sonner";

import { getApiErrorMessage } from "@/services/api";
import { adminService, type AdminMedicine } from "@/services/adminService";
import styles from "../admin.module.css";

export function MedicinesManagement() {
  const medicineTypeOptions = [
    "Giảm đau - Hạ sốt",
    "Kháng sinh",
    "Vitamin",
    "Ho - Đau họng",
    "Dị ứng",
    "Khác",
  ];

  const [meds, setMeds] = useState<AdminMedicine[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<"all" | "active" | "inactive">("all");

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingMed, setEditingMed] = useState<AdminMedicine | null>(null);
  const [formData, setFormData] = useState({
    medicineName: "",
    medicineType: "",
    unit: "",
    sellingPrice: "0",
    stockQuantity: "0",
    isActive: true,
  });

  const loadMedicines = async () => {
    try {
      setLoading(true);
      const data = await adminService.getMedicines();
      setMeds(data);
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể tải danh mục thuốc"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadMedicines();
  }, []);

  const filteredMeds = useMemo(() => {
    return meds.filter((med) => {
      const matchesSearch = med.medicineName.toLowerCase().includes(searchQuery.toLowerCase());
      const matchesStatus =
        statusFilter === "all" || (statusFilter === "active" ? med.isActive : !med.isActive);
      return matchesSearch && matchesStatus;
    });
  }, [meds, searchQuery, statusFilter]);

  const openModal = (medicine?: AdminMedicine) => {
    if (medicine) {
      setEditingMed(medicine);
      setFormData({
        medicineName: medicine.medicineName,
        medicineType: medicine.medicineType ?? "",
        unit: medicine.unit ?? "",
        sellingPrice: String(medicine.sellingPrice ?? 0),
        stockQuantity: String(medicine.stockQuantity ?? 0),
        isActive: medicine.isActive,
      });
    } else {
      setEditingMed(null);
      setFormData({ medicineName: "", medicineType: "", unit: "", sellingPrice: "0", stockQuantity: "0", isActive: true });
    }
    setIsModalOpen(true);
  };

  const handleSave = async () => {
    if (!formData.medicineName.trim()) {
      toast.error("Vui lòng nhập tên thuốc");
      return;
    }

    if (!formData.medicineType.trim()) {
      toast.error("Vui lòng nhập hoặc chọn loại thuốc");
      return;
    }

    const sellingPrice = Number(formData.sellingPrice);
    const stockQuantity = Number(formData.stockQuantity);
    if (Number.isNaN(sellingPrice) || sellingPrice < 0 || Number.isNaN(stockQuantity) || stockQuantity < 0) {
      toast.error("Giá bán và tồn kho phải lớn hơn hoặc bằng 0");
      return;
    }

    try {
      setSubmitting(true);
      if (editingMed) {
        await adminService.updateMedicine(editingMed.id, {
          medicineName: formData.medicineName.trim(),
          medicineType: formData.medicineType.trim(),
          unit: formData.unit.trim() || undefined,
          sellingPrice,
          stockQuantity,
          isActive: formData.isActive,
        });
        toast.success("Đã cập nhật thuốc");
      } else {
        await adminService.createMedicine({
          medicineName: formData.medicineName.trim(),
          medicineType: formData.medicineType.trim(),
          unit: formData.unit.trim() || undefined,
          sellingPrice,
          stockQuantity,
          isActive: formData.isActive,
        });
        toast.success("Đã thêm thuốc");
      }

      setIsModalOpen(false);
      await loadMedicines();
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể lưu thuốc"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Bạn có chắc muốn ngừng sử dụng thuốc này?")) {
      return;
    }

    try {
      await adminService.deactivateMedicine(id);
      toast.success("Đã ngừng sử dụng thuốc");
      await loadMedicines();
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể xóa thuốc"));
    }
  };

  return (
    <main className={styles.mainArea}>
      <div className={styles.container}>
        <div className={styles.header}>
          <h1>Quản lý danh mục thuốc</h1>
          <p>Quản lý kho thuốc và cập nhật giá bán hiện tại.</p>
        </div>

        <div className={styles.statsGrid}>
          <section className={styles.statCard}>
            <div className={`${styles.statIcon} ${styles.statBlue}`}>
              <Package size={20} />
            </div>
            <div>
              <p className={styles.statLabel}>Tổng loại thuốc</p>
              <p className={styles.statValue}>{meds.length}</p>
            </div>
          </section>

          <section className={styles.statCard}>
            <div className={`${styles.statIcon} ${styles.statGreen}`}>
              <Package size={20} />
            </div>
            <div>
              <p className={styles.statLabel}>Đang kinh doanh</p>
              <p className={styles.statValue}>{meds.filter((med) => med.isActive).length}</p>
            </div>
          </section>

          <section className={styles.statCard}>
            <div className={`${styles.statIcon} ${styles.statOrange}`}>
              <Package size={20} />
            </div>
            <div>
              <p className={styles.statLabel}>Sắp hết hàng</p>
              <p className={styles.statValue}>{meds.filter((med) => med.stockQuantity < 200).length}</p>
            </div>
          </section>

          <section className={styles.statCard}>
            <div className={`${styles.statIcon} ${styles.statRed}`}>
              <Package size={20} />
            </div>
            <div>
              <p className={styles.statLabel}>Tạm ngừng</p>
              <p className={styles.statValue}>{meds.filter((med) => !med.isActive).length}</p>
            </div>
          </section>
        </div>

        <div className={styles.toolbar}>
          <div className={styles.searchWrap}>
            <Search className={styles.searchIcon} size={18} />
            <input
              className={styles.searchInput}
              placeholder="Tìm theo tên thuốc..."
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
            />
          </div>

          <div className={styles.selectWrap}>
            <select
              className={styles.selectInput}
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value as "all" | "active" | "inactive")}
            >
              <option value="all">Tất cả</option>
              <option value="active">Đang kinh doanh</option>
              <option value="inactive">Tạm ngừng</option>
            </select>
          </div>

          <button type="button" className={styles.primaryButton} onClick={() => openModal()}>
            <Plus size={16} /> Thêm thuốc
          </button>
        </div>

        <section className={styles.card}>
          {loading ? (
            <div className={styles.emptyBox}>Đang tải dữ liệu...</div>
          ) : (
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>Tên thuốc</th>
                    <th>Loại thuốc</th>
                    <th>Đơn vị</th>
                    <th>Giá bán</th>
                    <th>Tồn kho</th>
                    <th>Trạng thái</th>
                    <th style={{ textAlign: "right" }}>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredMeds.map((med) => (
                    <tr key={med.id}>
                      <td>{med.medicineName}</td>
                      <td>{med.medicineType || "Khác"}</td>
                      <td>{med.unit || "-"}</td>
                      <td>{Number(med.sellingPrice || 0).toLocaleString("vi-VN")}đ</td>
                      <td>
                        <span
                          className={`${styles.badge} ${
                            med.stockQuantity < 200 ? styles.badgeOrange : styles.badgeGreen
                          }`}
                        >
                          {med.stockQuantity} {med.unit || "đv"}
                        </span>
                      </td>
                      <td>
                        <span className={`${styles.badge} ${med.isActive ? styles.badgeBlue : styles.badgeGray}`}>
                          {med.isActive ? "Đang kinh doanh" : "Tạm ngừng"}
                        </span>
                      </td>
                      <td>
                        <div className={styles.tableActions}>
                          <button type="button" className={styles.iconButton} onClick={() => openModal(med)}>
                            <Edit size={16} />
                          </button>
                          <button type="button" className={styles.iconButton} onClick={() => void handleDelete(med.id)}>
                            <Trash2 size={16} color="#dc2626" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {filteredMeds.length === 0 && <div className={styles.emptyBox}>Không tìm thấy thuốc nào.</div>}
            </div>
          )}
        </section>
      </div>

      {isModalOpen && (
        <>
          <div className={styles.modalOverlay} onClick={() => setIsModalOpen(false)} />
          <div className={styles.modal}>
            <h2 className={styles.modalTitle}>{editingMed ? "Chỉnh sửa thuốc" : "Thêm thuốc mới"}</h2>
            <div className={styles.formGrid}>
              <div>
                <label className={styles.fieldLabel}>Tên thuốc *</label>
                <input
                  className={styles.textInput}
                  placeholder="VD: Paracetamol 500mg"
                  value={formData.medicineName}
                  onChange={(event) => setFormData((prev) => ({ ...prev, medicineName: event.target.value }))}
                />
              </div>
              <div>
                <label className={styles.fieldLabel}>Loại thuốc *</label>
                <input
                  className={styles.textInput}
                  list="medicine-type-options"
                  placeholder="Chọn hoặc nhập loại thuốc"
                  value={formData.medicineType}
                  onChange={(event) => setFormData((prev) => ({ ...prev, medicineType: event.target.value }))}
                />
                <datalist id="medicine-type-options">
                  {medicineTypeOptions.map((option) => (
                    <option key={option} value={option} />
                  ))}
                </datalist>
              </div>
              <div>
                <label className={styles.fieldLabel}>Đơn vị *</label>
                <select
                  className={styles.selectInput}
                  value={formData.unit}
                  onChange={(event) => setFormData((prev) => ({ ...prev, unit: event.target.value }))}
                >
                  <option value="">Chọn đơn vị</option>
                  <option value="viên">Viên</option>
                  <option value="gói">Gói</option>
                  <option value="chai">Chai</option>
                  <option value="ống">Ống</option>
                  <option value="hộp">Hộp</option>
                </select>
              </div>
              <div>
                <label className={styles.fieldLabel}>Đơn giá (VND) *</label>
                <input
                  type="number"
                  min={0}
                  className={styles.textInput}
                  placeholder="5000"
                  value={formData.sellingPrice}
                  onChange={(event) => setFormData((prev) => ({ ...prev, sellingPrice: event.target.value }))}
                />
              </div>
              <div>
                <label className={styles.fieldLabel}>Số lượng tồn kho *</label>
                <input
                  type="number"
                  min={0}
                  className={styles.textInput}
                  placeholder="100"
                  value={formData.stockQuantity}
                  onChange={(event) => setFormData((prev) => ({ ...prev, stockQuantity: event.target.value }))}
                />
              </div>
            </div>
            <div className={styles.modalActions}>
              <button type="button" className={styles.primaryButton} onClick={() => void handleSave()} disabled={submitting}>
                {editingMed ? "Cập nhật" : "Thêm mới"}
              </button>
              <button type="button" className={styles.outlineButton} onClick={() => setIsModalOpen(false)} disabled={submitting}>
                Hủy
              </button>
            </div>
          </div>
        </>
      )}
    </main>
  );
}
