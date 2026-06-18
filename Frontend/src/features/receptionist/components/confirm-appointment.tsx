import type { ReceptionistAppointment } from "@/services/receptionistService";
import styles from "@/styles/common.module.css";

interface ConfirmedAppointmentsProps {
  appointments: ReceptionistAppointment[];
  resolveRoomName: (doctorId?: number) => string;
}

export function ConfirmedAppointments({ appointments, resolveRoomName }: ConfirmedAppointmentsProps) {
  const isNoShowCancelled = (appointment: ReceptionistAppointment) => {
    const status = (appointment.status ?? "").trim().toUpperCase();
    if (!["WAITING", "APPROVED", "CONFIRMED", "PENDING", "PENDING_CONFIRMATION", "DRAFT"].includes(status)) {
      return false;
    }

    const appointmentTimestamp = new Date(appointment.appointmentTime).getTime();
    if (Number.isNaN(appointmentTimestamp)) {
      return false;
    }

    return appointmentTimestamp < Date.now();
  };

  const toPaymentLabel = (paymentStatus?: string | null) => {
    const normalized = (paymentStatus ?? '').trim().toUpperCase();
    if (normalized === 'FULLY_PAID') {
      return 'Đã nộp tiền';
    }
    if (normalized === 'PENDING_TRANSFER') {
      return 'Chưa nộp tiền';
    }
    if (normalized === 'PARTIALLY_PAID') {
      return 'Thanh toán một phần';
    }
    return 'Chưa nộp tiền';
  };

  return (
    <div className={styles.card}>
      <h2 className={styles.mb3}>Lịch hẹn đã xác nhận ({appointments.length})</h2>
      <div>
        {appointments.length === 0 ? (
          <div className={styles.textCenter} style={{ padding: "3rem", color: "#9ca3af" }}>
            Không có lịch hẹn đã xác nhận
          </div>
        ) : (
          appointments.map((appointment) => {
            const noShowCancelled = isNoShowCancelled(appointment);

            return (
              <div key={appointment.id} className={styles.appointmentCard}>
                <div className={styles.flexBetween}>
                  <div>
                  <div style={{ display: "flex", alignItems: "center", gap: "0.75rem", marginBottom: "0.5rem" }}>
                    <h3 style={{ fontSize: "1.125rem", fontWeight: "600" }}>
                      {appointment.patient.fullName}
                    </h3>
                    <span className={`${styles.badge} ${noShowCancelled ? styles.cancelled : styles.confirmed}`}>
                      {noShowCancelled ? "Đã hủy do quá giờ" : "Đã xác nhận"}
                    </span>
                  </div>
                  <div style={{ fontSize: "0.875rem", color: "#6b7280", marginBottom: "0.5rem" }}>
                    <p>📅 Thời gian: {new Date(appointment.appointmentTime).toLocaleString("vi-VN")}</p>
                    <p>👨‍⚕️ Bác sĩ: {appointment.doctor?.username || "Chưa phân công"}</p>
                    <p>🏥 Phòng khám: {appointment.assignedRoom?.roomName || resolveRoomName(appointment.doctor?.id)}</p>
                  </div>
                  <div style={{ marginBottom: '0.5rem' }}>
                    <span className={`${styles.badge} ${appointment.paymentStatus?.toUpperCase() === 'FULLY_PAID' ? styles.confirmed : styles.cancelled}`}>
                      {toPaymentLabel(appointment.paymentStatus)}
                    </span>
                  </div>
                  {noShowCancelled && (
                    <div className={styles.panelWarning}>
                      Lịch hẹn đã quá thời gian và bệnh nhân không đến khám, hệ thống hiển thị là đã hủy.
                    </div>
                  )}
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}