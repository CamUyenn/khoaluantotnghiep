import { useState } from "react";
import type { ReceptionistAppointment } from "@/services/receptionistService";
import styles from "@/styles/common.module.css";

interface CancelAppointmentModalProps {
  appointment: ReceptionistAppointment;
  onClose: () => void;
  onConfirm: (reason: string) => void;
  submitting?: boolean;
}

export function CancelAppointmentModal({
  appointment,
  onClose,
  onConfirm,
  submitting = false,
}: CancelAppointmentModalProps) {
  const [reason, setReason] = useState(appointment.cancellationReason?.trim() ?? "");

  const handleSubmit = () => {
    const normalized = reason.trim();
    if (!normalized) {
      alert("Vui lòng nhập lý do hủy lịch hẹn");
      return;
    }

    onConfirm(normalized);
  };

  return (
    <div className={styles.modal} onClick={onClose}>
      <div className={styles.modalContent} onClick={(event) => event.stopPropagation()}>
        <div className={styles.modalHeader}>
          <h2>Hủy lịch hẹn - {appointment.patient.fullName}</h2>
        </div>

        <div style={{ marginBottom: "1rem" }}>
          <div style={{ backgroundColor: "#f9fafb", padding: "1rem", borderRadius: "0.375rem", marginBottom: "1rem" }}>
            <p style={{ fontSize: "0.875rem", color: "#6b7280" }}>Bệnh nhân: {appointment.patient.fullName}</p>
            <p style={{ fontSize: "0.875rem", color: "#6b7280" }}>SĐT: {appointment.patient.phoneNumber || "Chưa có"}</p>
            <p style={{ fontSize: "0.875rem", color: "#6b7280" }}>Triệu chứng: {appointment.symptoms || "Chưa có"}</p>
            <p style={{ fontSize: "0.875rem", color: "#6b7280" }}>
              Thời gian hẹn: {new Date(appointment.appointmentTime).toLocaleString("vi-VN")}
            </p>
          </div>

          <div className={styles.formGroup}>
            <label className={styles.label}>Lý do hủy *</label>
            <textarea
              className={styles.input}
              rows={4}
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              placeholder="Ví dụ: Bác sĩ bận đột xuất, cần đặt lại lịch mới..."
              disabled={submitting}
            />
          </div>
        </div>

        <div style={{ display: "flex", gap: "0.5rem" }}>
          <button className={`${styles.button} ${styles.primary}`} style={{ flex: 1 }} onClick={handleSubmit} disabled={submitting}>
            {submitting ? "Đang xử lý..." : "Xác nhận hủy"}
          </button>
          <button className={`${styles.button} ${styles.outline}`} style={{ flex: 1 }} onClick={onClose} disabled={submitting}>
            Quay lại
          </button>
        </div>
      </div>
    </div>
  );
}
