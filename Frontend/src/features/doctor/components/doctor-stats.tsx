import { CheckCircle, Clock, Stethoscope } from "lucide-react";
import styles from "@/styles/common.module.css";

interface DoctorStatsProps {
  waitingCount: number;
  examiningCount: number;
  completedCount: number;
}

export function DoctorStats({ waitingCount, examiningCount, completedCount }: DoctorStatsProps) {
  return (
    <div className={styles.statsGrid}>
      <div className={styles.statCard}>
        <div className={styles.statIconOrange}>
          <Clock size={20} />
        </div>
        <div className={styles.statContent}>
          <h3>Đang chờ khám</h3>
          <p className={styles.statValue}>{waitingCount}</p>
        </div>
      </div>
      <div className={styles.statCard}>
        <div className={styles.statIconBlue}>
          <Stethoscope size={20} />
        </div>
        <div className={styles.statContent}>
          <h3>Đang khám</h3>
          <p className={styles.statValue}>{examiningCount}</p>
        </div>
      </div>
      <div className={styles.statCard}>
        <div className={styles.statIconGreen}>
          <CheckCircle size={20} />
        </div>
        <div className={styles.statContent}>
          <h3>Đã hoàn thành</h3>
          <p className={styles.statValue}>{completedCount}</p>
        </div>
      </div>
    </div>
  );
}