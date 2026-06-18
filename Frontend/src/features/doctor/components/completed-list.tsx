import type { Appointment } from "@/types/doctor.type";
import styles from "@/styles/common.module.css";

interface CompletedListProps {
  appointments: Appointment[];
}

export function CompletedList({ appointments }: CompletedListProps) {
  if (appointments.length === 0) {
    return (
      <div className={styles.card}>
        <h2 className={styles.mb3}>Danh sách đã khám</h2>
        <div className={styles.emptyBox}>Chưa có bệnh nhân đã khám</div>
      </div>
    );
  }

  return (
    <div className={styles.card}>
      <h2 className={styles.mb3}>Danh sách đã khám</h2>
      {appointments.map((appointment) => (
        <div key={appointment.id} className={styles.completedCard}>
          <div className={styles.flexBetween}>
            <div>
              <h3 className={styles.appointmentName}>{appointment.patient.full_name}</h3>
              <p className={styles.textSmall}>{appointment.patient.phone_number || "Chưa cập nhật"}</p>
            </div>
            <span className={styles.completedBadge}>Đã khám</span>
          </div>

          <div className={styles.noteBlue}>
            <p className={styles.textSmall}><strong>Chẩn đoán:</strong> {appointment.diagnosis || "-"}</p>
          </div>
          <div className={styles.noteGreen}>
            <p className={styles.textSmall}><strong>Điều trị:</strong> {appointment.doctor_advice || "-"}</p>
          </div>

          {!!appointment.prescription_items?.length && (
            <div className={styles.notePurple}>
              <div className={styles.flexBetween}>
                <p className={styles.textSmall}><strong>Đơn thuốc</strong></p>
              </div>
              <div className={styles.rowStack}>
                {appointment.prescription_items.map((item) => (
                  <div key={item.medicine_id} className={styles.itemCard}>
                    <div className={styles.flexBetween}>
                      <span className={styles.textSmall}>
                        {item.medicine.medicine_name} {item.medicine.dosage}
                      </span>
                    </div>
                    <p className={styles.textSmall}>
                      Số lượng: {item.quantity} {item.medicine.unit} - {item.usage_instructions}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {!!appointment.service_items?.length && (
            <div className={styles.noteBlue}>
              <div className={styles.flexBetween}>
                <p className={styles.textSmall}><strong>Dịch vụ bổ sung</strong></p>
                <p className={styles.textSmall}><strong>Tổng: {appointment.total_service_cost?.toLocaleString("vi-VN")}đ</strong></p>
              </div>
              <div className={styles.rowStack}>
                {appointment.service_items.map((item) => (
                  <div key={item.service_id} className={styles.itemCard}>
                    <div className={styles.flexBetween}>
                      <span className={styles.textSmall}>{item.service_name}</span>
                      <span className={styles.textSmall}>{(item.actual_price * item.quantity).toLocaleString("vi-VN")}đ</span>
                    </div>
                    <p className={styles.textSmall}>Số lượng: {item.quantity} - Đơn giá: {item.actual_price.toLocaleString("vi-VN")}đ</p>
                  </div>
                ))}
              </div>
            </div>
          )}

        </div>
      ))}
    </div>
  );
}