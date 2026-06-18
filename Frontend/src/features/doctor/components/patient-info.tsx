import type { Patient } from "@/types/doctor.type";
import styles from "@/styles/common.module.css";

interface PatientInfoProps {
  patient: Patient;
  symptoms: string;
  categoryName?: string | null;
}

export function PatientInfo({ patient, symptoms, categoryName }: PatientInfoProps) {
  const genderLabel =
    patient.gender === "male" ? "Nam" : patient.gender === "female" ? "Nữ" : "Chưa cập nhật";

  return (
    <div className={styles.panelMuted}>
      <h3 className={styles.titleSmall}>
        Thông tin bệnh nhân
      </h3>
      <div className={styles.grid2}>
        <p className={styles.textSmall}>
          <span className={styles.textMuted}>Họ tên:</span> {patient.full_name}
        </p>
        <p className={styles.textSmall}>
          <span className={styles.textMuted}>Ngày sinh:</span>{" "}
          {patient.date_of_birth ? new Date(patient.date_of_birth).toLocaleDateString("vi-VN") : "Chưa cập nhật"}
        </p>
        <p className={styles.textSmall}>
          <span className={styles.textMuted}>SĐT:</span> {patient.phone_number || "Chưa cập nhật"}
        </p>
        <p className={styles.textSmall}>
          <span className={styles.textMuted}>Quê quán:</span> {patient.hometown || "Chưa cập nhật"}
        </p>
        <p className={styles.textSmall}>
          <span className={styles.textMuted}>CMND:</span> {patient.national_id || "Chưa cập nhật"}
        </p>
        <p className={styles.textSmall}>
          <span className={styles.textMuted}>Giới tính:</span> {genderLabel}
        </p>
        <p className={styles.textSmall}>
          <span className={styles.textMuted}>Chuyên khoa:</span> {categoryName || "Chưa cập nhật"}
        </p>
        {!!patient.insurance_number && (
          <p className={styles.textSmall}>
            <span className={styles.textMuted}>BHYT:</span> {patient.insurance_number}
          </p>
        )}
      </div>
      <p className={styles.panelWarning}>
        <span className={styles.textMedium}>Triệu chứng:</span> {symptoms}
      </p>
    </div>
  );
}