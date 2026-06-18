import { useEffect, useMemo, useState } from "react";
import type { ReceptionistAppointment, ReceptionistDoctorOption, ReceptionistRoomScheduleOption } from "@/services/receptionistService";
import { receptionistService } from "@/services/receptionistService";
import styles from "@/styles/common.module.css";

interface ConfirmModalProps {
  appointment: ReceptionistAppointment;
  doctors: ReceptionistDoctorOption[];
  onClose: () => void;
  onConfirm: (doctorId: number, appointmentTime: string, assignedRoomId?: number | null, specialty?: string | null) => void;
  approvalMessage?: string | null;
  submitting?: boolean;
}

export function ConfirmModal({ appointment, doctors, onClose, onConfirm, approvalMessage, submitting = false }: ConfirmModalProps) {
  // Quản lý state riêng cho Modal
  const [suggestedRoomName, setSuggestedRoomName] = useState<string>("Đang gợi ý phòng khám...");
  const [suggestedRoomId, setSuggestedRoomId] = useState<number | null>(null);
  const [suggestedSpecialty, setSuggestedSpecialty] = useState<string | null>(null);
  const [scheduleOptions, setScheduleOptions] = useState<ReceptionistRoomScheduleOption[]>([]);
  const [selectedScheduleKey, setSelectedScheduleKey] = useState<string>("");
  const [appointmentTime, setAppointmentTime] = useState(() => {
    const date = new Date(appointment.appointmentTime);
    if (Number.isNaN(date.getTime())) {
      return "";
    }

    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, "0");
    const dd = String(date.getDate()).padStart(2, "0");
    const hh = String(date.getHours()).padStart(2, "0");
    const mi = String(date.getMinutes()).padStart(2, "0");
    return `${yyyy}-${mm}-${dd}T${hh}:${mi}`;
  });

  const appointmentDate = useMemo(() => {
    if (!appointmentTime || appointmentTime.length < 10) {
      return "";
    }
    return appointmentTime.slice(0, 10);
  }, [appointmentTime]);

  const appointmentTimeOnly = useMemo(() => {
    if (!appointmentTime || appointmentTime.length < 16) {
      return "";
    }
    return appointmentTime.slice(11, 16);
  }, [appointmentTime]);

  const buildScheduleKey = (option: ReceptionistRoomScheduleOption) =>
    `${option.roomId ?? "NA"}-${option.doctorId ?? "NA"}-${option.startTime ?? ""}-${option.endTime ?? ""}`;

  const parseTimeToMinutes = (value?: string | null) => {
    if (!value) {
      return null;
    }
    const [hh, mm] = value.split(":");
    const hour = Number(hh);
    const minute = Number(mm);
    if (Number.isNaN(hour) || Number.isNaN(minute)) {
      return null;
    }
    return hour * 60 + minute;
  };

  const isTimeWithinSchedule = (timeValue: string, option: ReceptionistRoomScheduleOption) => {
    const timeMinutes = parseTimeToMinutes(timeValue);
    const startMinutes = parseTimeToMinutes(option.startTime ?? undefined);
    const endMinutes = parseTimeToMinutes(option.endTime ?? undefined);
    if (timeMinutes == null || startMinutes == null || endMinutes == null) {
      return false;
    }
    return timeMinutes >= startMinutes && timeMinutes < endMinutes;
  };

  useEffect(() => {
    let mounted = true;

    const loadSuggestion = async () => {
      try {
        const suggestion = await receptionistService.getSuggestedRoom(appointment.id);
        if (!mounted) {
          return;
        }

        setSuggestedRoomName(suggestion.roomName || "Chưa xác định được phòng khám");
        setSuggestedRoomId(suggestion.roomId ?? null);
        setSuggestedSpecialty(suggestion.specialty || null);
      } catch {
        if (!mounted) {
          return;
        }

        setSuggestedRoomName("Chưa xác định được phòng khám");
        setSuggestedRoomId(null);
        setSuggestedSpecialty(null);
      }
    };

    void loadSuggestion();

    return () => {
      mounted = false;
    };
  }, [appointment.id]);

  useEffect(() => {
    let mounted = true;

    const loadSchedules = async () => {
      try {
        const data = await receptionistService.getRoomSchedules(appointmentDate || undefined);
        if (!mounted) {
          return;
        }
        setScheduleOptions(data || []);
      } catch {
        if (!mounted) {
          return;
        }
        setScheduleOptions([]);
      }
    };

    if (appointmentDate) {
      void loadSchedules();
    } else {
      setScheduleOptions([]);
    }

    return () => {
      mounted = false;
    };
  }, [appointmentDate]);

  useEffect(() => {
    if (!scheduleOptions.length || selectedScheduleKey) {
      return;
    }

    const matchedBySuggestion = suggestedRoomId
      ? scheduleOptions.find((option) => option.roomId === suggestedRoomId
        && (!appointmentTimeOnly || isTimeWithinSchedule(appointmentTimeOnly, option)))
      : null;
    const matchedByTime = appointmentTimeOnly
      ? scheduleOptions.find((option) => isTimeWithinSchedule(appointmentTimeOnly, option))
      : null;
    const initial = matchedBySuggestion || matchedByTime || scheduleOptions[0];
    if (initial) {
      setSelectedScheduleKey(buildScheduleKey(initial));
    }
  }, [scheduleOptions, selectedScheduleKey, suggestedRoomId, appointmentTimeOnly]);

  const handleSubmit = () => {
    if (approvalMessage) {
      onClose();
      return;
    }

    if (!selectedScheduleKey) {
      alert("Vui lòng chọn phòng khám phù hợp");
      return;
    }
    if (!appointmentTime) {
      alert("Vui lòng chọn thời gian khám");
      return;
    }

    const selectedSchedule = scheduleOptions.find((option) => buildScheduleKey(option) === selectedScheduleKey);
    if (!selectedSchedule?.doctorId || !selectedSchedule.roomId) {
      alert("Phòng khám chưa có bác sĩ trong lịch phân công.");
      return;
    }

    if (appointmentTimeOnly && !isTimeWithinSchedule(appointmentTimeOnly, selectedSchedule)) {
      alert("Khung giờ khám không nằm trong lịch phân công của phòng đã chọn.");
      return;
    }

    onConfirm(selectedSchedule.doctorId, appointmentTime, selectedSchedule.roomId, suggestedSpecialty ?? appointment.category?.name ?? null);
  };

  const selectedSchedule = scheduleOptions.find((option) => buildScheduleKey(option) === selectedScheduleKey);
  const resolvedRoomName = selectedSchedule?.roomName || suggestedRoomName;
  const resolvedDoctorName = selectedSchedule?.doctorUsername || "";

  return (
    <div className={styles.modal} onClick={onClose}>
      <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
        <div className={styles.modalHeader}>
          <h2>Xác nhận lịch hẹn - {appointment.patient.fullName}</h2>
        </div>

        <div style={{ marginBottom: "1rem" }}>
          {approvalMessage && (
            <div
              style={{
                marginBottom: "1rem",
                padding: "0.875rem 1rem",
                borderRadius: "0.5rem",
                backgroundColor: "#ecfdf5",
                border: "1px solid #10b981",
                color: "#065f46",
                fontSize: "0.925rem",
                lineHeight: 1.5,
              }}
            >
              {approvalMessage}
            </div>
          )}

          {/* Tóm tắt thông tin bệnh nhân */}
          <div style={{ backgroundColor: "#f9fafb", padding: "1rem", borderRadius: "0.375rem", marginBottom: "1rem" }}>
            <p style={{ fontSize: "0.875rem", color: "#6b7280" }}>Bệnh nhân: {appointment.patient.fullName}</p>
            <p style={{ fontSize: "0.875rem", color: "#6b7280" }}>SĐT: {appointment.patient.phoneNumber || "Chưa có"}</p>
            <p style={{ fontSize: "0.875rem", color: "#6b7280" }}>Triệu chứng: {appointment.symptoms || "Chưa có"}</p>
            <p style={{ fontSize: "0.875rem", color: "#6b7280" }}>
              Thời gian hẹn: {new Date(appointment.appointmentTime).toLocaleString("vi-VN")}
            </p>
          </div>

          <div className={styles.formGroup}>
            <label className={styles.label}>Chọn phòng khám *</label>
            <select
              className={styles.input}
              value={selectedScheduleKey}
              onChange={(e) => setSelectedScheduleKey(e.target.value)}
              disabled={submitting || Boolean(approvalMessage)}
            >
              <option value="">-- Chọn phòng --</option>
              {scheduleOptions.map((option) => (
                <option key={buildScheduleKey(option)} value={buildScheduleKey(option)}>
                  {option.roomName || "Phòng chưa đặt tên"} - BS. {option.doctorUsername || "Chưa phân công"}
                  {option.startTime && option.endTime ? ` (${option.startTime} - ${option.endTime})` : ""}
                </option>
              ))}
            </select>
            {scheduleOptions.length === 0 && (
              <p style={{ marginTop: "0.5rem", fontSize: "0.8rem", color: "#b91c1c" }}>
                Chưa có lịch phân công phòng khám trong ngày này.
              </p>
            )}
          </div>

          <div className={styles.formGroup}>
            <label className={styles.label}>Thời gian khám *</label>
            <input
              type="datetime-local"
              className={styles.input}
              value={appointmentTime}
              onChange={(e) => setAppointmentTime(e.target.value)}
              disabled={submitting || Boolean(approvalMessage)}
            />
          </div>

          <div className={styles.formGroup}>
            <label className={styles.label}>Phòng khám *</label>
            <input
              className={styles.input}
              value={resolvedRoomName}
              readOnly
              disabled
            />
            <p style={{ marginTop: "0.5rem", fontSize: "0.8rem", color: "#6b7280" }}>
              {suggestedSpecialty
                ? `Phòng khám gợi ý theo triệu chứng: ${suggestedRoomName} (${suggestedSpecialty})`
                : `Phòng khám gợi ý theo lịch hẹn: ${suggestedRoomName}`}
            </p>
            {resolvedDoctorName && (
              <p style={{ marginTop: "0.25rem", fontSize: "0.8rem", color: "#6b7280" }}>
                Bác sĩ trực: {resolvedDoctorName}
              </p>
            )}
          </div>
        </div>

        <div style={{ display: "flex", gap: "0.5rem" }}>
          <button className={`${styles.button} ${styles.primary}`} style={{ flex: 1 }} onClick={handleSubmit} disabled={submitting}>
            {approvalMessage ? "Đóng" : submitting ? "Đang xử lý..." : "Xác nhận lịch hẹn"}
          </button>
          <button className={`${styles.button} ${styles.outline}`} style={{ flex: 1 }} onClick={onClose} disabled={submitting}>
            Hủy
          </button>
        </div>
      </div>
    </div>
  );
}