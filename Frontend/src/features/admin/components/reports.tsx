"use client";

import { useEffect, useMemo, useState } from "react";
import {
  Activity,
  Calendar,
  CalendarDays,
  CalendarRange,
  DollarSign,
  Package,
  ShoppingBag,
  Stethoscope,
  Users,
} from "lucide-react";
import { toast } from "sonner";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import { getApiErrorMessage } from "@/services/api";
import { adminService, type AdminDashboardData, type AdminRevenueChartPoint, type AdminRevenueReportResponse } from "@/services/adminService";
import styles from "../admin.module.css";

const emptyDashboard: AdminDashboardData = {
  totalPatients: 0,
  totalDoctors: 0,
  totalAppointments: 0,
  totalMedicalRecords: 0,
  todayAppointments: 0,
  pendingAppointments: 0,
  confirmedAppointments: 0,
  cancelledAppointments: 0,
  thisMonthAppointments: 0,
};

const formatCurrency = (amount: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(amount);

const toDateInputValue = (value: Date) => {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const toStartDateTime = (value: string) => `${value}T00:00:00`;
const toEndDateTime = (value: string) => `${value}T23:59:59`;

const groupByOptions = [
  { value: "DAY", label: "Theo ngày", icon: CalendarDays },
  { value: "MONTH", label: "Theo tháng", icon: CalendarRange },
  { value: "YEAR", label: "Theo năm", icon: Calendar },
] as const;

type RevenueGroupBy = (typeof groupByOptions)[number]["value"];

type RevenueFilterState = {
  startDate: string;
  endDate: string;
  groupBy: RevenueGroupBy;
};

export function AdminDashboard() {
  const [dashboard, setDashboard] = useState<AdminDashboardData>(emptyDashboard);
  const [revenueReport, setRevenueReport] = useState<AdminRevenueReportResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [startDate, setStartDate] = useState(() => {
    const value = new Date();
    value.setMonth(value.getMonth() - 11);
    return toDateInputValue(value);
  });
  const [endDate, setEndDate] = useState(() => toDateInputValue(new Date()));
  const [groupBy, setGroupBy] = useState<RevenueGroupBy>("MONTH");
  const [appliedRange, setAppliedRange] = useState<RevenueFilterState>({
    startDate,
    endDate,
    groupBy: "MONTH",
  });

  const loadDashboard = async (range?: RevenueFilterState) => {
    try {
      setLoading(true);
      const filter = range ?? appliedRange;
      const [data, report] = await Promise.all([
        adminService.getDashboard(),
        adminService.getRevenueReport({
          startTime: toStartDateTime(filter.startDate),
          endTime: toEndDateTime(filter.endDate),
          groupBy: filter.groupBy,
        }),
      ]);
      setDashboard(data);
      setRevenueReport(report);
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể tải dashboard quản trị"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadDashboard(appliedRange);
  }, []);

  const revenueChart = revenueReport?.chart ?? [];

  const monthlyComparison = useMemo(() => {
    if (revenueChart.length === 0) {
      return null;
    }

    const sorted = [...revenueChart];
    const best = sorted.reduce<AdminRevenueChartPoint | null>((currentBest, item) => {
      if (!currentBest || item.revenue > currentBest.revenue) {
        return item;
      }
      return currentBest;
    }, null);

    const worst = sorted.reduce<AdminRevenueChartPoint | null>((currentWorst, item) => {
      if (!currentWorst || item.revenue < currentWorst.revenue) {
        return item;
      }
      return currentWorst;
    }, null);

    const first = sorted[0];
    const last = sorted[sorted.length - 1];
    const growthPercent = first && last && first.revenue > 0 ? ((last.revenue - first.revenue) / first.revenue) * 100 : 0;

    return { best, worst, growthPercent };
  }, [revenueChart]);

  const handleApplyRevenueFilter = () => {
    const nextRange = { startDate, endDate, groupBy };
    setAppliedRange(nextRange);
    void loadDashboard(nextRange);
  };

  const handleResetRevenueFilter = () => {
    const nextEnd = toDateInputValue(new Date());
    const nextStart = (() => {
      const value = new Date();
      value.setMonth(value.getMonth() - 11);
      return toDateInputValue(value);
    })();

    setStartDate(nextStart);
    setEndDate(nextEnd);
    setGroupBy("MONTH");
    const nextRange = { startDate: nextStart, endDate: nextEnd, groupBy: "MONTH" as const };
    setAppliedRange(nextRange);
    void loadDashboard(nextRange);
  };

  return (
    <main className={styles.mainArea}>
      <div className={styles.container}>
        <div className={styles.header}>
          <h1>Bảng điều khiển quản trị</h1>
          <p>Tổng quan hệ thống phòng khám và các chỉ số vận hành.</p>
        </div>

        {loading ? (
          <section className={styles.card}>
            <div className={styles.emptyBox}>Đang tải dữ liệu...</div>
          </section>
        ) : (
          <>
            <div className={styles.statsGrid}>
              <section className={styles.statCard}>
                <div className={`${styles.statIcon} ${styles.statBlue}`}>
                  <Users size={20} />
                </div>
                <div>
                  <p className={styles.statLabel}>Tổng bệnh nhân</p>
                  <p className={styles.statValue}>{dashboard.totalPatients.toLocaleString("vi-VN")}</p>
                </div>
              </section>

              <section className={styles.statCard}>
                <div className={`${styles.statIcon} ${styles.statGreen}`}>
                  <Calendar size={20} />
                </div>
                <div>
                  <p className={styles.statLabel}>Lượt khám tháng này</p>
                  <p className={styles.statValue}>{dashboard.thisMonthAppointments.toLocaleString("vi-VN")}</p>
                </div>
              </section>

              <section className={styles.statCard}>
                <div className={`${styles.statIcon} ${styles.statOrange}`}>
                  <Stethoscope size={20} />
                </div>
                <div>
                  <p className={styles.statLabel}>Bác sĩ</p>
                  <p className={styles.statValue}>{dashboard.totalDoctors.toLocaleString("vi-VN")}</p>
                </div>
              </section>

              <section className={styles.statCard}>
                <div className={`${styles.statIcon} ${styles.statRed}`}>
                  <DollarSign size={20} />
                </div>
                <div>
                  <p className={styles.statLabel}>Doanh thu theo bộ lọc</p>
                  <p className={styles.statValue}>{formatCurrency(revenueReport?.totalRevenue ?? 0)}</p>
                </div>
              </section>
            </div>

            <section className={styles.cardSection}>
              <div className={styles.sectionHeader}>
                <div>
                  <h3 className={styles.sectionTitle}>Biểu đồ doanh thu</h3>
                  <p className={styles.sectionSubtitle}>So sánh doanh thu theo ngày, tháng hoặc năm trong khoảng thời gian bạn chọn.</p>
                </div>
                <div className={styles.filterChips}>
                  {groupByOptions.map(({ value, label, icon: Icon }) => (
                    <button
                      key={value}
                      type="button"
                      className={`${styles.filterChip} ${groupBy === value ? styles.filterChipActive : ""}`}
                      onClick={() => setGroupBy(value)}
                    >
                      <Icon size={15} />
                      {label}
                    </button>
                  ))}
                </div>
              </div>

              <div className={styles.toolbarRow}>
                <div className={styles.fieldGroup}>
                  <label className={styles.fieldLabel} htmlFor="admin-revenue-start">Từ ngày</label>
                  <input
                    id="admin-revenue-start"
                    type="date"
                    className={styles.textInput}
                    value={startDate}
                    onChange={(event) => setStartDate(event.target.value)}
                  />
                </div>
                <div className={styles.fieldGroup}>
                  <label className={styles.fieldLabel} htmlFor="admin-revenue-end">Đến ngày</label>
                  <input
                    id="admin-revenue-end"
                    type="date"
                    className={styles.textInput}
                    value={endDate}
                    onChange={(event) => setEndDate(event.target.value)}
                  />
                </div>
                <div className={styles.filterActions}>
                  <button type="button" className={styles.primaryButton} onClick={handleApplyRevenueFilter}>
                    Áp dụng
                  </button>
                  <button type="button" className={styles.outlineButton} onClick={handleResetRevenueFilter}>
                    12 tháng gần nhất
                  </button>
                </div>
              </div>

              <div className={styles.chartCard}>
                <ResponsiveContainer width="100%" height={360}>
                  <BarChart data={revenueChart} margin={{ top: 12, right: 12, left: 0, bottom: 6 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                    <XAxis dataKey="period" tick={{ fill: "#475569", fontSize: 12 }} />
                    <YAxis tick={{ fill: "#475569", fontSize: 12 }} tickFormatter={(value) => `${Math.round(Number(value) / 1000000)}M`} />
                    <Tooltip
                      formatter={(value) => [formatCurrency(Number(value ?? 0)), "Doanh thu"]}
                      labelFormatter={(label) => `Kỳ: ${label}`}
                    />
                    <Legend />
                    <Bar dataKey="revenue" fill="#2563eb" name="Doanh thu" radius={[10, 10, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>

              <div className={styles.comparisonGrid}>
                <div className={styles.comparisonCard}>
                  <p className={styles.comparisonLabel}>Kỳ doanh thu cao nhất</p>
                  <p className={styles.comparisonValue}>{monthlyComparison?.best?.period ?? "--"}</p>
                  <p className={styles.comparisonHint}>{formatCurrency(monthlyComparison?.best?.revenue ?? 0)}</p>
                </div>
                <div className={styles.comparisonCard}>
                  <p className={styles.comparisonLabel}>Kỳ doanh thu thấp nhất</p>
                  <p className={styles.comparisonValue}>{monthlyComparison?.worst?.period ?? "--"}</p>
                  <p className={styles.comparisonHint}>{formatCurrency(monthlyComparison?.worst?.revenue ?? 0)}</p>
                </div>
                <div className={styles.comparisonCard}>
                  <p className={styles.comparisonLabel}>Xu hướng đầu - cuối kỳ</p>
                  <p className={styles.comparisonValue}>
                    {monthlyComparison ? `${monthlyComparison.growthPercent >= 0 ? "+" : ""}${monthlyComparison.growthPercent.toFixed(1)}%` : "--"}
                  </p>
                  <p className={styles.comparisonHint}>So sánh kỳ đầu và kỳ cuối trong bộ lọc hiện tại</p>
                </div>
              </div>
            </section>

            <div className={styles.cardsGrid}>
              <section className={styles.itemCard}>
                <div className={styles.itemHeader}>
                  <h3 className={styles.itemTitle}>Tình trạng lịch hẹn</h3>
                  <Activity size={20} color="#16a34a" />
                </div>
                <div className={styles.barGroup}>
                  <div className={styles.barRow}>
                    <span className={styles.itemMeta}>Đang chờ</span>
                    <div className={styles.progress}>
                      <div className={styles.progressFill} style={{ width: `${dashboard.pendingAppointments || 0}%` }} />
                    </div>
                    <span className={styles.itemMeta}>{dashboard.pendingAppointments}</span>
                  </div>
                  <div className={styles.barRow}>
                    <span className={styles.itemMeta}>Đã xác nhận</span>
                    <div className={styles.progress}>
                      <div className={styles.progressFill} style={{ width: `${dashboard.confirmedAppointments || 0}%` }} />
                    </div>
                    <span className={styles.itemMeta}>{dashboard.confirmedAppointments}</span>
                  </div>
                  <div className={styles.barRow}>
                    <span className={styles.itemMeta}>Đã hủy</span>
                    <div className={styles.progress}>
                      <div className={styles.progressFill} style={{ width: `${dashboard.cancelledAppointments || 0}%` }} />
                    </div>
                    <span className={styles.itemMeta}>{dashboard.cancelledAppointments}</span>
                  </div>
                </div>
              </section>

              <section className={styles.itemCard}>
                <h3 className={styles.itemTitle}>Số liệu hồ sơ</h3>
                <div className={styles.barGroup}>
                  <div className={styles.barRow}>
                    <span className={styles.itemMeta}>Lịch hẹn hôm nay</span>
                    <div className={styles.progress}>
                      <div className={styles.progressFill} style={{ width: `${dashboard.todayAppointments || 0}%` }} />
                    </div>
                    <span className={styles.itemMeta}>{dashboard.todayAppointments}</span>
                  </div>
                  <div className={styles.barRow}>
                    <span className={styles.itemMeta}>Tổng lịch hẹn</span>
                    <div className={styles.progress}>
                      <div className={styles.progressFill} style={{ width: "100%" }} />
                    </div>
                    <span className={styles.itemMeta}>{dashboard.totalAppointments}</span>
                  </div>
                  <div className={styles.barRow}>
                    <span className={styles.itemMeta}>Hồ sơ bệnh án</span>
                    <div className={styles.progress}>
                      <div className={styles.progressFill} style={{ width: "100%" }} />
                    </div>
                    <span className={styles.itemMeta}>{dashboard.totalMedicalRecords}</span>
                  </div>
                </div>
              </section>
            </div>

            <div className={styles.cardsGrid}>
              <section className={styles.itemCard}>
                <div className={styles.itemHeader}>
                  <div className={`${styles.statIcon} ${styles.statBlue}`}>
                    <Activity size={20} />
                  </div>
                </div>
                <p className={styles.itemMeta}>Lịch hẹn đang chờ</p>
                <p className={styles.statValue}>{dashboard.pendingAppointments}</p>
              </section>

              <section className={styles.itemCard}>
                <div className={styles.itemHeader}>
                  <div className={`${styles.statIcon} ${styles.statGreen}`}>
                    <Package size={20} />
                  </div>
                </div>
                <p className={styles.itemMeta}>Hồ sơ bệnh án</p>
                <p className={styles.statValue}>{dashboard.totalMedicalRecords}</p>
              </section>

              <section className={styles.itemCard}>
                <div className={styles.itemHeader}>
                  <div className={`${styles.statIcon} ${styles.statOrange}`}>
                    <ShoppingBag size={20} />
                  </div>
                </div>
                <p className={styles.itemMeta}>Lịch hẹn hôm nay</p>
                <p className={styles.statValue}>{dashboard.todayAppointments}</p>
              </section>
            </div>
          </>
        )}
      </div>
    </main>
  );
}
