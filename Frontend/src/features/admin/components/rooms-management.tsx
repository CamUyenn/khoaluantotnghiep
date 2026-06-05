"use client";

import { useEffect, useMemo, useState } from "react";
import { ChevronLeft, ChevronRight, Edit, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";

import { getApiErrorMessage } from "@/services/api";
import { adminService, type AdminRoom, type AdminRoomSchedule, type AdminUser } from "@/services/adminService";
import styles from "../admin.module.css";

type RoomModalMode = "create" | "edit";
type ScheduleModalMode = "create" | "edit";

const WEEKDAY_LABELS = ["T2", "T3", "T4", "T5", "T6", "T7", "CN"];

const toIsoDate = (date: Date) => date.toISOString().slice(0, 10);

const toMonthValue = (date: Date) => date.toISOString().slice(0, 7);

const getMonday = (date: Date) => {
  const monday = new Date(date);
  const offset = (monday.getDay() + 6) % 7;
  monday.setDate(monday.getDate() - offset);
  return monday;
};

export function RoomsManagement() {
  const [rooms, setRooms] = useState<AdminRoom[]>([]);
  const [doctors, setDoctors] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const [selectedWeekStart, setSelectedWeekStart] = useState(() => toIsoDate(getMonday(new Date())));
  const [selectedMonth, setSelectedMonth] = useState(() => toMonthValue(new Date()));
  const [schedules, setSchedules] = useState<AdminRoomSchedule[]>([]);
  const [scheduleLoading, setScheduleLoading] = useState(true);
  const [scheduleSubmitting, setScheduleSubmitting] = useState(false);
  const [selectedDay, setSelectedDay] = useState(() => {
    const today = toIsoDate(new Date());
    const monday = toIsoDate(getMonday(new Date()));
    return today >= monday && today <= toIsoDate(new Date(new Date(monday).setDate(new Date(monday).getDate() + 6)))
      ? today
      : monday;
  });

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<RoomModalMode>("create");
  const [selectedRoom, setSelectedRoom] = useState<AdminRoom | null>(null);
  const [roomName, setRoomName] = useState("");
  const [doctorFilterId, setDoctorFilterId] = useState<number | "">("");

  const [isScheduleModalOpen, setIsScheduleModalOpen] = useState(false);
  const [scheduleModalMode, setScheduleModalMode] = useState<ScheduleModalMode>("create");
  const [selectedSchedule, setSelectedSchedule] = useState<AdminRoomSchedule | null>(null);
  const [scheduleRoomId, setScheduleRoomId] = useState<number | "">("");
  const [scheduleDoctorId, setScheduleDoctorId] = useState<number | "">("");
  const [scheduleDate, setScheduleDate] = useState("");
  const [scheduleStartTime, setScheduleStartTime] = useState("");
  const [scheduleEndTime, setScheduleEndTime] = useState("");

  const loadData = async () => {
    try {
      setLoading(true);
      const [roomsData, usersData] = await Promise.all([adminService.getRooms(), adminService.getUsers()]);
      setRooms(roomsData);
      setDoctors(usersData.filter((user) => user.role === "DOCTOR" && user.isActive));
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể tải dữ liệu phòng khám"));
    } finally {
      setLoading(false);
    }
  };

  const loadSchedules = async (monthValue: string) => {
    try {
      setScheduleLoading(true);
      const data = await adminService.getRoomSchedules({ month: monthValue });
      setSchedules(data);
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể tải lịch phân công"));
    } finally {
      setScheduleLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, []);

  useEffect(() => {
    if (selectedMonth) {
      void loadSchedules(selectedMonth);
    }
  }, [selectedMonth]);

  useEffect(() => {
    setSelectedMonth(selectedWeekStart.slice(0, 7));
    const weekStart = new Date(selectedWeekStart);
    const weekEnd = new Date(weekStart);
    weekEnd.setDate(weekEnd.getDate() + 6);
    const today = new Date();
    const nextSelected = today >= weekStart && today <= weekEnd ? toIsoDate(today) : toIsoDate(weekStart);
    setSelectedDay(nextSelected);
  }, [selectedWeekStart]);

  const monthRange = useMemo(() => {
    if (!selectedMonth) {
      return null;
    }

    const [yearValue, monthValue] = selectedMonth.split("-");
    const year = Number(yearValue);
    const month = Number(monthValue);
    if (!year || !month) {
      return null;
    }

    const lastDay = new Date(year, month, 0).getDate();
    return {
      start: `${selectedMonth}-01`,
      end: `${selectedMonth}-${String(lastDay).padStart(2, "0")}`,
    };
  }, [selectedMonth]);

  const scheduleByRoomDate = useMemo(() => {
    const map = new Map<number, Map<string, AdminRoomSchedule[]>>();
    schedules.forEach((schedule) => {
      if (doctorFilterId && schedule.doctorId !== doctorFilterId) {
        return;
      }

      if (!map.has(schedule.roomId)) {
        map.set(schedule.roomId, new Map());
      }
      const dateMap = map.get(schedule.roomId);
      if (!dateMap?.has(schedule.scheduleDate)) {
        dateMap?.set(schedule.scheduleDate, []);
      }
      dateMap?.get(schedule.scheduleDate)?.push(schedule);
    });

    map.forEach((dateMap) => {
      dateMap.forEach((list) => {
        list.sort((left, right) => left.startTime.localeCompare(right.startTime));
      });
    });

    return map;
  }, [schedules, doctorFilterId]);

  const busyRoomIds = useMemo(() => {
    const ids = new Set<number>();
    rooms.forEach((room) => {
      const hasSchedule = scheduleByRoomDate.get(room.id)?.get(selectedDay)?.length;
      if (hasSchedule) {
        ids.add(room.id);
      }
    });
    return ids;
  }, [rooms, scheduleByRoomDate, selectedDay]);

  const weekDates = useMemo(() => {
    const start = new Date(selectedWeekStart);
    return Array.from({ length: 7 }, (_, index) => {
      const date = new Date(start);
      date.setDate(start.getDate() + index);
      return date;
    });
  }, [selectedWeekStart]);

  const weekLabel = useMemo(() => {
    const first = weekDates[0];
    const last = weekDates[6];
    if (!first || !last) {
      return "";
    }
    const firstLabel = first.toLocaleDateString("vi-VN", { day: "2-digit", month: "short" });
    const lastLabel = last.toLocaleDateString("vi-VN", { day: "2-digit", month: "short", year: "numeric" });
    return `${firstLabel} - ${lastLabel}`;
  }, [weekDates]);

  const openCreateModal = () => {
    setModalMode("create");
    setSelectedRoom(null);
    setRoomName("");
    setIsModalOpen(true);
  };

  const openEditModal = (room: AdminRoom) => {
    setModalMode("edit");
    setSelectedRoom(room);
    setRoomName(room.roomName);
    setIsModalOpen(true);
  };

  const openScheduleModal = (
    mode: ScheduleModalMode,
    schedule?: AdminRoomSchedule,
    preset?: { roomId?: number; date?: string },
  ) => {
    setScheduleModalMode(mode);
    setSelectedSchedule(schedule ?? null);

    if (schedule) {
      setScheduleRoomId(schedule.roomId);
      setScheduleDoctorId(schedule.doctorId);
      setScheduleDate(schedule.scheduleDate);
      setScheduleStartTime(schedule.startTime);
      setScheduleEndTime(schedule.endTime);
    } else {
      setScheduleRoomId(preset?.roomId ?? "");
      setScheduleDoctorId("");
      setScheduleDate(preset?.date ?? monthRange?.start ?? new Date().toISOString().slice(0, 10));
      setScheduleStartTime("08:00");
      setScheduleEndTime("09:00");
    }

    setIsScheduleModalOpen(true);
  };

  const closeModal = () => {
    if (!submitting) {
      setIsModalOpen(false);
    }
  };

  const closeScheduleModal = () => {
    if (!scheduleSubmitting) {
      setIsScheduleModalOpen(false);
    }
  };

  const handleSubmit = async () => {
    if (!roomName.trim()) {
      toast.error("Vui lòng nhập tên phòng");
      return;
    }

    try {
      setSubmitting(true);
      if (modalMode === "create") {
        await adminService.createRoom(roomName.trim());
        toast.success("Đã thêm phòng khám");
      } else if (selectedRoom) {
        await adminService.updateRoom(selectedRoom.id, roomName.trim());
        toast.success("Đã cập nhật phòng khám");
      }
      setIsModalOpen(false);
      await loadData();
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể lưu phòng khám"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleScheduleSubmit = async () => {
    if (!scheduleRoomId || !scheduleDoctorId) {
      toast.error("Vui lòng chọn phòng và bác sĩ");
      return;
    }

    if (!scheduleDate) {
      toast.error("Vui lòng chọn ngày");
      return;
    }

    if (!scheduleStartTime || !scheduleEndTime) {
      toast.error("Vui lòng chọn giờ bắt đầu và kết thúc");
      return;
    }

    if (scheduleEndTime <= scheduleStartTime) {
      toast.error("Giờ kết thúc phải sau giờ bắt đầu");
      return;
    }

    const payload = {
      roomId: Number(scheduleRoomId),
      doctorId: Number(scheduleDoctorId),
      scheduleDate,
      timeSlot: `${scheduleStartTime}-${scheduleEndTime}`,
    };

    try {
      setScheduleSubmitting(true);
      if (scheduleModalMode === "create") {
        await adminService.createRoomSchedule(payload);
        toast.success("Đã tạo lịch phân công");
      } else if (selectedSchedule) {
        await adminService.updateRoomSchedule(selectedSchedule.id, payload);
        toast.success("Đã cập nhật lịch phân công");
      }
      setIsScheduleModalOpen(false);
      if (selectedMonth) {
        await loadSchedules(selectedMonth);
      }
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể lưu lịch phân công"));
    } finally {
      setScheduleSubmitting(false);
    }
  };

  const handleDeleteSchedule = async (schedule: AdminRoomSchedule) => {
    const confirmed = window.confirm("Bạn có chắc muốn xóa lịch phân công này?");
    if (!confirmed) {
      return;
    }

    try {
      await adminService.deleteRoomSchedule(schedule.id);
      toast.success("Đã xóa lịch phân công");
      if (selectedMonth) {
        await loadSchedules(selectedMonth);
      }
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể xóa lịch phân công"));
    }
  };

  return (
    <main className={styles.mainArea}>
      <div className={styles.container}>
        <div className={styles.header}>
          <h1>Quản lý phòng khám</h1>
          <p>Quản lý phòng khám và phân công bác sĩ theo lịch tháng.</p>
        </div>

        <div className={styles.toolbarColumn}>
          <button type="button" className={styles.primaryButton} onClick={openCreateModal}>
            <Plus size={16} /> Thêm phòng khám
          </button>
          <div className={styles.scheduleFilters}>
            <div className={styles.scheduleControls}>
              <button
                type="button"
                className={styles.iconButton}
                onClick={() => {
                  const start = new Date(selectedWeekStart);
                  start.setDate(start.getDate() - 7);
                  setSelectedWeekStart(toIsoDate(start));
                }}
              >
                <ChevronLeft size={16} />
              </button>
              <span className={styles.weekLabel}>{weekLabel}</span>
              <button
                type="button"
                className={styles.iconButton}
                onClick={() => {
                  const start = new Date(selectedWeekStart);
                  start.setDate(start.getDate() + 7);
                  setSelectedWeekStart(toIsoDate(start));
                }}
              >
                <ChevronRight size={16} />
              </button>
              <input
                type="month"
                className={styles.selectInput}
                value={selectedMonth}
                onChange={(event) => {
                  const value = event.target.value;
                  setSelectedMonth(value);
                  setSelectedWeekStart(toIsoDate(getMonday(new Date(`${value}-01`))));
                }}
              />
              <select
                className={styles.selectInput}
                value={doctorFilterId}
                onChange={(event) => setDoctorFilterId(event.target.value === "" ? "" : Number(event.target.value))}
              >
                <option value="">Tất cả bác sĩ</option>
                {doctors.map((doctor) => (
                  <option key={doctor.id} value={doctor.id}>
                    {doctor.fullName || doctor.username}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>

        <div className={styles.roomScheduleLayout}>
          <aside className={styles.roomSidebar}>
            <div className={styles.sidebarHeader}>Phòng khám</div>
            <div className={styles.roomHeaderSpacer} />
            {loading ? (
              <div className={styles.emptyBox}>Đang tải dữ liệu...</div>
            ) : (
              <div className={styles.roomList}>
                {rooms.map((room) => {
                  const isBusy = busyRoomIds.has(room.id);
                  return (
                    <div key={room.id} className={styles.roomItem}>
                      <div className={styles.roomItemHeader}>
                        <span className={styles.roomName}>{room.roomName}</span>
                        <span className={`${styles.badge} ${isBusy ? styles.badgeRed : styles.badgeGreen}`}>
                          {isBusy ? "Đang sử dụng" : "Còn trống"}
                        </span>
                      </div>
                      <button type="button" className={styles.iconButton} onClick={() => openEditModal(room)}>
                        <Edit size={16} />
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </aside>

          <section className={styles.scheduleBoard}>
            <div className={styles.scheduleTitleBlock}>
              <h2 className={styles.itemTitle}>Lịch phân công bác sĩ</h2>
              <p className={styles.itemMeta}>Nhấn dấu + để thêm lịch cho từng phòng.</p>
            </div>

            {scheduleLoading ? (
              <div className={styles.emptyBox}>Đang tải lịch phân công...</div>
            ) : (
              <div className={styles.scheduleMatrix}>
                <div className={styles.scheduleHeaderRow}>
                  {weekDates.map((date, index) => (
                    <button
                      key={date.toISOString()}
                      type="button"
                      className={`${styles.scheduleColumnHeader} ${
                        selectedDay === toIsoDate(date) ? styles.scheduleColumnHeaderActive : ""
                      }`}
                      onClick={() => setSelectedDay(toIsoDate(date))}
                    >
                      <div className={styles.scheduleDayName}>{WEEKDAY_LABELS[index]}</div>
                      <div className={styles.scheduleDayNumber}>{String(date.getDate()).padStart(2, "0")}</div>
                    </button>
                  ))}
                </div>

                {rooms.map((room) => (
                  <div key={room.id} className={styles.scheduleRowGrid}>
                    {weekDates.map((date) => {
                      const dateValue = toIsoDate(date);
                      const items = scheduleByRoomDate.get(room.id)?.get(dateValue) ?? [];
                      return (
                        <div key={`${room.id}-${dateValue}`} className={styles.scheduleMatrixCell}>
                          {items.length === 0 ? (
                            <button
                              type="button"
                              className={styles.scheduleAddButton}
                              onClick={() =>
                                openScheduleModal("create", undefined, {
                                  roomId: room.id,
                                  date: dateValue,
                                })
                              }
                            >
                              +
                            </button>
                          ) : (
                            items.map((item) => (
                              <div key={item.id} className={styles.scheduleSlot}>
                                <div>
                                  <div className={styles.scheduleItemTime}>{item.timeSlot}</div>
                                  <div className={styles.scheduleItemMeta}>{item.doctorUsername}</div>
                                </div>
                                <div className={styles.scheduleItemActions}>
                                  <button
                                    type="button"
                                    className={styles.iconButton}
                                    onClick={() => openScheduleModal("edit", item)}
                                  >
                                    <Edit size={14} />
                                  </button>
                                  <button
                                    type="button"
                                    className={styles.iconButton}
                                    onClick={() => void handleDeleteSchedule(item)}
                                  >
                                    <Trash2 size={14} />
                                  </button>
                                </div>
                              </div>
                            ))
                          )}
                        </div>
                      );
                    })}
                  </div>
                ))}
              </div>
            )}
          </section>

          
        </div>
      </div>

      {isModalOpen && (
        <>
          <div className={styles.modalOverlay} onClick={closeModal} />
          <div className={styles.modal}>
            <h2 className={styles.modalTitle}>
              {modalMode === "create" && "Thêm phòng khám"}
              {modalMode === "edit" && "Chỉnh sửa phòng khám"}
            </h2>

            <div>
              <label className={styles.fieldLabel}>Tên phòng khám</label>
              <input
                className={styles.textInput}
                value={roomName}
                onChange={(event) => setRoomName(event.target.value)}
              />
            </div>

            <div className={styles.modalActions}>
              <button type="button" className={styles.primaryButton} onClick={() => void handleSubmit()} disabled={submitting}>
                {modalMode === "create" ? "Thêm mới" : "Lưu thay đổi"}
              </button>
              <button type="button" className={styles.outlineButton} onClick={closeModal} disabled={submitting}>
                Hủy
              </button>
            </div>
          </div>
        </>
      )}

      {isScheduleModalOpen && (
        <>
          <div className={styles.modalOverlay} onClick={closeScheduleModal} />
          <div className={styles.modal}>
            <h2 className={styles.modalTitle}>
              {scheduleModalMode === "create" ? "Thêm lịch phân công" : "Cập nhật lịch phân công"}
            </h2>

            <div>
              <label className={styles.fieldLabel}>Ngày</label>
              <input
                type="date"
                className={styles.textInput}
                value={scheduleDate}
                min={monthRange?.start}
                max={monthRange?.end}
                onChange={(event) => setScheduleDate(event.target.value)}
              />
            </div>

            <div className={styles.fieldGrid}>
              <div>
                <label className={styles.fieldLabel}>Giờ bắt đầu</label>
                <input
                  type="time"
                  className={styles.textInput}
                  value={scheduleStartTime}
                  onChange={(event) => setScheduleStartTime(event.target.value)}
                />
              </div>
              <div>
                <label className={styles.fieldLabel}>Giờ kết thúc</label>
                <input
                  type="time"
                  className={styles.textInput}
                  value={scheduleEndTime}
                  onChange={(event) => setScheduleEndTime(event.target.value)}
                />
              </div>
            </div>

            <div className={styles.fieldGrid}>
              <div>
                <label className={styles.fieldLabel}>Phòng khám</label>
                <select
                  className={styles.selectInput}
                  value={scheduleRoomId}
                  onChange={(event) => setScheduleRoomId(event.target.value === "" ? "" : Number(event.target.value))}
                >
                  <option value="">-- Chọn phòng --</option>
                  {rooms.map((room) => (
                    <option key={room.id} value={room.id}>
                      {room.roomName}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className={styles.fieldLabel}>Bác sĩ</label>
                <select
                  className={styles.selectInput}
                  value={scheduleDoctorId}
                  onChange={(event) => setScheduleDoctorId(event.target.value === "" ? "" : Number(event.target.value))}
                >
                  <option value="">-- Chọn bác sĩ --</option>
                  {doctors.map((doctor) => (
                    <option key={doctor.id} value={doctor.id}>
                      {doctor.username}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className={styles.modalActions}>
              <button
                type="button"
                className={styles.primaryButton}
                onClick={() => void handleScheduleSubmit()}
                disabled={scheduleSubmitting}
              >
                {scheduleModalMode === "create" ? "Tạo lịch" : "Lưu thay đổi"}
              </button>
              <button type="button" className={styles.outlineButton} onClick={closeScheduleModal} disabled={scheduleSubmitting}>
                Hủy
              </button>
            </div>
          </div>
        </>
      )}
    </main>
  );
}
