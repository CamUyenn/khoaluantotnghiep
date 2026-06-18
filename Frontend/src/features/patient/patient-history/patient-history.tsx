'use client';

import React, { useEffect, useMemo, useState } from "react";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { getApiErrorMessage } from "@/services/api";
import { PaymentReferenceCard } from "@/features/patient/components/payment-reference-card";
import {
  patientService,
  type PatientMedicalRecordDetailResponse,
  type PatientMedicalRecordHistoryItemResponse,
  type PatientProfileResponse,
} from "@/services/patientService";
import { Activity, Calendar, FileText, Loader2, Pill, Stethoscope } from "lucide-react";
import styles from "./patient-history.module.css";
import { toast } from "sonner";

const formatDateTime = (value: string | null | undefined) => {
  if (!value) {
    return "Chưa cập nhật";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
};

const createInitials = (fullName: string | null | undefined) => {
  if (!fullName) {
    return "BN";
  }

  const parts = fullName.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return "BN";
  }

  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase();
  }

  return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
};

export function PatientHistory() {
  const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState<PatientProfileResponse | null>(null);
  const [records, setRecords] = useState<PatientMedicalRecordHistoryItemResponse[]>([]);
  const [recordDetails, setRecordDetails] = useState<Record<number, PatientMedicalRecordDetailResponse | null>>({});

  useEffect(() => {
    let isMounted = true;

    const loadHistory = async () => {
      try {
        setLoading(true);
        const [profileData, historyData] = await Promise.all([
          patientService.getProfile(),
          patientService.getMedicalRecordHistory(),
        ]);

        const sortedRecords = [...historyData].sort(
          (a, b) => new Date(b.createdAt ?? 0).getTime() - new Date(a.createdAt ?? 0).getTime()
        );

        const details = await Promise.all(
          sortedRecords.map((record) =>
            patientService.getMedicalRecordDetail(record.medicalRecordId).catch(() => null)
          )
        );

        if (!isMounted) {
          return;
        }

        const detailMap = sortedRecords.reduce<Record<number, PatientMedicalRecordDetailResponse | null>>(
          (map, record, index) => {
            map[record.medicalRecordId] = details[index];
            return map;
          },
          {}
        );

        setProfile(profileData);
        setRecords(sortedRecords);
        setRecordDetails(detailMap);
      } catch (error) {
        if (!isMounted) {
          return;
        }

        toast.error(getApiErrorMessage(error, "Không thể tải lịch sử khám bệnh"));
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    void loadHistory();

    return () => {
      isMounted = false;
    };
  }, []);

  const patientDisplayName = profile?.fullName || profile?.gmail || "Bệnh nhân";
  const patientAvatar = useMemo(() => createInitials(patientDisplayName), [patientDisplayName]);

  const getCostBreakdown = (
    record: PatientMedicalRecordHistoryItemResponse,
    detail: PatientMedicalRecordDetailResponse | null
  ) => {
    const examinationCost = Number(detail?.totalServiceFee ?? record.totalServiceFee ?? 0);
    const totalCost = examinationCost;

    return {
      examinationCost,
      totalCost,
    };
  };

  return (
    <div className={styles.container}>
      
      {/* Header */}
      <div className={styles.header}>
        <h1 className={styles.title}>Lịch sử khám bệnh</h1>
        <p className={styles.subtitle}>Xem lại hồ sơ bệnh án, dịch vụ đã sử dụng và đơn thuốc</p>
      </div>

      {/* Thông tin bệnh nhân */}
      <Card className={styles.patientCard}>
        <div className={styles.patientFlex}>
          <div className={styles.avatar}>{patientAvatar}</div>
          <div className={styles.patientInfo}>
            <h3 className={styles.patientName}>Thông tin bệnh nhân</h3>
            <div className={styles.infoGrid}>
              <div>
                <span className={styles.infoLabel}>Họ tên:</span>
                <span className={styles.infoValue}>{patientDisplayName}</span>
              </div>
              <div>
                <span className={styles.infoLabel}>SĐT:</span>
                <span className={styles.infoValue}>{profile?.phoneNumber || "Chưa cập nhật"}</span>
              </div>
              <div>
                <span className={styles.infoLabel}>Email:</span>
                <span className={styles.infoValue}>{profile?.gmail || "Chưa cập nhật"}</span>
              </div>
              <div>
                <span className={styles.infoLabel}>Mã BHYT:</span>
                <span className={styles.infoValue}>{profile?.healthInsuranceNumber || "Chưa cập nhật"}</span>
              </div>
            </div>
          </div>
        </div>
      </Card>

      {/* Danh sách Hồ sơ bệnh án */}
      <div>
        <h2 className={styles.sectionTitle}>Hồ sơ khám bệnh ({records.length})</h2>

        {loading && (
          <Card className={styles.emptyState}>
            <Loader2 size={24} className="animate-spin" />
            <p>Đang tải hồ sơ khám bệnh...</p>
          </Card>
        )}

        {!loading && records.map((record) => {
          const detail = recordDetails[record.medicalRecordId] ?? null;
          const { examinationCost, totalCost } = getCostBreakdown(record, detail);
          const serviceItems = detail?.services ?? [];
          const paymentReference = detail?.paymentReference ?? record.paymentReference ?? null;

          return (
          <Card key={record.medicalRecordId} className={styles.recordCard}>
            
            {/* Record Header */}
            <div className={styles.recordHeader}>
              <div className={styles.recordLeft}>
                <div className={styles.recordIcon}>
                  <FileText size={24} />
                </div>
                <div>
                  <div className={styles.badgeGroup}>
                    <Badge variant="secondary" className={styles.customBadge}>
                      <Calendar size={12} style={{ marginRight: '4px' }} />
                      {formatDateTime(record.appointmentTime)}
                    </Badge>
                    <Badge variant="secondary" className={styles.customBadge}>
                      <Stethoscope size={12} style={{ marginRight: '4px' }} />
                      {detail?.doctorUsername || record.doctorUsername || "Chưa cập nhật"}
                    </Badge>
                  </div>
                  <p className={styles.recordId}>Mã hồ sơ: #{record.medicalRecordId}</p>
                </div>
              </div>
              <div className={styles.costRight}>
                <p className={styles.costLabel}>Tổng tiền khám</p>
                <p className={styles.costValue}>
                  {totalCost.toLocaleString("vi-VN")}đ
                </p>
                <div className={styles.costBreakdown}>
                  <p>
                    Chi phí khám: <strong>{examinationCost.toLocaleString("vi-VN")}đ</strong>
                  </p>
                </div>
              </div>
            </div>

            {record.paid && (
              <PaymentReferenceCard
                paymentReference={paymentReference}
                amount={totalCost}
                title="Mã chuyển khoản của hóa đơn"
                subtitle="Bệnh nhân có thể dùng mã này để đối soát hoặc mở lại QR khi cần."
                paidAt={record.paidAt ? formatDateTime(record.paidAt) : undefined}
              />
            )}

            {/* Record Details */}
            <div className={styles.detailsContainer}>
              
              <div className={`${styles.block} ${styles.blockBlue}`}>
                <p className={styles.blockTitleBlue}>Chẩn đoán:</p>
                <p className={styles.blockTextBlue}>{detail?.diagnosis || record.diagnosis || "Chưa cập nhật"}</p>
              </div>

              <div className={`${styles.block} ${styles.blockGreen}`}>
                <p className={styles.blockTitleGreen}>Lời khuyên / Điều trị:</p>
                <p className={styles.blockTextGreen}>{detail?.doctorAdvice || record.doctorAdvice || "Chưa cập nhật"}</p>
              </div>

              {serviceItems.length > 0 && (
                <div className={`${styles.block} ${styles.blockBlue}`} style={{ backgroundColor: '#ecfeff' }}>
                  <div className={styles.blockTitleBlue} style={{ color: '#0f766e' }}>
                    <Activity size={16} /> Dịch vụ đã sử dụng:
                  </div>
                  <ul className={styles.medList} style={{ color: '#0f766e' }}>
                    {serviceItems.map((item, idx) => (
                      <li key={item.serviceId ?? idx}>
                        <span style={{ fontWeight: 500 }}>{item.serviceName || "Dịch vụ"}</span>
                        {' - Số lượng: '}{item.quantity ?? 0}
                        {' - Thành tiền: '}{((item.actualPrice || 0) * (item.quantity ?? 0)).toLocaleString("vi-VN")}đ
                        {item.resultNote ? ` - Ghi chú: ${item.resultNote}` : ""}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {record.createdAt && (
                <div className={`${styles.block} ${styles.blockBlue}`} style={{ backgroundColor: '#f0f9ff' }}>
                  <div className={styles.blockTitleBlue} style={{ color: '#0369a1' }}>
                    <Activity size={16} /> Thời gian hoàn thành khám:
                  </div>
                  <ul className={styles.medList} style={{ color: '#0ea5e9' }}>
                    <li>{formatDateTime(record.createdAt)}</li>
                  </ul>
                </div>
              )}

              {/* Danh sách Thuốc */}
              {detail?.prescriptionItems && detail.prescriptionItems.length > 0 && (
                <div className={`${styles.block} ${styles.blockPurple}`}>
                  <div className={styles.blockTitlePurple}>
                    <Pill size={16} /> Đơn thuốc:
                  </div>
                  <ul className={styles.medList}>
                    {detail.prescriptionItems.map((item, idx) => (
                      <li key={idx}>
                        <span style={{ fontWeight: 500 }}>{item.medicineName || "Thuốc"}</span>
                        {' - Số lượng: '}{item.quantity ?? 0}
                        {' - Cách dùng: '}{item.usageInstructions || "Chưa cập nhật"}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

            </div>
          </Card>
        );
        })}

        {/* Empty State */}
        {!loading && records.length === 0 && (
          <Card className={styles.emptyState}>
            <FileText size={48} color="#d1d5db" />
            <p>Chưa có hồ sơ khám bệnh</p>
          </Card>
        )}
        
      </div>
    </div>
  );
}