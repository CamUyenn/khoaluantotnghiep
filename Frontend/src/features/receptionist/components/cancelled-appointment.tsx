import type { ReceptionistAppointment } from "@/services/receptionistService";
import styles from "@/styles/common.module.css";

interface CancelledAppointmentsProps {
  appointments: ReceptionistAppointment[];
  resolveRoomName: (doctorId?: number) => string;
}

const normalizeStatus = (status: string | null | undefined) => (status ?? "").trim().toUpperCase();

const toStatusLabel = (status: string) => {
  if (status === "NO_SHOW_CANCELLED") {
    return "Đã hủy do quá giờ";
  }
  if (status === "CANCELLED_BY_CLINIC") {
    return "Đã hủy bởi lễ tân/phòng khám";
  }
  return "Đã hủy";
};

const toCancelledReason = (status: string, cancellationReason?: string | null) => {
  const reason = cancellationReason?.trim();
  if (reason) {
    return `Lý do: ${reason}`;
  }
  if (status === "NO_SHOW_CANCELLED") {
    return "Lịch hẹn đã quá giờ nhưng bệnh nhân không đến khám.";
  }
  if (status === "CANCELLED_BY_CLINIC") {
    return "Lịch hẹn đã bị hủy từ phía lễ tân/phòng khám.";
  }
  return "Lịch hẹn đã bị hủy.";
};

export function CancelledAppointments({ appointments, resolveRoomName }: CancelledAppointmentsProps) {
  return (
    <div className={styles.card}>
      <h2 className={styles.mb3}>Lịch hẹn đã hủy ({appointments.length})</h2>
      <div>
        {appointments.length === 0 ? (
          <div className={styles.textCenter} style={{ padding: "3rem", color: "#9ca3af" }}>
            Không có lịch hẹn đã hủy
          </div>
        ) : (
          appointments.map((appointment) => {
            const status = normalizeStatus(appointment.status);

            return (
              <div key={appointment.id} className={styles.appointmentCard}>
                <div className={styles.flexBetween}>
                  <div>
                    <div style={{ display: "flex", alignItems: "center", gap: "0.75rem", marginBottom: "0.5rem" }}>
                      <h3 style={{ fontSize: "1.125rem", fontWeight: "600" }}>
                        {appointment.patient.fullName}
                      </h3>
                      <span className={`${styles.badge} ${styles.cancelled}`}>
                        {toStatusLabel(status)}
                      </span>
                    </div>
                    <div style={{ fontSize: "0.875rem", color: "#6b7280", marginBottom: "0.5rem" }}>
                      <p>📅 Thời gian: {new Date(appointment.appointmentTime).toLocaleString("vi-VN")}</p>
                      <p>👨‍⚕️ Bác sĩ: {appointment.doctor?.username || "Chưa phân công"}</p>
                      <p>🏥 Phòng khám: {appointment.assignedRoom?.roomName || resolveRoomName(appointment.doctor?.id)}</p>
                      <p>📞 SĐT: {appointment.patient.phoneNumber || "Chưa có"}</p>
                    </div>
                    <div className={styles.panelWarning}>{toCancelledReason(status, appointment.cancellationReason)}</div>
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
