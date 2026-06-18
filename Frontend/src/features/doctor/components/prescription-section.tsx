import { useEffect, useMemo, useState } from "react";
import type { Medicine, PrescriptionItem } from "@/types/doctor.type";
import styles from "@/styles/common.module.css";
interface PrescriptionSectionProps {
  prescriptions: PrescriptionItem[];
  availableMedicines: Medicine[];
  catalogMessage?: string | null;
  onAddMedicine: (medicine: Medicine) => void;
  onUpdatePrescription: (
    medicineId: number,
    field: "quantity" | "usage_instructions",
    value: number | string,
  ) => void;
  onRemoveMedicine: (medicineId: number) => void;
}

export function PrescriptionSection({
  prescriptions,
  availableMedicines,
  catalogMessage,
  onAddMedicine,
  onUpdatePrescription,
  onRemoveMedicine,
}: PrescriptionSectionProps) {
  const categories = useMemo(
    () => Array.from(new Set(availableMedicines.map((medicine) => medicine.category))),
    [availableMedicines],
  );
  const [activeCategory, setActiveCategory] = useState<string>(categories[0] ?? "");

  useEffect(() => {
    if (!categories.includes(activeCategory)) {
      setActiveCategory(categories[0] ?? "");
    }
  }, [activeCategory, categories]);

  return (
    <div>
      <label className={styles.label}>Đơn thuốc</label>

      {/* Thuốc đã kê */}
      {prescriptions.length > 0 && (
        <div className={styles.panelPurple}>
          <div className={`${styles.flexBetween} ${styles.mb2}`}>
            <h4 className={styles.titleSmall}>Thuốc đã kê</h4>
          </div>

          <div className={styles.rowStack}>
            {prescriptions.map((item) => (
              <div key={item.medicine_id} className={styles.itemCard}>
                <div className={`${styles.flexBetween} ${styles.mb1}`}>
                  <span className={styles.titleSmall}>
                    {item.medicine?.medicine_name} {item.medicine?.dosage}
                  </span>
                </div>

                <div className={`${styles.grid2} ${styles.mb1}`}>
                  <div>
                    <label htmlFor={`presc-${item.medicine_id}-quantity`} className={`${styles.textSmall} ${styles.textMuted}`}>Số lượng</label>
                    <input
                      id={`presc-${item.medicine_id}-quantity`}
                      name={`quantity_${item.medicine_id}`}
                      type="number"
                      className={`${styles.input} ${styles.compactInput}`}
                      min="1"
                      value={item.quantity}
                      onChange={(e) => onUpdatePrescription(item.medicine_id, "quantity", parseInt(e.target.value) || 1)}
                    />
                  </div>
                  <div>
                    <label htmlFor={`presc-${item.medicine_id}-unit`} className={`${styles.textSmall} ${styles.textMuted}`}>Đơn vị</label>
                    <input id={`presc-${item.medicine_id}-unit`} name={`unit_${item.medicine_id}`} className={`${styles.input} ${styles.compactInput}`} value={item.medicine?.unit} disabled />
                  </div>
                </div>

                <div>
                  <label htmlFor={`presc-${item.medicine_id}-usage`} className={`${styles.textSmall} ${styles.textMuted}`}>Hướng dẫn sử dụng</label>
                  <textarea
                    id={`presc-${item.medicine_id}-usage`}
                    name={`usage_${item.medicine_id}`}
                    className={`${styles.input} ${styles.mt1}`}
                    rows={3}
                    value={item.usage_instructions || ""}
                    onChange={(e) => onUpdatePrescription(item.medicine_id, "usage_instructions", e.target.value)}
                  />
                </div>

                <button
                  className={`${styles.button} ${styles.outline} ${styles.fullButton} ${styles.textDanger}`}
                  onClick={() => onRemoveMedicine(item.medicine_id)}
                >
                  Xóa
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Kho thuốc */}
      <div className={styles.panelBorder}>
        <h4 className={styles.mb2}>Danh mục thuốc</h4>
        <div className={styles.categoryTabs}>
          {categories.map((category) => (
            <button
              key={category}
              type="button"
              className={`${styles.categoryTab} ${activeCategory === category ? styles.categoryTabActive : ""}`}
              onClick={() => setActiveCategory(category)}
            >
              {category}
            </button>
          ))}
        </div>

        <div className={styles.medicineGrid}>
          {availableMedicines
            .filter((medicine) => (activeCategory ? medicine.category === activeCategory : true))
            .map((medicine) => (
            <div
              key={medicine.id}
              className={styles.medicineItem}
              onClick={() => onAddMedicine(medicine)}
            >
              <p className={`${styles.textMedium} ${styles.textSmall}`}>{medicine.medicine_name}</p>
              <p className={`${styles.textSmall} ${styles.textMuted}`}>{medicine.dosage}</p>
              <p className={`${styles.textSmall} ${styles.textMuted}`}>Tồn: {medicine.stock_quantity}</p>
            </div>
          ))}

          {availableMedicines.length === 0 && (
            <p className={styles.textSmall}>{catalogMessage || "Không có thuốc đang hoạt động."}</p>
          )}
        </div>
      </div>
    </div>
  );
}