'use client';

import { useEffect, useState } from "react";
import { toast } from "sonner";

import { DashboardStats } from "./components/dashboard-stats";
import { PendingAppointments } from "./components/pending-appointment";
import { ConfirmedAppointments } from "./components/confirm-appointment";
import { CancelledAppointments } from "./components/cancelled-appointment";
import { ConfirmModal } from "./components/confirm-modal";
import { CancelAppointmentModal } from "./components/cancel-appointment-modal";
import { useReceptionistDashboard } from "@/hooks/useReceptionistDashboard";
import { getApiErrorMessage } from "@/services/api";
import { receptionistService, type ReceptionistAppointment } from "@/services/receptionistService";
import styles from "@/styles/common.module.css";

export function ReceptionistDashboard() {
  // Thành phần chính của lễ tân: hiển thị lịch hẹn chờ xác nhận/đã xác nhận và thao tác duyệt/hủy.
  const [activeView, setActiveView] = useState<"all" | "pending" | "confirmed" | "cancelled">("all");
  const [selectedAppointment, setSelectedAppointment] = useState<ReceptionistAppointment | null>(null);
  const [cancelTarget, setCancelTarget] = useState<ReceptionistAppointment | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [approvalMessage, setApprovalMessage] = useState<string | null>(null);

  const {
    pendingAppointments,
    confirmedAppointments,
    cancelledAppointments,
    doctorOptions,
    totalCount,
    isLoading,
    loadDashboardData,
  } = useReceptionistDashboard();

  const isOverdueAppointment = (appointment: ReceptionistAppointment) => {
    const appointmentTimestamp = new Date(appointment.appointmentTime).getTime();
    if (Number.isNaN(appointmentTimestamp)) {
      return false;
    }

    return appointmentTimestamp < Date.now();
  };

  const visiblePendingAppointments = pendingAppointments.filter((appointment) => !isOverdueAppointment(appointment));

  const isNoShowCancelled = (appointment: ReceptionistAppointment) => {
    const status = (appointment.status ?? "").trim().toUpperCase();
    if (!["WAITING", "APPROVED", "CONFIRMED"].includes(status)) {
      return false;
    }

    const appointmentTimestamp = new Date(appointment.appointmentTime).getTime();
    if (Number.isNaN(appointmentTimestamp)) {
      return false;
    }

    return appointmentTimestamp < Date.now();
  };

  const mergedCancelledAppointments = (() => {
    const idSet = new Set<number>();
    const result: ReceptionistAppointment[] = [];

    cancelledAppointments.forEach((appointment) => {
      if (!idSet.has(appointment.id)) {
        idSet.add(appointment.id);
        result.push(appointment);
      }
    });

    confirmedAppointments
      .filter((appointment) => isNoShowCancelled(appointment))
      .forEach((appointment) => {
        if (!idSet.has(appointment.id)) {
          idSet.add(appointment.id);
          result.push({ ...appointment, status: "NO_SHOW_CANCELLED" });
        }
      });

    pendingAppointments
      .filter((appointment) => isOverdueAppointment(appointment))
      .forEach((appointment) => {
        if (!idSet.has(appointment.id)) {
          idSet.add(appointment.id);
          result.push({ ...appointment, status: "NO_SHOW_CANCELLED" });
        }
      });

    return result.sort((a, b) => new Date(b.appointmentTime).getTime() - new Date(a.appointmentTime).getTime());
  })();

  // Tìm tên phòng theo doctorId để hiển thị ở danh sách lịch đã xác nhận.
  const resolveRoomName = (doctorId?: number) => {
    if (!doctorId) {
      return "Chưa có";
    }

    const matched = doctorOptions.find((doctor) => doctor.doctorId === doctorId);
    return matched?.roomName || "Chưa có";
  };

  useEffect(() => {
    let disposed = false;

    const refresh = async (showErrorToast: boolean) => {
      try {
        await loadDashboardData();
      } catch (error) {
        if (!disposed && showErrorToast) {
          toast.error(getApiErrorMessage(error, "Không thể tải dữ liệu lễ tân"));
        }
      }
    };

    void refresh(true);

    const intervalId = window.setInterval(() => {
      void loadDashboardData({ silent: true }).catch(() => {
        // Ignore background refresh errors to avoid noisy toasts every 30s.
      });
    }, 30000);

    return () => {
      disposed = true;
      window.clearInterval(intervalId);
    };
  }, [loadDashboardData]);

  // Xử lý khi Lễ tân bấm nút "Xác nhận" trên thẻ lịch hẹn
  const handleOpenConfirmModal = (appointment: ReceptionistAppointment) => {
    setApprovalMessage(null);
    setSelectedAppointment(appointment);
  };

  const handleCloseConfirmModal = () => {
    setApprovalMessage(null);
    setSelectedAppointment(null);
  };

  // Xử lý khi Lễ tân submit form trong Modal
  // Chuẩn hóa datetime-local sang định dạng LocalDateTime backend mong đợi.
  const toApiDateTime = (value: string) => {
    const normalized = value.trim();
    if (!normalized) {
      return normalized;
    }

    if (normalized.length === 16) {
      return `${normalized}:00`;
    }

    return normalized;
  };

  // Xác nhận lịch hẹn với bác sĩ và thời gian đã chọn.
  // Bước 1: Kiểm tra đã có lịch hẹn đang chọn.
  // Bước 2: Gọi API approve để gán bác sĩ + giờ khám.
  // Bước 3: Đóng modal, reload dữ liệu và hiển thị thông báo.
  const handleConfirmAppointment = async (
    doctorId: number,
    appointmentTime: string,
    assignedRoomId?: number | null,
    specialty?: string | null,
  ) => {
    if (!selectedAppointment) {
      return;
    }

    try {
      setIsSubmitting(true);
      const result = await receptionistService.approveAppointment(
        selectedAppointment.id,
        doctorId,
        toApiDateTime(appointmentTime),
        assignedRoomId,
        specialty,
      );
      setApprovalMessage(result.message || "Xác nhận lịch hẹn thành công");
      void loadDashboardData({ silent: true }).catch((refreshError) => {
        toast.error(getApiErrorMessage(refreshError, "Đã duyệt nhưng chưa thể làm mới danh sách"));
      });
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể xác nhận lịch hẹn"));
    } finally {
      setIsSubmitting(false);
    }
  };

  // Xử lý khi Lễ tân bấm Hủy
  // Hủy lịch hẹn sau khi người dùng xác nhận, sau đó tải lại dashboard.
  const handleOpenCancelModal = (appointment: ReceptionistAppointment) => {
    setCancelTarget(appointment);
  };

  const handleCancelAppointment = async (reason: string) => {
    if (!cancelTarget) {
      return;
    }

    try {
      setIsSubmitting(true);
      await receptionistService.cancelAppointment(cancelTarget.id, reason);
      toast.success("Đã hủy lịch hẹn");
      setCancelTarget(null);
      void loadDashboardData({ silent: true }).catch((refreshError) => {
        toast.error(getApiErrorMessage(refreshError, "Đã hủy nhưng chưa thể làm mới danh sách"));
      });
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể hủy lịch hẹn"));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      <main className={styles.mainArea}>
        <div className={styles.container}>
          <div className={styles.header}>
            <h1>Tiếp nhận bệnh nhân - Lễ tân</h1>
            <p>Quản lý lịch hẹn và phân công bác sĩ khám</p>
          </div>

          <DashboardStats 
            pendingCount={visiblePendingAppointments.length} 
            confirmedCount={confirmedAppointments.length} 
            totalCount={totalCount} 
          />

          <div className={styles.flexRow}>
            <button
              type="button"
              className={`${styles.button} ${activeView === "all" ? styles.primary : styles.outline}`}
              onClick={() => setActiveView("all")}
            >
              Tất cả
            </button>
            <button
              type="button"
              className={`${styles.button} ${activeView === "pending" ? styles.primary : styles.outline}`}
              onClick={() => setActiveView("pending")}
            >
              Chờ xác nhận
            </button>
            <button
              type="button"
              className={`${styles.button} ${activeView === "confirmed" ? styles.primary : styles.outline}`}
              onClick={() => setActiveView("confirmed")}
            >
              Đã xác nhận
            </button>
            <button
              type="button"
              className={`${styles.button} ${activeView === "cancelled" ? styles.primary : styles.outline}`}
              onClick={() => setActiveView("cancelled")}
            >
              Đã hủy
            </button>
          </div>

          {isLoading && (
            <div className={styles.card}>
              <div className={styles.textCenter} style={{ padding: "2rem", color: "#64748b" }}>
                Đang tải dữ liệu lịch hẹn...
              </div>
            </div>
          )}

          {!isLoading && (activeView === "all" || activeView === "pending") && (
            <PendingAppointments 
              appointments={visiblePendingAppointments} 
              onConfirmClick={handleOpenConfirmModal} 
              onCancelClick={handleOpenCancelModal} 
              disabled={isSubmitting}
            />
          )}

          {!isLoading && (activeView === "all" || activeView === "confirmed") && (
            <ConfirmedAppointments 
              appointments={confirmedAppointments} 
              resolveRoomName={resolveRoomName}
            />
          )}

          {!isLoading && activeView === "cancelled" && (
            <CancelledAppointments
              appointments={mergedCancelledAppointments}
              resolveRoomName={resolveRoomName}
            />
          )}
        </div>
      </main>

      {selectedAppointment && (
        <ConfirmModal
          appointment={selectedAppointment}
          doctors={doctorOptions}
          onClose={handleCloseConfirmModal}
          onConfirm={handleConfirmAppointment}
          approvalMessage={approvalMessage}
          submitting={isSubmitting}
        />
      )}

      {cancelTarget && (
        <CancelAppointmentModal
          appointment={cancelTarget}
          onClose={() => setCancelTarget(null)}
          onConfirm={handleCancelAppointment}
          submitting={isSubmitting}
        />
      )}
    </>
  );
}