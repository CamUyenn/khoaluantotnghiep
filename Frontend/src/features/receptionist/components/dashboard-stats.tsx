import styles from "@/styles/common.module.css";

interface DashboardStatsProps {
  pendingCount: number;
  confirmedCount: number;
  totalCount: number;
}

export function DashboardStats({ pendingCount, confirmedCount, totalCount }: DashboardStatsProps) {
  return (
    <div className={styles.statsGrid}>
      <div className={styles.statCard}>
        <div className={styles.iconWrapper}>
          <div className={`${styles.icon} ${styles.orange}`}>
            <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div className={styles.content}>
            <h3>Chờ xác nhận</h3>
            <div className={styles.value}>{pendingCount}</div>
          </div>
        </div>
      </div>

      <div className={styles.statCard}>
        <div className={styles.iconWrapper}>
          <div className={`${styles.icon} ${styles.blue}`}>
            <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div className={styles.content}>
            <h3>Đã xác nhận</h3>
            <div className={styles.value}>{confirmedCount}</div>
          </div>
        </div>
      </div>

      <div className={styles.statCard}>
        <div className={styles.iconWrapper}>
          <div className={`${styles.icon} ${styles.green}`}>
            <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
          </div>
          <div className={styles.content}>
            <h3>Tổng lịch hẹn</h3>
            <div className={styles.value}>{totalCount}</div>
          </div>
        </div>
      </div>
    </div>
  );
}