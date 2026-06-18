"use client";

import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { getApiErrorMessage } from "@/services/api";
import { patientService, type PatientAppointmentResponse } from "@/services/patientService";
import { toast } from "sonner";
import {
  BellRing,
  Calendar,
  CheckCircle2,
  Clock3,
  Loader2,
  Search,
  Stethoscope,
  XCircle,
} from "lucide-react";
import styles from "./appointments.module.css";

type AppointmentFilter = "all" | "pending" | "approved" | "in_progress" | "completed" | "cancelled";
const NO_SHOW_CANCELLED_STATUS = "NO_SHOW_CANCELLED";

const normalizeStatus = (status: string | null | undefined) => (status ?? "").trim().toUpperCase();

const isNoShowStatusCandidate = (status: string) =>
  ["PENDING", "PENDING_CONFIRMATION", "DRAFT", "APPROVED", "CONFIRMED", "WAITING"].includes(status);

const isNoShowCancelled = (appointment: PatientAppointmentResponse) => {
  const normalizedStatus = normalizeStatus(appointment.status);
  if (!isNoShowStatusCandidate(normalizedStatus) || !appointment.appointmentTime) {
    return false;
  }

  const appointmentTimestamp = new Date(appointment.appointmentTime).getTime();
  if (Number.isNaN(appointmentTimestamp)) {
    return false;
  }

  return appointmentTimestamp < Date.now();
};

const getEffectiveStatus = (appointment: PatientAppointmentResponse) => {
  if (isNoShowCancelled(appointment)) {
    return NO_SHOW_CANCELLED_STATUS;
  }
  return normalizeStatus(appointment.status);
};

const formatDateTime = (raw: string | null | undefined) => {
  if (!raw) {
    return "Chưa có lịch";
  }

  const date = new Date(raw);
  if (Number.isNaN(date.getTime())) {
    return raw;
  }

  return date.toLocaleString("vi-VN", {
    hour12: false,
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const statusLabelMap: Record<string, string> = {
  PENDING: "Đang chờ duyệt",
  DRAFT: "Đang chờ duyệt",
  PENDING_CONFIRMATION: "Đang chờ duyệt",
  APPROVED: "Đã duyệt - Chờ khám",
  CONFIRMED: "Đã duyệt - Chờ khám",
  WAITING: "Đã duyệt - Chờ khám",
  IN_PROGRESS: "Đang khám",
  COMPLETED: "Đã hoàn thành",
  CANCELLED: "Đã hủy",
  CANCELLED_BY_CLINIC: "Phòng khám đã hủy",
  NO_SHOW_CANCELLED: "Đã hủy do quá giờ",
};

const getBadgeClass = (status: string) => {
  switch (status) {
    case "COMPLETED":
      return styles.badgeCompleted;
    case "APPROVED":
    case "CONFIRMED":
    case "WAITING":
      return styles.badgeWaiting;
    case "IN_PROGRESS":
      return styles.badgeProgress;
    case "CANCELLED":
    case "CANCELLED_BY_CLINIC":
    case NO_SHOW_CANCELLED_STATUS:
      return styles.badgeCancelled;
    default:
      return styles.badgePending;
  }
};

const getSystemNotification = (appointment: PatientAppointmentResponse, status: string) => {
  const when = formatDateTime(appointment.appointmentTime);
  const reason = appointment.cancellationReason?.trim();

  switch (status) {
    case "APPROVED":
    case "CONFIRMED":
    case "WAITING":
      return {
        title: "Lịch hẹn đã được duyệt",
        message: `Lịch #${appointment.id} đã được duyệt. Vui lòng có mặt trước ${when} khoảng 10-15 phút để làm thủ tục.`,
      };
    case "IN_PROGRESS":
      return {
        title: "Lịch hẹn đang được thăm khám",
        message: `Lịch #${appointment.id} đang trong quá trình thăm khám. Bạn vui lòng chờ theo hướng dẫn của điều dưỡng.`,
      };
    case "COMPLETED":
      return {
        title: "Lịch hẹn đã hoàn thành",
        message: `Lịch #${appointment.id} đã hoàn thành. Bạn có thể xem hồ sơ khám và lịch sử hóa đơn tại các mục tương ứng.`,
      };
    case "CANCELLED":
      return {
        title: "Bạn đã hủy lịch hẹn",
        message: `Lịch #${appointment.id} đã được ghi nhận hủy thành công. Bạn có thể đặt lịch mới bất cứ lúc nào.`,
      };
    case "CANCELLED_BY_CLINIC":
      return {
        title: "Phòng khám đã hủy lịch",
        message: reason
          ? `Lịch #${appointment.id} đã bị hủy từ phía phòng khám. Lý do: ${reason}.`
          : `Lịch #${appointment.id} đã bị hủy từ phía phòng khám. Vui lòng đặt lịch mới hoặc liên hệ lễ tân để được hỗ trợ.`,
      };
    case NO_SHOW_CANCELLED_STATUS:
      return {
        title: "Lịch hẹn đã quá giờ",
        message: `Lịch #${appointment.id} đã quá thời gian khám (${when}), vui lòng đặt lại lịch khác.`,
      };
    default:
      return {
        title: "Lịch hẹn đang chờ xử lý",
        message: `Lịch #${appointment.id} đã được tiếp nhận và đang chờ duyệt. Hệ thống sẽ cập nhật ngay khi trạng thái thay đổi.`,
      };
  }
};

const isCancellable = (status: string) => {
  const normalized = normalizeStatus(status);
  return (
    normalized === "PENDING" ||
    normalized === "WAITING" ||
    normalized === "DRAFT" ||
    normalized === "PENDING_CONFIRMATION" ||
    normalized === "APPROVED" ||
    normalized === "CONFIRMED"
  );
};

export function PatientAppointments() {
  const searchParams = useSearchParams();
  const createdIdParam = searchParams.get("createdId");
  const createdAppointmentId = createdIdParam ? Number(createdIdParam) : null;
  const [appointments, setAppointments] = useState<PatientAppointmentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [cancellingId, setCancellingId] = useState<number | null>(null);
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState<AppointmentFilter>("all");
  const [highlightedId, setHighlightedId] = useState<number | null>(null);

  const loadAppointments = async (showLoading = true) => {
    try {
      if (showLoading) {
        setLoading(true);
      }
      const data = await patientService.getMyAppointments();
      setAppointments(
        [...data].sort((a, b) => {
          const timeA = a.appointmentTime ? new Date(a.appointmentTime).getTime() : 0;
          const timeB = b.appointmentTime ? new Date(b.appointmentTime).getTime() : 0;
          return timeB - timeA;
        })
      );
    } catch (error) {
      if (showLoading) {
        toast.error(getApiErrorMessage(error, "Không thể tải danh sách lịch hẹn"));
      }
    } finally {
      if (showLoading) {
        setLoading(false);
      }
    }
  };

  useEffect(() => {
    void loadAppointments();

    const intervalId = window.setInterval(() => {
      void loadAppointments(false);
    }, 30000);

    return () => {
      window.clearInterval(intervalId);
    };
  }, []);

  useEffect(() => {
    if (!createdAppointmentId || Number.isNaN(createdAppointmentId) || appointments.length === 0) {
      return;
    }

    const exists = appointments.some((item) => item.id === createdAppointmentId);
    if (!exists) {
      return;
    }

    setHighlightedId(createdAppointmentId);
    toast.success(`Lịch hẹn #${createdAppointmentId} đã được tạo thành công`);

    const timer = window.setTimeout(() => {
      setHighlightedId((current) => (current === createdAppointmentId ? null : current));
    }, 5000);

    window.requestAnimationFrame(() => {
      const element = document.getElementById(`appointment-${createdAppointmentId}`);
      element?.scrollIntoView({ behavior: "smooth", block: "center" });
    });

    return () => {
      window.clearTimeout(timer);
    };
  }, [appointments, createdAppointmentId]);

  const statusStats = useMemo(() => {
    return appointments.reduce(
      (stats, item) => {
        const status = getEffectiveStatus(item);
        stats.total += 1;
        if (status === "PENDING" || status === "PENDING_CONFIRMATION" || status === "DRAFT") {
          stats.pending += 1;
        } else if (status === "WAITING" || status === "APPROVED" || status === "CONFIRMED") {
          stats.approved += 1;
        } else if (status === "IN_PROGRESS") {
          stats.inProgress += 1;
        } else if (status === "COMPLETED") {
          stats.completed += 1;
        } else if (status === "CANCELLED" || status === "CANCELLED_BY_CLINIC" || status === NO_SHOW_CANCELLED_STATUS) {
          stats.cancelled += 1;
        }
        return stats;
      },
      { total: 0, pending: 0, approved: 0, inProgress: 0, completed: 0, cancelled: 0 }
    );
  }, [appointments]);

  const filteredAppointments = useMemo(() => {
    return appointments.filter((item) => {
      const normalizedStatus = getEffectiveStatus(item);
      const keyword = query.trim().toLowerCase();

      const passesFilter =
        filter === "all" ||
        (filter === "pending" && ["PENDING", "PENDING_CONFIRMATION", "DRAFT"].includes(normalizedStatus)) ||
        (filter === "approved" && ["WAITING", "APPROVED", "CONFIRMED"].includes(normalizedStatus)) ||
        (filter === "in_progress" && normalizedStatus === "IN_PROGRESS") ||
        (filter === "completed" && normalizedStatus === "COMPLETED") ||
        (filter === "cancelled" && ["CANCELLED", "CANCELLED_BY_CLINIC", NO_SHOW_CANCELLED_STATUS].includes(normalizedStatus));

      if (!passesFilter) {
        return false;
      }

      if (!keyword) {
        return true;
      }

      const doctorName = (item.doctor?.fullName ?? item.doctor?.username ?? "").toLowerCase();
      return (
        String(item.id).includes(keyword) ||
        doctorName.includes(keyword) ||
        (item.symptoms ?? "").toLowerCase().includes(keyword)
      );
    });
  }, [appointments, filter, query]);

  const notifications = useMemo(() => {
    return appointments.slice(0, 5).map((item) => {
      const effectiveStatus = getEffectiveStatus(item);
      return {
        appointmentId: item.id,
        time: formatDateTime(item.appointmentTime),
        status: effectiveStatus,
        ...getSystemNotification(item, effectiveStatus),
      };
    });
  }, [appointments]);

  const handleCancel = async (appointmentId: number) => {
    try {
      setCancellingId(appointmentId);
      await patientService.cancelMyAppointment(appointmentId);
      toast.success("Hủy lịch thành công");
      await loadAppointments();
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể hủy lịch hẹn"));
    } finally {
      setCancellingId(null);
    }
  };

  return (
    <main className={styles.page}>
      <section className={styles.headerSection}>
        <h1 className={styles.title}>Lịch hẹn của tôi</h1>
        <p className={styles.subtitle}>
          Theo dõi đầy đủ trạng thái sau khi đặt lịch và thông báo cập nhật từ hệ thống.
        </p>
      </section>

      <section className={styles.statGrid}>
        <Card className={styles.statCard}>
          <p className={styles.statLabel}>Tổng lịch hẹn</p>
          <p className={styles.statValue}>{statusStats.total}</p>
        </Card>
        <Card className={styles.statCard}>
          <p className={styles.statLabel}>Đang chờ duyệt</p>
          <p className={styles.statValue}>{statusStats.pending}</p>
        </Card>
        <Card className={styles.statCard}>
          <p className={styles.statLabel}>Đã duyệt / chờ khám</p>
          <p className={styles.statValue}>{statusStats.approved}</p>
        </Card>
        <Card className={styles.statCard}>
          <p className={styles.statLabel}>Đã hoàn thành</p>
          <p className={styles.statValue}>{statusStats.completed}</p>
        </Card>
      </section>

      <section className={styles.contentGrid}>
        <Card className={styles.appointmentsCard}>
          <div className={styles.toolbar}>
            <div className={styles.searchBox}>
              <Search size={16} />
              <Input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Tìm theo mã lịch, bác sĩ, triệu chứng"
                className={styles.searchInput}
              />
            </div>

            <div className={styles.filters}>
              <Button size="sm" variant={filter === "all" ? "default" : "outline"} onClick={() => setFilter("all")}>Tất cả</Button>
              <Button size="sm" variant={filter === "pending" ? "default" : "outline"} onClick={() => setFilter("pending")}>Đang chờ</Button>
              <Button size="sm" variant={filter === "approved" ? "default" : "outline"} onClick={() => setFilter("approved")}>Đã duyệt</Button>
              <Button size="sm" variant={filter === "in_progress" ? "default" : "outline"} onClick={() => setFilter("in_progress")}>Đang khám</Button>
              <Button size="sm" variant={filter === "completed" ? "default" : "outline"} onClick={() => setFilter("completed")}>Hoàn thành</Button>
              <Button size="sm" variant={filter === "cancelled" ? "default" : "outline"} onClick={() => setFilter("cancelled")}>Đã hủy</Button>
            </div>
          </div>

          {loading ? (
            <div className={styles.loadingBox}>
              <Loader2 className={styles.spinning} size={20} />
              <span>Đang tải lịch hẹn...</span>
            </div>
          ) : filteredAppointments.length === 0 ? (
            <div className={styles.emptyBox}>
              <Calendar size={38} />
              <p>Chưa có lịch hẹn phù hợp bộ lọc hiện tại.</p>
            </div>
          ) : (
            <div className={styles.list}>
              {filteredAppointments.map((item) => {
                const status = getEffectiveStatus(item);
                const doctorName = item.doctor?.fullName ?? item.doctor?.username ?? "Đang cập nhật";
                const notification = getSystemNotification(item, status);
                const cancellationReason = item.cancellationReason?.trim();

                return (
                  <article
                    id={`appointment-${item.id}`}
                    key={item.id}
                    className={`${styles.item} ${highlightedId === item.id ? styles.itemHighlight : ""}`}
                  >
                    <div className={styles.itemTop}>
                      <div>
                        <p className={styles.itemId}>Mã lịch hẹn #{item.id}</p>
                        <h3 className={styles.itemDate}>{formatDateTime(item.appointmentTime)}</h3>
                      </div>
                      <Badge className={getBadgeClass(status)}>{statusLabelMap[status] ?? status}</Badge>
                    </div>

                    <div className={styles.metaRow}>
                      <div className={styles.metaItem}>
                        <Stethoscope size={15} />
                        <span>Bác sĩ: {doctorName}</span>
                      </div>
                      <div className={styles.metaItem}>
                        <Clock3 size={15} />
                        <span>Triệu chứng: {item.symptoms || "Chưa cập nhật"}</span>
                      </div>
                    </div>

                    <div
                      className={`${styles.notificationInline} ${
                        status === NO_SHOW_CANCELLED_STATUS ? styles.notificationInlineAlert : ""
                      }`}
                    >
                      <BellRing size={15} />
                      <p>
                        <strong>{notification.title}:</strong> {notification.message}
                      </p>
                    </div>

                    {status === "CANCELLED_BY_CLINIC" && cancellationReason && (
                      <div className={`${styles.notificationInline} ${styles.notificationInlineAlert}`}>
                        <XCircle size={15} />
                        <p>
                          <strong>Lý do hủy:</strong> {cancellationReason}
                        </p>
                      </div>
                    )}

                    {isCancellable(status) && (
                      <div className={styles.actions}>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => void handleCancel(item.id)}
                          disabled={cancellingId === item.id}
                        >
                          {cancellingId === item.id ? "Đang hủy..." : "Hủy lịch"}
                        </Button>
                      </div>
                    )}
                  </article>
                );
              })}
            </div>
          )}
        </Card>

        <Card className={styles.notificationCard}>
          <div className={styles.notificationHeader}>
            <h2>Thông báo hệ thống</h2>
            <p>Cập nhật theo trạng thái lịch hẹn</p>
          </div>

          {notifications.length === 0 ? (
            <div className={styles.emptyNotice}>
              <XCircle size={18} />
              <span>Chưa có thông báo mới.</span>
            </div>
          ) : (
            <div className={styles.notificationList}>
              {notifications.map((notification) => (
                <div
                  key={`${notification.appointmentId}-${notification.time}`}
                  className={`${styles.notificationItem} ${
                    notification.status === NO_SHOW_CANCELLED_STATUS ? styles.notificationInlineAlert : ""
                  }`}
                >
                  <CheckCircle2 size={16} />
                  <div>
                    <p className={styles.noticeTitle}>{notification.title}</p>
                    <p className={styles.noticeText}>{notification.message}</p>
                    <p className={styles.noticeTime}>#{notification.appointmentId} - {notification.time}</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </section>
    </main>
  );
}
