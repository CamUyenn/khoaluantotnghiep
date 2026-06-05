import { Clock, Stethoscope } from "lucide-react";
import type { Appointment } from "@/types/doctor.type";
import styles from "@/styles/common.module.css";

interface WaitingListProps {
  appointments: Appointment[];
  saving: boolean;
  onStartExam: (appointment: Appointment) => void;
}

const formatDate = (value?: string | null) => {
  if (!value) return "";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "" : date.toLocaleDateString("vi-VN");
};

export function WaitingList({ appointments, saving, onStartExam }: WaitingListProps) {
  if (appointments.length === 0) {
    return (
      <div className={styles.card}>
        <h2 className={styles.mb3}>Danh sách bệnh nhân chờ khám</h2>
        <div className={styles.emptyBox}>Không có bệnh nhân trong hàng đợi</div>
      </div>
    );
  }

  return (
    <div className={styles.card}>
      <h2 className={styles.mb3}>Danh sách bệnh nhân chờ khám</h2>
      {appointments.map((appointment) => (
        <div key={appointment.id} className={styles.appointmentCard}>
          <div className={styles.cardLayout}>
            {/* Cột trái: Số thứ tự */}
            <div className={styles.queueColumn}>
              <div className={styles.queueBadge}>{appointment.queue_number}</div>
            </div>

            {/* Cột phải: Thông tin */}
            <div className={styles.appointmentMain}>
              <div className={styles.cardHeader}>
                <div>
                  <h3 className={styles.appointmentName}>{appointment.patient.full_name}</h3>
                  <div className={styles.doctorInfo}>
                    <Stethoscope size={16} />
                    <span>BS phụ trách: <strong>{appointment.doctor_name}</strong></span>
                  </div>
                </div>
                <span className={styles.timeBadge}>
                  <Clock size={14} style={{ marginRight: '6px' }} />
                  {appointment.scheduled_time}
                </span>
              </div>

              <div className={styles.infoGridWrapper}>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>SĐT:</span>
                  <span className={styles.infoValue}>{appointment.patient.phone_number}</span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>Ngày sinh:</span>
                  <span className={styles.infoValue}>{formatDate(appointment.patient.date_of_birth) || "Chưa cập nhật"}</span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>Quê quán:</span>
                  <span className={styles.infoValue}>{appointment.patient.hometown || "Chưa cập nhật"}</span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>CCCD:</span>
                  <span className={styles.infoValue}>{appointment.patient.national_id || "Chưa cập nhật"}</span>
                </div>
              </div>

              <div className={styles.reasonBox}>
                <span className={styles.reasonLabel}>Triệu chứng:</span>
                <span className={styles.reasonText}>{appointment.symptoms || "Chưa cập nhật"}</span>
              </div>

              <div className={styles.cardFooter}>
                <div>
                  {!!appointment.patient.insurance_number && (
                    <span className={styles.insuranceBadge}>BHYT: {appointment.patient.insurance_number}</span>
                  )}
                </div>
                <button
                  className={`${styles.button} ${styles.primary} ${styles.examBtn}`}
                  onClick={() => onStartExam(appointment)}
                  disabled={saving}
                >
                  Bắt đầu khám
                </button>
              </div>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}