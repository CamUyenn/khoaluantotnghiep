"use client";

import { useEffect, useMemo, useState } from "react";
import { Activity, Edit, Plus, Search, Trash2 } from "lucide-react";
import { toast } from "sonner";

import { getApiErrorMessage } from "@/services/api";
import { adminService, type AdminMedicalService } from "@/services/adminService";
import styles from "../admin.module.css";

export function ServicesManagement() {
  const [services, setServices] = useState<AdminMedicalService[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingService, setEditingService] = useState<AdminMedicalService | null>(null);
  const [formData, setFormData] = useState({ serviceName: "", currentPrice: "0" });

  const loadServices = async () => {
    try {
      setLoading(true);
      const data = await adminService.getMedicalServices();
      setServices(data);
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể tải danh mục dịch vụ"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadServices();
  }, []);

  const filteredServices = useMemo(() => {
    return services.filter((service) => service.serviceName.toLowerCase().includes(searchQuery.toLowerCase()));
  }, [services, searchQuery]);

  const openModal = (service?: AdminMedicalService) => {
    if (service) {
      setEditingService(service);
      setFormData({
        serviceName: service.serviceName,
        currentPrice: String(service.currentPrice),
      });
    } else {
      setEditingService(null);
      setFormData({ serviceName: "", currentPrice: "0" });
    }
    setIsModalOpen(true);
  };

  const handleSave = async () => {
    const currentPrice = Number(formData.currentPrice);
    if (!formData.serviceName.trim()) {
      toast.error("Vui lòng nhập tên dịch vụ");
      return;
    }
    if (Number.isNaN(currentPrice) || currentPrice < 0) {
      toast.error("Giá dịch vụ phải lớn hơn hoặc bằng 0");
      return;
    }

    try {
      setSubmitting(true);
      if (editingService) {
        if (formData.serviceName.trim() !== editingService.serviceName) {
          toast.info("Hiện tại hệ thống chỉ hỗ trợ cập nhật giá dịch vụ");
        }
        await adminService.updateMedicalServicePrice(editingService.id, currentPrice);
        toast.success("Đã cập nhật dịch vụ");
      } else {
        await adminService.createMedicalService({
          serviceName: formData.serviceName.trim(),
          currentPrice,
        });
        toast.success("Đã thêm dịch vụ");
      }

      setIsModalOpen(false);
      await loadServices();
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể lưu dịch vụ"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeactivate = async (serviceId: number) => {
    if (!confirm("Bạn có chắc muốn ngừng sử dụng dịch vụ này?")) {
      return;
    }

    try {
      await adminService.deactivateMedicalService(serviceId);
      toast.success("Đã ngừng sử dụng dịch vụ");
      await loadServices();
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể xóa dịch vụ"));
    }
  };

  return (
    <main className={styles.mainArea}>
      <div className={styles.container}>
        <div className={styles.header}>
          <div>
            <h1>Quản lý dịch vụ</h1>
            <p>Danh mục dịch vụ cận lâm sàng và giá hiện hành.</p>
          </div>
        </div>

        <div className={styles.toolbar}>
          <div className={styles.searchWrap}>
            <Search className={styles.searchIcon} size={18} />
            <input
              className={styles.searchInput}
              value={searchQuery}
              placeholder="Tìm theo tên dịch vụ..."
              onChange={(event) => setSearchQuery(event.target.value)}
            />
          </div>
          <button type="button" className={styles.primaryButton} onClick={() => openModal()}>
            <Plus size={16} /> Thêm dịch vụ
          </button>
        </div>

        {loading ? (
          <section className={styles.card}>
            <div className={styles.emptyBox}>Đang tải dữ liệu...</div>
          </section>
        ) : (
          <div className={styles.cardsGrid}>
            {filteredServices.map((service) => (
              <article className={styles.itemCard} key={service.id}>
                <div className={styles.itemHeader}>
                  <div className={`${styles.statIcon} ${styles.statBlue}`}>
                    <Activity size={20} />
                  </div>
                  <span className={`${styles.badge} ${service.isActive ? styles.badgeGreen : styles.badgeGray}`}>
                    {service.isActive ? "Đang sử dụng" : "Tạm dừng"}
                  </span>
                </div>
                <h3 className={styles.itemTitle}>{service.serviceName}</h3>
                <p className={styles.valueText}>{Number(service.currentPrice).toLocaleString("vi-VN")}đ</p>
                <div className={styles.modalActions}>
                  <button type="button" className={styles.outlineButton} onClick={() => openModal(service)}>
                    <Edit size={16} /> Chỉnh sửa
                  </button>
                  <button type="button" className={styles.outlineButton} onClick={() => void handleDeactivate(service.id)}>
                    <Trash2 size={16} /> Ngừng dùng
                  </button>
                </div>
              </article>
            ))}
            {filteredServices.length === 0 && (
              <section className={styles.card}>
                <div className={styles.emptyBox}>Không tìm thấy dịch vụ phù hợp.</div>
              </section>
            )}
          </div>
        )}
      </div>

      {isModalOpen && (
        <>
          <div className={styles.modalOverlay} onClick={() => setIsModalOpen(false)} />
          <div className={styles.modal}>
            <h2 className={styles.modalTitle}>{editingService ? "Chỉnh sửa dịch vụ" : "Thêm dịch vụ mới"}</h2>
            <div className={styles.formGrid}>
              <div>
                <label className={styles.fieldLabel}>Tên dịch vụ *</label>
                <input
                  className={styles.textInput}
                  placeholder="VD: Khám tổng quát"
                  value={formData.serviceName}
                  onChange={(event) => setFormData((prev) => ({ ...prev, serviceName: event.target.value }))}
                />
              </div>
              <div>
                <label className={styles.fieldLabel}>Giá dịch vụ (VND) *</label>
                <input
                  className={styles.textInput}
                  type="number"
                  min={0}
                  placeholder="150000"
                  value={formData.currentPrice}
                  onChange={(event) => setFormData((prev) => ({ ...prev, currentPrice: event.target.value }))}
                />
              </div>
            </div>
            <div className={styles.modalActions}>
              <button type="button" className={styles.primaryButton} onClick={() => void handleSave()} disabled={submitting}>
                {editingService ? "Cập nhật" : "Thêm mới"}
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
