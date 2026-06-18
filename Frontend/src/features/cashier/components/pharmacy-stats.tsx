import { Card } from "@/components/ui/card";
import { Clock, CheckCircle, DollarSign } from "lucide-react";
import styles from "../cashier.module.css";

interface PharmacyStatsProps {
  pendingCount: number;
  dispensedCount: number;
  totalRevenue: number;
}

function PharmacyStats({ pendingCount, dispensedCount, totalRevenue }: PharmacyStatsProps) {
  return (
    <div className={styles.statsGrid}>
      <Card className={styles.statCard}>
        <div className={styles.statInner}>
          <div className={`${styles.statIcon} ${styles.statIconOrange}`}>
            <Clock size={24} />
          </div>
          <div>
            <p className={styles.statLabel}>Chờ thanh toán</p>
            <p className={styles.statValue}>{pendingCount}</p>
          </div>
        </div>
      </Card>

      <Card className={styles.statCard}>
        <div className={styles.statInner}>
          <div className={`${styles.statIcon} ${styles.statIconGreen}`}>
            <CheckCircle size={24} />
          </div>
          <div>
            <p className={styles.statLabel}>Đã thanh toán</p>
            <p className={styles.statValue}>{dispensedCount}</p>
          </div>
        </div>
      </Card>

      <Card className={styles.statCard}>
        <div className={styles.statInner}>
          <div className={`${styles.statIcon} ${styles.statIconBlue}`}>
            <DollarSign size={24} />
          </div>
          <div>
            <p className={styles.statLabel}>Doanh thu</p>
            <p className={styles.statValue}>
              {totalRevenue.toLocaleString("vi-VN")}đ
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
}

export { PharmacyStats };
export default PharmacyStats;